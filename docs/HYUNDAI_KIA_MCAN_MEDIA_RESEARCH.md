# Hyundai/Kia M-CAN Media / Cluster Research

Дата: 2026-05-07

Цель: найти публичные данные, какие CAN-сообщения head unit отправляет в cluster,
чтобы на приборной панели отображались источники музыки, радио, USB, навигация,
TBT и связанные статусы.

## Главный Вывод

В открытом доступе есть полезная Hyundai/Kia M-CAN DBC, но она не дает полностью
готовую расшифровку текста радио/треков. Она дает карту сообщений и сигналов:

- какие arbitration ID относятся к `H_U -> CLU`;
- какие ID являются транспортными пакетами (`TP_HU_*_CLU`) под конкретные
  источники;
- какие обычные `HU_CLU_PE_*` кадры несут навигацию, номер трека, время
  воспроизведения, частоту, mute/volume/status.

То есть для нашей задачи это не финальная таблица байтов, а правильный список
CAN ID, на которых надо фокусироваться в логах.

## Источники

- BogGyver/opendbc: `hyundai_2015_mcan.dbc`
  - https://github.com/BogGyver/opendbc/blob/tesla_unity_dev/hyundai_2015_mcan.dbc
  - raw: https://raw.githubusercontent.com/BogGyver/opendbc/refs/heads/tesla_unity_dev/hyundai_2015_mcan.dbc
- BogGyver/opendbc: `hyundai_2015_ccan.dbc`
  - https://github.com/BogGyver/opendbc/blob/tesla_unity_dev/hyundai_2015_ccan.dbc
- BogGyver/opendbc README: DBC describes CAN traffic and recommends Cabana/CANdevStudio for reverse engineering and simulation.
  - https://github.com/BogGyver/opendbc
- commaai/opendbc current project / data browser:
  - https://github.com/commaai/opendbc
  - https://commaai.github.io/opendbc-data/
- Android `Location.getBearing()` official docs, for real compass heading while
  moving:
  - https://developer.android.com/reference/android/location/Location#getBearing()
- Android motion/position sensor docs, for fallback azimuth/rotation-vector
  compass when GPS bearing is unavailable:
  - https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion
  - https://developer.android.com/develop/sensors-and-location/sensors/sensors_position

## Important M-CAN Media IDs

Found in `hyundai_2015_mcan.dbc`. Decimal DBC IDs are converted to hex.

### Transport Packets, Head Unit -> Cluster

These are the most important candidates for displaying media/source/text on the
cluster:

| Hex ID | Decimal | DBC name | Sender | Meaning / candidate use |
|---:|---:|---|---|---|
| `0x4E8` | 1256 | `TP_HU_FM_CLU` | `H_U` | FM radio transport payload to cluster. |
| `0x4E6` | 1254 | `TP_HU_MLT_CLU` | `H_U` | Multimedia / media list transport payload. |
| `0x490` | 1168 | `TP_HU_USB_CLU` | `H_U` | USB source transport payload. Candidate for normal USB title/source display. |
| `0x4EA` | 1258 | `TP_HU_MP_CLU` | `H_U` | Media player transport payload. |
| `0x4EE` | 1262 | `TP_HU_IBOX_CLU` | `H_U` | iBox / Bluetooth-like connected media path candidate. |
| `0x4EC` | 1260 | `TP_HU_DLNA_CLU` | `H_U` | DLNA source. |
| `0x4F2` | 1266 | `TP_HU_CARPLAY_CLU` | `H_U` | CarPlay source. |
| `0x4F4` | 1268 | `TP_HU_ANDAUTO_CLU` | `H_U` | Android Auto source. |
| `0x4BB` | 1211 | `TP_HU_TBT_CLU` | `H_U` | Turn-by-turn navigation transport payload. |
| `0x49B` | 1179 | `TP_HU_NAVI_CLU` | `H_U` | Navigation transport payload. |
| `0x4B7` | 1207 | `TP_HU_DAB_CLU` | `H_U` | DAB radio. |
| `0x4B6` | 1206 | `TP_HU_XM_CLU` | `H_U` | XM / satellite radio. |
| `0x4B4` | 1204 | `TP_HU_DMB_CLU` | `H_U` | DMB source. |
| `0x4E4` | 1252 | `TP_HU_VCDC_CLU` | `H_U` | Virtual CD / CD changer candidate. |
| `0x48F` | 1167 | `TP_HU_Ipod_CLU` | `H_U` | iPod source. |
| `0x48E` | 1166 | `TP_HU_DVD_CLU` | `H_U` | DVD source. |
| `0x48D` | 1165 | `TP_HU_CD_CLU` | `H_U` | CD source. |
| `0x485` | 1157 | `TP_HU_CLU_HF` | `H_U` | Hands-free / phone path candidate. |

DBC describes these TP frames only as raw bytes:

```text
Byte0_TCP_xxx
Byte1_Data_xxx
...
Byte7_Data_xxx
```

So the public DBC tells us the IDs and source type, but not the full text
encoding or packet segmentation rules. We must recover that from logs.

### Cluster -> Head Unit Counterparts

The DBC also lists reverse-direction pairs, useful for handshake/ack checks:

| Hex ID | DBC name |
|---:|---|
| `0x4E9` | `TP_CLU_FM_HU` |
| `0x4E7` | `TP_CLU_MLT_HU` |
| `0x497` | `TP_CLU_USB_HU` |
| `0x4EB` | `TP_CLU_MP_HU` |
| `0x4EF` | `TP_CLU_IBOX_HU` |
| `0x4F5` | `TP_CLU_ANDAUTO_HU` |
| `0x4F3` | `TP_CLU_CARPLAY_HU` |
| `0x4AB` | `TP_CLU_TBT_HU` |
| `0x48C` | `TP_CLU_NAVI_HU` |

If cluster requires request/ack/session state, blindly sending only `H_U -> CLU`
may not be enough. We need log pairs.

## Non-TP Head Unit -> Cluster Signals

These are not raw text transport payloads; they are regular status frames.

### `0x114` / `HU_CLU_PE_01`

DBC signals:

| Signal | Meaning candidate |
|---|---|
| `HU_OpState` | Main head unit operating/source/display state candidate. |
| `HU_Navi_On_Off` | Navigation on/off bit. |
| `HU_Preset_Number` | Preset number. |
| `HU_Tuner_Area` | Radio tuner area. |
| `HU_Track_Number` | Track number. |
| `HU_Play_time_Sec` | Playback seconds. |
| `HU_Play_time_Min` | Playback minutes. |
| `HU_Play_time_Hour` | Playback hours. |
| `HU_Disc_select_No` | Disc select number. |
| `HU_Frequency` | Radio frequency. |

This is a priority message. It likely tells the cluster which display mode/source
is active while TP frames carry the text/content.

### `0x115` / `HU_CLU_PE_02`

TBT display:

- `TBT_Display_Type`
- `TBT_Side_Street`
- `TBT_Direction`
- `TBT_Distance_Turn_Point`
- `TBT_Combined_Side_Street`
- `TBT_Scale`
- `TBT_DistancetoTurnPoint`
- `TBT_Bar_Graph_Level`

### `0x197` / `HU_CLU_PE_05`

Navigation / cluster UI status:

- `HU_LanguageInfo`
- `HU_MuteStatus`
- `HU_VolumeStatus`
- `HU_NaviDisp`
- `HU_NaviStatus`
- `HU_DistanceUnit`
- `HU_Navigation_On_Off`

### `0x1E5` / `HU_CLU_PE_11`

Speed trap / speed limit:

- fixed/mobile/red-light/bus speed trap fields;
- `Navi_SpdLimit_Type`;
- `Navi_SpdLimit_Unit`;
- `Navi_SpdLimit`.

This matches the programmer's note that speed-limit output could cause issues
on cars without that feature.

### `0x1E6` / `HU_CLU_PE_12`

Destination and ETA:

- `Navi_DistToDest_I`
- `Navi_DistToDest_F`
- `Navi_DistToDest_U`
- `Navi_EstimHour`
- `Navi_EstimMin`
- `Navi_EstimTimeType`
- `Navi_Compass`

### `0x1E7` / `HU_CLU_PE_13`

Distance to next maneuver points:

- `Navi_DistToPoint1_*`
- `Navi_DistToPoint2_*`
- `Navi_DistToPoint3_*`

## What We Already Saw In Our Car Log

From `logs/car_can_cleanjump_20260506_220618.txt`:

| Channel | ID | DBC candidate | Count in 30s | Sample |
|---:|---:|---|---:|---|
| `ch0` | `0x114` | `HU_CLU_PE_01` | 136 | `0B 21 FF FF FF FF E1 0F` |
| `ch0` | `0x197` | `HU_CLU_PE_05` | 30 | `10 00 00 00 00 00 00 00` |
| `ch1` | `0x490` | `TP_HU_USB_CLU` | 600 | `00 00 08 20 00 00 00` |
| `ch1` | `0x4E6` | `TP_HU_MLT_CLU` | 300 | `49 C4 00 00 84 84 8B 00` |
| `ch1` | `0x4F4` | `TP_HU_ANDAUTO_CLU` | 600 | `00 00 C0 00 00 00 00 01` |

This is important: the public DBC says `0x490` is USB transport to cluster, and
our car log contains `0x490` at high rate. Treat it as the first candidate for
normal USB source/title rendering.

## Practical Test Plan For Media Source Coverage

Capture short logs while changing only source state:

1. `source_none_idle`: no media app, no USB source selected.
2. `source_usb_selected`: select USB mode in the head unit.
3. `source_radio_fm`: select FM.
4. `source_bt_music`: select Bluetooth music.
5. `source_android_auto_or_navi`: start navigation/media from Android side.
6. `source_carplay`: CarPlay source if available.
7. `source_android_auto_or_projection`: Android Auto/projection source if available.
8. `source_compass_default`: no route/no media default state.

For each log, compare these IDs first:

```text
0x114  HU_CLU_PE_01
0x197  HU_CLU_PE_05
0x490  TP_HU_USB_CLU
0x4E6  TP_HU_MLT_CLU
0x4E8  TP_HU_FM_CLU
0x4EA  TP_HU_MP_CLU
0x4EE  TP_HU_IBOX_CLU
0x4F4  TP_HU_ANDAUTO_CLU
0x4BB  TP_HU_TBT_CLU
0x49B  TP_HU_NAVI_CLU
```

## Current Hypothesis

The cluster display probably needs at least two layers:

1. Regular status frame, especially `0x114 HU_CLU_PE_01`, tells the cluster the
   source/mode/frequency/track context.
2. TP frame for that source, for example `0x490 TP_HU_USB_CLU`, carries the
   display payload or source-specific content.

Therefore removing one "USB text" payload can accidentally kill music/nav if it
also breaks the shared status/scheduler state. The final fix should be narrower:

- either suppress only `0x490 TP_HU_USB_CLU` when the source is not really USB;
- or correct `0x114 HU_OpState` / source state so the firmware does not select
  USB fallback;
- or send the right source-specific TP stream for FM/BT/Android instead of USB.

We need logs to decide which one is true.

## 2026-05-10: Navigation, Compass, And "USB Music" Cross-Check

This section is the current working theory after re-checking public DBC data,
our old programmer firmware notes, APK command analysis, and the failures we
saw during in-car tests.

### Confirmed public DBC facts

The public Hyundai/Kia M-CAN DBC confirms that default compass/navigation is not
a single UART command and not the same thing as text transport. The core M-CAN
frames are:

| CAN ID | DBC name | Role |
|---:|---|---|
| `0x114` | `HU_CLU_PE_01` | Main HU/source state: op state, nav on/off, track, play time, frequency. |
| `0x115` | `HU_CLU_PE_02` | Turn-by-turn display type, direction, distance to turn, bar graph. |
| `0x197` | `HU_CLU_PE_05` | HU/cluster UI status: mute, volume, nav display/status, distance unit, nav on/off. |
| `0x1E5` | `HU_CLU_PE_11` | Speed limit / camera / speed trap. Keep disabled unless trim support is proven. |
| `0x1E6` | `HU_CLU_PE_12` | Destination distance, ETA, and compass. |
| `0x1E7` | `HU_CLU_PE_13` | Distances to next maneuver points. |
| `0x49B` | `TP_HU_NAVI_CLU` | Navigation text transport. |
| `0x4BB` | `TP_HU_TBT_CLU` | Turn-by-turn text transport. |
| `0x490` | `TP_HU_USB_CLU` | USB/media text transport. |
| `0x4E8` | `TP_HU_FM_CLU` | FM radio text transport. |
| `0x4E6` | `TP_HU_MLT_CLU` | Generic multimedia/list transport. |
| `0x4EE` | `TP_HU_IBOX_CLU` | iBox / Bluetooth candidate transport. |
| `0x4F2` | `TP_HU_CARPLAY_CLU` | CarPlay transport. |
| `0x4F4` | `TP_HU_ANDAUTO_CLU` | Android Auto transport on M-CAN; do not confuse with C-CAN `0x4F4` SPAS. |

`0x1E6 HU_CLU_PE_12` contains `Navi_Compass`:

```text
Navi_Compass : 45|6@0+ (7.5,-7.5) [0|352.5] "Degree"
```

Practical encoding:

```text
raw = round((heading_degrees + 7.5) / 7.5) & 0x3F
heading_degrees ~= raw * 7.5 - 7.5
```

So the "default compass" that the programmer firmware shows when there is no
route is most likely a periodic `0x1E6` update with inactive/empty route fields
and a valid `Navi_Compass` value, gated by valid status in `0x114` and `0x197`.

### What the programmer APK/firmware path tells us

Our decoded Sportage APK notes show these app-to-adapter commands:

| APK command | Meaning in our notes | Expected M-CAN result |
|---:|---|---|
| `0x20` | FM/station text | `0x4E8` plus source/status frames. |
| `0x21` | media/radio/source text | Source-specific TP plus `0x114/0x197`. |
| `0x22` | track text | `0x490`, `0x4E6`, `0x4EE`, `0x4F2`, or `0x4F4` depending on actual source. |
| `0x45` | navigation maneuver/TBT | `0x115` plus `0x4BB`, sometimes `0x49B`. |
| `0x47` | ETA/distance to destination | `0x1E6` and possibly `0x1E7`. |
| `0x48` | navigation on/off | `0x114`, `0x197`, and scheduler state for nav frames. |

The important correction: these APK commands are not one-to-one raw CAN frames.
The firmware has to keep a scheduler/state machine and repeat the correct status
frames. A single injected TP frame is often invisible because the cluster was
not put into the matching source/navigation state.

### Why our earlier "remove USB music" patch broke more than USB

The visible bug was repeated "USB Music", but the bad patch class removed or
suppressed too broad a media path. The firmware appears to use the same
source/media scheduler for:

- default compass state;
- HU source/status `0x114/0x197`;
- source-specific text transport;
- navigation/TBT repeat cadence.

Therefore the correct fix is narrow:

1. Keep the programmer v08 scheduler/state behavior.
2. Suppress only false USB fallback display when the HU/app did not report USB.
3. Do not remove `0x114`, `0x197`, `0x1E6`, `0x49B`, or `0x4BB` periodic output.
4. Do not disable the media scheduler just because one source fallback is wrong.

### Correct navigation output model

For reliable cluster navigation output, treat it as a layered sequence:

1. Status layer, repeated:
   - `0x114 HU_CLU_PE_01`: source/op state and `HU_Navi_On_Off`.
   - `0x197 HU_CLU_PE_05`: `HU_NaviDisp`, `HU_NaviStatus`,
     `HU_DistanceUnit`, `HU_Navigation_On_Off`.
2. Compass/ETA layer, repeated:
   - `0x1E6 HU_CLU_PE_12`: always keep compass valid; route fields can be
     empty/inactive when no route.
   - `0x1E7 HU_CLU_PE_13`: only when route/maneuver points are valid.
3. TBT layer, active route only:
   - `0x115 HU_CLU_PE_02`: icon/direction/distance/bar graph.
   - `0x4BB TP_HU_TBT_CLU`: TBT text transport.
4. Street/name layer, active route only:
   - `0x49B TP_HU_NAVI_CLU`: street/name transport.

### Compass data source

For real compass movement we need a heading source before packing `Navi_Compass`:

| Source | Priority | Notes |
|---|---:|---|
| Android navigation bearing | 1 | Best source when route guidance is active. Use maneuver/location bearing from navigation app if exposed. |
| Android fused/GPS bearing | 2 | Works while moving; bad when parked. |
| Last valid bearing | 3 | Keep stable compass when no fresh GPS bearing exists. |
| Demo rotation | test only | Useful in dashboard tests; do not use as production car behavior. |

Do not rotate compass artificially in normal firmware. If no real heading is
available, hold last heading or use a neutral fixed value.

### Theories to test in-car

1. `0x1E6` alone will probably not show the compass unless `0x114/0x197` say
   navigation/cluster display is allowed.
2. `0x49B`/`0x4BB` text alone will probably not show route text unless `0x115`
   and nav status are also valid.
3. Source text needs both source state and correct TP stream:
   - FM: `0x114` + `0x4E8`.
   - USB: `0x114` + `0x490`.
   - Bluetooth/iBox: `0x114` + `0x4EE` or `0x485`.
   - CarPlay: `0x114` + `0x4F2`.
   - Android Auto: `0x114` + `0x4F4`.
4. The TP reverse-direction frames (`0x497`, `0x48C`, `0x4AB`, etc.) may be
   handshake/ack/session state. If cluster ignores repeated text, log these
   pairs before changing firmware.
5. `0x1E5` speed limit/camera should stay off until explicitly verified,
   because the programmer also noted speed-limit output could break trims
   without that function.

### Target capture set for the next road test

Capture M-CAN while only one state changes:

| Test | Required IDs to watch |
|---|---|
| No route, compass visible | `0x114`, `0x197`, `0x1E6`; also check if `0x49B/0x4BB` are absent. |
| Route active, next turn | `0x114`, `0x115`, `0x197`, `0x1E6`, `0x1E7`, `0x49B`, `0x4BB`. |
| Route canceled | Same IDs; find what clears TBT while keeping compass. |
| FM source | `0x114`, `0x197`, `0x4E8`, reverse `0x4E9`. |
| USB source | `0x114`, `0x197`, `0x490`, reverse `0x497`. |
| Bluetooth music | `0x114`, `0x197`, `0x4EE`, `0x485`, reverse `0x4EF`. |
| CarPlay | `0x114`, `0x197`, `0x4F2`, reverse `0x4F3`. |
| Android Auto | `0x114`, `0x197`, `0x4F4`, reverse `0x4F5`. |

For each test, save a clean segment before/after the action. Known steady OBD
signals and already-confirmed body/climate signals can be filtered out; do not
filter the M-CAN HU IDs above.

### Implementation rule for our firmware/dashboard

- The dashboard "Приборка CAN" sender must send a bundle, not a single frame:
  status (`0x114/0x197`) + content (`0x1E6` or TP frame) + repeat cadence.
- The normal firmware should keep programmer v08 behavior as baseline and only
  patch false fallback source selection.
- The logger should mark these IDs as "do not hide as noise" even if they are
  periodic, because periodicity is required for the cluster display.
