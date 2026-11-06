# Reel Monster (Project 6, Level 2: automatic)

A cute monster that floats on top of **Instagram** and **YouTube** and counts every reel or short you watch, **by itself**. The more you scroll, the bigger it gets and the cheekier its lines. Inside the app you see today's count, the time lost, and the last 7 days.

- Android only (8.0 or newer).
- Works fully offline. The app asks for **no permissions at all, not even internet**, and has no login.
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

## 2. Build

From the `reel-monster` folder:

```powershell
.\build.ps1
```

This first runs the **logic tests** for the reel counter, then builds `reel-monster.apk` (about 21 KB).

## 3. Install on your phone

**Option A, USB (recommended):**
1. On the phone, go to **Settings → About phone** and tap **Build number** 7 times. This turns on Developer options.
2. Go to **Settings → Developer options** and turn on **USB debugging**.
3. Plug the phone into the laptop, and tap **Allow** on the "Allow USB debugging?" popup.
4. Run:
   ```powershell
   .\build.ps1 -Install
   ```

**Option B, no cable:** copy `reel-monster.apk` to the phone (Drive, WhatsApp to yourself, etc.), open it and allow "install unknown apps".
On Android 13+, switching the monster on may then show **"Restricted setting"**. To fix this, go to **Settings → Apps → Reel Monster → ⋮ (top right) → Allow restricted settings**, then try again.

## 4. Switch the monster on (once)

1. Open **Reel Monster**, then tap **Turn on Reel Monster**.
2. In Accessibility settings, open **Reel Monster** (sometimes it's under "Installed apps" or "Downloaded apps").
3. Switch it on, then tap **Allow**.
4. Go back to the app. It should say **"The monster is awake"**.

Why accessibility? Only an accessibility service can tell which app is open and when a list scrolls, which is what lets the monster count by itself. It also lets the app draw over other apps, so no separate "display over other apps" permission is needed.

---

## 5. Testing the "Done when" list

| # | Done when | How to test | Pass if |
|---|---|---|---|
| 1 | Installs on a real Android phone | Section 3 | "Reel Monster" appears in the app drawer and opens |
| 2 | Stays on top of **both** Instagram and YouTube | Open Instagram → Reels. Then open YouTube → Shorts. Then go to the home screen. | The monster is visible in both apps, can be dragged, and disappears on the home screen and in other apps |
| 3 | Count survives a phone restart | Note today's count, then restart the phone. Open the app, then open Instagram. | Same count in the app and on the monster. The next reel adds +1 to it. |
| 4 | Level 2: over 20 reels, count within 2 of yours | Note the count. Open Instagram Reels and swipe through exactly **20** reels, counting them on your fingers. Repeat in YouTube Shorts. | The monster went up by 18–22 each time |
| 5 | No internet, no login | Turn on **Airplane mode**, then use the app and the monster. Also run `aapt dump badging` (below). | Everything still works. No login screen. The APK lists no `uses-permission` at all. |

**Results so far** (Nothing Phone A059, Android 17, 6 Nov 2026, swipes sent with `adb shell input swipe`):

| Check | Result |
|---|---|
| Installs | ✅ installed with `.\build.ps1 -Install` |
| On top of Instagram and YouTube | ✅ shown in both, hidden on the home screen and on Reel Monster's own screen |
| In-app stats | ✅ Today 67 reels · 6m lost, and the Today bar in the last 7 days |
| Instagram, 20 reels | ✅ counted **20** |
| YouTube, 20 shorts (first one + 19 swipes) | ✅ counted **19**. The log showed only 19 different shorts appeared, so one swipe didn't move YouTube on, and every short that did appear was counted. |
| Monster grows + funny lines | ✅ green at 6, red and much bigger at 47 |
| Count survives a phone restart | ✅ 69 before `adb reboot`. After the restart the monster started by itself, showed 69 over Instagram, and 3 more reels took it to 72. |
| No internet, no login | ✅ The app and the monster worked with Airplane mode on (tested by me). The APK lists no permissions, and there's no login. |
| My own finger test | ✅ Fast scrolling through Instagram Reels by hand was counted correctly. The log showed 17 reels in about 6 seconds, each counted once. |

Also check the "What to build" features:
- **Counter on the monster:** a badge in its top-right corner shows 1, 2, 3 and up.
- **Grows and says funny lines:** each new reel shows a speech bubble for 3.5 s. The monster grows from 64 dp to 120 dp by 60 reels and changes colour at 10 / 30 / 60 reels.
- **In the app:** Today (count + time lost), and the Last 7 days with a bar per day.

Commands to check permissions, and to watch the monster's log while you scroll (with the phone on USB):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\aapt.exe" dump badging reel-monster.apk | Select-String "uses-permission"
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -s ReelMonster
```

The first command prints nothing, because there are no permissions. The second prints a line per scroll and `counted +1 -> N` for each counted reel.

---

## 6. How it works (for presenting)

```
Instagram / YouTube ──scroll events──▶ MonsterService ──▶ ReelDetector ──"new reel!"──▶ Stats (saved on phone)
                     ──"app in front"─▶      │                                              │
                                             └──▶ floating bubble (MonsterView + speech) ◀──┘
MainActivity (the app screen) ──reads──▶ Stats
```

| File | What it does |
|---|---|
| `AndroidManifest.xml` | The app's ID card: name, icon, the screen, the monster service. No permissions. |
| `res/xml/accessibility_config.xml` | Which events the monster listens to: "app changed", "list scrolled" and "screen content changed". |
| `src/.../MonsterService.java` | The monster. Shows the bubble only over Instagram/YouTube and lets you drag it. Sends Instagram scroll events and YouTube Shorts pages to the detector. Updates the count and the funny line, and measures time spent in the two apps. Android restarts it by itself after a reboot. |
| `src/.../ReelDetector.java` | The counting brain, in plain Java. **Instagram:** the Reels list (`clips_viewer_view_pager`) reports its position when it scrolls, and each *new* position counts once, so scrolling back doesn't double count. **YouTube:** the Shorts list sends no scroll events. Instead, each short sits in its own page view (`reel_player_page_container`). At most every 0.4 s the monster checks which page fills the screen, and a different page means a new short. |
| `src/.../MonsterView.java` | Draws the monster with circles and ovals, plus the count badge. Bigger, wider-mouthed and redder as the count rises. Also holds the funny lines. |
| `src/.../Stats.java` | Saves `count_<date>` and `millis_<date>` in SharedPreferences (a small file on the phone), so the numbers survive a restart. |
| `src/.../MainActivity.java` | The app screen: on/off status with a "Turn on" button, Today, and the Last 7 days. |
| `test/ReelDetectorTest.java` | Laptop tests that replay fake Instagram/YouTube scroll events: 20 reels → 20, scrolling back, resets. |
| `build.ps1` | Tests, compiles and signs the APK, and installs it with `-Install`. |

**"Time lost"** = time with Instagram or YouTube open in front (screen on), saved every 30 s.

**Known small quirk:** when you leave Instagram or YouTube and come back, the reel already on screen is counted once more. The monster can't tell it apart from a new reel after a fresh start.

## 7. Troubleshooting

- **The monster doesn't appear:** open the app. If it says "asleep", switch it on again (section 4). Some phones (Xiaomi, Oppo, Vivo, Realme) switch accessibility apps off after a restart. If so, allow **Autostart** and set battery to **No restrictions** for Reel Monster.
- **Don't run `adb shell uiautomator dump` while testing.** It pauses all accessibility services, so the monster stops counting while it runs.
- **The count doesn't go up, or goes up too much:** plug in the phone, run the `logcat` command above, swipe a few reels, and send the output. App updates can rename the reel list, and the detector looks for `clips`/`reel` in its name.
- **Reset everything:** Settings → Apps → Reel Monster → Storage → Clear data.
