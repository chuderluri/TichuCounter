import math
import re
from PIL import Image, ImageDraw

W = 512
VIEW = 108
S = W / VIEW

TEAL = (0x00, 0x69, 0x5C, 255)
BLUE = (0x15, 0x65, 0xC0, 255)
WHITE = (0xFF, 0xFF, 0xFF, 255)

img = Image.new("RGBA", (W, W), TEAL)
d = ImageDraw.Draw(img)

def P(x, y):
    return (x * S, y * S)

def scale(pts):
    return [P(x, y) for x, y in pts]

# Medallion (blue circle)
d.ellipse([P(54 - 30, 54 - 30), P(54 + 30, 54 + 30)], fill=BLUE)

def parse(path):
    toks = re.findall(r"[MLCZ]|-?\d+\.?\d*", path)
    out = []
    i = 0
    def num():
        nonlocal i
        v = float(toks[i]); i += 1; return v
    while i < len(toks):
        c = toks[i]
        if c in ("M", "L"):
            i += 1; x, y = num(), num(); out.append((c, x, y))
        elif c == "C":
            i += 1; out.append(("C", num(), num(), num(), num(), num(), num()))
        elif c == "Z":
            out.append(("Z",)); i += 1
        else:
            raise ValueError(c)
    return out

def flatten(ops, samples=24):
    pts = []
    cur = None
    start = None
    for op in ops:
        if op[0] == "M":
            cur = (op[1], op[2]); start = cur; pts.append(cur)
        elif op[0] == "L":
            nxt = (op[1], op[2]); pts.append(nxt); cur = nxt
        elif op[0] == "C":
            p0 = cur
            p1 = (op[1], op[2]); p2 = (op[3], op[4]); p3 = (op[5], op[6])
            for k in range(1, samples + 1):
                t = k / samples
                mt = 1 - t
                x = (mt**3)*p0[0] + 3*(mt**2)*t*p1[0] + 3*mt*(t**2)*p2[0] + (t**3)*p3[0]
                y = (mt**3)*p0[1] + 3*(mt**2)*t*p1[1] + 3*mt*(t**2)*p2[1] + (t**3)*p3[1]
                pts.append((x, y))
            cur = p3
        elif op[0] == "Z":
            pts.append(start); cur = start
    return pts

# Dragon head (white)
head = "M54,22 C61,22 68,26 71,33 L77,20 L81,30 C86,33 88,42 82,48 L84,56 L68,57 C66,61 60,63 54,63 C48,63 42,61 40,57 L24,56 L26,48 C20,42 22,33 27,30 L31,20 L37,33 C40,26 47,22 54,22 Z"
d.polygon(scale(flatten(parse(head))), fill=WHITE)

# Eyes (teal)
for cx in (40, 68):
    r = 3
    d.ellipse([P(cx - r, 40 - r), P(cx + r, 40 + r)], fill=TEAL)

# "1000" strokes
d.line([P(42, 67), P(45, 63), P(45, 79)], fill=WHITE, width=round(4 * S), joint="curve")
for cx in (54, 63, 72):
    rw, rh = 4, 8
    d.ellipse([P(cx - rw, 64 - rh), P(cx + rw, 64 + rh)], outline=WHITE, width=round(4 * S))

img.save("fastlane/metadata/android/en-US/images/icon.png")
print("saved", img.size)