# Sportage Android app

Android 15/TEYES app for Kia Sportage CANBOX integration.

Current package: `com.sorento.navi`

Current app version: `9.3-raw-can-logger` (`versionCode 73`)

Main protocol notes:

- штатный canbox remains on the normal USB CDC interface;
- `0x70 01` enables the raw CAN logger;
- `0x70 00` disables the raw CAN logger;
- `0x76` reads the next raw CAN frame from the logger queue;
- bus `0` is C-CAN;
- bus `1` is M-CAN;
- OBD UI derives speed, rpm, temperatures and body state from raw CAN frames.

Build:

```bash
gradle assembleRelease
```

The repository intentionally does not track generated APK files, build folders or local Android SDK settings.
