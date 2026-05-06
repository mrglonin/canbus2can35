# 2CAN35 Sportage Reverse Engineering

Дата анализа: 2026-05-06

## Цель

Восстановить максимум логики из готовой прошивки и APK:

- понять формат штатного USB-обновления;
- получить декодированный app-образ прошивки;
- восстановить APK-протокол обмена с адаптером;
- зафиксировать карту памяти, CAN/UART/USB точки и стартовый C-подобный псевдокод.

Оригинальный C-исходник из бинарника обратно байт-в-байт не восстанавливается. Реально восстановимый результат: дизассемблированный ARM Thumb, Java-подобная декомпиляция APK, таблицы/протоколы и чистая реконструкция исходников, которую потом можно довести до сборки.

## Входные файлы

- `/Users/legion/Downloads/2CAN35_base_1uart_usb_v3_0.bin`
  - SHA256: `9c6cf03a1179244fa44635a7b56ea20801f879af0d46b19c8beae0c826fba5c9`
  - базовый raw ST-Link/flash образ с загрузчиком и приложением.
- `/Users/legion/Downloads/37FFDA054247303859412243_04350004.bin`
  - SHA256: `a3cb8fc7edd3fd95083eb5f3332ca662a17ac686e4e543d9ce94b30fd046aa3d`
  - APK update package под твой UID.
- `/Users/legion/Downloads/Sportage.apk`
  - SHA256: `fb41a1f38c4f2a020aed3003c27301e527e24c01fc83c94512a2357e9e26898e`
  - package `com.sorento.navi`, version `2.1`.

## Артефакты

- Декодер/энкодер пакета:
  `/Users/legion/Downloads/canbox-fw-lab/tools/decode_2can35_update.py`
- Анализатор firmware image:
  `/Users/legion/Downloads/canbox-fw-lab/tools/analyze_2can35_firmware.py`
- APK scanner:
  `/Users/legion/Downloads/canbox-fw-lab/tools/apk_scan_sportage.py`
- Fixed hybrid image builder:
  `/Users/legion/Downloads/canbox-fw-lab/tools/make_2can35_hybrid.py`
  - now decodes update packages before embedding;
  - writes UID/version header to `0x08003FF0`;
  - validates app vector before producing an image.
- Декодированный package:
  `/Users/legion/Downloads/canbox-fw-lab/re_sportage/37FFDA054247303859412243_04350004.decoded_package.bin`
- Декодированный payload приложения:
  `/Users/legion/Downloads/canbox-fw-lab/re_sportage/37FFDA054247303859412243_04350004.decoded_payload.bin`
- Декодированный 64 KiB ST-Link-layout app image:
  `/Users/legion/Downloads/canbox-fw-lab/re_sportage/37FFDA054247303859412243_04350004.decoded_app_stlink64k.bin`
- Thumb disassembly:
  `/Users/legion/Downloads/canbox-fw-lab/re_sportage/37FFDA054247303859412243_04350004.decoded_app.thumb.S`
- APK decompile:
  `/Users/legion/Downloads/canbox-fw-lab/re_sportage/androguard_decompile`
- C-style recovered pseudocode:
  `/Users/legion/Downloads/canbox-fw-lab/re_sportage/reconstructed/firmware_recovered_pseudocode.c`

## Формат Update Package

Первые 16 байт пакета открытые:

```text
37 FF DA 05 42 47 30 38 59 41 22 43 04 35 00 04
|------------- STM32 UID -------------| | version |
```

UID: `37 FF DA 05 42 47 30 38 59 41 22 43`

Версия: `04 35 00 04`

Payload начинается сразу после этих 16 байт и при записи штатным загрузчиком попадает в flash-приложение около `0x08004000`. Header хранится перед приложением по адресу `0x08003FF0`.

Декодированный app vector:

```text
app flash: 0x08004000
initial SP: 0x20010000
reset:     0x08006D1D
```

## Шифрование/обфускация пакета

В loader-функции `0x08000918` восстановлен поблочный декодер.

Условие декодирования:

```c
offset >= 0x151 && offset <= 0x350f
```

Так как APK шлёт блоки по 16 байт, реально декодируются блоки:

```text
0x160, 0x170, ... 0x3500
```

Для пакета `04 35 00 04` ключи:

```text
key_a = 0x04
key_b = 0x5B
```

Алгоритм:

```c
xor_key = key_a;
rot = key_b & 0x0f;
if ((offset >> 8) & 0x08) {
    xor_key = key_b;
    rot = key_a & 0x0f;
}
for i in 0..15:
    tmp[i] = encoded[i] ^ xor_key;
for i in 0..15:
    decoded[(rot + i) & 15] = tmp[i];
```

Проверка пройдена: decode -> encode даёт исходный файл байт-в-байт (`roundtrip_ok: true`).

## APK USB Protocol

APK работает через `com.hoho.android.usbserial.driver.*`.

USB devices, которые ищет APK:

```text
VID 0x0403 or 0x0483
PID 0x6001 or 0x5740
```

Параметры serial:

```text
19200 baud, 8 data bits, 1 stop bit, no parity
```

Формат кадра:

```text
BB dst src len cmd payload... checksum
checksum = sum(bytes[0..len-2]) & 0xff
```

Направление APK -> adapter:

```text
BB 41 A1 ...
```

Направление adapter -> APK:

```text
BB A1 41 ...
```

## APK Firmware Update Flow

Старт обновления:

```text
BB 41 A1 07 55 01 CS
```

Блок данных:

```text
BB 41 A1 19 55 off_hi off_mid off_lo 16_bytes CS
```

Финиш:

```text
BB 41 A1 07 55 00 CS
```

Подтверждения от адаптера:

```text
BB A1 41 07 55 01 CS  // update mode entered
BB A1 41 07 55 02 CS  // block accepted
BB A1 41 07 55 00 CS  // finish
```

APK не требует ручной перезагрузки: он сам переводит адаптер в update mode, шлёт блоки, затем адаптер сразу продолжает работу с новой прошивкой.

## APK Commands

Команды, восстановленные из `Sportage.apk`:

| Command | Direction | Meaning |
|---|---|---|
| `0x20` | adapter -> APK / APK -> adapter | FM station frequency/name |
| `0x21` | APK -> adapter | media/radio text |
| `0x22` | adapter -> APK / APK -> adapter | track number/text |
| `0x30` | both | штатный усилитель: request/apply/report |
| `0x44` | APK -> adapter | speed limit |
| `0x45` | APK -> adapter | navigation maneuver |
| `0x47` | APK -> adapter | ETA/distance |
| `0x48` | APK -> adapter | navigation on/off |
| `0x51` | both | TPMS request/report |
| `0x55` | both | firmware update |
| `0x56` | both | UID/version request/report |
| `0x60` | both | settings |

Settings frames from APK:

```text
BB 41 A1 08 60 00 value CS  // speed/TBT mode
BB 41 A1 08 60 01 value CS  // SAS/calibration delay
BB 41 A1 08 60 02 value CS  // engine temp enable
```

AMP frames from APK:

```text
BB 41 A1 07 30 01 CS
BB 41 A1 0F 30 02 volume balance fader bass mid treble mode 00 CS
```

APK assets contain `/assets/stations.txt`, a frequency-to-station table; APK uses it to map FM frequency to display text.

## Firmware Memory Map

Base image:

```text
0x08000000 loader vector, reset 0x08000ED5
0x08003FF0 app UID/version header
0x08004000 app vector, reset 0x080059CD in base image
```

Decoded Sportage update app:

```text
0x08003FF0 UID/version header
0x08004000 app vector, reset 0x08006D1D
0x08004150 app_init()
0x080041A4 app_main()
0x08005F2C literal ref to app header 0x08003FF0
0x08005F48 literal ref to STM32 UID 0x1FFFF7E8
```

Observed peripherals:

```text
CAN1/bxCAN: 0x40006400
CAN2/bxCAN: 0x40006800
USART2:     0x40004400
USART1:     0x40013800
RCC:        0x40021000
FLASH:      0x40022000
GPIOA/B/C:  0x40010800 / 0x40010C00 / 0x40011000
```

Strings in decoded firmware:

```text
HYK-RZ-10-0001-VK
Home Made Technologies
CDC-ACM
```

То есть `HYK-RZ-10-0001-VK` не случайная строка: это идентификатор/профиль, который прошивка отдаёт наружу, чтобы магнитола/приложение понимали тип canbox.

## CAN Speeds

В init-коде decoded app:

```text
CAN1 speed code 6 = 500 kbit/s
CAN2 speed code 3 = 100 kbit/s
```

Это совпадает с подсказкой программиста:

```text
C-CAN = 500 kbit/s
M-CAN = 100 kbit/s
```

## External Reference Sources

Локально использованы клоны:

- `/Users/legion/Downloads/canbox-fw-lab/external/candleLight_fw`
- `/Users/legion/Downloads/canbox-fw-lab/external/lib-slcan`
- `/Users/legion/Downloads/canbox-fw-lab/external/candleLight_fw-SavvyCAN-Windows-plugin`
- `/Users/legion/Downloads/canbox-fw-lab/external/cangaroo`
- `/Users/legion/Downloads/canbox-fw-lab/external/SavvyCAN`
- `/Users/legion/Downloads/canbox-fw-lab/external/stm32f1-picopwner`
- `/Users/legion/Downloads/canbox-fw-lab/external/f103-analysis`

Важный вывод: `candleLight_fw` полезен как GS USB протокол и архитектурный референс, но не является прямой drop-in прошивкой для нашего 2CAN35. Наша штатная прошивка использует CDC-ACM/кастомный протокол, а не чистый GS USB интерфейс.

## Что Уже Можно Воспроизвести

1. Декодировать update package в настоящий app payload.
2. Получить ST-Link-layout image с header по `0x3FF0` и app по `0x4000`.
3. Снова заэнкодить protected-блоки и проверить, что получается исходный пакет.
4. Повторить APK-compatible USB update flow.
5. Видеть протокол команд APK <-> adapter.
6. Начать clean-room C-прошивку, совместимую с APK и Teyes, вместо слепой склейки бинарников.

## Следующие Шаги

1. Разметить функции decoded app по дизассемблеру: CAN RX handlers, UART parser, USB CDC writer, settings storage, amp/TPMS/nav handlers.
2. Снять live CAN log с двумя шинами: M-CAN 100 kbit/s и C-CAN 500 kbit/s.
3. Связать CAN IDs из лога с событиями: двери, ACC, задний ход, скорость, обороты, климат, TPMS, кнопки руля.
4. Перенести найденные CAN IDs в чистый C-проект и сохранить APK/TEYES совместимый протокол.
5. Только после этого собирать новую прошивку, а не прошивать гибриды вслепую.
