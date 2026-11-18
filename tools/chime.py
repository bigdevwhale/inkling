"""The bell that rings when the candle has burnt down: app/src/main/res/raw/chime.wav.

Two soft strikes of a small bell (a fifth apart), each a handful of decaying partials.
Run from the repository root: python tools/chime.py
"""
import math
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/raw/chime.wav"

# (frequency ratio, loudness, how fast it dies away) – the partials of a small bell
PARTIALS = [(1.0, 1.0, 2.2), (2.0, 0.45, 3.2), (3.01, 0.22, 4.5), (4.2, 0.12, 6.0), (5.43, 0.06, 8.0)]


def strike(freq, at, length, total):
    out = [0.0] * total
    start = int(at * RATE)
    for i in range(int(length * RATE)):
        if start + i >= total:
            break
        t = i / RATE
        attack = min(1.0, t / 0.006)
        out[start + i] = attack * sum(a * math.exp(-d * t) * math.sin(2 * math.pi * freq * r * t) for r, a, d in PARTIALS)
    return out


def main():
    seconds = 2.6
    total = int(seconds * RATE)
    a = strike(783.99, 0.0, 2.6, total)   # G5
    b = strike(1174.66, 0.45, 2.1, total)  # D6
    mix = [x + 0.8 * y for x, y in zip(a, b)]
    fade = int(0.25 * RATE)
    for i in range(fade):
        mix[total - 1 - i] *= i / fade
    peak = max(abs(v) for v in mix)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(v / peak * 0.8 * 32767)) for v in mix))
    print(OUT, OUT.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
