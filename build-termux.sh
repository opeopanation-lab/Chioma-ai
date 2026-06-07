#!/data/data/com.termux/files/usr/bin/bash
# CHIOMA AI AGENT — Build APK directly in Termux
# No PC needed. Runs entirely on your Android device.
# Usage: bash build-termux.sh

set -e

CYAN='\033[0;36m'; PURPLE='\033[0;35m'; GREEN='\033[0;32m'
RED='\033[0;31m'; BOLD='\033[1m'; RESET='\033[0m'

step() { echo -e "${CYAN}▶ $1${RESET}"; }
ok()   { echo -e "${GREEN}✓ $1${RESET}"; }
fail() { echo -e "${RED}✗ $1${RESET}"; exit 1; }

echo ""
echo -e "${PURPLE}${BOLD}╔══════════════════════════════════╗${RESET}"
echo -e "${PURPLE}${BOLD}║   CHIOMA AI — APK Builder        ║${RESET}"
echo -e "${CYAN}${BOLD}║   Building on-device in Termux   ║${RESET}"
echo -e "${PURPLE}${BOLD}╚══════════════════════════════════╝${RESET}"
echo ""

BUILD_DIR="$HOME/.chioma-build"
APK_OUT="$HOME/storage/downloads/ChiomaAI.apk"
JAVA_SRC="$BUILD_DIR/src/com/chioma/agent/MainActivity.java"

# ── Step 1: Install build tools ──────────────────────────────
step "Installing build tools..."
pkg install -y aapt apksigner dx ecj android-tools 2>/dev/null || \
pkg install -y aapt apksigner dx ecj 2>/dev/null || true

# Check what we have
if ! command -v aapt &>/dev/null; then
  echo ""
  echo -e "${RED}aapt not found. Try:${RESET}"
  echo "  pkg install aapt"
  echo ""
  echo -e "${CYAN}Alternative: Use GitHub Actions (free) to build the APK.${RESET}"
  echo "See README.md for instructions."
  exit 1
fi
ok "Build tools ready"

# ── Step 2: Setup directories ────────────────────────────────
step "Setting up build directory..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/src/com/chioma/agent"
mkdir -p "$BUILD_DIR/res/values"
mkdir -p "$BUILD_DIR/res/drawable"
mkdir -p "$BUILD_DIR/bin"
mkdir -p "$BUILD_DIR/obj"
mkdir -p "$BUILD_DIR/gen"
ok "Directories created"

# ── Step 3: Find android.jar ────────────────────────────────
step "Locating android.jar..."
ANDROID_JAR=""
for path in \
  "/data/data/com.termux/files/usr/share/android-tools/android.jar" \
  "/data/data/com.termux/files/usr/lib/android.jar" \
  "$(find /data/data/com.termux -name 'android.jar' 2>/dev/null | head -1)"; do
  if [ -f "$path" ]; then
    ANDROID_JAR="$path"
    break
  fi
done

if [ -z "$ANDROID_JAR" ]; then
  echo -e "${RED}android.jar not found.${RESET}"
  echo "Install with: pkg install android-tools"
  echo ""
  echo "Or build on PC / GitHub Actions. See README.md"
  exit 1
fi
ok "android.jar: $ANDROID_JAR"

# ── Step 4: Write source files ───────────────────────────────
step "Writing source files..."

cat > "$BUILD_DIR/res/values/styles.xml" << 'STYLESEOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="android:Theme.Material.NoTitleBar.Fullscreen">
        <item name="android:windowBackground">#FF080812</item>
        <item name="android:statusBarColor">#FF080812</item>
        <item name="android:navigationBarColor">#FF0F0F1E</item>
    </style>
</resources>
STYLESEOF

cat > "$BUILD_DIR/AndroidManifest.xml" << 'MANIFESTEOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.chioma.agent">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.FLASHLIGHT" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.SEND_SMS" />
    <application
        android:label="Chioma AI"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
MANIFESTEOF

# Copy MainActivity.java from the project
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/android/app/src/main/java/com/chioma/agent/MainActivity.java" ]; then
  cp "$SCRIPT_DIR/android/app/src/main/java/com/chioma/agent/MainActivity.java" "$JAVA_SRC"
  ok "Copied MainActivity.java from project"
else
  fail "MainActivity.java not found at $SCRIPT_DIR/android/app/src/main/java/com/chioma/agent/"
fi

ok "Source files written"

# ── Step 5: Generate R.java ──────────────────────────────────
step "Generating R.java with aapt..."
aapt package -f -m \
  -J "$BUILD_DIR/gen" \
  -M "$BUILD_DIR/AndroidManifest.xml" \
  -S "$BUILD_DIR/res" \
  -I "$ANDROID_JAR" \
  || fail "aapt failed"
ok "R.java generated"

# ── Step 6: Compile Java ─────────────────────────────────────
step "Compiling Java sources..."
ecj -source 11 -target 11 \
  -cp "$ANDROID_JAR" \
  -d "$BUILD_DIR/obj" \
  "$BUILD_DIR/gen/com/chioma/agent/R.java" \
  "$JAVA_SRC" \
  || fail "Java compilation failed"
ok "Compiled"

# ── Step 7: Convert to DEX ───────────────────────────────────
step "Converting to DEX (Dalvik)..."
dx --dex \
  --output="$BUILD_DIR/bin/classes.dex" \
  "$BUILD_DIR/obj" \
  || fail "dx failed"
ok "DEX created"

# ── Step 8: Package APK ──────────────────────────────────────
step "Packaging APK..."
UNSIGNED="$BUILD_DIR/chioma-unsigned.apk"
aapt package -f \
  -M "$BUILD_DIR/AndroidManifest.xml" \
  -S "$BUILD_DIR/res" \
  -I "$ANDROID_JAR" \
  -F "$UNSIGNED" \
  || fail "aapt package failed"
cd "$BUILD_DIR/bin"
aapt add "$UNSIGNED" classes.dex
cd - > /dev/null
ok "APK packaged"

# ── Step 9: Sign APK ─────────────────────────────────────────
step "Signing APK..."
KEYSTORE="$HOME/.chioma/chioma.keystore"
mkdir -p "$(dirname "$KEYSTORE")"

if [ ! -f "$KEYSTORE" ]; then
  keytool -genkey -v \
    -keystore "$KEYSTORE" \
    -alias chioma \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass chioma123 \
    -keypass chioma123 \
    -dname "CN=Chioma, OU=AI, O=Free, L=Lagos, S=Lagos, C=NG" \
    2>/dev/null || fail "keytool failed"
  ok "Keystore created"
fi

apksigner sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:chioma123 \
  --key-pass pass:chioma123 \
  --ks-key-alias chioma \
  --out "$APK_OUT" \
  "$UNSIGNED" \
  || fail "apksigner failed"

ok "APK signed"

# ── Done! ────────────────────────────────────────────────────
echo ""
echo -e "${PURPLE}${BOLD}════════════════════════════════════${RESET}"
echo -e "${GREEN}${BOLD}✓ BUILD COMPLETE!${RESET}"
echo -e "${PURPLE}${BOLD}════════════════════════════════════${RESET}"
echo ""
echo -e "  APK: ${CYAN}${BOLD}$APK_OUT${RESET}"
echo ""
echo -e "  Install with:"
echo -e "  ${CYAN}termux-open $APK_OUT${RESET}"
echo ""
echo -e "${CYAN}∞ No limits. Free forever. Chioma is yours.${RESET}"
echo ""
