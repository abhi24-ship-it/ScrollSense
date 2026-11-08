"""
Makes the 4 logo stage images for ScrollSense from ONE master logo.

    python logo/make_logo_stages.py

Input  (logo/source/):
    master_logo.png          - the digitized black logo: the ONE geometry used for every stage
    calm_eyes_reference.png  - stage-1 illustration, used ONLY to copy the calm horizontal eye shape
Output (res/drawable-nodpi/):
    logo_stage1.png ... logo_stage4.png   (512 x 512, round: transparent outside the circle)

Stage rules (from the handwritten note in logo/source/):
    stage 1 (0-19 reels)  : face cyan,   eyes black, calm eyes
    stage 2 (20-59)       : face indigo, eyes black, calm eyes
    stage 3 (60-79)       : face red,    eyes black, sharp angled eyes (as in the master)
    stage 4 (80+)         : face black,  eyes red,   sharp angled eyes
    every stage           : black outer circle, then a thin grey gap and a thin black border
                            around it, so it looks like an emblem
    Outside the border is transparent and the picture is cropped to it, so the logo is
    shown alone as a round emblem (the user asked for no grey square). Grey is still used for the
    inner strokes in stage 4, where the black face would otherwise hide them.

How it works: the master is split into parts (masks), then each part is painted a colour.
    background  = everything outside the outer circle (made transparent)
    ring        = the thick black outer circle
    lines       = the other black strokes inside the circle
    face        = the white areas inside the circle
    angled eyes = the two white almonds between the slanted eye strokes
    calm eyes   = the two horizontal almonds taken from the reference picture
Only colours and the eye shape change between stages; size and position never change.
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(HERE, "source")
OUT = os.path.join(HERE, "..", "res", "drawable-nodpi")
SIZE = 512  # final image size in pixels

# Colours named in the handwritten note (no exact codes were given, so standard ones are used)
GREY = (128, 128, 128)
BLACK = (0, 0, 0)
CYAN = (0, 191, 255)
INDIGO = (75, 0, 130)
RED = (255, 0, 0)

#          file name       face    eyes   eye shape  inner lines (stage 4: grey, or the black face would hide them)
STAGES = [("logo_stage1", CYAN,   BLACK, "calm",    BLACK),
          ("logo_stage2", INDIGO, BLACK, "calm",    BLACK),
          ("logo_stage3", RED,    BLACK, "angled",  BLACK),
          ("logo_stage4", BLACK,  RED,   "angled",  GREY)]

# Points that sit inside each eye almond (checked by hand on the 1254 x 1254 pictures)
ANGLED_EYE_SEEDS = [(480, 560), (760, 560)]
CALM_EYE_SEEDS = [(450, 540), (810, 540)]
# Each angled eye almond is open at its inner tip (the two strokes end side by side, ~30 px apart).
# These short "door" lines close the openings ONLY while filling the eye; they are never drawn.
BORDER_GAP = 18    # emblem border: grey gap between the logo's circle and the thin border (px)
BORDER_WIDTH = 14  # emblem border: thickness of the thin black outer circle (px, on the 1254 px master)
EYE_DOORS = [((548, 652), (614, 636)), ((646, 636), (712, 652))]


def flood(mask, seed):
    """All pixels of `mask` connected to `seed` (like the paint-bucket tool)."""
    img = Image.fromarray(np.where(mask, 255, 0).astype(np.uint8)).copy()  # copy: fromarray is read-only
    ImageDraw.floodfill(img, seed, 128)
    return np.array(img) == 128


def grow(mask, pixels):
    """Make a mask a few pixels fatter (pixels must be odd)."""
    img = Image.fromarray(np.where(mask, 255, 0).astype(np.uint8))
    return np.array(img.filter(ImageFilter.MaxFilter(pixels))) > 0


def shrink(mask, pixels):
    """Make a mask a few pixels thinner (removes thin slivers)."""
    img = Image.fromarray(np.where(mask, 255, 0).astype(np.uint8))
    return np.array(img.filter(ImageFilter.MinFilter(pixels))) > 0


def keep_big(mask, min_pixels):
    """Only the separate pieces of `mask` that have at least `min_pixels` pixels."""
    left = mask.copy()
    big = np.zeros_like(mask)
    while left.any():
        y, x = np.argwhere(left)[0]
        piece = flood(left, (int(x), int(y)))
        if piece.sum() >= min_pixels:
            big |= piece
        left &= ~piece
    return big


def load_gray(name):
    return np.array(Image.open(os.path.join(SOURCE, name)).convert("L")).astype(int)


def build_masks():
    master = load_gray("master_logo.png")
    calm_ref = load_gray("calm_eyes_reference.png")
    if master.shape != calm_ref.shape:
        sys.exit("master and calm reference must be the same size")
    h, w = master.shape
    ink = master < 128

    background = flood(~ink, (0, 0))
    inside = ~background

    # Outer circle: centre and radius from the area inside it, ring thickness from a scan line
    ys, xs = np.nonzero(inside)
    cx, cy = (xs.min() + xs.max()) / 2, (ys.min() + ys.max()) / 2
    radius = (xs.max() - xs.min()) / 2
    row = ink[int(cy)]
    thickness = int(np.argmin(row[xs.min():])) + 2
    yy, xx = np.mgrid[0:h, 0:w]
    dist = np.hypot(xx - cx, yy - cy)
    ring = ink & (dist >= radius - thickness)

    # Emblem border: a thin black circle around the logo, with a small grey gap in between
    gap = (dist <= radius + BORDER_GAP) & ~inside
    border = (dist > radius + BORDER_GAP) & (dist <= radius + BORDER_GAP + BORDER_WIDTH)

    # Sharp angled eyes = the white almonds inside the slanted eye strokes.
    # Close each almond's opening with a door line first, or the fill leaks into the whole face.
    doors = Image.new("L", (w, h), 0)
    for a, b in EYE_DOORS:
        ImageDraw.Draw(doors).line([a, b], fill=255, width=7)
    walls = ink | (np.array(doors) > 0)
    angled_eyes = np.zeros_like(ink)
    for seed in ANGLED_EYE_SEEDS:
        if walls[seed[1], seed[0]]:
            sys.exit(f"angled eye seed {seed} is not inside an eye")
        eye = flood(~walls, seed)
        if eye.sum() > 30000:
            sys.exit(f"eye at {seed} leaked into the face ({int(eye.sum())} px): check EYE_DOORS")
        angled_eyes |= eye
    angled_eyes = grow(angled_eyes, 9) & ~ink  # also colour the pixels under the door lines

    # Calm horizontal eyes = the dark almonds in the reference picture
    calm_dark = calm_ref < 60
    calm_eyes = np.zeros_like(ink)
    for seed in CALM_EYE_SEEDS:
        if not calm_dark[seed[1], seed[0]]:
            sys.exit(f"calm eye seed {seed} is not inside an eye")
        calm_eyes |= flood(calm_dark, seed)

    # For calm stages the slanted eye strokes (with their long tails) are removed, like in the
    # reference: they are the BIG shapes that are black in the master but face colour in the
    # reference. Thin slivers along every edge (the reference is drawn a few px off) are ignored.
    master_only = ink & (calm_ref > 100)
    big_parts = keep_big(shrink(master_only, 9), min_pixels=2000)  # thick strokes and tails
    hugging_eyes = master_only & grow(angled_eyes, 41)            # thin strokes along the almonds
    strokes = grow(big_parts | hugging_eyes, 61) & master_only    # grow back edges and thin tips
    # Where a removed tail meets a kept brow, a few px of jagged black can stay behind: clean up
    # leftover black thinner than 7 px, but only right next to the removed strokes.
    kept = ink & ~strokes
    thin_leftovers = kept & ~grow(shrink(kept, 7), 7) & grow(strokes, 21)
    strokes |= thin_leftovers

    for name, mask in [("angled eyes", angled_eyes), ("calm eyes", calm_eyes), ("removed strokes", strokes)]:
        print(f"  {name}: {int(mask.sum())} px")
    return dict(background=background, inside=inside, ink=ink, ring=ring, gap=gap, border=border,
                angled_eyes=angled_eyes, calm_eyes=calm_eyes, strokes=strokes)


def render(m, face, eyes, shape, lines):
    h, w = m["ink"].shape
    img = np.zeros((h, w, 4), dtype=np.uint8)       # RGBA; starts fully transparent
    img[m["inside"] & ~m["ink"]] = face + (255,)    # face
    img[m["inside"] & m["ink"]] = lines + (255,)    # inner strokes
    if shape == "calm":
        img[m["strokes"]] = face + (255,)           # slanted eye strokes removed
        img[m["calm_eyes"]] = eyes + (255,)
    else:
        img[m["angled_eyes"]] = eyes + (255,)
    img[m["ring"]] = BLACK + (255,)                 # outer circle always black
    img[m["gap"]] = GREY + (255,)                   # emblem border: grey gap...
    img[m["border"]] = BLACK + (255,)               # ...and a thin black circle around it
    # outside that stays transparent (its colour is black, so the shrunk edge has no grey halo)

    # crop to the border (plus a 2 px margin) so the emblem fills the picture
    ys, xs = np.nonzero(m["inside"] | m["gap"] | m["border"])
    left, top, right, bottom = xs.min() - 2, ys.min() - 2, xs.max() + 3, ys.max() + 3
    return Image.fromarray(img, "RGBA").crop((left, top, right, bottom)).resize((SIZE, SIZE), Image.LANCZOS)


def main():
    print("Reading master logo...")
    masks = build_masks()
    os.makedirs(OUT, exist_ok=True)
    for name, face, eyes, shape, lines in STAGES:
        path = os.path.join(OUT, name + ".png")
        render(masks, face, eyes, shape, lines).save(path, optimize=True)
        print(f"  wrote {os.path.relpath(path)}")


if __name__ == "__main__":
    main()
