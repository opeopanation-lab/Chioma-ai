# CHIOMA AI AGENT — Native Android App

## Build the APK on your own machine

### Requirements
- Java 17+ (JDK)
- Android Studio OR Android command-line tools
- ~4 GB free disk space (Gradle cache + SDK)

---

## Option A — Android Studio (Easiest)

1. Open Android Studio
2. File → Open → select the `android/` folder
3. Wait for Gradle sync to finish
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. APK appears in: `android/app/build/outputs/apk/debug/app-debug.apk`
6. Transfer to your Android device and install

---

## Option B — Command Line (Linux/Mac)

```bash
# 1. Install Android SDK command-line tools
# Download from: https://developer.android.com/studio#command-line-tools-only

# 2. Set ANDROID_HOME
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 3. Accept licenses and install required SDK
sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0"

# 4. Enter project and build
cd android/
chmod +x gradlew
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug

# 5. APK output:
# app/build/outputs/apk/debug/app-debug.apk

# 6. Install directly to connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Option C — Build inside Termux itself (on your phone)

```bash
# In Termux:
pkg install aapt apksigner dx ecj -y

# Then use the manual compile script below
# (no Gradle needed — pure javac + aapt + dx)
```

See `build-termux.sh` for the manual Termux build.

---

## Option D — GitHub Actions (free CI, produces downloadable APK)

Push this folder to GitHub, then add `.github/workflows/build.yml`:

```yaml
name: Build APK
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build APK
        run: |
          cd android
          chmod +x gradlew
          ./gradlew assembleRelease
      - uses: actions/upload-artifact@v4
        with:
          name: chioma-apk
          path: android/app/build/outputs/apk/**/*.apk
```

GitHub Actions will build and attach the APK as a downloadable artifact on every push. **Free.**

---

## After Installing the APK

1. Make sure Termux is installed (F-Droid)
2. In Termux, run the bridge:
   ```
   node ~/.chioma/server.js
   ```
3. Open the Chioma AI Agent app
4. Tap "Connect to Chioma"
5. Start chatting — no limits, no fees

---

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/chioma/agent/
│   │   │   └── MainActivity.java     ← entire app (single file)
│   │   ├── res/
│   │   │   ├── values/styles.xml
│   │   │   └── drawable/ic_launcher.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
└── gradlew
```

All UI is built programmatically in `MainActivity.java` — no XML layouts needed.

---

## What the App Does

- Connects to `localhost:8080` (the Chioma MCP bridge in Termux)
- Parses 40+ natural language commands client-side
- Sends `POST /run {"command": "..."}` to execute Termux commands
- Displays results in a chat interface
- No internet required (everything is local)
- No limits, no tracking, no premium

∞ Free Forever.
