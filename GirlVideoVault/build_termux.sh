#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"

if [ -z "$ANDROID_HOME" ]; then
  if [ -d "$HOME/android-sdk" ]; then
    export ANDROID_HOME="$HOME/android-sdk"
  elif [ -d "$PREFIX/share/android-sdk" ]; then
    export ANDROID_HOME="$PREFIX/share/android-sdk"
  fi
fi

SDK=36
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME/platforms" ]; then
  FOUND=$(ls "$ANDROID_HOME/platforms" 2>/dev/null | sed -n 's/android-//p' | sort -n | tail -1)
  [ -n "$FOUND" ] && SDK="$FOUND"
fi

echo "Android SDK: $SDK"

GRADLE=""
for CANDIDATE in \
  "$HOME/.gradle-dist/gradle-8.9/bin/gradle" \
  "$HOME/gradle-8.9/bin/gradle" \
  "$PREFIX/bin/gradle" \
  "$(command -v gradle 2>/dev/null || true)"; do
  if [ -n "$CANDIDATE" ] && [ -x "$CANDIDATE" ]; then GRADLE="$CANDIDATE"; break; fi
done

if [ -z "$GRADLE" ]; then
  echo "Gradle 8.9 が見つかりません。"
  echo "既にGradle 8.9がある場合は、そのbin/gradleを使ってください。"
  exit 1
fi

"$GRADLE" --no-daemon --stacktrace -PandroidSdk="$SDK" clean assembleDebug
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
  mkdir -p "$HOME/storage/downloads"
  cp "$APK" "$HOME/storage/downloads/GirlVideoVault-1.1.0-debug.apk"
  echo
  echo "BUILD SUCCESS"
  echo "$HOME/storage/downloads/GirlVideoVault-1.1.0-debug.apk"
fi
