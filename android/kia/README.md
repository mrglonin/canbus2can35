# Kia Android app

Android 15/TEYES app for Kia CANBOX integration.

Current package: `kia.app`

Current app version: `10.9-kia` (`versionCode 89`)

Main protocol notes:

- штатный canbox remains on the normal USB CDC interface;
- `0x70 01` enables the raw CAN logger;
- `0x70 00` disables the raw CAN logger;
- `0x76` reads the next raw CAN frame from the logger queue;
- bus `0` is C-CAN;
- bus `1` is M-CAN;
- OBD UI derives speed, rpm, voltage, temperatures and body state from raw CAN frames.
- RCTA/blind-spot overlay uses reverse state plus C-CAN `0x58B` side warnings.

Build:

```bash
gradle assembleRelease
```

TEYES sandbox emulator:

```bash
scripts/run_teyes_sandbox.sh
```

The target TEYES screen is `2000x1200`. The sandbox launches at `1000x600` by
default: half size, same 5:3 aspect ratio, so it does not take the full desktop.
Use `KIA_EMULATOR_WIDTH=2000 KIA_EMULATOR_HEIGHT=1200` when a full
logical-resolution run is needed.

RCTA overlay debug:

```bash
adb shell am broadcast -a kia.app.DEBUG_RCTA --es side left
adb shell am broadcast -a kia.app.DEBUG_RCTA --es side right
adb shell am broadcast -a kia.app.DEBUG_RCTA --es side both
adb shell am broadcast -a kia.app.DEBUG_RCTA --es side off
```

The repository intentionally does not track generated APK files, build folders or local Android SDK settings.
