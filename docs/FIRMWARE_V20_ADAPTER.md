# Прошивка v20: эталонный минимум поверх авторского canbox

v20 построена от авторской `04350008` и сохраняет штатный canbox/update-loader.
USB identity не меняется: `0483:5740`, stock CDC/proprietary protocol.

## Статус эталона

Этот вариант считать текущей опорной прошивкой.

Что нельзя ломать при следующих правках:

- штатный canbox автора должен стартовать сразу после загрузки;
- USB update-loader `0x55` должен оставаться рабочим;
- UID адаптера должен совпадать с пакетом обновления;
- `0x56` должен отвечать без подключенных CAN-шин;
- `0x79` должен отвечать без подключенных CAN-шин;
- `0x77` должен возвращать snapshot без падения в `0x78`;
- `0x78` должен отвечать ACK-кодами `00/01/02/FF`;
- USB VID/PID и CDC identity не менять без отдельного recovery-плана через ST-Link.

Базовые файлы:

```text
firmware/trusted/v20/20_v08_mode1_v20_USB.bin
firmware/trusted/v20/20_v08_mode1_v20_STLINK64K.bin
firmware/trusted/v20/20_v08_mode1_v20.report.json
```

## Что изменено относительно v19

- Оставлены `0x70`, `0x76`, `0x77`, `0x78`.
- Добавлена команда `0x79` для проверки, что адаптер жив и прошит V20.
- Исправлен выход после `0x77`: snapshot больше не проваливается в `0x78`.
- UART sideband и старый `0x74` raw TX не возвращались.

## Команды

| Команда | Назначение |
|---:|---|
| `0x70` | raw CAN stream on/off |
| `0x76` | прочитать один raw CAN кадр из ring buffer |
| `0x77` | compact decoded snapshot |
| `0x78` | one-shot raw CAN TX |
| `0x79` | V20 health/capabilities |

`0x79` ответ:

```text
BB A1 41 0F 79 20 02 00 FF 01 56 32 30 00 FF
```

Payload:

```text
20 02 00 FF 01 56 32 30 00
```

- `20` - V20 marker;
- `02 00` - API major/minor;
- `FF 01` - capabilities bitfield;
- `56 32 30 00` - ASCII `V20\0`.

## Файлы

- USB update: `firmware/trusted/v20/20_v08_mode1_v20_USB.bin`
- ST-Link full image: `firmware/trusted/v20/20_v08_mode1_v20_STLINK64K.bin`
- Report: `firmware/trusted/v20/20_v08_mode1_v20.report.json`

## Как править дальше

Править V20 только поверх генератора:

```bash
cd /Volumes/SSD/canbus/repo
python3 firmware/scripts/build_v08_mode1_raw_can_stream.py
```

Перед заливкой проверять:

```bash
python3 -m py_compile firmware/scripts/build_v08_mode1_raw_can_stream.py tools/usb_update_2can35.py
shasum -a 256 firmware/trusted/v20/20_v08_mode1_v20_USB.bin
```

После заливки проверять минимум:

```text
0x56 UID
0x79 V20 status
0x77 snapshot
0x70 on/off
0x76 raw read
0x78 invalid bus -> ACK 02
```

Если нужен новый функционал, сначала выбирать один из двух путей:

- штатный USB/API автора: `0x20/0x21/0x22`, `0x30`, `0x44/0x45/0x47/0x48/0x4A`, `0x60`;
- raw CAN TX: `0x78`, когда штатный API не покрывает сценарий.

Большие таблицы, DBC, названия параметров и UI в прошивку не переносить. Это зона Android-приложения.

## Проверка после прошивки 2026-05-13

Адаптер `/dev/cu.usbmodemKIA1` ответил:

```text
UID: 37 FF DA 05 42 47 30 38 59 41 22 43
0x79: BB A1 41 0F 79 20 02 00 FF 01 56 32 30 00 FF
0x77: snapshot status=1, known=[]
0x70 on/off: ACK 00/01
0x78 invalid bus: ACK 02
```
