#!/usr/bin/env python3
"""Build a Windows icon from the *shipped* JAR, using only stdlib."""
import pathlib
import struct
import sys
import zipfile

ICON_ENTRY = "veil-original-art/pack02/malefic-veil-64.png"

def make_icon(jar, destination):
    with zipfile.ZipFile(jar) as archive:
        png = archive.read(ICON_ENTRY)
    if png[:8] != bytes.fromhex("89504e470d0a1a0a") or png[12:16] != b"IHDR":
        raise ValueError("Packaged game icon is not a PNG")
    width, height = struct.unpack(">II", png[16:24])
    if (width, height) != (64, 64):
        raise ValueError(f"Invalid application icon dimensions {width}x{height}")
    ico = struct.pack("<HHH", 0, 1, 1)
    ico += struct.pack("<BBBBHHII", width, height, 0, 0, 1, 32, len(png), 22)
    ico += png
    pathlib.Path(destination).write_bytes(ico)
    print(f"PASS MALEFIC_NATIVE_ICON size={width}x{height} bytes={len(ico)}")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("usage: make_windows_icon.py shaded.jar output.ico")
    make_icon(sys.argv[1], sys.argv[2])
