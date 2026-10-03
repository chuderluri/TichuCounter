import os

from PIL import Image

SRC = "art/IconMaxResolution.png"
DST = "fastlane/metadata/android/en-US/images/icon.png"
SIZE = 512

img = Image.open(SRC).convert("RGBA")
w, h = img.size
side = min(w, h)
img = img.crop(((w - side) // 2, (h - side) // 2, (w + side) // 2, (h + side) // 2))
img = img.resize((SIZE, SIZE), Image.LANCZOS)
os.makedirs(os.path.dirname(DST), exist_ok=True)
img.save(DST)
print("saved", DST, img.size)