# Kia Android app

Android 15/TEYES app for Kia CANBOX integration.

Current package: `kia.app`

Current app version: `13.0-kia` (`versionCode 110`)

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
- Media/nav source events are queued while USB is opening, so the adapter receives `0x7A` before title/nav frames instead of losing the first source.
- Navigation UI has no compass/TBT/text-mode switches: APK sends state changes, while V21 adapter holds/replays `0x48/0x45/0x47/0x4A/0x44` from firmware.
- Compass is sent only on value changes from APK; V21 repeats the stored `0x45` compass frame on compact `0x77` ticks when no active route is held.
- On USB connect and on CANBUS tab open the app asks adapter UID/version/status automatically and sends `0x70 off` unless explicit CAN debug is enabled.
- Raw CAN TX `0x78` is M-CAN only; C-CAN TX is blocked in app and firmware.
- Trip time is synthesized from live vehicle updates if the adapter/ECU does not provide runtime directly.
- Default profile keeps Vehicle/RCTA/TPMS/media/nav active and disables debug/raw recording, media overlays, UART debug and test leftovers.
- Release settings hide old dashboard animation, UART debug, auto-hide delay, and service-only SAS calibration unless CAN debug is explicitly enabled.
- Navigation adapter state is shown as a table: route, source, `0x48`, `0x45`, `0x47`, `0x4A`, `0x44`, and compass.
- Media settings show both the real display preview and the compact cluster preview for the selected universal format.
- The app log can be shown as a system overlay on the lower half of the screen. It uses a translucent red background, shows USB/media/navigation status plus the latest journal lines, and does not enable CAN raw/debug by itself.
- CAN log export writes to the public `Downloads` folder. The visible preview stays small, while a full capture stores up to `50 000` selected CAN frames and then auto-stops, saves, and compresses to `.log.gz`.

Build:

```bash
./gradlew assembleRelease
```

Release APK name is kept short and stable:

```text
/Volumes/SSD/canbus/release/kia_130.apk
```

The release build is signed with the local Android debug keystore
(`~/.android/debug.keystore`) so it can be installed/tested immediately on the
head unit. A production key can be added later without changing the output path.

The local Gradle output uses the same filename:

```text
app/build/outputs/apk/release/kia_130.apk
```

TEYES sandbox emulator:

```bash
scripts/run_teyes_sandbox.sh
```

The target TEYES screen is `2000x1200`. The sandbox launches at `1000x600` by
default: half size, same 5:3 aspect ratio, so it does not take the full desktop.
Use `KIA_EMULATOR_WIDTH=2000 KIA_EMULATOR_HEIGHT=1200 KIA_EMULATOR_DENSITY=260`
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
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_fm
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_am
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_active
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_teyes_open
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_teyes_active
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_preview
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_failed
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_off
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario rcta_left
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario rcta_right
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario rcta_unknown
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario tpms_low
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario tpms_close
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario vehicle
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario compass --ei step 9
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario log_overlay_on
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario log_overlay_off
```

Direct TEYES nav broadcast check:

```bash
adb shell 'am broadcast --receiver-foreground -a com.yf.navinfo --es state open --es app ru.yandex.yandexnavi'
adb shell 'am broadcast --receiver-foreground -a com.yf.navinfo --es state open --es app ru.yandex.yandexnavi --ef distance_val 120 --es distance_val_str 120 --es distance_unit "м" --es total_distance "4.2 км" --es describe "через 7 мин" --es position "Дружбы" --es direction "turn right" --ei direction_lr 2'
```

The repository intentionally does not track build folders or local Android SDK settings. Release APKs are copied to `/Volumes/SSD/canbus/release`, and the update artifact under `updates/` is committed when publishing a new in-app update.
