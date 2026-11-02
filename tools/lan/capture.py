#!/usr/bin/env python3
"""Composite screenshot of a LAN game window, including popup/dialog windows
that overlap it (this X server does not paint child windows into the root
pixmap, so `import -window root` cannot see them).

Usage: capture.py <lan_dir> <p1|p2> <out.png>
"""
import os
import sys

from PIL import Image
from Xlib import X, display as xdisplay

# LAN_DISPLAY wins over the ambient DISPLAY (agent shell usually has :0).
os.environ["DISPLAY"] = os.environ.get("LAN_DISPLAY") or os.environ.get("DISPLAY", ":0")


def abs_rect(root, w):
    """Absolute (x, y, width, height, depth) of a window's drawable area."""
    x = y = 0
    cur = w
    while cur.id != root.id:
        g = cur.get_geometry()
        x += g.x
        y += g.y
        cur = cur.query_tree().parent
    g = w.get_geometry()
    return x, y, g.width, g.height, g.depth


def main() -> int:
    lan_dir, target, out = sys.argv[1], sys.argv[2], sys.argv[3]
    with open(f"{lan_dir}/run/{target}.win") as f:
        win_id = int(f.read().strip(), 0)

    d = xdisplay.Display()
    root = d.screen().root
    win = d.create_resource_object("window", win_id)
    tx, ty, tw, th, root_depth = abs_rect(root, win)

    # root children in stacking order (bottom first) -> composite in that order
    canvas = Image.new("RGBA", (tw, th), (0, 0, 0, 255))
    for k in root.query_tree().children:
        try:
            attrs = k.get_attributes()
            kx, ky, kw, kh, depth = abs_rect(root, k)
            if attrs.map_state != X.IsViewable or depth != root_depth:
                continue
        except Exception:
            continue
        if kw <= 0 or kh <= 0:
            continue
        if k.id == win_id:
            ox, oy, w, h, dx, dy = 0, 0, tw, th, 0, 0
        else:
            ix0, iy0 = max(kx, tx), max(ky, ty)
            ix1, iy1 = min(kx + kw, tx + tw), min(ky + kh, ty + th)
            if ix1 <= ix0 or iy1 <= iy0:
                continue
            ox, oy = ix0 - kx, iy0 - ky
            w, h = ix1 - ix0, iy1 - iy0
            dx, dy = ix0 - tx, iy0 - ty
        try:
            raw = k.get_image(ox, oy, w, h, X.ZPixmap, 0xFFFFFFFF)
        except Exception:
            continue
        im = Image.frombytes("RGB", (w, h), raw.data, "raw", "BGRX").convert("RGBA")
        canvas.alpha_composite(im, (dx, dy))

    canvas.convert("RGB").save(out)
    print(f"saved {out} ({tw}x{th})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
