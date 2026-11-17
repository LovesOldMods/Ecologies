#!/usr/bin/env python3
"""Converts the muskox model file into ModelPart code.
Re-packs the texture since ModelPart cubes only do box UV."""
import json
import math
import sys
from pathlib import Path

from PIL import Image

# editor face -> ModelPart polygon. x mirrored, y flipped, up/down rects corner-reversed.
BB_TO_MC = {"east": "west", "west": "east", "north": "north", "south": "south", "up": "down", "down": "up"}
REVERSED = {"up", "down"}


class Cube:
    def __init__(self, frm, to, faces=None, box_offset=None, mirror=False, inflate=0.0):
        self.frm, self.to = list(frm), list(to)
        self.faces = faces  # bb face name -> [u1, v1, u2, v2] (source texture units), or None for box UV
        self.box_offset = box_offset
        self.mirror = mirror
        self.inflate = inflate


class Group:
    def __init__(self, name, origin, rotation, mirror):
        self.name, self.origin = name, list(origin)
        self.rotation = list(rotation or [0, 0, 0])
        self.mirror = mirror
        self.cubes, self.children, self.parent = [], [], None


# ------------------------------------------------------------------ 1. read like a model editor's importer

def read_jem(path):
    data = json.loads(Path(path).read_text())
    tex_size = data.get("textureSize", [64, 32])
    roots, names = [], set()

    def unique(name):
        base, n = name, 2
        while name in names:
            name, n = f"{base}_{n}", n + 1
        names.add(name)
        return name

    def read_content(sub, group, depth, is_root_group):
        for box in sub.get("boxes", []):
            c = box["coordinates"]
            frm = [c[0], c[1], c[2]]
            to = [c[0] + c[3], c[1] + c[4], c[2] + c[5]]
            faces = None
            if "textureOffset" not in box:
                faces = {f: box[f"uv{f.capitalize()}"] for f in ("north", "east", "south", "west", "up", "down")
                         if f"uv{f.capitalize()}" in box}
            if not is_root_group:
                frm = [frm[i] + group.origin[i] for i in range(3)]
                to = [to[i] + group.origin[i] for i in range(3)]
            group.cubes.append(Cube(frm, to, faces, box.get("textureOffset"), group.mirror, box.get("sizeAdd", 0.0)))
        for i, s in enumerate(sub.get("submodels", [])):
            t = list(s.get("translate", sub.get("translate", [0, 0, 0]) if depth >= 1 else [0, 0, 0]))
            if depth >= 1 and "translate" in s:
                t = [t[k] + group.origin[k] for k in range(3)]
            child = Group(unique(s.get("id") or f"{group.name}_sub_{i}"), t, s.get("rotate"),
                          "u" in (s.get("mirrorTexture") or ""))
            child.parent = group
            group.children.append(child)
            read_content(s, child, depth + 1, False)

    for m in data["models"]:
        origin = [-v for v in m.get("translate", [0, 0, 0])]
        g = Group(unique(m.get("part") or m.get("id")), origin, m.get("rotate"), "u" in (m.get("mirrorTexture") or ""))
        roots.append(g)
        read_content(m, g, 0, True)
    return roots, tex_size


# ------------------------------------------------------------------ 2. convert like a Java model export

def pose(g):
    o = list(g.origin)
    if g.parent:
        o = [o[i] - g.parent.origin[i] for i in range(3)]
    o[0] *= -1
    o[1] *= -1
    if not g.parent:
        o[1] += 24
    rot = [math.radians(-g.rotation[0]), math.radians(-g.rotation[1]), math.radians(g.rotation[2])]
    return o, rot


def normalize(c):
    """Flip negative boxes, keeping each face where it was."""
    faces = dict(c.faces) if c.faces else None
    for axis, (lo, hi) in enumerate([("west", "east"), ("down", "up"), ("north", "south")]):
        if c.to[axis] >= c.frm[axis]:
            continue
        c.frm[axis], c.to[axis] = c.to[axis], c.frm[axis]
        if faces is None:
            continue
        # opposite faces swap
        a, b = faces.get(lo), faces.get(hi)
        faces.pop(lo, None); faces.pop(hi, None)
        if a: faces[hi] = [a[2], a[1], a[0], a[3]]
        if b: faces[lo] = [b[2], b[1], b[0], b[3]]
        # mirror faces along this axis
        for f, uv in list(faces.items()):
            if f in (lo, hi):
                continue
            along_u = {0: ("north", "south", "up", "down"), 2: ("east", "west")}.get(axis, ())
            along_v = {1: ("north", "south", "east", "west"), 2: ("up", "down")}.get(axis, ())
            if f in along_u:
                faces[f] = [uv[2], uv[1], uv[0], uv[3]]
            elif f in along_v:
                faces[f] = [uv[0], uv[3], uv[2], uv[1]]
    c.faces = faces


def mc_box(g, c):
    size = [c.to[i] - c.frm[i] for i in range(3)]
    x = g.origin[0] - c.to[0]
    y = -c.frm[1] - size[1] + g.origin[1]
    z = c.frm[2] - g.origin[2]
    return [x, y, z], size


# ------------------------------------------------------------------ 3. re-pack the texture

def std_rects(u, v, w, h, d):
    """ModelPart.Cube polygon rects (u1, v1, u2, v2) for a box at texOffs(u, v)."""
    return {
        "down": (u + d, v, u + d + w, v + d),
        "up": (u + d + w, v + d, u + d + 2 * w, v),
        "west": (u, v + d, u + d, v + d + h),
        "north": (u + d, v + d, u + d + w, v + d + h),
        "east": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def copy_rect(src, dst, s_rect, d_rect, scale):
    u1, v1, u2, v2 = d_rect
    if u1 == u2 or v1 == v2:
        return
    x0, x1 = sorted((u1, u2)); y0, y1 = sorted((v1, v2))
    for py in range(int(math.floor(y0)), int(math.ceil(y1))):
        for px in range(int(math.floor(x0)), int(math.ceil(x1))):
            s = (px + 0.5 - u1) / (u2 - u1)
            t = (py + 0.5 - v1) / (v2 - v1)
            sx = (s_rect[0] + s * (s_rect[2] - s_rect[0])) * scale
            sy = (s_rect[1] + t * (s_rect[3] - s_rect[1])) * scale
            ix, iy = int(math.floor(sx)), int(math.floor(sy))
            if 0 <= ix < src.width and 0 <= iy < src.height and 0 <= px < dst.width and 0 <= py < dst.height:
                dst.putpixel((px, py), src.getpixel((ix, iy)))


class Packer:
    """Shelf packer."""
    def __init__(self, width):
        self.width, self.x, self.y, self.shelf = width, 0, 0, 0

    def place(self, w, h):
        if self.x + w > self.width:
            self.x, self.y, self.shelf = 0, self.y + self.shelf, 0
        pos = (self.x, self.y)
        self.x += w
        self.shelf = max(self.shelf, h)
        return pos


# ------------------------------------------------------------------ 4. write Java

def f(v):
    s = f"{v:.4f}".rstrip("0").rstrip(".")
    return (s if s not in ("-0", "") else "0") + "F"


def convert(jem, textures, out_dir, cls):
    roots, (tw, th) = read_jem(jem)
    srcs = [Image.open(t).convert("RGBA") for t in textures]
    scale = srcs[0].width / tw
    # each cube gets its own box UV region
    groups = []
    def walk(g):
        groups.append(g)
        for ch in g.children:
            walk(ch)
    for r in roots:
        walk(r)
    for g in groups:
        for c in g.cubes:
            normalize(c)
    packer = Packer(128)
    placements = []
    for g in groups:
        for c in g.cubes:
            w, h, d = [int(math.ceil(abs(c.to[i] - c.frm[i]) - 1e-6)) for i in range(3)]  # 6.000000000000001 is 6, not 7
            placements.append((g, c, packer.place(max(1, 2 * d + 2 * w), max(1, d + h)), (w, h, d)))
    out_h = 1 << max(5, math.ceil(math.log2(packer.y + packer.shelf + 1)))
    outs = [Image.new("RGBA", (128, out_h), (0, 0, 0, 0)) for _ in srcs]
    for g, c, (u, v), (w, h, d) in placements:
        rects = std_rects(u, v, w, h, d)
        for src, out in zip(srcs, outs):
            if c.faces is None:  # already box UV, copy it straight
                ou, ov = c.box_offset
                for face, rect in std_rects(ou, ov, w, h, d).items():
                    copy_rect(src, out, rect, rects[face], scale)
            else:
                for bb_face, uv in c.faces.items():
                    mc = BB_TO_MC[bb_face]
                    s_rect = (uv[2], uv[3], uv[0], uv[1]) if bb_face in REVERSED else tuple(uv)
                    copy_rect(src, out, s_rect, rects[mc], scale)
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    tex_paths = []
    for t, out in zip(textures, outs):
        p = out_dir / (Path(t).stem + ".png")
        out.save(p)
        tex_paths.append(p)

    lines, preview = [], []
    def emit(g, parent_var):
        var = "p_" + g.name  # never clashes with the mesh root variable
        (ox, oy, oz), (rx, ry, rz) = pose(g)
        cubes = ""
        for gg, c, (u, v), (w, h, d) in placements:
            if gg is not g:
                continue
            (x, y, z), (sx, sy, sz) = mc_box(g, c)
            m = ".mirror()" if c.mirror and c.faces is None else ""
            un = ".mirror(false)" if m else ""
            cubes += (f"\n                .texOffs({u}, {v}){m}.addBox({f(x)}, {f(y)}, {f(z)}, {f(sx)}, {f(sy)}, {f(sz)}, "
                      f"new CubeDeformation({f(c.inflate)})){un}")
        preview.append({"name": g.name, "parent": g.parent.name if g.parent else None, "offset": [ox, oy, oz],
                        "rotation": [rx, ry, rz],
                        "cubes": [{"tex": [u, v], "box": mc_box(g, c)[0], "size": mc_box(g, c)[1],
                                   "mirror": bool(c.mirror and c.faces is None)}
                                  for gg, c, (u, v), _ in placements if gg is g]})
        rot = any(abs(a) > 1e-6 for a in (rx, ry, rz))
        p = (f"PartPose.offsetAndRotation({f(ox)}, {f(oy)}, {f(oz)}, {f(rx)}, {f(ry)}, {f(rz)})" if rot
             else f"PartPose.offset({f(ox)}, {f(oy)}, {f(oz)})")
        lines.append(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{g.name}\", CubeListBuilder.create(){cubes}, {p});")
        for ch in g.children:
            emit(ch, var)
    for r in roots:
        emit(r, "root")
    (out_dir / f"{cls}.json").write_text(json.dumps({"texture": [128, out_h], "parts": preview}, indent=1))
    return lines, (128, out_h), tex_paths


def main():
    jem, textures, out_dir, cls = sys.argv[1], sys.argv[2].split(","), sys.argv[3], sys.argv[4]
    lines, (w, h), paths = convert(jem, textures, out_dir, cls)
    print(f"// {cls}: generated by tools/model_to_java.py from {Path(jem).name}; texture {w}x{h}")
    print("\n".join(lines))
    print(f"        return LayerDefinition.create(mesh, {w}, {h});")
    for p in paths:
        print(f"// texture: {p}")


if __name__ == "__main__":
    main()
