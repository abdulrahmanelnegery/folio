#!/usr/bin/env python3
"""Fail when the Arabic strings drift from the English ones: same keys, same placeholders, same plural names."""
import re, sys
from xml.etree import ElementTree as ET
res = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res"
def load(path):
    out = {}
    for el in ET.parse(path).getroot():
        text = "".join(el.itertext()) if el.tag == "string" else " ".join("".join(i.itertext()) for i in el)
        out[(el.tag, el.get("name"))] = text
    return out
en = {**load(f"{res}/values/strings.xml"), **load(f"{res}/values/plurals.xml")}
ar = {**load(f"{res}/values-ar/strings.xml"), **load(f"{res}/values-ar/plurals.xml")}
problems = [f"missing in Arabic: {k[1]}" for k in en if k not in ar] + [f"only in Arabic: {k[1]}" for k in ar if k not in en]
ph = lambda t: sorted(re.findall(r"%\d\$[sd]", t))
problems += [f"placeholders differ: {k[1]}" for k in en if k in ar and k[0] == "string" and ph(en[k]) != ph(ar[k])]
print("\n".join(problems) or f"ok: {len(en)} entries match")
sys.exit(1 if problems else 0)
