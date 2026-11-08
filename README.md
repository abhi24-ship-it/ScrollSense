# ScrollSense


> The app was first named Reel Monster. Only names nobody sees on the phone still use it: the internal app ID `com.reelmonster`, the class names `MonsterService` / `MonsterView`, and the `reel-monster` folder. Changing the app ID would make Android treat it as a new app (counts lost, setup redone).

A small round emblem that floats on top of **Instagram** and **YouTube** and counts every reel or short you watch, **by itself**. It is drawn as your own logo, which changes colour in 4 stages as the count rises, and it grows bigger. Inside the app you see today's count, the time lost, and the last 7 days.

- **Daily limit of 80 reels:** at 80, Reels and Shorts are closed until midnight. Chats, Home, Search and everything else still work (see section 6).
- **Off everywhere else:** ScrollSense is only switched on while Instagram or YouTube is open. GPay, banking apps and every other app never see it (see section 6).
- Android only (8.0 or newer).
- Works fully offline. **No internet permission and no login.**
- Built from the command line with plain Java. No Android Studio, no Gradle, no libraries.

---

## 1. One-time setup (laptop)

These are already installed on this laptop. Only run them on a new computer (Windows PowerShell, about 450 MB of downloads, no admin rights needed):

```powershell
$ProgressPreference = 'SilentlyContinue'
$dl = "$env:LOCALAPPDATA\reel-monster-downloads"; New-Item -ItemType Directory -Force $dl | Out-Null
Invoke-WebRequest "https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip" -OutFile "$dl\jdk17.zip" -UseBasicParsing
Invoke-WebRequest "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -OutFile "$dl\cmdline-tools.zip" -UseBasicParsing
Expand-Archive "$dl\jdk17.zip" -DestinationPath "$env:LOCALAPPDATA\Programs" -Force
$sdk = "$env:LOCALAPPDATA\Android\Sdk"; New-Item -ItemType Directory -Force "$sdk\cmdline-tools" | Out-Null
Expand-Archive "$dl\cmdline-tools.zip" -DestinationPath "$dl\ct" -Force
Move-Item "$dl\ct\cmdline-tools" "$sdk\cmdline-tools\latest"
$env:JAVA_HOME = (Get-ChildItem "$env:LOCALAPPDATA\Programs" -Directory -Filter "jdk-17*")[0].FullName
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses        # read and type y to accept
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

| Tool | Why it's needed |
|---|---|
| Microsoft OpenJDK 17 | compiles the Java code and creates the signing key |
| Android SDK `platforms;android-34` | `android.jar`, the Android library the code is compiled against |
| Android SDK `build-tools;34.0.0` | `aapt2` (resources), `d8` (Java → Android code), `zipalign` and `apksigner` (signing) |
| Android SDK `platform-tools` | `adb`, which installs the app over USB and reads its logs |
| Python 3 + Pillow + numpy | only to re-make the logo stage pictures (`logo/make_logo_stages.py`); the finished pictures are already in `res/drawable-nodpi/` |

> **On this laptop the tools were installed from inside the Claude app**, which keeps them in its private folder. In a normal PowerShell window use this folder instead of `%LOCALAPPDATA%`:
> `C:\Users\Abhishek Bhat\AppData\Local\Packages\Claude_pzs8sxrjxfjjc\LocalCache\Local\`
> For example, adb is at `...\LocalCache\Local\Android\Sdk\platform-tools\adb.exe`.

## 2. Build

From the `reel-monster` folder:

```powershell
.\build.ps1
```

This first runs the **laptop tests** (reel counting, logo stages and popups, auto on/off), then builds `ScrollSense.apk` (about 250 KB, mostly the 4 logo pictures).

## 3. Install on your phone

**Option A, USB (recommended):**
1. On the phone, go to **Settings → About phone** and tap **Build number** 7 times. This turns on Developer options.
2. Go to **Settings → Developer options** and turn on **USB debugging**.
3. Plug the phone into the laptop, and tap **Allow** on the "Allow USB debugging?" popup.
4. Run:
   ```powershell
   .\build.ps1 -Install
   ```

**Option B, no cable:** copy `ScrollSense.apk` to the phone (Drive, WhatsApp to yourself, etc.), open it and allow "install unknown apps". You still need the cable once for setup step 2 below.

## 4. One-time setup on the phone (auto mode)

Open ScrollSense. The setup card lists two steps:

1. **Usage access:** tap **Allow usage access**, find **ScrollSense** and switch it on. This lets the watcher see which app is open (app names only, never the screen).
2. **Auto on/off permission:** with the phone plugged in, run this once on the laptop:
   ```powershell
   adb shell pm grant com.reelmonster android.permission.WRITE_SECURE_SETTINGS
   ```
   (Use the full adb path from section 1.) This lets the app switch its own accessibility service on and off. Normal apps can't get this permission any other way. It stays after app updates, but not after an uninstall.

When both are done, the card turns green: **"Auto mode is on"**. You never switch ScrollSense on by hand: it wakes up by itself in Instagram and YouTube.

---

## 5. Testing the "Done when" list

| # | Done when | How to test | Pass if |
|---|---|---|---|
| 1 | Installs on a real Android phone | Section 3 | "ScrollSense" appears in the app drawer and opens |
| 2 | Stays on top of **both** Instagram and YouTube | Open Instagram → Reels. Then open YouTube → Shorts. Then go to the home screen. | The bubble is visible in both apps, can be dragged, and disappears on the home screen and in other apps |
| 3 | Count survives a phone restart | Note today's count, then restart the phone. Open the app, then open Instagram. | Same count in the app and on the bubble. The next reel adds +1 to it. |
| 4 | Level 2: over 20 reels, count within 2 of yours | Note the count. Open Instagram Reels and swipe through exactly **20** reels, counting them on your fingers. Repeat in YouTube Shorts. | The bubble's count went up by 18–22 each time |
| 5 | No internet, no login | Turn on **Airplane mode**, then use the app and the bubble. Also run `aapt dump badging` (below). | Everything still works. No login screen. The APK has no `INTERNET` permission. |

**Results** (Nothing Phone A059, Android 17, 6 Nov 2026, swipes sent with `adb shell input swipe`):

| Check | Result |
|---|---|
| Installs | ✅ installed with `.\build.ps1 -Install` |
| On top of Instagram and YouTube | ✅ shown in both, hidden on the home screen and on ScrollSense's own screen |
| In-app stats | ✅ Today 67 reels · 6m lost, and the Today bar in the last 7 days |
| Instagram, 20 reels | ✅ counted **20** |
| YouTube, 20 shorts (first one + 19 swipes) | ✅ counted **19**. The log showed only 19 different shorts appeared, so one swipe didn't move YouTube on, and every short that did appear was counted. |
| Count survives a phone restart | ✅ 69 before `adb reboot`. After the restart ScrollSense started by itself, showed 69 over Instagram, and 3 more reels took it to 72. |
| No internet, no login | ✅ The app and ScrollSense worked with Airplane mode on (tested by me). No login. |
| My own finger test | ✅ Fast scrolling through Instagram Reels by hand was counted correctly. The log showed 17 reels in about 6 seconds, each counted once. |

**Daily limit test** (8 Nov 2026, with a temporary test build where `DAILY_LIMIT = 4`, so 80 reels weren't needed):

| Check | Result |
|---|---|
| Reaching the limit inside Instagram Reels | ✅ At 4 the block screen came on, ScrollSense pressed Back once, and Instagram went to its Home tab |
| Reopening Instagram Reels | ✅ Blocked again, back on the Home tab |
| Instagram chats and Home feed | ✅ Chats opened with no block, and the Home feed scrolls normally |
| Reopening YouTube Shorts (tab and thumbnails) | ✅ Blocked, and YouTube stays open on its Home screen |
| The in-app limit line | ✅ "Daily limit: 80 reels · 76 left" |

To test it yourself without watching 80 reels, set `DAILY_LIMIT` in `Stats.java` to a small number, run `.\build.ps1 -Install`, and set it back to 80 afterwards.

**Logo stages, popups and auto mode test** (8 Nov 2026, version 1.2):

| Check | Result |
|---|---|
| ScrollSense ON only in Instagram and YouTube | ✅ ON in Instagram and YouTube, the bubble appeared 0.1–0.2 s after switching on |
| ScrollSense OFF everywhere else | ✅ off on the home screen, in Settings, in **GPay**, and in ScrollSense's own screen |
| Screen off | ✅ switched off when the screen turned off, stayed off on the lock screen, and came back on by itself after unlocking into YouTube |
| Logo stage 1 → 2 at 20 reels | ✅ cyan logo up to 19, indigo at 20 |
| Popup at 20 | ✅ "Do you even remember the first reel?" appeared at 20, and was gone 3 s later; no popup for reels 14–19 |
| App icon | ✅ the cyan stage-1 logo |
| Round logo, no grey square (later change) | ✅ in-app header showed the round stage-4 logo at 80 reels; bubble and launcher icon not re-checked on the phone (screen was off) |
| Thin black emblem border on all 4 stages (later change) | ✅ checked in the laptop previews on light and dark backgrounds, and in the app header on the phone |
| Renamed to ScrollSense (later change) | ✅ app name, in-app title and setup text show ScrollSense; count, history and both setup steps were kept |
| Stages 3 and 4, popups at 60 and 80 | covered by the laptop tests (`LogoStageTest`), not yet seen on the phone |

Also check the "What to build" features:
- **Counter on the bubble:** a badge in its top-right corner shows 1, 2, 3 and up.
- **Grows and says lines:** the bubble grows from 64 dp to 120 dp by 60 reels, and its logo changes stage at 20 / 60 / 80. It says only 3 short lines, each a popup for 2 s: "Do you even remember the first reel?" at 20, "Pack it up" at 60, "Son" at 80.
- **In the app:** Today (count + time lost + "Daily limit: 80 reels · N left"), and the Last 7 days with a bar per day.

Commands to check permissions, and to watch ScrollSense's log while you scroll (with the phone on USB; see the folder note in section 1):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\aapt.exe" dump badging ScrollSense.apk | Select-String "uses-permission"
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -s ScrollSense
```

The first command lists the permissions: usage stats, write secure settings, foreground service and boot. There is no `INTERNET`. The second prints `ScrollSense switched ON/OFF`, and `counted +1 -> N` for each counted reel.

---

## 6. How it works (for presenting)

```
WatcherService ──"Instagram/YouTube in front?"──▶ switches MonsterService ON / OFF
                  (usage access, once a second)

Instagram / YouTube ──scroll events──▶ MonsterService ──▶ ReelDetector ──"new reel!"──▶ Stats (saved on phone)
                                             │                                              │
                                             └──▶ floating bubble (MonsterView: logo stage) ◀┘
MainActivity (the app screen) ──reads──▶ Stats
```

| File | What it does |
|---|---|
| `AndroidManifest.xml` | The app's ID card: name, icon, the screen, the two services, and the permissions (no internet). |
| `res/xml/accessibility_config.xml` | Which events ScrollSense listens to: "app changed", "list scrolled" and "screen content changed". |
| `src/.../WatcherService.java` | The watcher. Once a second, while the screen is on and unlocked, it asks usage access which app is in front, and switches ScrollSense's accessibility service **on** only for Instagram/YouTube, **off** for everything else. It stops checking when the screen goes off. It runs as a foreground service with a quiet "Sleeping" notification. |
| `src/.../AutoSwitch.java` | Plain Java rules for the watcher: which apps switch ScrollSense on, and how to add/remove our service in Android's list of switched-on accessibility services without touching other services. |
| `src/.../BootReceiver.java` | Starts the watcher after a phone restart or an app update. |
| `src/.../MonsterService.java` | The ScrollSense service (an accessibility service). While on, it shows the bubble over Instagram/YouTube, sends Instagram scroll events and YouTube Shorts pages to the detector, updates the count, shows the popups, measures time, and blocks Reels at the limit. |
| `src/.../ReelDetector.java` | The counting brain, in plain Java. **Instagram:** the Reels list (`clips_viewer_view_pager`) reports its position when it scrolls, and each *new* position counts once, so scrolling back doesn't double count. **YouTube:** the Shorts list sends no scroll events. Instead, each short sits in its own page view (`reel_player_page_container`). At most every 0.4 s ScrollSense checks which page fills the screen, and a different page means a new short. |
| `src/.../LogoStage.java` | Plain Java: which logo stage to show (switches at 20, 60 and 80), and the 3 popup lines. |
| `src/.../MonsterView.java` | Draws the floating bubble: the logo picture for the current stage, plus the count badge. Grows with the count. |
| `src/.../Stats.java` | Saves `count_<date>` and `millis_<date>` in SharedPreferences (a small file on the phone), so the numbers survive a restart. Holds `DAILY_LIMIT = 80`. |
| `src/.../MainActivity.java` | The app screen: setup status, Today with how many reels are left, and the Last 7 days. |
| `logo/make_logo_stages.py` | Makes the 4 logo stage pictures from **one** master logo (see below). |
| `test/*.java` | Laptop tests: reel counting (20 reels → 20, scrolling back, resets), logo stage thresholds and popups, auto on/off rules. |
| `build.ps1` | Tests, compiles and signs the APK, and installs it with `-Install`. |

**Logo stages.** The rules come from the handwritten note in `logo/source/` (face / eyes / background):

| Reels today | Face | Eyes | Eye shape |
|---|---|---|---|
| 0–19 | cyan | black | calm, horizontal |
| 20–59 | indigo | black | calm, horizontal |
| 60–79 | red | black | sharp, angled |
| 80+ (Reels blocked) | black | red | sharp, angled |

The black outer circle never changes. Around it every stage has the same **emblem border**: a slim grey gap, then a thin black circle. The pictures are **round**: transparent outside the border and cropped to it, so the emblem is shown alone with no square behind it. The note asked for a grey background, but I asked for the logo alone. The border's size is set by `BORDER_GAP` and `BORDER_WIDTH` in the script. `make_logo_stages.py` splits the master logo (`logo/source/master_logo.png`) into parts: background, outer circle, strokes, face and eyes. Then it paints each part per stage, so size and position can never drift between stages. For the calm stages it removes the slanted eye strokes and copies the horizontal eye shape from `logo/source/calm_eyes_reference.png`. In stage 4 the inner strokes are grey, otherwise the black face would hide them. To re-make the pictures: `python logo/make_logo_stages.py`.

**Off everywhere except Instagram and YouTube.** Payment apps like GPay refuse to work while an app that can read the screen (an accessibility service) is switched on. So ScrollSense's accessibility service is **off almost all the time**. The watcher switches it on only while Instagram or YouTube is the app in front, and off again as soon as you leave, open another app, or turn the screen off. Each time it switches on, it reads back the saved count, so nothing is lost.

**"Time lost"** = time with Instagram or YouTube open in front (screen on), saved every 30 s.

**Known small quirk:** when you leave Instagram or YouTube and come back, the reel already on screen is counted once more. ScrollSense can't tell it apart from a new reel after a fresh start.

**Daily limit (80 reels):** once today's count reaches 80, whenever the full-screen Reels player (Instagram) or Shorts player (YouTube) is open, ScrollSense:
1. covers it with a dark **block screen** ("Son. Reels and Shorts are closed until tomorrow.") that swallows every touch, so nothing behind it can be scrolled;
2. presses **Back once** for you, which leaves the player (to the Home tab, or back to the chat a reel was opened from). It presses only once per visit, at most every 3 s. Pressing again while the app is still closing the player would close the whole app, which happened with YouTube in testing;
3. removes the block screen as soon as the player is gone. A **Leave Reels** button is there in case the app doesn't leave by itself.

Chats, the Home feed, Search, profiles and stories don't use the full-screen player, so they keep working. The count is per day, so Reels open again at midnight. Reel videos that play inside the normal Home feed aren't blocked (they aren't counted either).

**Which app is really open?** YouTube keeps playing in a small picture-in-picture window when you leave it, and that window also reports "YouTube is on screen". So ScrollSense asks Android which app window has **focus** (`flagRetrieveInteractiveWindows`), and only shows itself or counts when Instagram or YouTube really has focus.

## 7. Troubleshooting

- **ScrollSense doesn't appear in Instagram/YouTube:** open the app and check the setup card says "Auto mode is on". If not, do the missing step from section 4. Some phones (Xiaomi, Oppo, Vivo, Realme) stop background apps: allow **Autostart** and set battery to **No restrictions** for ScrollSense.
- **`adb` or `build.ps1` "not recognized" in a normal terminal:** the tools are in the Claude app's private folder (see the note in section 1).
- **Don't run `adb shell uiautomator dump` while testing.** It pauses all accessibility services, so ScrollSense stops counting while it runs.
- **The count doesn't go up, or goes up too much:** plug in the phone, run the `logcat` command above, swipe a few reels, and send the output. App updates can rename the reel list, and the detector looks for `clips`/`reel` in its name.
- **Reset everything:** Settings → Apps → ScrollSense → Storage → Clear data. Afterwards, open the app and check the setup card again.
- **Reset only today's count** (keeps history and setup; there's deliberately no button, so the limit can't be dodged):
  1. add `android:debuggable="true"` to `<application>` in `AndroidManifest.xml`, then `.\build.ps1 -Install`;
  2. `adb shell am force-stop com.reelmonster`;
  3. `adb shell "run-as com.reelmonster sed -i '/count_YYYY-MM-DD/d' shared_prefs/stats.xml"` (today's date);
  4. remove the debuggable line again and `.\build.ps1 -Install`.
