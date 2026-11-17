#!/usr/bin/env python3
"""Checks every model/texture our assets point to exists.
usage: check_assets.py <version> <vanilla client resources jar>"""
import json, sys, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
v, jar = sys.argv[1], sys.argv[2]
vanilla = set(zipfile.ZipFile(jar).namelist())
dirs = [ROOT / "src/main/resources", ROOT / f"src/main/resources-{v}"]

def exists(kind, ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    rel = f"assets/{ns}/{kind}/{path}.{'png' if kind == 'textures' else 'json'}"
    return rel in vanilla or any((d / rel).exists() for d in dirs)

bad = []
for d in dirs:
    for f in (d / "assets/ecologies").rglob("*.json"):
        data = json.loads(f.read_text())
        refs = []
        if "blockstates" in f.parts:
            applied = list(data.get("variants", {}).values())
            for part in data.get("multipart", []):
                applied += part["apply"] if isinstance(part["apply"], list) else [part["apply"]]
            applied = [a for x in applied for a in (x if isinstance(x, list) else [x])]
            refs = [("models", x["model"]) for x in applied]
        elif "items" in f.parts:
            refs = [("models", data["model"]["model"])]
        elif "models" in f.parts:
            if "parent" in data and not data["parent"].startswith("builtin"):
                refs.append(("models", data["parent"]))
            refs += [("textures", t) for t in data.get("textures", {}).values() if not t.startswith("#")]
        bad += [f"{f.relative_to(ROOT)} -> {k}/{r}" for k, r in refs if not exists(k, r)]
print(f"{v}: {'all references resolve' if not bad else str(len(bad)) + ' missing'}")
print("\n".join(bad))
sys.exit(1 if bad else 0)
