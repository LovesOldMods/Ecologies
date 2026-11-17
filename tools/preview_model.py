#!/usr/bin/env python3
"""Renders a converted model like ModelPart would, to check it.
usage: preview_model.py <model.json> <texture.png> <out.png> [yaw_degrees]"""
import json, math, sys
import numpy as np
from PIL import Image

model = json.load(open(sys.argv[1]))
tex = np.asarray(Image.open(sys.argv[2]).convert("RGBA"))
TW, TH = model["texture"]
yaw = math.radians(float(sys.argv[4]) if len(sys.argv) > 4 else 35)
parts = {p["name"]: p for p in model["parts"]}

def rot_zyx(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]); Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx  # Quaternionf.rotationZYX(z, y, x)

def world_matrix(p):
    M = np.eye(4)
    T = np.eye(4); T[:3, 3] = np.array(p["offset"]) / 16.0
    R = np.eye(4); R[:3, :3] = rot_zyx(*p["rotation"])
    M = T @ R
    return world_matrix(parts[p["parent"]]) @ M if p["parent"] else M

tris = []
for p in model["parts"]:
    M = world_matrix(p)
    for c in p["cubes"]:
        (x0, y0, z0), (w, h, d) = c["box"], c["size"]
        x1, y1, z1 = x0 + w, y0 + h, z0 + d
        if c["mirror"]: x0, x1 = x1, x0
        u, v = c["tex"]
        f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + 2 * w, u + d + w + d, u + 2 * d + 2 * w
        f10, f11, f12 = v, v + d, v + d + h
        V = {7: (x0, y0, z0), 0: (x1, y0, z0), 1: (x1, y1, z0), 2: (x0, y1, z0), 3: (x0, y0, z1), 4: (x1, y0, z1), 5: (x1, y1, z1), 6: (x0, y1, z1)}
        faces = [([4, 3, 7, 0], (f5, f10, f6, f11)), ([1, 2, 6, 5], (f6, f11, f7, f10)), ([7, 3, 6, 2], (f4, f11, f5, f12)),
                 ([0, 7, 2, 1], (f5, f11, f6, f12)), ([4, 0, 1, 5], (f6, f11, f8, f12)), ([3, 4, 5, 6], (f8, f11, f9, f12))]
        for vs, (u1, v1, u2, v2) in faces:
            uvs = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
            pts = []
            for i, vi in enumerate(vs):
                pos = np.array([*V[vi], 16.0]) / 16.0
                wp = M @ pos
                # LivingEntityRenderer: scale(-1, -1, 1), translate(0, -1.501, 0)
                pts.append((np.array([-wp[0], -wp[1] + 1.501, wp[2]]), uvs[i]))
            tris.append([pts[0], pts[1], pts[2]]); tris.append([pts[0], pts[2], pts[3]])

S, scale = 420, 120
img = np.zeros((S, S, 4), np.uint8); img[..., :3] = (40, 40, 48); img[..., 3] = 255
zb = np.full((S, S), -1e9)
cy_, sy_ = math.cos(yaw), math.sin(yaw); pitch = math.radians(20); cp, sp = math.cos(pitch), math.sin(pitch)
def project(p):
    x, y, z = p
    x, z = x * cy_ - z * sy_, x * sy_ + z * cy_       # yaw around the vertical axis
    y, z = y * cp - z * sp, y * sp + z * cp          # tilt to look slightly down
    return S / 2 + x * scale, S * 0.72 - y * scale, z
for tri in tris:
    P = [project(p) for p, _ in tri]; UV = [uv for _, uv in tri]
    xs, ys = [p[0] for p in P], [p[1] for p in P]
    minx, maxx, miny, maxy = int(max(0, min(xs))), int(min(S - 1, max(xs) + 1)), int(max(0, min(ys))), int(min(S - 1, max(ys) + 1))
    (ax, ay, az), (bx, by, bz), (qx, qy, qz) = P
    den = (by - qy) * (ax - qx) + (qx - bx) * (ay - qy)
    if abs(den) < 1e-9: continue
    for py in range(miny, maxy + 1):
        for px in range(minx, maxx + 1):
            l1 = ((by - qy) * (px - qx) + (qx - bx) * (py - qy)) / den
            l2 = ((qy - ay) * (px - qx) + (ax - qx) * (py - qy)) / den
            l3 = 1 - l1 - l2
            if l1 < 0 or l2 < 0 or l3 < 0: continue
            z = l1 * az + l2 * bz + l3 * qz
            if z <= zb[py, px]: continue   # larger z = closer to the viewer
            uu = l1 * UV[0][0] + l2 * UV[1][0] + l3 * UV[2][0]; vv = l1 * UV[0][1] + l2 * UV[1][1] + l3 * UV[2][1]
            tx, ty = int(uu * tex.shape[1] / TW), int(vv * tex.shape[0] / TH)
            if not (0 <= tx < tex.shape[1] and 0 <= ty < tex.shape[0]): continue
            col = tex[ty, tx]
            if col[3] < 128: continue
            zb[py, px] = z; img[py, px] = col
Image.fromarray(img).save(sys.argv[3])
