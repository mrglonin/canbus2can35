# Kia Android app

Android 15/TEYES app for Kia CANBOX integration.

Current package: `kia.app`

Current app version: `13.2-kia` (`versionCode 112`)

Main notes:

- штатный canbox remains on the normal adapter interface;
- Vehicle, TPMS, RCTA/blind-spot, media and navigation features are enabled by
  default;
- debug/raw recording is explicit opt-in and is not used for normal UI;
- media source hints never overwrite a playing MediaSession;
- music is sent to the cluster once per source/track change, not held by
  repeat spam;
- trip time is synthesized from live vehicle updates if the adapter/ECU does
  not provide runtime directly;
- release settings hide old dashboard animation, UART debug, auto-hide delay,
  and service-only SAS calibration unless CAN debug is explicitly enabled;
- CAN log export writes to the public `Downloads` folder. The visible preview
  stays small, while a full capture stores up to `50 000` selected CAN frames
  and then auto-stops, saves, and compresses to `.log.gz`.

The detailed adapter USB/API exchange is intentionally not documented in this
public README.

Build:

```bash
./gradlew assembleRelease
```

Release APK name is kept short and stable:

```text
/Volumes/SSD/canbus/release/kia_132.apk
```

The release build is signed with the local Android debug keystore
(`~/.android/debug.keystore`) so it can be installed/tested immediately on the
head unit. A production key can be added later without changing the output path.

The local Gradle output uses the same filename:

```text
app/build/outputs/apk/release/kia_132.apk
```

TEYES sandbox emulator:

```bash
scripts/run_teyes_sandbox.sh
```
