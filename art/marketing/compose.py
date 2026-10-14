#!/usr/bin/env python3
"""Composes store-ready marketing screenshots from the raw captures in raw/<mode>/."""

import math
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).parent
RAW = ROOT / "raw"
OUT = ROOT / "output"
LOGO = ROOT.parent / "logo512.png"
FONT_DIR = Path("/usr/share/fonts/truetype/lato")

W, H = 1440, 2560
SS = 2  # supersampling factor for smooth edges
BEZEL = 22
DEVICE_W = 1080

MAGENTA = (215, 0, 215)
PURPLE = (92, 16, 120)
NIGHT = (30, 8, 48)


def font(name, size):
    return ImageFont.truetype(str(FONT_DIR / f"Lato-{name}.ttf"), size)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def background(top, bottom, accent, seed):
    """Vertical gradient with brand-style wave bands and two soft glows."""
    grad = Image.new("RGB", (1, H))
    for y in range(H):
        t = y / (H - 1)
        grad.putpixel((0, y), lerp(top, bottom, t ** 0.9))
    bg = grad.resize((W, H)).convert("RGBA")

    waves = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(waves)
    band = 150
    for i, y0 in enumerate(range(-band, H + band, band)):
        pts = [(x, y0 + 22 * math.sin(x / 70 + i * 0.6 + seed)) for x in range(-10, W + 20, 10)]
        pts += [(W + 20, H + band), (-10, H + band)]
        d.polygon(pts, fill=(255, 255, 255, 13))
    bg = Image.alpha_composite(bg, waves)

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.ellipse((-400, -300, 900, 900), fill=accent + (90,))
    g.ellipse((700, 1500, 1900, 2800), fill=(255, 120, 255, 50))
    glow = glow.filter(ImageFilter.GaussianBlur(220))
    return Image.alpha_composite(bg, glow)


def rounded_mask(size, radius):
    w, h = size
    m = Image.new("L", (w * SS, h * SS), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, w * SS - 1, h * SS - 1), radius * SS, fill=255)
    return m.resize((w, h), Image.LANCZOS)


def shadow(size, radius, blur, opacity, spread=0):
    w, h = size
    pad = blur * 3
    s = Image.new("RGBA", (w + 2 * pad, h + 2 * pad), (0, 0, 0, 0))
    ImageDraw.Draw(s).rounded_rectangle(
        (pad - spread, pad - spread, pad + w + spread, pad + h + spread), radius, fill=(10, 0, 20, opacity)
    )
    return s.filter(ImageFilter.GaussianBlur(blur)), pad


def phone(screen_path, screen_w):
    """Renders the screenshot inside a slim dark device frame."""
    shot = Image.open(screen_path).convert("RGBA")
    screen_h = round(screen_w * shot.height / shot.width)
    shot = shot.resize((screen_w, screen_h), Image.LANCZOS)
    bezel = BEZEL
    radius = 48
    fw, fh = screen_w + 2 * bezel, screen_h + 2 * bezel

    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    body = Image.new("RGBA", (fw, fh), (24, 22, 30, 255))
    frame.paste(body, (0, 0), rounded_mask((fw, fh), radius + bezel))
    # Thin highlight ring on the frame edge.
    ring = Image.new("RGBA", (fw * SS, fh * SS), (0, 0, 0, 0))
    ImageDraw.Draw(ring).rounded_rectangle(
        (2, 2, fw * SS - 3, fh * SS - 3), (radius + bezel) * SS, outline=(110, 100, 125, 255), width=3 * SS
    )
    frame = Image.alpha_composite(frame, ring.resize((fw, fh), Image.LANCZOS))
    frame.paste(shot, (bezel, bezel), rounded_mask((screen_w, screen_h), radius))
    cam = 24
    cx = fw // 2
    ImageDraw.Draw(frame).ellipse(
        (cx - cam // 2, bezel + 26, cx + cam // 2, bezel + 26 + cam), fill=(8, 8, 12, 255)
    )
    return frame


def callout(screen_path, box, width):
    """Zoomed crop of a UI region, presented as a floating card."""
    crop = Image.open(screen_path).convert("RGBA").crop(box)
    h = round(width * crop.height / crop.width)
    crop = crop.resize((width, h), Image.LANCZOS)
    card = Image.new("RGBA", (width, h), (0, 0, 0, 0))
    card.paste(crop, (0, 0), rounded_mask((width, h), 40))
    ring = Image.new("RGBA", (width * SS, h * SS), (0, 0, 0, 0))
    ImageDraw.Draw(ring).rounded_rectangle(
        (0, 0, width * SS - 1, h * SS - 1), 40 * SS, outline=(255, 255, 255, 170), width=4 * SS
    )
    return Image.alpha_composite(card, ring.resize((width, h), Image.LANCZOS))


def place(canvas, layer, xy, radius=70, blur=40, opacity=150, offset=(0, 30), angle=0.0):
    if angle:
        layer = layer.rotate(angle, resample=Image.BICUBIC, expand=True)
    sh, pad = shadow(layer.size, radius, blur, opacity)
    if angle:
        alpha = layer.getchannel("A").filter(ImageFilter.GaussianBlur(blur))
        sh = Image.new("RGBA", layer.size, (10, 0, 20, 0))
        sh.putalpha(ImageChops.multiply(alpha, Image.new("L", layer.size, opacity)))
        pad = 0
    x, y = xy
    canvas.alpha_composite(sh, (x - pad + offset[0], y - pad + offset[1]))
    canvas.alpha_composite(layer, (x, y))


def brand(canvas, y):
    icon_size = 76
    icon = Image.open(LOGO).convert("RGBA").resize((icon_size, icon_size), Image.LANCZOS)
    x = 110
    canvas.paste(icon, (x, y), rounded_mask((icon_size, icon_size), 20))
    ImageDraw.Draw(canvas).text(
        (x + icon_size + 22, y + icon_size // 2), "Aevum", font=font("Heavy", 46), fill="white", anchor="lm"
    )


def wrap(draw, text, fnt, max_w):
    lines, line = [], ""
    for word in text.split():
        trial = f"{line} {word}".strip()
        if draw.textlength(trial, font=fnt) <= max_w:
            line = trial
        else:
            lines.append(line)
            line = word
    lines.append(line)
    return lines


def headline(canvas, title, subtitle, y=300):
    d = ImageDraw.Draw(canvas)
    tf = font("Black", 108)
    sf = font("Regular", 50)
    for line in title.split("\n"):
        d.text((110, y), line, font=tf, fill="white")
        y += 118
    y += 24
    for line in wrap(d, subtitle, sf, W - 240):
        d.text((110, y), line, font=sf, fill=(255, 235, 255, 225))
        y += 66
    return y


SCREENS = [
    dict(
        out="01_track_time",
        title="Track Jira time\nwith one tap",
        subtitle="Tap a ticket and the timer starts. Aevum keeps counting while you work.",
        shot=("light", "02_timer"),
        back=("dark", "05_pending"),
        palette=(MAGENTA, PURPLE),
    ),
    dict(
        out="02_favourites",
        title="Your tickets,\nalways at hand",
        subtitle="Pin the issues you work on most and see today's progress at a glance.",
        shot=("dark", "01_favourites"),
        callout=dict(box=(0, 210, 1080, 380), zoom=1.25, dy=1140),
        palette=(PURPLE, NIGHT),
    ),
    dict(
        out="03_summary_note",
        title="Wrap up with\na quick note",
        subtitle="Stop the timer, then type or dictate what you did. Done in seconds.",
        shot=("light", "03_stop_dialog"),
        callout=dict(box=(122, 825, 961, 1570), zoom=1.4),
        palette=(MAGENTA, PURPLE),
    ),
    dict(
        out="04_pending_upload",
        title="Review, then send\nit all to Jira",
        subtitle="See pending time per issue and upload every worklog with one button.",
        shot=("light", "05_pending"),
        callout=dict(box=(42, 262, 1038, 867), zoom=1.3, dy=60),
        palette=(PURPLE, NIGHT),
    ),
    dict(
        out="05_calendar",
        title="Know where your\nweek stands",
        subtitle="See at a glance which days hit your target, fell short or ran into overtime.",
        shot=("dark", "06_summary"),
        callout=dict(box=(0, 625, 1080, 1145), zoom=1.22, dy=40),
        palette=(MAGENTA, PURPLE),
    ),
    dict(
        out="06_day_details",
        title="Every minute,\naccounted for",
        subtitle="Open any day to see each worklog with its ticket, time range and comment.",
        shot=("light", "07_day"),
        callout=dict(box=(42, 262, 1038, 457), zoom=1.3, dy=20),
        palette=(PURPLE, NIGHT),
    ),
    dict(
        out="07_split",
        title="Split time\nbetween tickets",
        subtitle="Switched tasks halfway? Divide a worklog across two issues with a slider.",
        shot=("dark", "05_pending"),
        callout_shot=("light", "09_split"),
        callout=dict(box=(0, 234, 1080, 700), zoom=1.25, center_y=1450),
        palette=(MAGENTA, PURPLE),
    ),
    dict(
        out="08_schedule",
        title="Your schedule,\nyour targets",
        subtitle="Set hours per day and working days. Daily and weekly goals follow along.",
        shot=("light", "08_settings"),
        callout=dict(box=(42, 1075, 1038, 1935), zoom=1.3),
        palette=(PURPLE, NIGHT),
    ),
]


def compose(spec, index):
    top, bottom = spec["palette"]
    canvas = background(top, bottom, (255, 90, 255), seed=index)
    brand(canvas, 150)
    text_bottom = headline(canvas, spec["title"], spec["subtitle"])

    screen_w = 860
    phone_top = text_bottom + 80
    shot_path = RAW / spec["shot"][0] / f"{spec['shot'][1]}.png"

    if "back" in spec:
        back = phone(RAW / spec["back"][0] / f"{spec['back'][1]}.png", 760)
        place(canvas, back, (720, phone_top + 120), opacity=120, angle=-8)
        front = phone(shot_path, 800)
        place(canvas, front, (90, phone_top + 20), opacity=170, angle=4)
    else:
        front = phone(shot_path, screen_w)
        phone_x = (W - front.width) // 2
        place(canvas, front, (phone_x, phone_top), opacity=170)

    if "callout" in spec:
        c = spec["callout"]
        src = spec.get("callout_shot", spec["shot"])
        left, top_, right, bottom_ = c["box"]
        scale = screen_w / DEVICE_W
        width = round((right - left) * scale * c["zoom"])
        card = callout(RAW / src[0] / f"{src[1]}.png", c["box"], width)
        # Centered on the original element so the card reads as a magnified pop-out.
        cx = phone_x + BEZEL + (left + right) / 2 * scale
        cy = phone_top + BEZEL + c.get("center_y", (top_ + bottom_) / 2) * scale + c.get("dy", 0)
        x = int(min(max(cx - card.width / 2, 40), W - card.width - 40))
        y = int(cy - card.height / 2)
        place(canvas, card, (x, y), radius=40, blur=34, opacity=190, offset=(0, 28))

    OUT.mkdir(exist_ok=True)
    path = OUT / f"{spec['out']}.png"
    canvas.convert("RGB").save(path, optimize=True)
    print(path)


if __name__ == "__main__":
    for i, spec in enumerate(SCREENS):
        compose(spec, i)
