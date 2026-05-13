# Kia Android app

Android 15/TEYES app for Kia CANBOX integration.

Current package: `kia.app`

Current app version: `12.4-kia` (`versionCode 104`)

Main protocol notes:

- штатный canbox remains on the normal USB CDC interface;
- TEYES app implementation details: `../../docs/TEYES_APP_IMPLEMENTATION_README_20260513.md`;
- `0x77` reads compact Vehicle/RCTA snapshot in normal mode;
- `0x70/0x76` raw CAN stream is debug-only;
- bus `0` is C-CAN;
- bus `1` is M-CAN;
- Vehicle UI derives speed, rpm, voltage, temperatures and RCTA from `0x77` snapshot.
- Voltage uses M-CAN `0x132 DATA[0] / 10`; legacy `0x545` is only fallback.
- RCTA/blind-spot overlay uses latest `0x4F4` carried by the firmware snapshot; legacy `0x58B` remains debug-only.
- Media source hints never overwrite a playing MediaSession; music is sent to the cluster once per source/track change, not held by repeat spam.
- Default profile keeps Vehicle/RCTA/TPMS/media/nav active and disables debug/raw recording, media overlays, UART debug and test leftovers.
- CAN log export writes to the public `Downloads` folder. The visible preview stays small, while a full capture stores up to `50 000` selected CAN frames and then auto-stops, saves, and compresses to `.log.gz`.

Build:

```bash
./gradlew assembleRelease
```

Release APK name is kept short and stable:

```text
/Volumes/SSD/canbus/release/kia_124.apk
```

The release build is signed with the local Android debug keystore
(`~/.android/debug.keystore`) so it can be installed/tested immediately on the
head unit. A production key can be added later without changing the output path.

The local Gradle output uses the same filename:

```text
app/build/outputs/apk/release/kia_124.apk
```

TEYES sandbox emulator:

```bash
scripts/run_teyes_sandbox.sh
```

The target TEYES screen is `2000x1200`. The sandbox launches at `1000x600` by
default: half size, same 5:3 aspect ratio, so it does not take the full desktop.
Use `KIA_EMULATOR_WIDTH=2000 KIA_EMULATOR_HEIGHT=1200 KIA_EMULATOR_DENSITY=160`
when a full logical-resolution run is needed.

RCTA overlay debug:

```bash
adb shell am broadcast -a kia.app.DEBUG_RCTA --es side left
adb shell am broadcast -a kia.app.DEBUG_RCTA --es side right
adb shell am broadcast -a kia.app.DEBUG_RCTA --es side both
adb shell am broadcast -a kia.app.DEBUG_RCTA --es side unknown
adb shell am broadcast -a kia.app.DEBUG_RCTA --es side off
```

ADB QA scenarios:

```bash
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_yandex
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_bt_selected_paused
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_bt_playing
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_usb
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_active
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_preview
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_failed
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario rcta_left
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario rcta_right
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario rcta_unknown
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario tpms_low
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario tpms_close
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario vehicle
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario compass --ei step 9
```

The repository intentionally does not track generated APK files, build folders or local Android SDK settings.
