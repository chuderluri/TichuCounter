import os
from PIL import Image

SRC = "art/IconMaxResolution.png"
RES = "app/src/main/res"

DENSITY = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}

LEGACY_DP = 48
FOREGROUND_DP = 108
SAFE_DP = 66

img = Image.open(SRC).convert("RGBA")
w, h = img.size
size = min(w, h)
img = img.crop(((w - size) // 2, (h - size) // 2, (w + size) // 2, (h + size) // 2))


def px(dp, factor):
    return max(1, round(dp * factor))


def render_legacy():
    for name, f in DENSITY.items():
        d = os.path.join(RES, "mipmap-" + name)
        os.makedirs(d, exist_ok=True)
        s = px(LEGACY_DP, f)
        resized = img.resize((s, s), Image.LANCZOS)
        for base in ("ic_launcher", "ic_launcher_round"):
            resized.save(os.path.join(d, base + ".png"))
        print("  mipmap-" + name, s)


def render_foreground():
    for name, f in DENSITY.items():
        d = os.path.join(RES, "mipmap-" + name)
        os.makedirs(d, exist_ok=True)
        canvas_px = px(FOREGROUND_DP, f)
        content_px = px(SAFE_DP, f)
        resized = img.resize((content_px, content_px), Image.LANCZOS)
        canvas = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
        canvas.paste(resized, ((canvas_px - content_px) // 2, (canvas_px - content_px) // 2))
        canvas.save(os.path.join(d, "ic_launcher_foreground.png"))
        print("  mipmap-" + name, canvas.size)


print("legacy launcher icons")
render_legacy()
print("adaptive foregrounds")
render_foreground()