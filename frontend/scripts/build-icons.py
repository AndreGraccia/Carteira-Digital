"""Rasterises the Wall.ia mark (same geometry as public/favicon.svg) into the PNG/ICO favicons.

Run from the repo root:  python frontend/scripts/build-icons.py   (needs Pillow)
"""
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / 'public'
SURFACE, TEXT, HORIZON, VOLT = '#10141C', '#E9EDF3', '#5CE1E6', '#D4FF3A'
SS = 8  # supersampling factor for smooth edges


def mark(size, padding=0.0, background=None):
    canvas = size * SS
    img = Image.new('RGBA', (canvas, canvas), background or (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    inner = canvas * (1 - 2 * padding)
    offset = canvas * padding
    s = inner / 32

    def p(x, y):
        return offset + x * s, offset + y * s

    draw.rounded_rectangle([p(0, 0), p(32, 32)], radius=9 * s, fill=SURFACE)
    width = round(2.6 * s)
    points = [p(6.5, 10), p(11, 21.5), p(16, 13.5), p(21, 21.5)]
    draw.line(points, fill=TEXT, width=width, joint='curve')
    for x, y in points[::3] + points[1:3]:
        r = width / 2
        draw.ellipse([x - r, y - r, x + r, y + r], fill=TEXT)
    # dotted projection towards the future point
    for x, y in (p(22.55, 17.75), p(24.05, 14.15)):
        r = 1.3 * s
        draw.ellipse([x - r, y - r, x + r, y + r], fill=HORIZON)
    cx, cy = p(25.6, 10.4)
    r = 2.6 * s
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=VOLT)
    return img.resize((size, size), Image.LANCZOS)


def main():
    icons = OUT / 'favicon'
    for name, size in [('favicon-16x16.png', 16), ('favicon-32x32.png', 32), ('android-chrome-192x192.png', 192),
                       ('android-chrome-512x512.png', 512)]:
        mark(size).save(icons / name)
    # iOS and Windows tiles need an opaque, padded square.
    mark(180, padding=0.12, background='#0A0D13').save(icons / 'apple-touch-icon.png')
    mark(150, padding=0.2, background='#0A0D13').save(icons / 'mstile-150x150.png')
    mark(512, padding=0.12, background='#0A0D13').save(OUT / 'logo.png')
    mark(64).save(icons / 'favicon.ico', sizes=[(16, 16), (32, 32), (48, 48), (64, 64)])
    print('icons written to', icons)


if __name__ == '__main__':
    main()
