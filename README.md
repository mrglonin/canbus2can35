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

## Проверенная база

- [docs/TRUSTED_FIRMWARE_FACTS.md](docs/TRUSTED_FIRMWARE_FACTS.md)
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

После сборки APK автоматически копируется в:

```text
/Volumes/SSD/canbus/release/kia_109.apk
```

Номер в имени берется из `versionName`: `10.9-kia` -> `kia_109.apk`.

## Что намеренно удалено

- Нерабочая чистая C-прошивка.
- Старые `mode3`/`gs_usb` сборки.
- Гигабайты raw live logs.
- APK/reverse decompile dumps.
- Handoff/dist-дубликаты.
- DBC-only таблицы как рабочая документация.

Если нужно снова исследовать старую гипотезу, ее лучше поднять из истории git,
а в текущей ветке держать только то, что можно объяснить и повторить.
