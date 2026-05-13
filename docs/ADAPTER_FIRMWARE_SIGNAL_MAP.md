# Adapter firmware signal map

Дата: 2026-05-14.

Цель карты: описать, что именно должна держать прошивка адаптера, что ей
отдает APK и что уходит дальше в штатный parser/M-CAN. Это не DBC и не большая
таблица Android-логики. Это рабочая карта для чистой прошивки адаптера.

Текущая V21 уже реализует базовый adapter-owned hold для compass/nav. Целевая
карта расширения и acceptance зафиксированы отдельно:
[FIRMWARE_ADAPTER_OWNED_LOGIC_TARGET.md](FIRMWARE_ADAPTER_OWNED_LOGIC_TARGET.md).

## Главная модель

```text
Android APK
  -> stock USB/API commands
  -> adapter firmware stock parser / small state holder
  -> штатный вывод в M-CAN/cluster
```

APK собирает сложное состояние: источник музыки, artist/title, навигацию,
GPS bearing, UI, предупреждения. Прошивка не должна тащить большие таблицы,
названия приложений, DBC и UI-логику.

Прошивка должна делать четыре простые вещи:

1. Принимать проверенные USB/API команды и отдавать их штатному parser.
2. Держать маленькие последние состояния, где нужен hold/repeat.
3. Пассивно слушать CAN-C/M-CAN и отдавать compact snapshot.
4. Давать чистый logger и диагностический TX без включения в обычный режим.

## CAN роли

| Шина | Роль | Правило |
|---|---|---|
| CAN-C / bus0 | слушать машину: скорость, RPM, температуры, двери, reverse, RCTA candidates | production read/listen-only |
| M-CAN / bus1 | слушать cluster/HU сторону: voltage и будущие штатные cluster events | production read/listen-only |
| M-CAN TX | diagnostic/raw TX, если штатный API не покрывает тест | только explicit debug/TX unlock |
| CAN-C TX | не использовать в production | только отдельная lab-сборка или явный unsafe unlock |

Практический вывод: `0x78` в чистой production-модели должен быть M-CAN-only.
CAN-C отправку лучше не держать в обычной прошивке, потому что это сторона
автомобиля, где случайная TX-команда может мешать штатным ECU. Logger по обеим
шинам оставить.

## USB/API команды адаптера

| Команда | Кто вызывает | Что значит | Период |
|---:|---|---|---|
| `0x20` | APK media | radio/AM/BT artist text candidate, UTF-16LE | event-only |
| `0x21` | APK media | FM/media source/station text candidate, UTF-16LE | event-only |
| `0x22` | APK media | title text, UTF-16LE | event-only |
| `0x44` | APK nav | speed limit | event/change; V21 repeats active route |
| `0x45` | APK nav/compass | maneuver or compass direction | event/change; V21 repeats compass/nav |
| `0x47` | APK nav | ETA / route distance | event/change; V21 repeats active route |
| `0x48` | APK nav | nav on/off | route state change |
| `0x4A` | APK nav | street/text, UTF-16LE | event/change; V21 repeats active route |
| `0x55` | updater | firmware update mode/block | updater only |
| `0x56` | APK/tools | UID/version | request only |
| `0x70` | debug UI | raw stream on/off | debug only |
| `0x76` | debug UI | pop raw frame from logger ring | debug only |
| `0x77` | APK Vehicle/RCTA/nav/compass | compact snapshot + hold tick | about `500 ms` lightweight poll |
| `0x78` | debug UI | one-shot raw TX | explicit debug; target M-CAN |
| `0x79` | APK/tools | health/capabilities | request only |
| `0x7A` | APK media/nav source | inject Raise/RZC `FD .. 09 ...` into stock parser | event-only |

## Compass

APK input:

```text
GPS/fused bearing
valid only when location fresh, accuracy good, and movement is believable
```

Adapter command:

```text
BB 41 A1 0E 45 08 00 00 DD 00 78 00 A0 CS
```

Formula:

```text
uiStep = 0, 3, 6, ... 33
DD = (36 - uiStep) % 36
```

Firmware behavior:

| State | Behavior |
|---|---|
| valid bearing | accept `0x45` and let stock parser output cluster compass |
| hold active | V21 repeats last compass frame on compact `0x77` tick |
| bearing stale | APK stops sending new values; V21 keeps last stored frame until a future timeout extension |
| nav maneuver active | nav `0x45` has priority over compass-only frame |

Current APK sends only compass value changes. V21 owns the repeat by replaying
the stored `0x45` compass frame through the stock parser.

## Navigation

Navigation source is separate from music source:

```text
0x7A payload: FD 06 09 06 00 00 15
```

Route command bundle:

| Command | Meaning | Notes |
|---:|---|---|
| `0x48` | nav on/off | send once on route active/off |
| `0x45` | maneuver/TBT | also shares command id with compass |
| `0x47` | ETA / distance left | active route only |
| `0x4A` | street/text | UTF-16LE |
| `0x44` | speed limit | active route only |

State rules:

| APK state | Firmware/adapter behavior |
|---|---|
| active route starts | source `0x7A nav`, then `0x48 on`, then route frames |
| maneuver/street/ETA/speed changes | update only changed fields |
| active route continues | repeat last active nav bundle about `1 s` if cluster needs hold |
| preview/search/failed | do not overwrite active route immediately |
| finish | hold finish about `5 s`, then one clean nav off |
| navigator off | clear once, no off/on blinking |

V21 stores the last nav bundle and does active-route repeat inside the adapter.
APK sends only state changes and finish/off.

## Media sources

Media is event-only. Do not hold the same source/title by repeating packets.
Send again only when source, artist, title or duration/progress identity changes.

| Source | Source-status into `0x7A` | Text fields |
|---|---|---|
| USB/local/cloud/Yandex | `FD 0A 09 16 00 01 00 00 SS CS` | `0x22 title` |
| Bluetooth audio | `FD 06 09 0B 04 00 CS` | `0x20 subtype 0x1F artist`, `0x22 title` |
| FM radio | `FD 08 09 02 00 65 46 00 CS` | `0x21 station/text` |
| AM radio | `FD 06 09 09 00 00 CS` | `0x20 station/text` |
| BT phone | `FD 06 09 07 01 00 CS` | no confirmed visible text |
| Navigation source | `FD 06 09 06 00 00 15` | not a music source |

`SS` in USB-like source-status is a small seconds/progress byte. It should not
force a 1 Hz music spam. Use it when a new source/track event is emitted.

Text naming:

| Name in app | Cluster command | Meaning |
|---|---:|---|
| `source` | `0x7A FD .. 09 ..` | mode/source selector |
| `artist` | usually `0x20 subtype 0x1F` for BT | performer/station helper |
| `title` | `0x22` for USB/cloud/BT | main track/title field |
| `stationText` | `0x21` FM, `0x20` AM | radio label/frequency |

Universal pretty text rule:

```text
preferred: title field = "Title"
if title is empty: title field = source/station
artist field = "Artist" where confirmed
do not pack "Artist - Title - time" into every field until a wider confirmed
cluster field is found
```

Reason: current text fields are short and visible output differs by source.
Packing everything into one field can make the cluster show clipped garbage.

## Vehicle snapshot `0x77`

APK polls `0x77` every `500 ms` as the lightweight production poll when
Vehicle/RCTA/nav/compass is active. No raw stream is needed for normal UI.

Payload v21:

```text
status,
known24,
counter_le32,
speed_kmh_u16,
rpm_u16,
coolant_c_s16,
voltage_mv_u16,
throttle_pct_u8,
brake_u8,
gear_u8,
fuel_pct_u8,
outside_c_s16,
fuel_rate_x10_u16,
odometer_km_u32,
reserved2,
rcta_valid_u8,
rcta_bus_u8,
rcta_dlc_u8,
rcta_id_le32,
rcta_data8
```

Listener map:

| CAN ID | Bus | Firmware state | Notes |
|---:|---|---|---|
| `0x316` | C-CAN | speed/RPM | `DATA[6]`, RPM raw / 4 |
| `0x329` | C-CAN | coolant | `(DATA[1] - 0x40) * 0.75` |
| `0x044` | C-CAN | outside temp | `(DATA[3] - 0x52) / 2` |
| `0x169` | C-CAN/M-CAN candidate | reverse/gear | reverse when low nibble is `0x07` |
| `0x132` | M-CAN | voltage | `DATA[0] / 10 V`, stored as mV |
| `0x4F4` | C-CAN or M-CAN candidate | RCTA/blind spot raw payload | snapshot carries actual bus |
| `0x541` | C-CAN | body/front doors | APK may decode from raw/debug or future snapshot |
| `0x553` | C-CAN | rear doors | APK may decode from raw/debug or future snapshot |

`0x4F4` RCTA rules:

| Payload | Meaning |
|---|---|
| `0000C00000000001` | idle |
| starts with `0001` | active warning |
| known left/right payloads | classify by table/masks in APK |
| unknown active payload | show rear warning and log short |

Important: until the real car confirms exact bus, firmware should listen for
`0x4F4` on both CAN-C and M-CAN and expose `rcta_bus` in snapshot.

## Logger and debug

Keep logger clean and separate from production state.

| Tool | Keep | Rule |
|---|---|---|
| `0x70` stream switch | yes | off by default |
| `0x76` raw ring pop | yes | only after explicit debug |
| ring buffer C-CAN/M-CAN | yes | no UI spam unless debug/recording |
| compressed CAN log | yes | app saves `.log.gz` to `Downloads`; capture is limited to `50 000` CAN frames and auto-stops/saves at the limit |
| `0x78` TX | yes, but restricted | V21 production accepts M-CAN only; C-CAN returns bad/blocked bus |
| UART bridge | no | not part of clean firmware |
| old `0x74` TX | no | obsolete |

## What can be removed from clean firmware

Remove or keep out of production firmware:

- transparent UART sideband;
- production use of continuous raw stream;
- Android app source detection;
- large DBC/tables/names;
- media title composition;
- route parsing;
- automatic raw TX to CAN-C;
- old `0x74` TX path;
- one-off source scanners and brute-force test paths.

Keep:

- stock canbox/update-loader;
- UID/version;
- `0x79` health;
- `0x7A` stock parser injection;
- `0x20/0x21/0x22`, `0x44/0x45/0x47/0x48/0x4A`;
- compact `0x77` snapshot;
- clean debug logger for CAN-C and M-CAN;
- guarded raw TX, M-CAN only in production.

## Target update periods

| Surface | Normal period |
|---|---|
| Media source/text | no period, event-only |
| Compass hold | V21 repeat on compact `0x77` tick while no active route is held |
| Active nav bundle | about `1 s` from V21 |
| Vehicle/RCTA snapshot polling by APK | about `500 ms` lightweight production poll |
| Full raw logger | user-controlled debug only |
| Health `0x79` | manual/request only |
| UID/version `0x56` | startup/request only |

If something needs sub-500 ms reaction in the car, first confirm it by live log,
then add only that field to the compact snapshot or a dedicated event command.
