# Прошивка v21: штатный canbox + adapter-owned nav/compass hold

Дата: 2026-05-14.

v21 построена от авторской `04350008` и сохраняет штатный canbox/update-loader.
USB identity не меняется: `0483:5740`, stock CDC/proprietary protocol.

Полная карта ролей прошивки, команд, периодов обновления и CAN-шин:
[ADAPTER_FIRMWARE_SIGNAL_MAP.md](ADAPTER_FIRMWARE_SIGNAL_MAP.md).

## Что изменено относительно v20

- Обычный Vehicle/RCTA режим больше не требует постоянного raw stream в APK.
- FIFO hook прошивки пассивно обновляет маленькое состояние и отдает его через `0x77`.
- M-CAN `0x132 DATA[0]` пишется в snapshot как voltage mV (`DATA[0] / 10 V` в APK).
- Последний `0x4F4` blind spot/RCTA копируется в snapshot: valid, bus, dlc, id, data8.
- `0x44/0x45/0x47/0x48/0x4A` сохраняются в маленький hold-state прошивки.
- Активная навигация повторяется из адаптера примерно раз в `1 s` на `0x77` tick.
- Компас `0x45` повторяется из адаптера на `0x77` tick, когда нет active route.
- `0x70/0x76` оставлены только для debug/raw log.
- `0x7A`, update-loader `0x55`, UID/version `0x56` не менялись.
- `0x78` оставлен только как guarded diagnostic TX в M-CAN; bus `0`/C-CAN теперь отклоняется `ACK 02`.

## Файлы

```text
firmware/trusted/v21/21_v08_mode1_v21_USB.bin
firmware/trusted/v21/21_v08_mode1_v21_STLINK64K.bin
firmware/trusted/v21/21_v08_mode1_v21.report.json
```

## Команды

| Команда | Назначение |
|---:|---|
| `0x70` | raw CAN stream on/off только для debug |
| `0x76` | pop один raw C-CAN/M-CAN frame только для debug |
| `0x77` | compact Vehicle/RCTA snapshot + hold tick для nav/compass |
| `0x78` | one-shot raw CAN TX только в M-CAN (`bus=1`) |
| `0x79` | V21 health/capabilities |
| `0x7A` | inject Raise/RZC `FD .. 09 ...` media/navigation source status |

`0x79` ответ:

```text
BB A1 41 0F 79 21 03 00 FF 01 56 32 31 00 02
```

Payload:

```text
21 03 00 FF 01 56 32 31 00
```

## Snapshot `0x77`

Payload 45 bytes:

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

APK опрашивает `0x77` примерно раз в `500 ms` как легкий production poll для
Vehicle/RCTA/voltage и как tick удержания nav/compass. Raw stream включается
только явным debug switch.

## Adapter-owned hold

Прошивка перехватывает входящие штатные USB/API команды до stock dispatcher,
копирует полезный кадр в RAM и сразу пропускает его дальше в parser автора:

| Command | Hold slot |
|---:|---|
| `0x45` with payload byte `0x08` | compass frame |
| other `0x45` | nav maneuver frame |
| `0x48` | nav on/off and active flag |
| `0x47` | ETA/distance frame |
| `0x44` | speed-limit frame |
| `0x4A` | street/text frame |

Когда `0x48` active is true, V21 на каждом втором `0x77` poll повторяет last
nav on/maneuver/ETA/speed/text bundle через stock dispatcher. Когда active route
нет, V21 повторяет last compass frame. Media/text source не повторяются:
`0x7A`, `0x20`, `0x21`, `0x22` остаются event-only.

## Проверка

```bash
cd /Volumes/SSD/canbus/repo
python3 -m py_compile firmware/scripts/build_v08_mode1_raw_can_stream.py tools/usb_update_2can35.py
python3 firmware/scripts/build_v08_mode1_raw_can_stream.py
shasum -a 256 firmware/trusted/v21/21_v08_mode1_v21_USB.bin
```

После заливки:

```text
0x56 UID
0x79 V21 status
0x77 snapshot
nav/compass hold tick по compact poll
0x70 on/off только если нужен debug
0x76 raw read только если debug stream включен
0x78 invalid bus -> ACK 02
```
