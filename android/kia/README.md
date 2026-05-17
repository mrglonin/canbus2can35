# Kia Android app

Android 15/TEYES app for Kia CANBOX integration.

Current package: `kia.app`

Current app version: `14.6-kia` (`versionCode 126`)

Main notes:

- штатный canbox remains on the normal adapter interface;
- Vehicle, TPMS, RCTA/blind-spot, media and navigation features are enabled by
  default;
- debug/raw recording is explicit opt-in; TPMS may open the sideband raw reader
  quietly so CAN `0x593` works without enabling the visible logger;
- media source hints never overwrite a playing MediaSession;
- BT music source is emitted as the Bluetooth source event with title text;
- stock CAN TPMS pressure is read only from `0x593` frames with `00 11` prefix;
- compact/floating main window switches to metric-only mode instead of squeezing
  the car artwork;
- compact/floating TPMS window switches to four full-screen tire widgets instead
  of squeezing the car artwork;
- TPMS top icon is hidden when TPMS is disabled;
- home dashboard widgets can be toggled and reordered individually from OBD /
  Vehicle settings;
- settings screens use a dark theme with high-contrast active/inactive switches;
- AMP tab can be hidden from general settings for cars without the stock
  amplifier block;
- media-to-cluster formatting moved from general settings to CANBUS settings;
- startup screen checks APK and adapter BIN updates and shows an update dialog
  when a newer package is detected;
- turn signals are PNG overlays on the car image with alpha-only blinking, no
  scale or position movement;
- the home outside-temperature widget keeps the old `0x044` value, while the
  separate climate widget shows the set climate temperature parsed from CAN
  climate frames when available;
- the home trip widget shows synthesized trip distance plus trip time when the
  adapter/ECU does not provide odometer data directly;
- compass `0x45` is resent periodically while the heading is stable, so a missed
  adapter write does not leave the cluster stuck;
- navigation street/TBT text is pushed immediately on new events instead of
  waiting for the replay tick;
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
/Volumes/SSD/canbus/release/kia_146.apk
```

The release build is signed with the local Android debug keystore
(`~/.android/debug.keystore`) so it can be installed/tested immediately on the
head unit. A production key can be added later without changing the output path.

The local Gradle output uses the same filename:

```text
app/build/outputs/apk/release/kia_146.apk
```

TEYES sandbox emulator:

```bash
scripts/run_teyes_sandbox.sh
```
