# Prompts used to build Reel Monster

The prompts I gave the AI agent (Claude Code), in order, with a short note on what each one produced.

---

## Prompt 1: plan and first working version

> WHO: You are an experienced software developer helping me complete a GenAI internship project.
> WHAT: I am building Project 6 — Reel Monster (Try going with the automatic one , the manual one doesnt really seem convenient) from the attached Week 2 Projects PDF.
> GOAL: Meet every mandatory "What to build" feature and every "Done when" condition. Do not add optional extras or overengineer the project.
> DO:
> 1. Read the relevant PDF page thoroughly.
> 2. Identify all mandatory requirements.
> 3. List external software, dependencies, permissions and test files required.
> 4. Recommend the simplest practical implementation.
> 5. Build the working project incrementally.
> 6. Give me exact installation and execution commands.
> 7. Help me test every mandatory requirement.
> 8. Create a README with installation, execution and test instructions.
> 9. Help maintain a prompts.md file documenting the prompts used.
>
> RULES:
> * Prefer free, offline functionality without paid APIs.
> * Use sample data wherever possible.
> * Keep the code simple and understandable for a beginner.
> * Avoid unnecessary frameworks, files and dependencies.
> * Do not claim a feature works without testing it.
> * Do not replace mandatory working features with mockups.
> * Ask me to perform real-device or browser tests when necessary.
> * Explain what each major component does so I can present it.
>
> First, produce a short implementation plan and the exact external requirements. Then begin with the simplest working version.

**Result:**
- **Brief:** the agent found the brief in `Downloads\Week-2-Projects.pdf`, read page 8, and listed the 6 "What to build" features and the 5 "Done when" checks.
- **Approach:** it chose an Android **accessibility service**. This one piece can:
  - tell when Instagram or YouTube is in front,
  - see the reels list scroll, which makes automatic counting (Level 2) possible,
  - draw the floating bubble on top.

  So the app needs no permissions at all, not even internet.
- **Build tools:** plain Java and a PowerShell build script (`build.ps1`) using the Android SDK tools directly. There is no Android Studio and no Gradle.
- **Setup:** after I agreed, it downloaded Microsoft OpenJDK 17 and the Android SDK command-line tools, accepted the SDK licence, and installed `platform-tools`, `android-34` and `build-tools 34.0.0`.
- **Code:** it wrote the monster service, the counting logic, the monster drawing with funny lines, saved daily stats, and the app screen (today, time lost, last 7 days).
- **Tests on the laptop:** the counting logic passed all 10 tests, including 20 fake reels counted as 20 for both kinds of scroll event. The APK built (21 KB), its signature verified, and `aapt` showed it has no permissions.
- **Build fix:** the first build failed because Java lambdas aren't supported in this simple build. The agent replaced the two lambdas with ordinary anonymous classes.
- **Still to do:** test on my real phone.

---

## Prompt 2: phone plugged in

> Phone is plugged in

**Result:**
- **Phone:** the agent found a Nothing Phone (A059) on Android 17 with Instagram and YouTube installed.
- **Install and screen:** it installed the app with `.\build.ps1 -Install`, and a screenshot over adb showed the app screen.
- **Log:** it started recording the monster's log, then asked me to switch the monster on in Accessibility settings.

---

## Prompt 3: let the agent drive the phone

> try doing it now
> i have given it acessibility button

**Result:** the agent drove the phone over USB (`adb shell input swipe`) and read the monster's log.
- **Instagram worked straight away.** The Reels list (`clips_viewer_view_pager`) reports its position, so each new reel counted once. **20 reels → +20.**
- **YouTube Shorts counted nothing at first.** Its list is marked "not scrollable" and sends no scroll events. A screen dump showed that each short sits in its own page view (`reel_player_page_container`). The agent changed YouTube counting to: "when a different page fills the screen, it's a new short". This is checked at most every 0.4 s. **Result:** 19 counted out of 19 shorts that appeared. One of the 20 automated swipes didn't move YouTube on; the log showed every channel that appeared.
- **Bugs found and fixed:**
  1. **The monster jumped sideways when it talked.** The bubble is now left-aligned.
  2. **The monster vanished after Android restarted the service.** It now checks which app is open as soon as it starts, and again whenever Instagram or YouTube change the screen.
  3. **The monster floated over Reel Monster's own screen.** It now hides there.
- **Testing mistake:** the agent's own `uiautomator` screen dumps paused the monster, which made counting look broken for a while. This is now a warning in the README.
- **Checked on the phone:**
  - it grows and changes colour (green at 6, red at 47, purple at 67),
  - it says the funny lines,
  - the app screen shows today's count, time lost and the 7-day bar,
  - it shows and hides correctly across Instagram, YouTube, the home screen and its own app.
- **Tests:** 12 laptop logic tests pass.

---

## Prompt 4: restart test

> Yes, restart it

**Result:**
- **Restart:** the agent restarted the phone with `adb reboot`. The count was 69 before.
- **After the restart:** the accessibility service was still switched on, and Android started it again by itself. Opening Instagram showed the monster with **69**.
- **New reels:** 3 more reels took it to 72, so the count carries on from the saved number.
- **App lock:** Instagram's app lock (Nothing applocker) appeared first, and the monster correctly hid behind it.

---

## Prompt 5: my own tests

> Doing them now

> Done, check results

> The airplane mode worked
> the fast scrolling worked too

**Result:**
- **My tests:** I did the airplane-mode test and a hand-scrolling test myself, while the agent recorded the log.
- **Log check:** the phone's log buffer had dropped the early part. The rest showed 17 Instagram reels flicked in about 6 seconds (positions 37→53), each counted exactly once. The app screen showed 123 reels and 9 minutes lost.
- **Airplane mode:** I confirmed the app and the monster worked with it on.
- **Done-when list:** every item is now ticked in the README.

---
