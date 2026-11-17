#!/usr/bin/env python3
"""Roost textures: the dripstone block with the hole from art/roost/ cut in.
usage: roost_textures.py <vanilla client jar>"""
import io, sys, zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/ecologies/textures/block"
jar = zipfile.ZipFile(sys.argv[1])
drip = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/dripstone_block.png"))).convert("RGBA")
lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]

for name in ("bat_roost_side", "bat_roost_top"):
    src = Image.open(ROOT / "art/roost" / f"{name}.png").convert("RGBA")
    px = src.load()
    # flood fill the hole from its darkest pixel
    box = [(x, y) for y in range(2, 13) for x in range(2, 14)]
    start = min(box, key=lambda p: lum(px[p]))
    dark = lum(px[start]) + 38
    darkest = [p for p in box if lum(px[p]) <= lum(px[start]) + 12]
    cx = sum(p[0] for p in darkest) / len(darkest)
    cy = sum(p[1] for p in darkest) / len(darkest)
    hole, todo = set(), [start]
    while todo:
        p = todo.pop()
        # stop at the rim
        if p in hole or (p[0] - cx) ** 2 + (p[1] - cy) ** 2 > 4.3 ** 2 or lum(px[p]) > dark:
            continue
        hole.add(p)
        todo += [(p[0] + 1, p[1]), (p[0] - 1, p[1]), (p[0], p[1] + 1), (p[0], p[1] - 1)]
    out = drip.copy()
    for p in hole:
        out.putpixel(p, px[p])
    out.save(OUT / f"{name}.png")
    print(name, len(hole), "hole pixels")
