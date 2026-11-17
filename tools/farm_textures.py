#!/usr/bin/env python3
"""Mushroom farm textures, built from the composter and art/farm/.
art/farm/mushroom_farm_side_final.png gets used as is if it exists."""
import colorsys, io, sys, zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "art/farm"
OUT = ROOT / "src/main/resources/assets/ecologies/textures/block"
jar = zipfile.ZipFile(sys.argv[1])
compost = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/composter_compost.png"))).convert("RGBA")

# side: colours in art/farm/mushroom_farm_side_layout.png mark the parts, textured from the composter side
#   b4753e  rim and frame
#   512b0a  seams under the bars
#   251405  uprights and behind the mushroom
#   5e390e  side bars
#   a37043 / 87582e  bottom bar
#   a8e61d  compost between the uprights
#   anything else is the mushroom
comp_side = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/composter_side.png"))).convert("RGBA")
layout = Image.open(SRC / "mushroom_farm_side_layout.png").convert("RGBA")
C = lambda x, y: comp_side.getpixel((x % 16, y % 16))[:3]
lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
hexc = lambda c: "%02x%02x%02x" % c[:3]
side = Image.new("RGBA", (16, 16))
compost_rows = {}  # layout row -> composter groove row to take compost from
for y in range(16):
    for x in range(16):
        k = hexc(layout.getpixel((x, y)))
        if k == "b4753e":
            if y == 0:
                c = C(x, 0)  # the rim: the composter's top plank row
            else:
                # frame: light top/left, darker bottom/right
                top_left = layout.getpixel((x, y - 1))[:3] != layout.getpixel((x, y))[:3] and y <= 4 or \
                    (x > 0 and hexc(layout.getpixel((x - 1, y))) != "b4753e" and x <= 5)
                c = (0xb4, 0x75, 0x3e) if top_left else (0xa3, 0x70, 0x43)
        elif k == "512b0a":
            c = C(x, {1: 3, 6: 7, 11: 11, 15: 15}.get(y, 3))
        elif k == "251405":
            # darkest lines (251405, 3e250e), varied by the composter groove
            g = C(x + 3 * y, (4, 8, 12)[y % 3])
            c = (0x25, 0x14, 0x05) if lum(g) < 45 else (0x3e, 0x25, 0x0e)
        elif k == "5e390e":
            c = C(x, 5 if y < 8 else 9)
        elif k == "a37043":
            c = C(x, 13)
        elif k == "87582e":
            c = C(x, 14)
        elif k == "a8e61d":
            # compost from the composter's three compost rows
            row = {2: 4, 3: 8, 4: 12, 7: 8, 8: 12, 9: 4, 12: 12}.get(y, 4)
            c = C(x + y, row)
            if lum(c) < 45:
                c = (0x46, 0x31, 0x0e)  # keep the compost a step lighter than the dark lines beside it
        else:
            c = layout.getpixel((x, y))[:3]  # the mushroom
        side.putpixel((x, y), c + (255,))
# hand-drawn side wins if there is one
final = SRC / "mushroom_farm_side_final.png"
(Image.open(final).convert("RGBA") if final.exists() else side).save(OUT / "mushroom_farm_side.png")

# soil: drawn pattern, shades swapped for compost shades
top = Image.open(SRC / "mushroom_farm_top.png").convert("RGBA")
growing = Image.open(SRC / "mushroom_farm_top_growing.png").convert("RGBA")
lum = lambda p: 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]
inner = [(x, y) for y in range(2, 14) for x in range(2, 14)]
soil_shades = sorted({top.getpixel(p)[:3] for p in inner}, key=lum)
compost_shades = sorted({compost.getpixel((x, y))[:3] for y in range(16) for x in range(16) if compost.getpixel((x, y))[3] > 0
                         and compost.getpixel((x, y))[0] > compost.getpixel((x, y))[1] + 12}, key=lum)  # browns only, no leaf greens
# skip the brightest browns, they're leaf and straw
compost_shades = compost_shades[: max(min(len(soil_shades), len(compost_shades)), int(len(compost_shades) * 0.8))]
mapping = {s: compost_shades[round(i * (len(compost_shades) - 1) / max(1, len(soil_shades) - 1))] for i, s in enumerate(soil_shades)}
soil = Image.new("RGBA", (16, 16))
spotted = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        # inside is 12x12, wrap the pattern
        sx, sy = 2 + x % 12, 2 + y % 12
        c = mapping[top.getpixel((sx, sy))[:3]] + (255,)
        soil.putpixel((x, y), c)
        g = growing.getpixel((sx, sy))
        # pale mycelium spots, so they work for any species
        if g[0] > 200 and g[1] > 180:
            spotted.putpixel((x, y), (0xee, 0xe6, 0xd2, 255))
        elif g[0] > g[1] + 60:
            spotted.putpixel((x, y), (0xd2, 0xc6, 0xa8, 255) if lum(g) > 90 else (0xb0, 0xa2, 0x84, 255))
        else:
            spotted.putpixel((x, y), c)
soil.save(OUT / "mushroom_farm_soil.png")
spotted.save(OUT / "mushroom_farm_soil_spotted.png")
print("wrote side, soil, soil_spotted;", len(soil_shades), "soil shades ->", len(compost_shades), "compost shades")
