#!/usr/bin/env python3
"""Synthetic X11 input (XTEST via python-xlib) into one of the LAN game windows.

Usage:
  input.py <lan_dir> <p1|p2> click <x> <y>     # left click at window-relative coords
  input.py <lan_dir> <p1|p2> rclick <x> <y>    # right click
  input.py <lan_dir> <p1|p2> key <keysym>      # e.g. Return, Escape, Tab, BackSpace
  input.py <lan_dir> <p1|p2> type <text>       # type ASCII text
  input.py <lan_dir> <p1|p2> move <x> <y>      # move pointer only
  input.py <lan_dir> <p1|p2> probe <x> <y>     # debug: where does the pointer land
Coordinates are relative to the game window's client-area top-left corner.
"""
import os
import subprocess
import sys

from Xlib import X, XK, display as xdisplay
from Xlib.ext import xtest


def main() -> int:
    lan_dir, target, cmd = sys.argv[1], sys.argv[2], sys.argv[3]
    args = sys.argv[4:]

    # LAN_DISPLAY wins over the ambient DISPLAY (the agent shell usually has
    # DISPLAY=:0 while the stack runs on a private Xvfb).
    os.environ["DISPLAY"] = os.environ.get("LAN_DISPLAY") or os.environ.get("DISPLAY", ":0")

    with open(f"{lan_dir}/run/{target}.win") as f:
        win_id = int(f.read().strip(), 0)

    d = xdisplay.Display()
    root = d.screen().root
    win = d.create_resource_object("window", win_id)

    # absolute client-area origin: walk up the window tree summing offsets
    abs_x, abs_y = 0, 0
    top = win
    cur = win
    while cur.id != root.id:
        g = cur.get_geometry()
        abs_x += g.x
        abs_y += g.y
        top = cur
        cur = cur.query_tree().parent
    geom = win.get_geometry()

    def focus():
        # WM-level activation (raises the top-level frame and focuses it),
        # then a direct raise + focus as a fallback
        subprocess.run(["wmctrl", "-i", "-a", hex(win_id)], check=False)
        top.configure(stack_mode=X.Above)
        win.set_input_focus(X.RevertToParent, X.CurrentTime)
        d.sync()

    def warp(px, py):
        xtest.fake_input(d, X.MotionNotify, x=abs_x + px, y=abs_y + py)
        d.sync()

    def button(btn, press):
        xtest.fake_input(d, X.ButtonPress if press else X.ButtonRelease, btn)
        d.sync()

    if cmd == "probe":
        px, py = int(args[0]), int(args[1])
        warp(px, py)
        pt = root.query_pointer()
        child = pt.child
        print(f"window {hex(win_id)} client origin=({abs_x},{abs_y}) size={geom.width}x{geom.height}")
        print(f"pointer now at root=({pt.root_x},{pt.root_y}) top-level child={hex(child.id) if child and child.id != X.NONE else 'none'}")
        return 0

    if cmd == "click" or cmd == "rclick":
        import time
        px, py = int(args[0]), int(args[1])
        btn = 1 if cmd == "click" else 3
        focus()
        warp(px, py)
        time.sleep(0.3)
        button(btn, True)
        time.sleep(0.1)
        button(btn, False)
    elif cmd == "move":
        focus()
        warp(int(args[0]), int(args[1]))
    elif cmd == "key":
        import time
        focus()
        keysym = XK.string_to_keysym(args[0])
        keycode = d.keysym_to_keycode(keysym)
        xtest.fake_input(d, X.KeyPress, keycode)
        time.sleep(0.05)
        xtest.fake_input(d, X.KeyRelease, keycode)
        d.sync()
    elif cmd == "type":
        import time
        focus()
        text = args[0]
        shift = d.keysym_to_keycode(XK.string_to_keysym("Shift_L"))
        for ch in text:
            keysym = ord(ch)
            keycode = d.keysym_to_keycode(keysym)
            if keycode == 0:
                print(f"no keycode for {ch!r}, skipped", file=sys.stderr)
                continue
            shifted = ch.isupper() or ch in "!@#$%^&*()_+{}|:\"<>?~"
            if shifted:
                xtest.fake_input(d, X.KeyPress, shift)
            xtest.fake_input(d, X.KeyPress, keycode)
            time.sleep(0.03)
            xtest.fake_input(d, X.KeyRelease, keycode)
            if shifted:
                xtest.fake_input(d, X.KeyRelease, shift)
            d.sync()
    else:
        print(f"unknown command {cmd!r}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
