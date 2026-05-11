# Sportage Android app

Android 15/TEYES app for Kia Sportage CANBOX integration.

Current package: `com.sorento.navi`

Current app version: `9.4-obd-bsm-polish` (`versionCode 74`)

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

The repository intentionally does not track generated APK files, build folders or local Android SDK settings.
