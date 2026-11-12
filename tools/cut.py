"""Cuts the Codex sheets in art/sheets into app drawables.

    python tools/cut.py

Creatures (3×2): pet_<id>_<0..5>.webp – baby, teen, adult, signature move, cheer, asleep.
Items (4×4): item_<id>.webp. Props (4×4): prop_<id>.webp. Eggs (6×4): egg_<species>.webp.
Tower: tower_bg.webp with the magenta window glass made see-through (the sky is painted behind it).

The art is full of purples, so the magenta is not keyed by colour alone: only magenta-ish pixels joined to the
cell's border are taken out (a flood fill), then the remaining pink fringe along the cut is softened.
"""
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

PETS = ["inkling", "sphinx", "salamander", "golem", "pixie", "chimera", "hydra", "moonhare", "griffin", "kitsune", "mimic", "bookwyrm",
        "raven", "mandrake", "kraken", "shade", "owl", "gargoyle", "hob", "magpie", "cat", "dragon", "phoenix", "unicorn"]
ITEMS = ["desk", "shelf", "cauldron", "telescope", "alchemy", "crystal", "armillary", "chandelier",
         "perch", "banner", "grimoires", "skull", "aquarium", "harp", "hourglass", "painting"]
PROPS = ["candle", "stub", "mana", "manas", "quill", "book", "scroll", "satchel",
         "letter", "star", "orb", "egg", "wand", "bell", "books", "medal"]
MAX = 320


def dist(arr):
    """How far each pixel is from pure magenta (0 = exactly #FF00FF)."""
    r, g, b = arr[..., 0].astype(float), arr[..., 1].astype(float), arr[..., 2].astype(float)
    return np.sqrt((255 - r) ** 2 + g ** 2 + (255 - b) ** 2)


def key(im, flood=True):
    arr = np.array(im.convert("RGBA"))
    d = dist(arr)
    near = d < 150          # could be background
    if flood:
        lab, _ = ndimage.label(near)
        edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
        bg = np.isin(lab, list(edge))
        # background shut inside a shape (between an arm and a bag): pure magenta patches of some size
        lab2, n2 = ndimage.label(d < 70)
        if n2:
            sizes = ndimage.sum(np.ones_like(lab2), lab2, range(n2 + 1))
            holes = np.isin(lab2, [k for k in range(1, n2 + 1) if sizes[k] >= 25])
            bg = bg | ndimage.binary_dilation(holes, iterations=2) & near
    else:
        bg = d < 110
    alpha = arr[..., 3].astype(float)
    alpha[bg & (d < 95)] = 0
    fringe = bg & (d >= 95)
    # pull the pink out of the fringe and fade it
    k = np.clip((150 - d) / 55, 0, 1)
    for c in (0, 2):
        ch = arr[..., c].astype(float)
        arr[..., c] = np.where(fringe, ch - (ch - arr[..., 1]) * k, ch).astype(np.uint8)
    alpha = np.where(fringe, alpha * (1 - k), alpha)
    # pixels next to the cut-out that still look pink lose their tint too
    grown = ndimage.binary_dilation(alpha == 0, iterations=1) & (alpha > 0) & (d < 200)
    for c in (0, 2):
        ch = arr[..., c].astype(float)
        arr[..., c] = np.where(grown, np.minimum(ch, arr[..., 1] + 40), ch).astype(np.uint8)
    arr[..., 3] = alpha.astype(np.uint8)
    return Image.fromarray(arr)


def border_clean(cell):
    """Drops bits of the neighbouring cells: anything opaque that touches the cell's edge, except the biggest piece."""
    arr = np.array(cell)
    lab, n = ndimage.label(arr[:, :, 3] > 30)
    if n == 0:
        return cell
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    big = max(range(1, n + 1), key=lambda k: sizes[k])
    for k in edge:
        if k != big:
            arr[lab == k, 3] = 0
    # specks far smaller than the creature are noise
    for k in range(1, n + 1):
        if sizes[k] < sizes[big] * .004:
            arr[lab == k, 3] = 0
    return Image.fromarray(arr)


def frames(sheet, cols, rows):
    src = Image.open(sheet).convert("RGBA")
    w, h = src.size
    cw, ch = w / cols, h / rows
    out = []
    for j in range(rows):
        for i in range(cols):
            cell = src.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch)))
            cell = border_clean(key(cell))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            cell = cell.crop(box) if box else cell
            s = MAX / max(cell.size)
            if s < 1:
                cell = cell.resize((max(1, round(cell.width * s)), max(1, round(cell.height * s))), Image.LANCZOS)
            out.append(cell)
    return out


def save(im, name):
    im.save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for p in PETS:
        f = SHEETS / f"{p}.png"
        if not f.exists():
            print("missing", p)
            continue
        for n, fr in enumerate(frames(f, 3, 2)):
            save(fr, f"pet_{p}_{n}")
        print("pet", p)
    for sheet, names, prefix in (("items", ITEMS, "item"), ("props", PROPS, "prop")):
        if (SHEETS / f"{sheet}.png").exists():
            for name, fr in zip(names, frames(SHEETS / f"{sheet}.png", 4, 4)):
                save(fr, f"{prefix}_{name}")
            print(sheet)
    if (SHEETS / "eggs.png").exists():
        for name, fr in zip(PETS, frames(SHEETS / "eggs.png", 6, 4)):
            save(fr, f"egg_{name}")
        print("eggs")
    if (SHEETS / "tower.png").exists():
        g = key(Image.open(SHEETS / "tower.png"), flood=False)
        g = g.resize((1152, round(1152 * g.height / g.width)), Image.LANCZOS)
        g.save(OUT / "tower_bg.webp", "WEBP", quality=88, method=6)
        a = np.array(g)[..., 3]
        ys, xs = np.where(a < 128)
        if len(xs):
            print("window", xs.min() / g.width, xs.max() / g.width, ys.min() / g.height, ys.max() / g.height)
        print("tower")


if __name__ == "__main__":
    main()
