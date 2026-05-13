# Прошивка v18: canbox + RAW CAN + OBD snapshot

Файл v18 основан на прошивке программиста `37FFDA054247303859412243_04350008.bin`.
Штатный режим canbox не меняется: адаптер сразу работает как обычный canbox.
Поверх штатной логики добавлены две отладочные функции по тому же USB CDC:

- `0x70/0x76` - включить и читать поток чистых RAW CAN кадров C-CAN/M-CAN.
- `0x77` - прочитать уже разобранное состояние машины для OBD/UI экрана.

## USB команды

Запросы имеют формат:

```text
BB 41 A1 LEN CMD PAYLOAD CHECKSUM
```

Checksum - сумма всех байт кроме checksum, младший байт.

### RAW CAN

```text
BB 41 A1 07 70 01 15  ; start raw stream
BB 41 A1 07 70 00 14  ; stop raw stream
BB 41 A1 06 76 19     ; pop one raw CAN frame
```

Ответ `0x76`:

```text
status, bus, flags, dlc, reserved, id_le32, data[8]
```

`bus=0` - C-CAN, `bus=1` - M-CAN.

### OBD snapshot

```text
BB 41 A1 06 77 1A
```

Ответ `0x77`, payload 30 байт:

| Offset | Поле | Формат |
| --- | --- | --- |
| 0 | status | `u8`, `1` если структура инициализирована |
| 1..3 | known | `u24` bitmask достоверных полей |
| 4..7 | counter | `u32`, счетчик обработанных CAN кадров |
| 8..9 | speed_kmh | `u16` |
| 10..11 | rpm | `u16` |
| 12..13 | coolant_c | `s16` |
| 14..15 | voltage_mv | `u16` |
| 16 | throttle_pct | `u8`, `0xFF` если неизвестно |
| 17 | brake | `u8`, `0/1`, `0xFF` если неизвестно |
| 18 | gear | `u8`, `0=unknown`, `1=P`, `2=R`, `3=N`, `4=D`, `0xFF` если неизвестно |
| 19 | fuel_pct | `u8`, `0xFF` если неизвестно |
| 20..21 | outside_c | `s16` |
| 22..23 | fuel_rate_x10 | `u16`, `0xFFFF` если неизвестно |
| 24..27 | odometer_km | `u32`, `0xFFFFFFFF` если неизвестно |
| 28..29 | reserved | `0` |

Known flags:

| Bit | Поле |
| --- | --- |
| 0 | speed |
| 1 | rpm |
| 2 | coolant temperature |
| 3 | voltage |
| 4 | throttle position |
| 5 | brake |
| 6 | gear |
| 7 | fuel level |
| 8 | fuel rate |
| 9 | odometer |
| 10 | outside temperature |

## Уже подключенные CAN источники

| Поле | CAN | Формула | Статус |
| --- | --- | --- | --- |
| speed | C-CAN `0x316` | `data[6]` km/h | включено |
| rpm | C-CAN `0x316` | `(data[2] + data[3] * 256) / 4` | включено |
| coolant_c | C-CAN `0x329` | `(data[1] - 0x40) * 0.75` | включено |
| outside_c | C-CAN `0x044` | `(data[3] - 0x52) / 2` | включено |
| voltage_mv | C-CAN `0x545` | `data[3] * 100` | включено |
| gear | C-CAN `0x169` | подтвержден только `R` по `DATA[0] & 0x0F == 0x07` | включено |
| throttle_pct | неизвестно | зарезервировано | нужно найти |
| fuel_pct | неизвестно | зарезервировано | нужно найти |
| odometer_km | неизвестно | зарезервировано | нужно найти |
| fuel_rate_x10 | неизвестно | зарезервировано | нужно найти |

Поле `brake` в snapshot оставлено в формате ответа, но не считается
подтвержденным источником до отдельного доказательства. Непроверенные CAN ID не
перечисляются в рабочей документации.

## Файлы сборки

- USB update: `firmware/trusted/v18_v19/18_v08_mode1_raw_can_stream_obd_snapshot_USB.bin`
- ST-Link full image: `firmware/trusted/v18_v19/18_v08_mode1_raw_can_stream_obd_snapshot_STLINK64K.bin`
- Report: `firmware/trusted/v18_v19/18_v08_mode1_raw_can_stream_obd_snapshot.report.json`

Проверка через консоль:

```bash
python3 tools/stockusb_canlog_2can35.py --snapshot
python3 tools/stockusb_canlog_2can35.py --poll-raw --seconds 5
```
