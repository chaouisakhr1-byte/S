#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK" || ! -d "$SDK" ]]; then echo "ERROR: Set ANDROID_HOME to an installed Android SDK." >&2; exit 2; fi
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-36.0.0}"
BT="$SDK/build-tools/$BUILD_TOOLS_VERSION"
PLATFORM="${ANDROID_PLATFORM:-android-35}"
ANDROID_JAR="$SDK/platforms/$PLATFORM/android.jar"
for f in "$BT/aapt2" "$BT/d8" "$BT/zipalign" "$BT/apksigner" "$ANDROID_JAR"; do [[ -x "$f" || -f "$f" ]] || { echo "ERROR: Missing $f" >&2; exit 2; }; done
OUT="$ROOT/build/out"; GEN="$ROOT/build/gen"; CLASSES="$ROOT/build/classes"; RES="$ROOT/build/res"
rm -rf "$OUT" "$GEN" "$CLASSES" "$RES"; mkdir -p "$OUT" "$GEN" "$CLASSES" "$RES"
"$BT/aapt2" compile --dir "$ROOT/res" -o "$RES/resources.zip"
"$BT/aapt2" link -o "$OUT/base.apk" --manifest "$ROOT/AndroidManifest.xml" -I "$ANDROID_JAR" --java "$GEN" "$RES/resources.zip"
find "$ROOT/src" "$GEN" -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -source 8 -target 8 -classpath "$ANDROID_JAR" -d "$CLASSES"
"$BT/d8" --lib "$ANDROID_JAR" --min-api 23 --output "$OUT" "$CLASSES"
(cd "$OUT" && zip -q -u base.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"
KEY="$ROOT/build/debug.keystore"
if [[ ! -f "$KEY" ]]; then keytool -genkeypair -v -keystore "$KEY" -storepass android -alias androiddebugkey -keypass android -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1; fi
"$BT/apksigner" sign --ks "$KEY" --ks-pass pass:android --key-pass pass:android --out "$OUT/S-debug.apk" "$OUT/aligned.apk"
"$BT/apksigner" verify --verbose "$OUT/S-debug.apk"
echo "APK: $OUT/S-debug.apk"
