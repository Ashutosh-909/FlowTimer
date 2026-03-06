"""
Generates Play Store assets for Flow Time:
  - icon_512.png       512×512 hi-res launcher icon
  - feature_graphic.png  1024×500 feature graphic
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import random, math, os

HOURGLASS = "app/src/main/res/drawable-nodpi/hourglass.png"
FONT_PATH  = "app/src/main/res/font/press_start_2p.ttf"
OUT        = "store-assets"
os.makedirs(OUT, exist_ok=True)

# ── Palette ──────────────────────────────────────────────────────────────────
BG      = (13,  27,  42)       # #0D1B2A  SpaceBackground
WHITE   = (255, 255, 255)
CYAN    = (126, 200, 227)      # #7EC8E3  accent
GOLD    = (255, 215, 100)      # warm star glow
DIM     = ( 80, 120, 150)      # dim star

hourglass_src = Image.open(HOURGLASS).convert("RGBA")   # 1024×1536

# ─────────────────────────────────────────────────────────────────────────────
# Helper: scatter pixel-art stars
# ─────────────────────────────────────────────────────────────────────────────
def add_stars(draw, w, h, count=120, seed=42):
    rng = random.Random(seed)
    for _ in range(count):
        x = rng.randint(0, w - 1)
        y = rng.randint(0, h - 1)
        bright = rng.choice([WHITE, CYAN, DIM, DIM, DIM])
        size   = rng.choice([1, 1, 1, 2])
        draw.rectangle([x, y, x + size - 1, y + size - 1], fill=bright)

# ─────────────────────────────────────────────────────────────────────────────
# Helper: paste hourglass centred in a box, return bounding rect
# ─────────────────────────────────────────────────────────────────────────────
def paste_hourglass(canvas, src, box_x, box_y, box_w, box_h, glow=False):
    """Scale hourglass to fit box while keeping aspect ratio; paste centred."""
    ow, oh = src.size                          # 1024 × 1536  (2:3)
    scale  = min(box_w / ow, box_h / oh)
    nw, nh = int(ow * scale), int(oh * scale)
    hg = src.resize((nw, nh), Image.NEAREST)

    px = box_x + (box_w - nw) // 2
    py = box_y + (box_h - nh) // 2

    if glow:
        # Soft cyan glow behind the hourglass
        glow_layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
        mask = hg.split()[3]                   # alpha channel
        glow_col = Image.new("RGBA", (nw, nh), (*CYAN, 120))
        glow_layer.paste(glow_col, (px, py), mask)
        glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(radius=18))
        canvas.alpha_composite(glow_layer)

    canvas.alpha_composite(hg, (px, py))
    return px, py, nw, nh

# ═════════════════════════════════════════════════════════════════════════════
# 1.  512 × 512 Hi-res Icon
# ═════════════════════════════════════════════════════════════════════════════
W, H = 512, 512
icon = Image.new("RGBA", (W, H), (*BG, 255))
draw = ImageDraw.Draw(icon)

add_stars(draw, W, H, count=80)

# Hourglass fills 68 % of the icon (≈ safe-zone friendly)
paste_hourglass(icon, hourglass_src,
                box_x=0, box_y=0, box_w=W, box_h=H,
                glow=True)

icon_rgb = icon.convert("RGB")
icon_rgb.save(f"{OUT}/icon_512.png", "PNG")
print(f"Saved {OUT}/icon_512.png  ({W}×{H})")

# ═════════════════════════════════════════════════════════════════════════════
# 2.  1024 × 500 Feature Graphic
# ═════════════════════════════════════════════════════════════════════════════
FW, FH = 1024, 500
feat = Image.new("RGBA", (FW, FH), (*BG, 255))
draw = ImageDraw.Draw(feat)

add_stars(draw, FW, FH, count=160, seed=7)

# ── Hourglass on the right half ──────────────────────────────────────────────
paste_hourglass(feat, hourglass_src,
                box_x=560, box_y=20, box_w=420, box_h=460,
                glow=True)

# ── Subtle horizontal gradient overlay on the left so text pops ─────────────
grad = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
for x in range(560):
    alpha = int(160 * (1 - x / 560))
    ImageDraw.Draw(grad).line([(x, 0), (x, FH)], fill=(*BG, alpha))
feat.alpha_composite(grad)

# ── Typography ───────────────────────────────────────────────────────────────
try:
    font_title = ImageFont.truetype(FONT_PATH, 52)
    font_sub   = ImageFont.truetype(FONT_PATH, 18)
    font_tag   = ImageFont.truetype(FONT_PATH, 13)
except Exception as e:
    print(f"Font load failed ({e}), using default")
    font_title = font_sub = font_tag = ImageFont.load_default()

draw = ImageDraw.Draw(feat)

# App name — two lines so it fits the pixel-art style
draw.text((54, 90),  "FLOW",   font=font_title, fill=WHITE)
draw.text((54, 158), "TIME",   font=font_title, fill=CYAN)

# Tagline
draw.text((56, 250), "pixel-art focus timer",    font=font_sub, fill=WHITE)
draw.text((56, 286), "start from your widget",   font=font_sub, fill=DIM)
draw.text((56, 322), "or Wear OS tile.",          font=font_sub, fill=DIM)

# Tiny bottom badge
draw.text((56, 440), "NO ADS · NO ACCOUNTS · OFFLINE",
          font=font_tag, fill=(*CYAN, 180))

feat_rgb = feat.convert("RGB")
feat_rgb.save(f"{OUT}/feature_graphic.png", "PNG")
print(f"Saved {OUT}/feature_graphic.png  ({FW}×{FH})")
