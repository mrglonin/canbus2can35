#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../.."

TOOLS_DIR="$PWD/.tools"
JDK_URL="https://api.adoptium.net/v3/binary/latest/17/ga/mac/aarch64/jdk/hotspot/normal/eclipse"
GRADLE_URL="https://services.gradle.org/distributions/gradle-8.10.2-bin.zip"
ANDROID_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-14742923_latest.zip"

mkdir -p "$TOOLS_DIR/downloads"

if [ ! -x "$TOOLS_DIR/jdk/Contents/Home/bin/java" ]; then
  curl -L --fail --retry 3 -o "$TOOLS_DIR/downloads/jdk17.tar.gz" "$JDK_URL"
  rm -rf "$TOOLS_DIR/jdk"
  mkdir -p "$TOOLS_DIR/jdk"
  tar -xzf "$TOOLS_DIR/downloads/jdk17.tar.gz" -C "$TOOLS_DIR/jdk" --strip-components=1
fi

if [ ! -x "$TOOLS_DIR/gradle/gradle-8.10.2/bin/gradle" ]; then
  curl -L --fail --retry 3 -o "$TOOLS_DIR/downloads/gradle.zip" "$GRADLE_URL"
  rm -rf "$TOOLS_DIR/gradle"
  mkdir -p "$TOOLS_DIR/gradle"
  unzip -q "$TOOLS_DIR/downloads/gradle.zip" -d "$TOOLS_DIR/gradle"
fi

if [ ! -x "$TOOLS_DIR/android-sdk/cmdline-tools/latest/bin/sdkmanager" ]; then
  curl -L --fail --retry 3 -o "$TOOLS_DIR/downloads/android-commandlinetools.zip" "$ANDROID_TOOLS_URL"
  rm -rf "$TOOLS_DIR/android-sdk/cmdline-tools"
  mkdir -p "$TOOLS_DIR/android-sdk/cmdline-tools/latest"
  unzip -q "$TOOLS_DIR/downloads/android-commandlinetools.zip" -d "$TOOLS_DIR/android-sdk/cmdline-tools"
  mv "$TOOLS_DIR/android-sdk/cmdline-tools/cmdline-tools/"* "$TOOLS_DIR/android-sdk/cmdline-tools/latest/"
  rmdir "$TOOLS_DIR/android-sdk/cmdline-tools/cmdline-tools"
fi

export JAVA_HOME="$TOOLS_DIR/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$TOOLS_DIR/android-sdk/cmdline-tools/latest/bin:$PATH"
export ANDROID_HOME="$TOOLS_DIR/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null
sdkmanager --sdk_root="$ANDROID_HOME" \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0"

echo "Installed local Android toolchain under $TOOLS_DIR"
