#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then
  echo "Java/JDK not found. Install Android Studio or JDK 17+ first." >&2
  exit 2
fi

if command -v gradle >/dev/null 2>&1; then
  gradle assembleDebug
elif [ -x ./gradlew ]; then
  ./gradlew assembleDebug
else
  echo "Gradle not found. Open this folder in Android Studio or install Gradle." >&2
  exit 2
fi

echo "APK: app/build/outputs/apk/debug/app-debug.apk"
