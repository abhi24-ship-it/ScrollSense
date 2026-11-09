# LOGO STAGES - STRICT AGENT BUILD INSTRUCTIONS

## Source-of-truth precedence
1. ORIGINAL_handwritten_stage_rules.jpg — authoritative stage/color definitions, plus user instruction: first two eye shapes are calm/horizontal.
2. ORIGINAL_digitized_black_logo.png — general emblem geometry reference.
3. Four numbered stage images — illustrations for corresponding states.

**Do not redesign the logo.** The SAME master emblem/geometry must be reused across all four states. Do not silently re-generate each stage or introduce geometry differences. No new symbols, text, eyes, gradients, icons, effects, animation, or color variations unless explicitly requested.

## Handwritten element key
1 = the face
2 = the eyes
3 = the background (BG)

## Stage table — transcribed from the user's handwritten note

| Stage | Range as handwritten | Face (1) | Eyes (2) | BG (3) | Eye shape |
|---|---|---|---|---|---|
| Initial stage | 1–20 | Blue (Cyan) | Black | Grey | Calm, horizontal |
| Mid stage | 20–60 | Indigo | Black | Grey | Calm, horizontal |
| Final stage | 60–80 | Red | Black | Grey | Sharp, angled, like original digitized reference |
| Once it reaches 80 | 80 reached | Black | Red | Grey | Sharp, angled, like original digitized reference |

The outer circular border remains black for every stage. Grey background persists throughout.

## Implementation requirements
- Use one base geometry with two pre-defined eye shapes: calm/horizontal for stages 1–2, and sharp/angled for stages 3–4. Do not change other shapes.
- Only switch the color roles and selected eye shape at transitions. Keep logo size, position, outline thickness, and emblem composition invariant.
- Use the actual instructions (Blue/Cyan, Indigo, Red, Black, Grey). The photos did not prescribe exact HEX codes: do not treat arbitrary sampled previews as mandatory numeric color specifications.
- Stage 4 must use a BLACK FACE and RED EYES; grey is the background/negative-space color, not the face fill.
- The handwritten ranges share transition endpoints at 20, 60, and 80. Do NOT silently invent different thresholds or a different scoring/progression metric; clarify if the implementation requires exact inclusive/exclusive comparisons.
- Any implementation must preserve existing surrounding project behavior; this brief only specifies appearance/stage mapping.

## Agent acceptance checklist
- Same emblem geometry throughout.
- 4 stage states corresponding to the note.
- Stage 1: cyan face, black calm eyes, grey BG.
- Stage 2: indigo face, black calm eyes, grey BG.
- Stage 3: red face, black sharp eyes, grey BG.
- Stage 4: black face, red sharp eyes, grey BG.
- Circular black border on all 4 stages.
- No inventions beyond these requirements.
