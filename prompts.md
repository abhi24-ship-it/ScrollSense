# Prompts used to build ScrollSense

The prompts I gave the AI agent (Claude Code), in order, with a short note on what each one produced.
The app was called **Reel Monster** (the brief's name) until prompt 11 renamed it to ScrollSense, so the older notes below still use the old name.

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

## Prompt 6: daily limit of 80 reels (extra feature)

> I want you to make an improvement in this
> Try adding a limit of 80 reels per day
> If the number 80 is reached make sure i cant scroll anymore
> One thing to note down is that when the max number of scrolls is met then i should not be able to sroll.  The reel option should basically be blocked when max reel count is reached.
> i should still be able to chat to people on the app and open other sections.
> Yeah i think you know what to do know

(Then: "Plugged in", "It's on", "Ready, go ahead" while the agent tested on my phone.)

**Result:**
- **New setting:** `Stats.DAILY_LIMIT = 80`. The app's Today card shows "Daily limit: 80 reels · N left", or a red "Limit reached" line.
- **First version:** at the limit, the monster pressed Back whenever the Reels/Shorts player was open. On the phone (with a test build where the limit was 4):
  - Instagram Reels closed to Home, and reopening it was blocked.
  - **Problem:** in YouTube it pressed Back twice while YouTube was still closing Shorts, which **closed YouTube completely**.
- **Final version:**
  - A full-screen **block screen** covers the player and swallows touches, so nothing can be scrolled.
  - The monster presses **Back only once** per visit (at most every 3 s).
  - The block screen disappears when the player is gone, and has a **Leave Reels** button.
  - Tested: Instagram Reels blocked and landed on Home, chats opened normally, and YouTube Shorts was blocked twice while YouTube stayed open.
- **Bug found and fixed:** the bubble came back over Reel Monster's own screen. When YouTube is left with a video playing, it shrinks into picture-in-picture, which sends a "YouTube is on screen" event. The monster now asks Android which app window has **focus**, and a picture-in-picture window never does.
- **Small fix:** a PowerShell edit added a hidden BOM character to `Stats.java`, which Java refused, so the agent rewrote the file without it.
- **Not tested:** the block screen was only on screen for under a second each time, so the agent couldn't screenshot it, and the **Leave Reels** button wasn't tested by tapping it.
- **Final build:** limit set back to 80 and reinstalled. Shorts counting still works (5 swipes → 5), and all 12 laptop tests pass.

---

## Prompt 7: logo stages from a design pack (extra feature)

> @"C:\Users\Abhishek Bhat\Downloads\Logo_Stage_Agent_Pack.zip"
> I will give you a zip file
> Follow that and build it
> I can connect the phone and u can do it when you have figured it all out

**The pack:**
- my handwritten stage rules, a digitized master logo, 4 illustration images, a reference PDF, and an agent brief;
- the main rule: one master geometry for all stages, recoloured in 4 stages (cyan → indigo → red → black with red eyes), calm eyes for stages 1–2 and sharp eyes for 3–4.

**Questions the agent asked me:**
- Thresholds → switch **at** 20, 60, 80.
- Use the logo as the app icon → **yes**.

**Result:**
- **`logo/make_logo_stages.py`** (Python + Pillow + numpy) splits the master logo into parts (background, outer circle, strokes, face, eyes) and paints them per stage. The 4 PNGs go to `res/drawable-nodpi/`.
- **Problems solved along the way:**
  - The eye almonds have a ~30 px opening at their tips, so the agent added short "door" lines used only while filling.
  - For the calm eyes, it removed the slanted eye strokes (including their long tails) by comparing the master with the calm reference, and ignored thin misalignment slivers.
  - PIL's flood fill silently did nothing on a read-only image until `.copy()` was added.
- **In the app:**
  - New `LogoStage.java` with laptop tests.
  - `MonsterView` draws the stage picture instead of the old circle monster, and still grows and shows the badge.
  - The app icon is stage 1.
- **Tested:** every preview was checked by eye on the laptop. On the phone, the stage-1 logo, the app icon and the change to indigo at 20 were confirmed (in prompt 8).

---

## Prompt 8: simpler lines, and no more GPay problems

> Could you remove the funny lines
> keep it simple and make sure there are only few lines, at the end that says "Son" when it reaches, after the 20 reels that says "Do you even remember the first reel?", after the 60 reels that says " Pack it up ". Make sure the lines dont stay for too long it should be like a popup and the line should basically go after 2 seconds.
> While i was using this i was also facing an issue where i could not make any payments while the app was running in background as gpay doesnt permit payments when an another app can see the display or is displaying over the other app.
> I want the app to turn off automatically when i open gpay so that i dont face any issues while paying. and later when i close it make sure there is a 5-10 second gap after which the app enables itself again.
> Or something better
> Make sure the app always stays off but switches on when i open instagram and youtube.
> when i close insta or yt it should go back to off status again.
> I think that would be more convenient as it protect the privacy of every other app.
> Go ahead and execute this
> After which i will plug in my phone so that you can apply it on my phone.

(Then: "Plugged in", "getting some error in step 2", "Run the correct command and make sure it runs", "which command", "Ran it".)

**Result:**
- **Lines:**
  - All the funny lines are removed.
  - Only 3 popups remain, each gone after 2 s: "Do you even remember the first reel?" at 20, "Pack it up" at 60, "Son" at 80.
  - The 80-reel block screen now says "Son".
- **Always off, on only in Instagram/YouTube:**
  - New `WatcherService` checks the app in front once a second with *usage access* (app names only), and switches the monster's accessibility service on only for Instagram and YouTube.
  - It switches it off for every other app (GPay included) and when the screen turns off.
  - New `AutoSwitch.java` (laptop-tested) and `BootReceiver.java`.
  - The app screen became a setup card with two one-time steps.
- **Setup:**
  - I allowed usage access on the phone.
  - The agent didn't run the permission grant itself (it's a security setting), so I ran `adb shell pm grant com.reelmonster android.permission.WRITE_SECURE_SETTINGS`.
  - **Path problem:** my first try failed with "not recognized", because the Claude app keeps the Android SDK in its private folder (`AppData\Local\Packages\Claude_…\LocalCache\Local`). The agent found the real path, and the README now explains this.
- **Tested on the phone:**
  - ON in Instagram and YouTube (bubble 0.1–0.2 s later).
  - OFF on the home screen, in Settings, in GPay and in its own app.
  - OFF when the screen turns off and on the lock screen.
  - At 20 reels the logo turned indigo, and the popup showed then disappeared within 3 s.
  - All laptop tests pass.

---

## Prompt 9: the logo alone, as a circle

> ok nice
> I see that you have created it as a grey square within which there is the logo
> I dont want that square
> I want the logo alone to be there as a circle
> Make those changes

**Result:**
- **Round pictures:** `make_logo_stages.py` now makes transparent PNGs. Everything outside the circle is see-through, and each picture is cropped to the circle, so the logo fills the bubble.
- **Clean edge:** transparent pixels are black underneath, so shrinking the picture leaves no grey halo. The agent checked this on a light and a dark background.
- **Grey kept where needed:** grey still colours the inner strokes in stage 4, because the black face would hide them otherwise.
- **On the phone:** the in-app header showed the round stage-4 logo (my count had reached 80). The phone's screen turned off during the test, so the bubble and launcher icon weren't screenshotted. All laptop tests pass.

---

## Prompt 10: emblem border

> Now that you have made it into a circle
> Put a thin black border around each of them
> so that it looks like an emblem
> it should be around all of them not only one

**Result:**
- **The border:** `make_logo_stages.py` adds the same border to all 4 stages: a slim grey gap (18 px on the master), then a thin black circle (14 px). It's drawn from the circle's centre, so it's identical for every stage, and the pictures are cropped to the new outer edge.
- **Checked:** the agent previewed all 4 on light and dark backgrounds and at small size. All laptop tests pass.
- **On the phone:** the new version is installed, but the phone was locked, so it wasn't screenshotted. The border was seen later, in the app header after prompt 11.

---

## Prompt 11: new name

> I also need the name to be changed to "ScrollSense"
> CHange the name everywhere

**Question the agent asked me:** rename the hidden internal app ID (`com.reelmonster`) and the folder too? → **Keep the internal ID.** Changing it would make Android treat it as a new app: the old one uninstalled, counts lost, and usage access and the grant command redone.

**Result:**
- **Everywhere you can see it:** "ScrollSense" is now the app name and icon label, the in-app title and setup text, the notification and its channel, and the Accessibility description. The built file is now `ScrollSense.apk`.
- **Code and docs:** the log tag is `ScrollSense`. Every code comment and the README say ScrollSense, or "the bubble" where they meant the floating logo. The version is 1.3.
- **Kept on purpose:** names nobody sees on the phone (`com.reelmonster`, the class names `MonsterService` / `MonsterView`, and the `reel-monster` folder). The README explains why.
- **Small fix:** the general rename also changed the brief's project name in the README, so the agent put "Reel Monster" back there.
- **Tested on the phone:**
  - installed as ScrollSense 1.3;
  - the app shows the ScrollSense title and setup text;
  - my count (80) and history were kept;
  - the auto on/off permission was still granted;
  - the bordered emblem shows in the header.
  - All laptop tests pass.

---

## Prompt 12: reset today's count

> could you also clear todays count and make it start from 0 ?

**Result:**
- **Why not a button:** the app has no reset button on purpose, because one would make the 80 limit easy to dodge.
- **Why not Clear data:** it would also wipe my history and switch off the setup.
- **What the agent did instead:**
  - installed a temporary **debuggable** build;
  - stopped the app, so it couldn't write the old number back;
  - deleted only `count_2026-11-08` from the app's saved stats (`run-as … sed`);
  - reinstalled the normal, non-debuggable build.
- **Checked on the phone:**
  - Today shows **0**, with "80 left";
  - the logo is back to cyan;
  - Sat 7 Nov (83) and today's time lost (17m) were kept;
  - the auto on/off permission is still granted.
- **README:** these steps are now in the troubleshooting section.

---
