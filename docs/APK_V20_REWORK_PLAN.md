# APK V20: протокол, структура и следующий этап

Цель: Android-приложение становится главным мозгом проекта, а 2CAN35 V20 остается маленьким стабильным CAN-шлюзом.

## Обязательные правки протокола

1. Исправить raw CAN TX:

```text
сейчас: packet(0x74, payload)
надо:  packet(0x78, payload)
```

Менять в `CanbusControl.sendRawCan()` и `sendRawCanQuiet()`. ACK разбирать по `0x78`, не по `0x74`.

2. Добавить V20 health/capabilities:

```text
APK -> BB 41 A1 06 79 1C
V20 -> BB A1 41 0F 79 20 02 00 FF 01 56 32 30 00 FF
```

В UI показывать:

```text
adapter: online/offline
firmware: V20
api: 2.0
capabilities: raw stream, snapshot, raw tx, stock canbox
```

3. Оставить штатные команды автора:

```text
0x20 = radio/FM/station text
0x21 = media source/artist
0x22 = media track/title
0x30 = AMP get/set
0x44 = speed limit
0x45 = maneuver
0x47 = route distance / ETA
0x48 = navigation on/off
0x4A = street/text
0x55 = firmware update
0x56 = UID/version
0x60 = simple settings
```

4. Компас держать на штатной команде `0x45`, не на raw M-CAN:

```text
BB 41 A1 0E 45 08 00 00 DD 00 78 00 A0 CS
DD = (36 - uiStep) % 36
uiStep = 0,3,6,...,33
```

Повторять `350-500 ms`. Raw `0x115/0x1E6` больше не использовать как основной
путь компаса в APK.

## Что приложение уже умеет

- USB CDC connection through `AppService`.
- Firmware update through `CanbusFirmwareUpdater`.
- Media sources through TEYES/SPD, widget, notifications and media sessions.
- Navigation through Yandex-style and TEYES `com.yf.navinfo` events.
- OBD/snapshot/raw CAN reading.
- TPMS/RCTA overlays and debug hooks.

## Новая структура приложения

### 1. Главная

Назначение: состояние машины и быстрый контроль.

Показывает:

```text
Adapter: connected / V20 / UID / API
CAN: C-CAN activity / M-CAN activity
Navigation: active / street / maneuver / compass
Media: source / artist / title / time
Vehicle: speed / rpm / temp / voltage / gear
Warnings: TPMS / RCTA / errors
```

Кнопки:

```text
Подключить адаптер
Проверить V20
Включить сервис
Открыть диагностику
```

### 2. Навигация

Назначение: всё, что уходит в приборку как navigation/TBT.

Содержит:

```text
route on/off
street text
maneuver icon
distance to turn
ETA / route distance
speed limit
compass without route
classic/TBT mode
text mode: street / speed / auto
```

ADB-only QA:

```text
nav_active
nav_preview
nav_failed
nav_finish
nav_off
compass --ei step N
```

### 3. Медиа

Назначение: источник, радио, трек, исполнитель, время.

Содержит:

```text
active source
package/source priority
artist
title
duration/progress
play state
last sent 0x21/0x22
```

ADB-only QA:

```text
media_yandex
media_bt_selected_paused
media_bt_playing
media_usb
media_fm
media_am
```

### 4. Машина

Назначение: чтение состояния из C-CAN/M-CAN.

Содержит:

```text
speed
rpm
coolant temp
outside temp
voltage
gear
brake
doors / trunk / hood
reverse
```

Источник:

```text
0x70/0x76 passive raw stream для whitelisted vehicle/RCTA IDs
0x132 DATA[0] / 10 для voltage
0x4F4 для blind spot/RCTA
```

### 5. Штатные настройки

Назначение: то, что можно читать/менять через подтвержденные команды.

Содержит:

```text
AMP: volume / balance / fader / bass / mid / treble / mode
SAS Ratio
engine temp setting
future confirmed cluster/HU settings
```

Правило: только whitelist. Новая настройка появляется тут только после live-подтверждения.

### 6. TPMS / RCTA

Назначение: отдельный рабочий экран предупреждений.

Содержит:

```text
TPMS dashboard
pressure/temp thresholds
alert sound
RCTA enable
RCTA overlay
ADB-only debug left/right/both/unknown/off
```

### 7. Диагностика

Назначение: всё опасное и техническое.

Содержит:

```text
UID/version
V20 capabilities 0x79
raw stream 0x70
raw frame read 0x76
snapshot 0x77
raw CAN TX 0x78
last USB frames
ACK decoder
save/copy test log
firmware update
```

Raw CAN TX должен быть спрятан за явным переключателем `Разрешить TX`.

### 8. Лаборатория параметров

Назначение: вытаскивать настройки панели приборов и режимы.

Рабочий процесс:

```text
1. Включить raw stream.
2. Снять baseline 5-10 секунд.
3. Изменить один параметр на панели/магнитоле.
4. Снять diff.
5. Показать изменившиеся ID/bytes.
6. Сохранить кандидат.
7. Проверить обратной отправкой через 0x78 только после подтверждения.
```

Результат сохранять как:

```text
can/learned/learned_assignments.jsonl
can/confirmed_can_signals.csv
```

## Практический порядок APK-работ

1. Исправить `0x74 -> 0x78`.
2. Добавить `0x79` request/parse/status.
3. Обновить `CompassBridge`: `0x45`, GPS-bearing validity, repeat `350-500 ms`.
4. Разделить selected source и playing source; hints не должны затирать playing MediaSession.
5. Перевести RCTA на `0x4F4`, voltage на `0x132`.
6. Убрать подтвержденные тесты из UI и оставить ADB-only QA receiver.
7. Собрать `kia_122.apk`, подписать, поставить на TEYES.
8. Проверить на машине: V20 status, compass, nav text, media text, RCTA, raw stream без debug spam.

## Что не делать

- Не переносить DBC/большие таблицы в STM32.
- Не слать brute-force TX без live-baseline и whitelist.
- Не ломать штатные `0x20/0x21/0x22`, `0x30`, `0x44/0x45/0x47/0x48/0x4A`.
- Не менять USB descriptors до отдельного recovery-плана.
