#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

REPO_ROOT="$(cd ../.. && pwd)"
LOCAL_JAVA_HOME="$REPO_ROOT/.tools/jdk/Contents/Home"
LOCAL_GRADLE="$REPO_ROOT/.tools/gradle/gradle-8.10.2/bin/gradle"
LOCAL_ANDROID_HOME="$REPO_ROOT/.tools/android-sdk"

if [ -x "$LOCAL_JAVA_HOME/bin/java" ]; then
  export JAVA_HOME="$LOCAL_JAVA_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [ -d "$LOCAL_ANDROID_HOME" ]; then
  export ANDROID_HOME="$LOCAL_ANDROID_HOME"
  export ANDROID_SDK_ROOT="$LOCAL_ANDROID_HOME"
fi

if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then
  echo "Java/JDK not found. Install Android Studio or JDK 17+ first." >&2
  exit 2
fi

if [ -x "$LOCAL_GRADLE" ]; then
  "$LOCAL_GRADLE" assembleDebug
elif command -v gradle >/dev/null 2>&1; then
  gradle assembleDebug
elif [ -x ./gradlew ]; then
  ./gradlew assembleDebug
else
  echo "Gradle not found. Open this folder in Android Studio or install Gradle." >&2
  exit 2
fi

echo "APK: app/build/outputs/apk/debug/app-debug.apk"
