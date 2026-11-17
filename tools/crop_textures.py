#!/usr/bin/env python3
"""Bolted crop item textures. Spots on the vanilla sprite, like the poisonous potato.
usage: crop_textures.py <vanilla client jar>"""
import colorsys, io, math, random, sys, zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/ecologies/textures/item"
lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
leafy = lambda c: c[1] > c[0] + 20  # vanilla's green leaves and tops

# palettes, dark to light: rim, body, highlight
GREEN = [(49, 82, 55), (73, 103, 63), (107, 134, 62), (176, 200, 58), (196, 217, 81)]   # the poisonous potato's
MOLD = [(126, 130, 104), (164, 168, 138), (199, 199, 172), (222, 220, 196), (236, 233, 214)]
RUST = [(86, 30, 26), (112, 40, 32), (140, 54, 40), (166, 74, 48), (186, 96, 60)]

CROPS = {
    # name: (vanilla item, palette, number of big spots, number of small ones, min spacing, region filter)
    "poisonous_carrot": ("carrot", GREEN, 2, 2, 3.0, lambda x, y: y >= 7),       # the root, away from its leaves
    "moldy_beetroot": ("beetroot", MOLD, 2, 2, 3.0, lambda x, y: y >= 5),        # the root, not the stem
    "rusted_wheat": ("wheat", RUST, 1, 5, 2.6, lambda x, y: x >= 6 and y <= 9),  # the ears, not the stalks
}

N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


def make(name, vanilla, palette, big, small, spacing, region, jar):
    im = Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/item/{vanilla}.png"))).convert("RGBA")
    px = im.load()
    body = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and px[x, y][3] > 0 and not leafy(px[x, y])
    cells = [(x, y) for y in range(16) for x in range(16) if body(x, y) and (region is None or region(x, y))]
    lums = [lum(px[p]) for p in cells]
    lo, hi = min(lums), max(lums)
    shade = lambda p: (lum(px[p]) - lo) / max(1.0, hi - lo)  # 0 dark .. 1 light, from the crop itself
    inner = lambda p: sum(body(p[0] + dx, p[1] + dy) for dx, dy in N4)
    rnd = random.Random(name)
    out = im.copy()
    o = out.load()

    def paint(p, level):
        # level: 0 rim .. 4 highlight; the crop's own shading moves it a step up or down
        s = shade(p)
        level += 1 if s > 0.75 else -1 if s < 0.3 else 0
        o[p] = palette[max(0, min(4, level))] + (255,)

    taken = []
    def pick(min_inner):
        options = [p for p in cells if inner(p) >= min_inner and all(math.dist(p, q) >= spacing for q in taken)]
        if not options:
            return None
        p = rnd.choice(options)
        taken.append(p)
        return p

    for _ in range(big):
        c = pick(4)
        if c is None:
            break
        # small blob with a dark pixel underneath
        arms = [q for q in ((c[0] + dx, c[1] + dy) for dx, dy in N4) if body(*q)]
        rnd.shuffle(arms)
        blob = [c] + arms[:3]
        diag = [q for q in ((c[0] + dx, c[1] + dy) for dx in (-1, 1) for dy in (-1, 1)) if body(*q)]
        if diag and rnd.random() < 0.6:
            blob.append(rnd.choice(diag))
        for q in blob:
            paint(q, 3 if q == c else 2)
        shadow = [q for q in ((b[0] + dx, b[1] + dy) for b in blob for dx, dy in ((1, 1), (0, 1), (1, 0))) if body(*q) and q not in blob]
        for q in rnd.sample(shadow, k=min(len(shadow), 2)):
            paint(q, 0)
    for _ in range(small):
        c = pick(3)
        if c is None:
            break
        paint(c, 3)
        # a second pixel of the same fleck, up or left
        mate = [q for q in ((c[0] - 1, c[1]), (c[0], c[1] - 1)) if body(*q)]
        if mate:
            paint(rnd.choice(mate), 2)
        # darker pixel beside it so it reads as a bump
        nb = [q for q in ((c[0] + 1, c[1]), (c[0], c[1] + 1)) if body(*q) and q not in taken]
        if nb:
            paint(rnd.choice(nb), 0)

    if name == "moldy_beetroot":
        # dull the leaves a bit too
        for y in range(16):
            for x in range(16):
                c = px[x, y]
                if c[3] and leafy(c):
                    h, l, s = colorsys.rgb_to_hls(*(v / 255 for v in c[:3]))
                    r, g, b = colorsys.hls_to_rgb(h - 0.03, l * 0.95, s * 0.55)
                    o[x, y] = (round(r * 255), round(g * 255), round(b * 255), c[3])
    out.save(OUT / f"{name}.png")
    return taken


jar = zipfile.ZipFile(sys.argv[1])
for name, spec in CROPS.items():
    print(name, make(name, *spec, jar))
