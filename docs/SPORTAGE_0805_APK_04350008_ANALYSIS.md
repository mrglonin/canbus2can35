# Sportage 08.05 APK And 04350008 Firmware Analysis

Date: 2026-05-08

## Input Files

| File | Size | SHA256 | Notes |
|---|---:|---|---|
| `/Users/legion/Downloads/ReadMe.txt` | 6131 | `1407ef2563878d0780491fc250cb59639beccc5a4bb311c0c94f6e12d9ac59d2` | Programmer release notes and test checklist. |
| `/Users/legion/Downloads/Sportage_08_05.apk` | 2458369 | `476976c54c032808f5f84fb2b8da78bdf20f393f53d6d880a74939869759e227` | Sportage APK, package `com.sorento.navi`, versionName `2.1`, versionCode `1`. |
| `/Users/legion/Downloads/37FFDA054247303859412243_04350008.bin` | 19464 | `6b0f718f89e1bc1955dbeb03c0874d9ec83f1bf506cb1ce66920f7bca70d186f` | Programmer update for our adapter UID `37 FF DA 05 42 47 30 38 59 41 22 43`, version `04 35 00 08`. |

## Programmer Changelog For 08

Release note summary:

- TPMS request disabled because the request block/message id is not known.
- Bluetooth music title and artist are added.
- USB music display now updates not only when the track number changes, but
  also when the title changes.
- Internal handling optimized.

The release checklist still separates functions by bus:

| Bus | Functions listed for validation |
|---|---|
| M-CAN | FM, AM, music, BL music, BL call, navigation street names, TBT, distance, ETA/time, climate parameters, amplifier control. |
| C-CAN | Doors/trunk, reverse, dynamic parking lines, SPAS parking sensors, outside temperature, engine temperature, turn signals, speed to head unit, TPMS disabled. |

Known status from the note:

- FM and navigation paths are expected to work.
- Hood is still not confirmed in the body-open group.
- BL call has no known message id for phone number sending.
- Outside and engine temperature need in-car correctness checks.
- TPMS request is intentionally disabled in v08.

## APK 08.05 Scan

Manifest/protocol level:

| Field | Value |
|---|---|
| package | `com.sorento.navi` |
| app name | `Sportage` |
| version | `2.1` / `1` |
| USB service | `com.sorento.navi.UartService` |
| USB permission action | `com.sorento.USB_PERMISSION` |
| station asset | `assets/stations.txt`, SHA256 `d8eea4e551561546b070ce7e3769647c5658d7ba6d3cc8cdf45b1bc69a2e4399` |

The high-signal DEX strings still show the same adapter protocol family:

| Item | Evidence |
|---|---|
| USB serial stack | `com.hoho.android.usbserial.driver.*` |
| Navigation actions | `ACTION_NAVI_ON_DATA`, `ACTION_MANEUVER_DATA`, `ACTION_ETA_DATA`, `ACTION_SPEED_DATA`, `ACTION_EXCEEDED_DATA` |
| Adapter replies | `SETTING_DATA_RECEIVED`, `TPMS_DATA_RECEIVED`, `AMP_DATA_RECEIVED`, `CONFIRMATION` |
| Firmware metadata | `uid_data_bytes`, `ver_data_bytes` |
| User assets | `stations.txt` |

Conclusion: no new USB transport was found. v08 keeps the same USB/APK command
family as the previous Sportage app work; the main change is behavior in media
data preparation and the matching firmware-side CAN output logic.

## Firmware 04350008 Decode

`04350008` decodes with the same key pair used for our `0435` updates:

```text
key_a = 0x04
key_b = 0x5B
```

Decode evidence:

| Field | Value |
|---|---|
| roundtrip encode/decode | OK |
| input size | 19464 bytes |
| version bytes | `04 35 00 08` |
| initial SP | `0x20010000` |
| reset vector | `0x08006E3D` |
| decoded payload end | `0x08008BF8` |

The new application still leaves enough free room before the preserved local
wrapper at `0x08008C20`, so the existing working mode3 logger slot can be kept.

## Updated Three-Mode Package

New package:

```text
firmware/canlog/2can35_04350008_canlog_v4_mode3_preserve_beeps_usb.bin
SHA256 846d761cd1e7c26673c40bd9e8193e142b744f9ad2d7ab2a5a152b4d42b6c053
```

Local flash copy:

```text
/Users/legion/Downloads/2CAN35_04350008_CanLog_v4_Mode3_PreserveBeeps_USB.bin
SHA256 846d761cd1e7c26673c40bd9e8193e142b744f9ad2d7ab2a5a152b4d42b6c053
```

ST-Link recovery image generated locally:

```text
/Users/legion/Downloads/2CAN35_04350008_CanLog_v4_Mode3_PreserveBeeps_STLINK64K.bin
SHA256 c520b3aa3cee3da79c33549f8a822014b58868b8cb01a9a06b3f9cea6dee8d6a
```

Behavior:

| Mode | Behavior |
|---|---|
| mode1 | Programmer v08 canbox behavior. |
| mode2 | Stock update path through existing command `0x55`, value `0x01`. |
| mode3 | Preserved `gs_usb` / budgetcan CAN logger at `0x08009000`; hardware-tested entry is command `0x51`, value `0x03`. |
| reset | Existing software reset request, value `0x04`. |

Porting detail: the v08 command dispatch sequence moved by four bytes compared
with the older v06 patch, so hooks were updated to the v08 addresses:

| Purpose | v08 address | Action |
|---|---:|---|
| command dispatch A | `0x08005478` | jump to local dispatch `0x08008C21` |
| command dispatch B | `0x08005486` | jump to local dispatch `0x08008C51` |
| reset vector | `0x08004004` | changed from `0x08006E3D` to `0x08008CE1` |
| update target A literal | `0x08008D64` | changed to `0x08005DA1` |
| update target B literal | `0x08008D68` | changed to `0x08005DA5` |
| original reset literal | `0x08008D80` | changed to `0x08006E3D` |

The builder is reproducible:

```sh
python3 tools/build_04350008_mode3_package.py
```

## Practical Test Plan

1. Flash `2CAN35_04350008_canlog_v4_mode3_preserve_beeps_usb.bin` through the
   current working update path.
2. Boot mode1 and check normal canbox behavior:
   - reverse output +12V;
   - doors/trunk;
   - climate display;
   - FM/AM/music/BL music;
   - navigation street/TBT/distance/ETA;
   - dynamic parking lines and SPAS if safe to test.
3. From mode1, send software mode3 request `0x51/0x03` and verify USB enumerates as
   `gs_usb` / budgetcan, VID/PID `1d50:606f`.
4. Return from mode3 to mode1 and confirm normal canbox behavior comes back.
5. Use the APK 08.05 update flow only if we need to compare pure programmer
   behavior against the three-mode package.

## Current Conclusion

The v08 programmer behavior is now the mode1 base for our practical package.
The local additions are limited to the mode switch hooks, reset hook and
preserved logger area. No media/TPMS/parking behavior was cut from v08.
