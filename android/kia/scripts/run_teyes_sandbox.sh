#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$APP_DIR/../.." && pwd)"

ANDROID_HOME="${ANDROID_HOME:-$REPO_ROOT/.tools/android-sdk}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
JAVA_HOME="${JAVA_HOME:-$REPO_ROOT/.tools/jdk/Contents/Home}"
export ANDROID_HOME ANDROID_SDK_ROOT JAVA_HOME

LOGICAL_WIDTH="${KIA_EMULATOR_WIDTH:-1000}"
LOGICAL_HEIGHT="${KIA_EMULATOR_HEIGHT:-600}"
LCD_DENSITY="${KIA_EMULATOR_DENSITY:-160}"
SYSTEM_IMAGE="${KIA_SYSTEM_IMAGE:-system-images;android-35;google_apis;arm64-v8a}"
AVD_NAME="${KIA_AVD_NAME:-kia_teyes_${LOGICAL_WIDTH}x${LOGICAL_HEIGHT}}"
DRY_RUN=false

if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
  shift
fi

AVD_DIR="$HOME/.android/avd/$AVD_NAME.avd"
CONFIG="$AVD_DIR/config.ini"

if [[ ! -x "$ANDROID_HOME/emulator/emulator" ]]; then
  echo "Android emulator not found: $ANDROID_HOME/emulator/emulator" >&2
  exit 1
fi

if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" ]]; then
  echo "avdmanager not found: $ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" >&2
  exit 1
fi

if [[ ! -d "$AVD_DIR" ]]; then
  printf 'no\n' | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd \
    --force \
    --name "$AVD_NAME" \
    --package "$SYSTEM_IMAGE" \
    --device pixel_c
fi

python3 - "$CONFIG" "$LOGICAL_WIDTH" "$LOGICAL_HEIGHT" "$LCD_DENSITY" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
width, height, density = sys.argv[2:5]
updates = {
    "hw.initialOrientation": "landscape",
    "hw.lcd.width": width,
    "hw.lcd.height": height,
    "hw.lcd.density": density,
    "hw.ramSize": "2048",
    "hw.gpu.enabled": "yes",
    "hw.gpu.mode": "swiftshader_indirect",
    "showDeviceFrame": "no",
    "skin.dynamic": "yes",
    "skin.name": f"{width}x{height}",
    "skin.path": f"{width}x{height}",
    "vm.heapSize": "256",
}

lines = path.read_text().splitlines()
seen = set()
out = []
for line in lines:
    if "=" not in line:
        out.append(line)
        continue
    key = line.split("=", 1)[0]
    if key in updates:
        out.append(f"{key}={updates[key]}")
        seen.add(key)
    else:
        out.append(line)

for key, value in updates.items():
    if key not in seen:
        out.append(f"{key}={value}")

path.write_text("\n".join(out) + "\n")
PY

CMD=(
  "$ANDROID_HOME/emulator/emulator"
  -avd "$AVD_NAME"
  -skin "${LOGICAL_WIDTH}x${LOGICAL_HEIGHT}"
  -gpu swiftshader_indirect
  -no-boot-anim
  "$@"
)

if [[ "$DRY_RUN" == true ]]; then
  printf '%q ' "${CMD[@]}"
  echo
  exit 0
fi

exec "${CMD[@]}"
