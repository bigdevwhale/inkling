"""Generates creature sprite sheets and tower art with the Codex CLI image tool.

Codex saves images into ~/.codex/generated_images/<session>/; the image of the session named in
the run's output is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py               # everything missing
    python tools/gen_art.py inkling owl   # only these
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, cozy candlelit palette of deep indigo, plum, warm gold and ember orange, cute and round, big shiny eyes."
)
# objects are plain things: the generator likes to give everything a cute face, so it is told twice not to
OBJ_STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, cozy candlelit palette of deep indigo, plum, warm gold and ember orange. These are plain inanimate objects: "
    "NO faces, NO eyes, NO mouths, NO blush, nothing alive about them. No letters or text anywhere."
)
GRID = (
    "Layout: 3 columns x 2 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. "
    "One creature per cell, centered, with clear margin; nothing touches the cell borders. No text, no letters, no grid lines, "
    "no shadows on the background, no other objects except those listed."
)

# id: (look, signature move)
PETS = {
    "inkling": ("a small round ink-blot spirit made of glossy deep-indigo ink, a shiny drip on its head, a tiny white quill feather tucked behind it", "writing on a little parchment scroll with a quill dipped into its own ink"),
    "sphinx": ("a tiny winged sphinx kitten with golden sand-coloured fur and a small striped blue-gold headdress", "holding up a small rolled parchment scroll with a glowing seal, posing a riddle"),
    "salamander": ("a fire salamander, glossy black with glowing ember-orange spots", "curled around a lit candle, reading a tiny open book by its flame, very focused"),
    "golem": ("a small round stone golem with moss on its shoulders and one glowing blue rune on its chest", "carrying a tall stack of books steadily in its arms"),
    "pixie": ("a tiny pixie with dragonfly wings, a leaf hat and a sparkle trail", "flying while carrying a small golden hourglass"),
    "chimera": ("a cute chimera cub: a lion cub with little goat horns and a friendly green snake for a tail", "juggling three small glowing books of different colours"),
    "hydra": ("a little three-headed teal water hydra with fin crests", "each of its three heads reading a different tiny book"),
    "moonhare": ("a silver-white hare with a crescent moon mark on its forehead and starry ear tips", "reading a book by the light of a small moon lantern"),
    "griffin": ("a golden griffin chick: eagle head and wings, lion cub body", "stretching its wings at sunrise, holding a tiny steaming cup"),
    "kitsune": ("a white fox with several fluffy tails tipped with soft blue foxfire", "floating a small blue foxfire flame above an open book"),
    "mimic": ("a small wooden treasure-chest mimic with brass corners, a playful tongue and little stubby legs", "snapping up a glowing light-blue crystal orb"),
    "bookwyrm": ("a small green bookworm dragon, long and noodle-like, with tiny round spectacles", "coiled around a stack of books, turning a page with its tail"),
    "raven": ("a glossy black raven with a small white scholar's collar", "lecturing with one wing raised, little musical sound sparkles around its beak"),
    "mandrake": ("a cute mandrake root creature with a plump beige root body and a tuft of green leaves on its head", "watering its own leaves with a tiny copper watering can"),
    "kraken": ("a baby purple kraken with big eyes", "each of its tentacles holding something different: a quill, a book, a compass, a potion flask"),
    "shade": ("a little friendly shadow spirit, a wisp of soft dark smoke with two glowing golden eyes", "rising out of a curl of smoke holding up a freshly lit candle"),
    "owl": ("a little tawny messenger owl with a small leather satchel strap", "flying in with a sealed envelope with a red wax seal in its talons"),
    "gargoyle": ("a little grey stone gargoyle with small bat wings and stubby horns", "perched alert on a stone pedestal, holding a small brass bell"),
    "hob": ("a tiny house spirit (brownie) with a big fluffy beard, a red knitted cap and bare feet", "packing books into a small leather school satchel"),
    "magpie": ("a black and white magpie with a glossy blue-green tail and a tiny gold star pin", "holding up a shiny golden star"),
    "cat": ("a fluffy black cat familiar with a tiny pointed purple wizard hat with stars", "stretching lazily on a velvet cushion next to a steaming teacup"),
    "dragon": ("a small ruby-red dragon with golden belly scales and a tiny graduation cap", "breathing a little flame to melt a red wax seal onto a scroll"),
    "phoenix": ("a phoenix chick with flame-coloured feathers of gold, orange and crimson", "rising from a small pile of glowing ashes with wings spread wide"),
    "unicorn": ("a unicorn foal with a pastel lilac and gold mane and a glowing spiral horn", "its horn glowing brightly, lighting up an open spellbook"),
}


def pet_prompt(desc, signature):
    return f"""Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet for a cozy study app pet, a magical creature that lives with a student in a tower of an academy of magic: {desc}.
{STYLE}
{GRID}
Cells, left to right, top to bottom:
1) baby: tiny, round, stubby limbs, very cute
2) teen: bigger, a bit more confident
3) adult: grown, content, standing proud
4) adult {signature}
5) adult cheering, jumping with joy, paws (or wings) up in triumph
6) adult asleep, curled up, peaceful face
The same character and palette in every cell, facing three-quarters right."""


EGG_HINTS = [
    "glossy deep indigo with ink drips (ink spirit)", "sand-gold with a blue striped band (sphinx)", "black with glowing ember spots (salamander)",
    "grey stone with moss and a blue rune (golem)", "pale green with dragonfly-wing shimmer (pixie)", "tawny with a green scale band (chimera)",
    "teal with three fin ridges (hydra)", "silver-white with a crescent moon (moon hare)", "golden with feather scallops (griffin)",
    "white with blue flame tips (kitsune)", "wooden planks with brass corners (mimic)", "green with page-like stripes (bookwyrm)",
    "glossy black with a white collar band (raven)", "beige root texture with a leaf sprout (mandrake)", "purple with suckers (kraken)",
    "smoky dark with two golden dots (shade)", "tawny feathered with a wax-seal dot (owl)", "grey stone with tiny horns (gargoyle)",
    "red knitted pattern (hob)", "black and white with a gold star (magpie)", "black with purple stars (cat)",
    "ruby red with gold scales, glowing (dragon)", "flame gold and crimson, glowing (phoenix)", "pearly white with lilac swirl and a tiny horn, glowing (unicorn)",
]

OTHER = {
    "eggs": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 24 magical creature eggs for a cozy study app set in an academy of magic.
""" + OBJ_STYLE + """
Layout: 6 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One egg per cell, centered, with clear margin; no text, no grid lines.
Each egg is the same size and egg-shaped, but with its own pattern and colours that hint at the creature inside, in this order (left to right, top to bottom):
""" + ", ".join(f"{i + 1} {h}" for i, h in enumerate(EGG_HINTS)) + ".",
    "tower": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy study app: the inside of a student's round room at the top of a tower in an old academy of magic, seen from the front, side-on like a 2D game room.
""" + STYLE + """ Landscape 3:2.
The room: curved warm stone walls with a few wooden beams, a tall arched window on the LEFT wall whose glass is filled with flat solid pure magenta (#FF00FF), a wooden plank floor with a round deep-blue rug with a gold star pattern in the middle, warm candle light from a couple of wall sconces.
IMPORTANT: keep the room mostly EMPTY: no furniture, no creatures, no people, no text. The centre of the floor and the walls on the right must be clear, free space (furniture will be added later).
Only small details: a wall sconce with a candle on each side, a small arched wooden door on the far right, a few stars painted on the beams.""",
    "items": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 furniture objects for a student's room in a tower of an academy of magic, each a separate object, seen from the front (side-on like a 2D game room).
""" + OBJ_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a wooden study desk with an open spellbook, an ink pot and a candle, 2 a tall bookshelf full of old leather books, 3 a black iron cauldron on small legs with a faint green glow, 4 a brass telescope on a wooden tripod,
5 an alchemy table with coloured potion flasks and a small burner, 6 a crystal ball on a carved wooden stand, 7 a brass armillary sphere of rings, 8 a hanging iron chandelier with candles,
9 a wooden perch stand for a bird, 10 a hanging deep-blue banner with a gold star emblem (no letters), 11 a tall wobbly stack of old grimoires, 12 a fossil dragon skull on a stand,
13 a round glass aquarium with glowing jellyfish, 14 a small golden harp, 15 a big brass hourglass, 16 a framed painting of a castle on a cliff at night (no people).""",
    "props": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 small objects for a cozy study app set in an academy of magic, each separate, front view.
""" + OBJ_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a tall lit white candle in a brass holder, 2 a short melted-down candle stub with a tiny flame, 3 a small glowing blue mana crystal, 4 a small heap of three glowing mana crystals,
5 a quill pen in an ink pot, 6 an open old spellbook with glowing runes (no letters), 7 a rolled parchment scroll tied with a red ribbon and a red wax seal, 8 a leather school satchel,
9 a sealed envelope with a red wax seal, 10 a golden star, 11 a glowing light-blue crystal orb, 12 a speckled magic egg in a nest of straw,
13 a wooden magic wand with a carved handle, 14 a small brass bell, 15 a small stack of three closed books, 16 a golden laurel medal.""",
    "academy": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a wide banner scene for a cozy study app: the inner courtyard of an old academy of magic at dusk, seen from the front, side-on like a 2D game background.
""" + STYLE + """ Wide panorama, aspect ratio 3:1.
Tall stone towers with pointed roofs and warm lit windows on both sides, a big arched gate to a great hall in the middle, a wooden notice board with pinned parchment notes (no readable text) on the left, a few lanterns, an old tree, cobblestones, first stars in a deep indigo sky.
No people, no creatures, no letters or text anywhere.""",
    "chronicle": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a wide banner scene for a cozy study app: a quiet scriptorium archive in a tower of an academy of magic, seen from the front, side-on like a 2D game background.
""" + STYLE + """ Wide panorama, aspect ratio 3:1.
In the middle a huge open chronicle book on a carved wooden lectern, glowing softly; tall shelves of scrolls and bound volumes on both sides, a brass astrolabe, candles, hanging star charts, a round window with night sky.
No people, no creatures, no letters or text anywhere (pages show only faint scribbles and small drawings).""",
}


def image_of(output):
    """The image made in the Codex session named in its output (other sessions may draw at the same time)."""
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(job):
    name, prompt = job
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name, flush=True)
        return
    r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
    p = image_of((r.stdout or "") + (r.stderr or ""))
    if p is None:
        print("FAIL", name, (r.stdout or "")[-300:], (r.stderr or "")[-300:], flush=True)
        return
    shutil.copy(p, dst)
    print("ok", name, p, flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = list(OTHER.items()) + [(k, pet_prompt(*v)) for k, v in PETS.items()]
    jobs = [j for j in jobs if not want or j[0] in want]
    with ThreadPoolExecutor(3) as ex:
        list(ex.map(run, jobs))


if __name__ == "__main__":
    main()
