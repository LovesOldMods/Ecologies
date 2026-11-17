#!/usr/bin/env python3
"""Log top where a crown sits on it. Four bark slopes running inward instead of a flat cut.
usage: log_top_textures.py <vanilla client jar>"""
import io, sys, zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/ecologies/textures/block"
jar = zipfile.ZipFile(sys.argv[1])
for bark, name in (("spruce_log", "dwarf_spruce_log_joined_top"), ("stripped_spruce_log", "stripped_dwarf_spruce_log_joined_top")):
    src = Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{bark}.png"))).convert("RGBA")
    out = Image.new("RGBA", (16, 16))
    pix = [src.getpixel((x, y)) for y in range(16) for x in range(16)]
    avg = tuple(sum(p[i] for p in pix) / len(pix) for i in range(3))
    end = Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{bark}_top.png"))).convert("RGBA")
    palette = sorted({p[:3] for p in pix} | {end.getpixel((x, y))[:3] for y in range(16) for x in range(16)})
    a, b = 4, 12  # the thick log's top face
    for y in range(16):
        for x in range(16):
            if not (a <= x < b and a <= y < b):
                out.putpixel((x, y), src.getpixel((x, y)))
                continue
            dx, dy = x - 7.5, y - 7.5
            if abs(dy) >= abs(dx):   # north or south slope: grain runs along y toward the middle
                depth = (y - a) if dy < 0 else (b - 1 - y)
                c = src.getpixel((x, (depth * 3 + (0 if dy < 0 else 8)) % 16))
            else:                    # east or west slope: grain runs along x
                depth = (x - a) if dx < 0 else (b - 1 - x)
                c = src.getpixel((y, (depth * 3 + (4 if dx < 0 else 12)) % 16))
            # lit like a pyramid (N, W, E, S), darker toward the middle
            if abs(dy) >= abs(dx):
                f = 1.3 if dy < 0 else 0.78
            else:
                f = 1.12 if dx < 0 else 0.92
            f *= 1.0 - 0.05 * depth
            # half grain, half average colour
            c = tuple(0.5 * c[i] + 0.5 * avg[i] for i in range(3))
            shaded = tuple(min(255, v * f) for v in c)
            # snap to the bark's own palette
            out.putpixel((x, y), min(palette, key=lambda p: sum((p[i] - shaded[i]) ** 2 for i in range(3))) + (255,))
    out.save(OUT / f"{name}.png")
    print("wrote", name)
