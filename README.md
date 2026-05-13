# Kia CANBUS / 2CAN35

Рабочий репозиторий по кастомному 2CAN35/Sigma10 CANBOX на `STM32F105RBT6`
для Kia/Hyundai Sportage.

## Локальная структура

На SSD проект держится одной чистой папкой:

| Путь | Назначение |
|---|---|
| `/Volumes/SSD/canbus/repo` | Этот git-репозиторий. |
| `/Volumes/SSD/canbus/tools` | Локальные toolchain/OpenOCD/SDK и прочие тяжелые инструменты, не git. |
| `/Volumes/SSD/canbus/release` | Готовые APK/прошивки с короткими именами. |

В репозитории остаются только исходники, проверенные таблицы и короткая
документация. Генерируемые APK, старые дампы, временные логи, reverse-директории
и нерабочие C-эксперименты в git не хранятся.

## Что лежит в repo

| Папка | Что внутри |
|---|---|
| `android/kia/` | Текущее Android-приложение для TEYES/Kia. |
| `can/` | Подтвержденные CAN-C/M-CAN сигналы и результаты обучения. |
| `dashboard/` | Локальная web/OBD-лаборатория. |
| `docs/` | Проверенная документация по прошивке, железу и восстановлению. |
| `firmware/` | Только доверенные моды и скрипты сборки текущей ветки. |
| `protocols/raise/` | Расшифровка Raise/RZC UART и подтвержденные кадры. |
| `protocols/simple_soft/` | Отдельный worklist Simple Soft / `2E` UART. |
| `protocols/teyes_profiles/` | Выжимка из TEYES CANBUS/MS/US APK по профилям и MCU-слоям. |
| `tools/` | Малые утилиты проекта: update, mode switch, decode, stock-USB CAN logger. |

## Рабочая модель

Штатная прошивка автора работает как `canbox + update`:

| Режим | Назначение |
|---|---|
| canbox | Обычная работа. Адаптер читает CAN автомобиля, физический задний ход и входные USB/UART команды, затем сам конвертирует данные в нужный CAN/UART вывод. |
| update | Обновление через USB командой штатного приложения/утилиты. |

Важно: APK/магнитола не отправляет музыку и навигацию как готовые raw CAN
кадры. Подтвержденная модель такая: приложение/магнитола говорит с прошивкой
через ее USB/API или Raise/HU протокол, а прошивка уже переводит это в M-CAN.

## Текущая рабочая база V21

Эталонная прошивка для адаптера:

```text
firmware/trusted/v21/21_v08_mode1_v21_USB.bin
firmware/trusted/v21/21_v08_mode1_v21_STLINK64K.bin
firmware/trusted/v21/21_v08_mode1_v21.report.json
```

Проверенный адаптер:

```text
/dev/cu.usbmodemKIA1 @ 19200
UID: 37 FF DA 05 42 47 30 38 59 41 22 43
0x79: BB A1 41 0F 79 21 03 00 FF 01 56 32 31 00 02
```

V21 команды:

| Команда | Назначение |
|---:|---|
| `0x70` | raw CAN stream on/off только для debug |
| `0x76` | pop raw CAN frame из ring buffer только для debug |
| `0x77` | compact Vehicle/RCTA snapshot без raw-потока в APK |
| `0x78` | one-shot raw CAN TX только M-CAN (`bus=1`) |
| `0x79` | V21 health/capabilities, проверка что адаптер жив |
| `0x7A` | inject Raise/RZC `FD .. 09 ...` source-status в штатный parser прошивки |

Правило архитектуры: прошивка остается маленьким gateway. Она пассивно слушает
CAN и хранит маленький snapshot, а Android забирает только нужное через `0x77`.
Raw stream `0x70/0x76` не используется для обычных Vehicle/RCTA, только для
ручной диагностики. Большие таблицы, названия, DBC, UI, обновления и логика
выбора источника живут в Android.

## Проверенный компас

Компас лучше отправлять штатной USB/API командой `0x45`, не raw `CAN M`.

Форма кадра:

```text
BB 41 A1 0E 45 08 00 00 DD 00 78 00 A0 CS
```

`DD` - направление. Рабочая сетка только кратная `3`: `0, 3, 6, ... 33`.
Для удержания отображения кадр надо повторять примерно раз в `350-500 ms`.
Без повтора приборка может вернуться в дефолт.

В локальной панели используется удобная UI-инверсия:

```text
sent = (36 - ui) % 36
```

Подтверждено на машине:

| UI | Отправляется `DD` | Статус |
|---:|---:|---|
| `0` | `0` | стабильно держит `N` |
| `12` | `24` | стабильная кардинальная точка после инверсии |
| `24` | `12` | стабильная противоположная точка после инверсии |

Практический вывод: для APK использовать уже проверенную формулу панели, а не
пытаться напрямую крутить M-CAN кадры `0x115`.

## Проверенная навигация

Навигация уже поддерживается штатными командами прошивки. Raw CAN для этого не
нужен.

| Команда | Назначение |
|---:|---|
| `0x48` | nav on/off |
| `0x45` | maneuver/TBT/compass |
| `0x47` | ETA/route status |
| `0x4A` | street/text, UTF-16LE |
| `0x44` | speed limit |

Минимальный nav on:

```text
BB 41 A1 07 48 01 ED
```

Nav off:

```text
BB 41 A1 07 48 00 EC
```

## Проверенные источники музыки

Ключевой вывод live-тестов: музыка и источники работают через `0x7A` и штатный
media/source parser прошивки. Одиночный raw CAN ACK через `0x78` не означает, что
приборка покажет источник.

Рабочие источники:

| Источник | Статус | База кадра |
|---|---|---|
| USB music | работает | `0x7A` + `FD 0A 09 16 ...` |
| BT audio | работает | `0x7A` + `FD 06 09 0B 04 00` |
| BT combo | работает | `0x0B04` + доп. BT/status |
| FM radio | работает | `0x7A` + `FD 08 09 02 00 65 46 00` |
| AM radio 24 | работает | `0x7A` + `FD 06 09 09 00 00` |
| BT phone | работает как source | `0x7A` + `FD 06 09 07 01 00` |

Нерабочие/неподтвержденные источники:

| Источник | Статус |
|---|---|
| BT music `0x11` | не выводит нужный BT audio |
| AUX `0x12` | не работает |
| Other media `0x83` | не работает |
| Navigation source `0x06` | не нужен как music source |
| Media off `0x80` | не полезен для вывода |

Source byte scanner после проверки:

| Byte | Статус |
|---:|---|
| `0x02` | FM/radio подтверждено |
| `0x09` | AM 24 radio подтверждено |
| остальные 46 значений | закрыты как нерабочие в текущем сценарии |

## Проверенные поля текста

Текстовые команды идут штатным USB/API слоем:

| Команда | Назначение |
|---:|---|
| `0x20` | radio/FM/AM text candidate |
| `0x21` | media/source text candidate |
| `0x22` | USB/title text |

Строки кодируются `UTF-16LE`.

Итог по live-тестам:

| Источник | Рабочие поля |
|---|---|
| USB | `Full bundle`, `0x22 title`, `0x22 subtype 0x1F` |
| BT audio | `0x22 title`, `0x20 subtype 0x1F` |
| BT combo | `0x21 no subtype` |
| FM radio | `0x21 no subtype` |
| AM radio 24 | `0x20 no subtype` |
| BT phone | рабочих text-полей не найдено |

Важно по USB: старое предположение, что `0x21 subtype 0x1F/0x20` стабильно
дает source/artist для USB, не подтвердилось как отдельный видимый вывод.
Для APK считать надежным USB title через `0x22`, а source включать через
`0x7A FD 0A 09 16 ...`.

## Что делать в APK

Текущий Android слой должен отправлять не raw CAN, а правильные USB/API команды:

1. Компас: `0x45` с повтором `350-500 ms` и формулой панели.
2. Навигация: `0x48`, `0x45`, `0x47`, `0x4A`, `0x44`.
3. USB music: при смене источника/трека один раз `0x7A FD 0A 09 16 ...`, потом title через `0x22`.
4. BT audio: `0x7A FD 06 09 0B 04 00`, затем проверенные поля `0x22`/`0x20 1F`.
5. FM/AM: отдельные ветки `0x02` и `0x09`, не смешивать с USB/BT.
6. Vehicle/RCTA: обычный режим через `0x77` snapshot; raw stream не включать без debug.
7. Raw CAN `0x78` оставить только для диагностики M-CAN, точечного TX и будущих
   редких сценариев, но не использовать как основной путь media/nav.

Локальная панель для live-тестов сейчас находится вне git:

```text
/Volumes/SSD/canbus/tools/compass_panel/server.py
http://127.0.0.1:8765/
```

## Проверенная база

- [docs/TRUSTED_FIRMWARE_FACTS.md](docs/TRUSTED_FIRMWARE_FACTS.md)
- [docs/ADAPTER_FIRMWARE_SIGNAL_MAP.md](docs/ADAPTER_FIRMWARE_SIGNAL_MAP.md)
- [docs/FIRMWARE_ADAPTER_OWNED_LOGIC_TARGET.md](docs/FIRMWARE_ADAPTER_OWNED_LOGIC_TARGET.md)
- [docs/FIRMWARE_V21_ADAPTER.md](docs/FIRMWARE_V21_ADAPTER.md)
- [docs/TEYES_APP_IMPLEMENTATION_README_20260513.md](docs/TEYES_APP_IMPLEMENTATION_README_20260513.md)
- [docs/PROGRAMMER_FIRMWARE_DECODE.md](docs/PROGRAMMER_FIRMWARE_DECODE.md)
- [docs/FM_UART_TO_MCAN_TRACE.md](docs/FM_UART_TO_MCAN_TRACE.md)
- [docs/PROGRAMMER_FIRMWARE_UART_CHECK.md](docs/PROGRAMMER_FIRMWARE_UART_CHECK.md)
- [can/confirmed_can_signals.csv](can/confirmed_can_signals.csv)
- [docs/HARDWARE_WIRING_MOD_GUIDE.md](docs/HARDWARE_WIRING_MOD_GUIDE.md)
- [docs/RECOVERY_STLINK_SEQUENCE.md](docs/RECOVERY_STLINK_SEQUENCE.md)

## Сборка Android

```bash
cd /Volumes/SSD/canbus/repo/android/kia
./gradlew assembleRelease
```

После сборки APK автоматически копируется в release-папку с номером версии.

```text
/Volumes/SSD/canbus/release/kia_125.apk
```

Номер в имени берется из `versionName`: `12.5-kia` -> `kia_125.apk`.

## Что намеренно удалено

- Нерабочая чистая C-прошивка.
- Старые `mode3`/`gs_usb` сборки.
- Гигабайты raw live logs.
- APK/reverse decompile dumps.
- Handoff/dist-дубликаты.
- DBC-only таблицы как рабочая документация.

Если нужно снова исследовать старую гипотезу, ее лучше поднять из истории git,
а в текущей ветке держать только то, что можно объяснить и повторить.
