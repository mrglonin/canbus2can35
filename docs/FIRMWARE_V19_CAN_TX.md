# Прошивка v19: v18 + одноразовая CAN TX команда

v19 построена от рабочей базы v18 и сохраняет штатный `canbox` от прошивки программиста v08.
USB identity не меняется: это тот же CDC/proprietary интерфейс, не `gs_usb`.

## Что есть

- Штатный canbox работает сразу после запуска.
- `0x70/0x76` - включаемый вручную RAW поток C-CAN/M-CAN.
- `0x77` - OBD snapshot для приложения.
- `0x78` - одноразовая отправка CAN кадра в C-CAN или M-CAN.

## CAN TX `0x78`

Запрос:

```text
BB 41 A1 LEN 78 PAYLOAD CHECKSUM
```

Payload 15 байт:

| Offset | Поле | Значение |
| --- | --- | --- |
| 0 | bus | `0` = C-CAN/CAN1, `1` = M-CAN/CAN2 |
| 1 | flags | bit0 = EXT, bit1 = RTR |
| 2..5 | id | CAN ID little-endian |
| 6 | dlc | 0..8 |
| 7..14 | data | 8 байт, неиспользуемые забить `00` |

ACK:

| Код | Значение |
| --- | --- |
| `00` | кадр поставлен в CAN mailbox |
| `01` | нет свободного CAN mailbox |
| `02` | неверный bus |
| `FF` | неверная длина команды |

Пример отправки STD кадра `0x123` в M-CAN:

```bash
python3 tools/stockusb_canlog_2can35.py --can-tx '1,0x123,11223344'
```

Сформированный USB запрос:

```text
BB 41 A1 15 78 01 00 23 01 00 00 04 11 22 33 44 00 00 00 00 FD
```

## Файлы

- USB update: `firmware/trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx_USB.bin`
- ST-Link full image: `firmware/trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx_STLINK64K.bin`
- Report: `firmware/trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx.report.json`

## Важно для теста

TX не включается сам и не работает циклом. Приложение должно само решать, когда отправлять кадр.
Для безопасной проверки сначала слать тестовый кадр в свободный/неиспользуемый ID и смотреть ACK, потом уже подбирать реальные команды.
