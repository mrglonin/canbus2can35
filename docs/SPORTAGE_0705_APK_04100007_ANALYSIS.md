# Sportage 07.05 APK And 04100007 Firmware Analysis

Дата: 2026-05-08

## Input Files

| File | Size | SHA256 | Notes |
|---|---:|---|---|
| `/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/Sportage.apk` | 2458549 | `fb41a1f38c4f2a020aed3003c27301e527e24c01fc83c94512a2357e9e26898e` | Previous Sportage app reference. |
| `/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/Sportage_07_05.apk` | 2458561 | `c5a922b322558192db789ccc48d1510930cda0200cf451215cd0a2c8d9231a2b` | New app from 07.05. Manifest still `com.sorento.navi`, versionName `2.1`, versionCode `1`. |
| `/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/30FFD3054747353756551343_04100005.bin` | 19400 | `4fba0d32af864979a1945ed2528f21ddfa3ee6d66ac78940f0482ea381e7a2c8` | Other-adapter profile, version `04 10 00 05`. Reference for v05 media/reverse/speed behavior. |
| `/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/30FFD3054747353756551343_04100007.bin` | 19616 | `0a93d1eb0e0283f48c563738b78c468d2b5ddc1f4921f3b2c882bf786c48aa2d` | New other-adapter profile, version `04 10 00 07`. UID is not our adapter UID. |

Important: `30 FF D3 05 47 47 35 37 56 55 13 43` is not our adapter UID
(`37 FF DA 05 42 47 30 38 59 41 22 43`). This firmware is useful for reverse
engineering and porting behavior, but should not be flashed as-is through the
stock USB loader.

## APK Diff

ZIP-level diff is small:

| Entry | Old | New | Meaning |
|---|---:|---:|---|
| `classes.dex` | 1242492 bytes, CRC `0x9cc342b7` | 1242504 bytes, CRC `0x5db1c5d9` | Real code change. |
| `assets/dexopt/baseline.prof` | CRC `0x9104157a` | CRC `0x7d9d6145` | ART profile regenerated. |

Resources, manifest, station table, permissions, activities, service names and
receivers are unchanged.

### USB/Adapter Protocol Is Unchanged

The app still talks to the adapter through the same USB serial path:

- supported USB VID/PID: `0403:6001` and `0483:5740`;
- serial parameters: `19200 8N1`;
- APK to adapter frame prefix: `BB 41 A1`;
- adapter to APK frame prefix: `BB A1 41`;
- update command is still command `0x55`;
- UID/version request is still command `0x56`.

High-level command map is unchanged:

| Command | Direction | Meaning |
|---:|---|---|
| `0x20` | APK -> adapter | FM/station text path. |
| `0x21` | APK -> adapter | media/radio/source text path, UTF-16LE, 16 chars. |
| `0x22` | APK -> adapter | track text path, UTF-16LE, 16 chars. |
| `0x30` | bidirectional/app UI | amplifier settings/status. |
| `0x44` | APK -> adapter | speed limit. |
| `0x45` | APK -> adapter | navigation maneuver/TBT. |
| `0x47` | APK -> adapter | ETA/distance to destination. |
| `0x48` | APK -> adapter | navigation on/off. |
| `0x51` | adapter -> APK | TPMS/status broadcast path. |
| `0x55` | APK -> adapter | firmware update. |
| `0x56` | adapter -> APK | adapter UID/version reply. |
| `0x60` | adapter -> APK | settings/status. |

### Real APK Change: BL/FYT Media Callback Cleanup

Changed methods with real signal:

| Class/method | Old | New | Meaning |
|---|---:|---:|---|
| `Ld00.<clinit>` | 25 dex words | 23 dex words | callback arrays changed, new state fields inserted. |
| `Lb00.onServiceConnected` | 144 dex words | 134 dex words | app registers fewer FYT callbacks. |
| `Lc00.onTransact` | 208 dex words | 242 dex words | app adds media fallback/debounce logic. |
| `UartService.onDestroy` | 118 dex words | 103 dex words | unregister path matches new callback list. |

Decoded behavior:

- Old app registered module `0` callbacks `[49, 74]`.
- New app registers module `0` callback `[74]` only.
- Module `2` callbacks `[0, 1, 8, 18, 13]` are unchanged.
- New static fields were added:
  - `Ld00.m: String` = last BL/FYT media string/path;
  - `Ld00.n: long` = last send timestamp;
  - arrays were shifted from `m/n` to `o/p`.
- In `Lc00.onTransact` the new app:
  - handles only callback id `74`;
  - when it receives int array `[8, 0]`, stores string0 as fallback/source text;
  - when it receives int array `[8, 2]`, sends string1 as track source;
  - if the same media string repeats within 500 ms, it suppresses the duplicate;
  - if `MediaMetadataRetriever` cannot extract title metadata, it falls back to the stored string.

Practical meaning: the new APK mostly improves BL/FYT media duplicate
suppression and fallback-title handling. It does not add a new CAN protocol, a
new update protocol, or new USB transport.

## Firmware 04100007 Decode

`04100007` uses the same decode key as the already-analyzed `04100005`:

```text
key_a = 0x04
key_b = 0x58
```

Evidence:

- decoded app vector is valid: SP `0x20010000`, reset `0x08006EB9`;
- `04100005 -> 04100007` size delta is exactly `+216` bytes (`0xD8`);
- after a `+0xD8` shift the decoded images match about `86.94%`, which is a
  strong close-version match;
- protocol frame literals remain structurally identical and shift by `0xD8`.

Decoded local artifacts:

```text
/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/canbox-fw-lab/decode_04100007_key0458/30FFD3054747353756551343_04100007.decoded_package.bin
/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/canbox-fw-lab/decode_04100007_key0458/30FFD3054747353756551343_04100007.decoded_payload.bin
/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/canbox-fw-lab/decode_04100007_key0458/30FFD3054747353756551343_04100007.decoded_app_stlink64k.bin
```

### 07 vs 05 Firmware Differences

| Area | v05 | v07 | Meaning |
|---|---:|---:|---|
| package size | 19400 | 19616 | `+216` bytes. |
| payload size | 19384 | 19600 | `+216` bytes. |
| reset vector | `0x08006DE1` | `0x08006EB9` | reset code shifted by `0xD8`. |
| APK command literals | present | same set, shifted | no new USB command family. |
| adapter reply literals | present | same set, shifted | update/status protocol preserved. |
| media/source parser region | around `0x08005764..0x080059C0` | changed around `0x0800583C..0x08005A98` plus local inserts | more media/source cleanup/debounce logic. |
| C-CAN/body parser region | around `0x08004EF4..0x080051A8` | changed around `0x08004EF0..0x080052xx` | vehicle state paths changed; includes reverse/speed/steering-related logic. |

Protocol literals moved as expected:

| Meaning | v05 address | v07 address |
|---|---:|---:|
| APK command `0x22` template | `0x08008A5C` | `0x08008B34` |
| APK command `0x20` template | `0x08008A64` | `0x08008B3C` |
| adapter reply `0x20` | `0x08008A8C` | `0x08008B64` |
| adapter reply `0x22` | `0x08008A94` | `0x08008B6C` |
| adapter reply `0x51` | `0x08008B00` | `0x08008BD8` |
| adapter reply `0x56` | `0x08008B30` | `0x08008C08` |
| adapter reply `0x60` | `0x08008B50..0x08008B61` | `0x08008C28..0x08008C39` |
| adapter reply `0x30` | `0x08008B8C` | `0x08008C64` |

Practical meaning: v07 keeps the same app/adapter protocol. It is a behavior
update inside CAN/media handling, not a transport/interface change.

## Cross-Reference With Our Logger Data

| Function group | Our log/matrix evidence | New APK/FW relevance | Current conclusion |
|---|---|---|---|
| USB/FYT/BL music | APK sends command `0x22`; our matrix has M-CAN media candidates `0x490`, `0x4E6`, `0x4E8`, `0x4EE`. | APK 07.05 removes callback `49`, keeps `74`, adds 500 ms duplicate suppression and fallback title logic. | This directly targets repeated BL/USB track events. Keep real track command path; do not delete scheduler states. |
| FM/radio | APK command `0x20/0x21`; matrix candidate `0x4E8`. | Protocol unchanged. Firmware literals shifted only. | FM path should remain compatible with previous app/firmware behavior. |
| Navigation/TBT | APK commands `0x45`, `0x47`, `0x48`; matrix candidates `0x115`, `0x4BB`, `0x49B`, `0x1E6`, `0x1E7`; `0x1E5` speed limit is disabled-risky. | APK nav path unchanged. Firmware protocol unchanged. | Existing nav sender/test tool should stay valid. Missing display is likely firmware-side gating/source-state timing, not a new APK command. |
| Vehicle speed to HU | Our matrix/log has C-CAN `0x316` (`EMS11`, `VS/RPM`). v05 firmware already used `0x316` for speed to HU. | v07 changes the same parser region. | Strong match: `0x316` remains the correct speed source candidate. |
| Reverse gear / 12V reverse output | Our Sportage log best current candidate is C-CAN `0x111`/`0x354`; v05 other-profile reverse used id `0x169` low nibble == `7`. | v07 still has profile-specific reverse/body parser changes. | Do not blindly port other-profile `0x169` to our car. Our user-profile `04350006` already proved reverse 12V can work, so compare against that path separately. |
| Steering angle / dynamic lines | Our matrix: `0x2B0` (`SAS11`) and `0x381` (`MDPS11`). | v07 body/parser region references steering-related constants and keeps parking logic family. | Use our logger ids for dynamic-line validation; do not infer final byte mapping from v07 alone. |
| Parking sensors/SPAS/RCTA | Matrix candidates: `0x436`, `0x390`, `0x4F4`, `0x58B`. Captured parking walk did not produce a clean obstacle/RCTA event. | v07 does not expose new APK-side commands for this. | Need a better in-car segment with reverse active and real obstacle events if we want exact zone bytes. |
| Climate display | Matrix candidates: M-CAN `0x131/0x132/0x134`, C-CAN `0x042/0x043`; many states were seen in our log. | APK 07.05 does not change climate logic. | Climate should be handled by firmware/CAN profile, not by the APK update. |
| Steering wheel buttons | Matrix candidate `0x523`, but our captured CAN did not confidently identify button presses. | APK 07.05 does not change SWRC logic. | Buttons likely go through UART/SimpleSoft/analog bridge on this setup, or need a cleaner isolated CAN segment. |

## Practical Porting Rules

1. For our adapter, keep UID `37 FF DA 05 42 47 30 38 59 41 22 43`.
2. Do not flash `30FFD...04100007.bin` directly as our firmware; it is a
   reference profile.
3. For full media/source support, port logic style from v05/v07:
   - cache previous source/track state;
   - clear stale media only when source changes;
   - debounce duplicate track events;
   - preserve normal `0x20/0x21/0x22` text paths.
4. Do not remove media scheduler entries as a feature strategy. We tested that
   class of patch already: it removes useful source/compass/media behavior.
5. Keep logger mode independent from normal mode. The logger should observe
   `ch0=100000` and `ch1=500000` as before; v07 does not change the GS USB
   logger protocol.

## What Changed In Plain Terms

- APK 07.05: BL/FYT media duplicate suppression and fallback title handling are
  useful for complete source/track behavior.
- Firmware 04100007: close successor of 04100005, `+216` bytes, same USB
  protocol, changed CAN/media handling regions.
- Our CAN logger table is still valid. The main confirmed overlap is speed
  source `0x316` and media text/source ids. Reverse and parking still need
  profile-specific handling, because other-person `0410` profile does not prove
  the same reverse id for our `0435` adapter.
