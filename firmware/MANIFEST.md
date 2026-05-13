# Firmware manifest

В репозитории оставлены только текущие рабочие моды поверх прошивки автора v08.
Нерабочая чистая C-прошивка, старые mode3/gs_usb пакеты и промежуточные
варианты удалены.

## Base firmware

Проверенная основа автора:

```text
37FFDA054247303859412243_04350008.bin
UID: 37 FF DA 05 42 47 30 38 59 41 22 43
```

Стоковые бинарники автора лежат локально в `Downloads`; в git они не
дублируются.

## Trusted variants

| File | SHA256 | Назначение |
|---|---|---|
| `trusted/v18_v19/18_v08_mode1_raw_can_stream_obd_snapshot_USB.bin` | `e1899bfe888e4cf77dfbbb90c4987d5c39dada0e5eddd8ddef13847f725e8868` | USB-update: штатный canbox v08 + RAW CAN read + OBD snapshot. |
| `trusted/v18_v19/18_v08_mode1_raw_can_stream_obd_snapshot_STLINK64K.bin` | `46a3654085ea9d9251ac1d28421070a0fd25b73fbd8419d957c13d94fcc8bf1a` | ST-Link full image для той же v18 ветки. |
| `trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx_USB.bin` | `54597f7bb9b3a416ea53bab6d26b2cbb6507246cccc15e1506d68f9274aaaff0` | USB-update: v18 + одноразовая CAN TX команда `0x78`. |
| `trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx_STLINK64K.bin` | `a94208b4d5ebee90f8e1b17f276a10e8b3fbbc98832232dc2da2d7c5b03be756` | ST-Link full image для v19. |
| `trusted/v20/20_v08_mode1_v20_USB.bin` | `b790f7b1077358c0bac5e6eee81f96efea2fbcf90e0b39bdaf2e3e799953f95b` | USB-update: v19 + `0x79` V20 health/capabilities и исправленный выход после `0x77`. |
| `trusted/v20/20_v08_mode1_v20_STLINK64K.bin` | `10ef4cbccc7cd8802cecf9a8ef4dc1c32e38edbc9eda17b9cd80a0cbea9f22f0` | ST-Link full image для v20. |

## USB sideband commands

| Команда | Что делает |
|---:|---|
| `0x70` | включить/выключить RAW CAN stream |
| `0x76` | прочитать один RAW CAN кадр |
| `0x77` | прочитать decoded OBD snapshot |
| `0x78` | одноразово отправить CAN кадр |
| `0x79` | V20 health/capabilities без CAN-подключения |

Эти команды не заменяют штатный протокол музыки/навигации автора. Они нужны для
диагностики и приложения.
