# Builds reel-monster.apk from the command line. No Android Studio, no Gradle.
#   .\build.ps1            -> run the logic tests, then build reel-monster.apk
#   .\build.ps1 -Install   -> same, then install it on the phone plugged in by USB
param([switch]$Install)

Set-Location $PSScriptRoot

# ---- Where the tools are (setup steps are in README.md) ----
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else {
    (Get-ChildItem "$env:LOCALAPPDATA\Programs" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
        Select-Object -First 1).FullName
}
$bt  = "$sdk\build-tools\34.0.0"
$jar = "$sdk\platforms\android-34\android.jar"
$adb = "$sdk\platform-tools\adb.exe"
$env:JAVA_HOME = $jdk

if (-not $jdk -or -not (Test-Path "$jdk\bin\javac.exe")) { Write-Host "Java 17 not found. See README.md setup." -ForegroundColor Red; exit 1 }
if (-not (Test-Path $jar)) { Write-Host "Android SDK not found at $sdk. See README.md setup." -ForegroundColor Red; exit 1 }

function Check($step) {
    if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: $step" -ForegroundColor Red; exit 1 }
    Write-Host "ok: $step" -ForegroundColor Green
}

if (Test-Path build) { Remove-Item build -Recurse -Force }
New-Item -ItemType Directory build\gen, build\classes, build\dex, build\test | Out-Null

# 1. Logic tests for the reel counter (plain Java, runs on the laptop)
& "$jdk\bin\javac.exe" -encoding UTF-8 -d build\test src\com\reelmonster\ReelDetector.java test\ReelDetectorTest.java
Check "compile logic tests"
& "$jdk\bin\java.exe" -cp build\test com.reelmonster.ReelDetectorTest
Check "logic tests"

# 2. Resources (strings, icon, accessibility config) -> compiled + R.java
& "$bt\aapt2.exe" compile --dir res -o build\res.zip
Check "compile resources"
& "$bt\aapt2.exe" link -I $jar --manifest AndroidManifest.xml --java build\gen -o build\unsigned.apk build\res.zip
Check "link resources"

# 3. Java source -> .class files -> classes.dex (the format Android runs)
$sources = Get-ChildItem src, build\gen -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& "$jdk\bin\javac.exe" -source 8 -target 8 -bootclasspath $jar -Xlint:-options -encoding UTF-8 -d build\classes $sources
Check "compile Java"
& "$jdk\bin\jar.exe" cf build\classes.jar -C build\classes .
& "$bt\d8.bat" --release --min-api 26 --lib $jar --output build\dex build\classes.jar
Check "convert to dex"

# 4. Put classes.dex into the APK, align it, and sign it (Android only installs signed APKs)
Push-Location build\dex
& "$bt\aapt.exe" add ..\unsigned.apk classes.dex | Out-Null
$code = $LASTEXITCODE
Pop-Location
$LASTEXITCODE = $code; Check "add code to APK"
& "$bt\zipalign.exe" -f -p 4 build\unsigned.apk build\aligned.apk
Check "align APK"
if (-not (Test-Path debug.keystore)) {
    & "$jdk\bin\keytool.exe" -genkeypair -keystore debug.keystore -storepass android -keypass android `
        -alias reelmonster -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Reel Monster" 2>$null
    Check "create signing key"
}
& "$bt\apksigner.bat" sign --ks debug.keystore --ks-pass pass:android --out reel-monster.apk build\aligned.apk
Check "sign APK"
Write-Host "Built reel-monster.apk ($([math]::Round((Get-Item reel-monster.apk).Length / 1KB)) KB)" -ForegroundColor Cyan

# 5. Optional: install on the phone over USB
if ($Install) {
    & $adb install -r reel-monster.apk
    Check "install on phone"
}
