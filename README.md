# canbus2can35

Рабочая лаборатория по 2CAN35/Sigma10 canbox для Kia/Hyundai Sportage/Sorento-подобной логики.

Цель: получить воспроизводимую схему работы адаптера, снять чистые логи с двух CAN-шин, разложить сообщения по событиям машины, убрать спам `Музыка USB`, сохранить штатное обновление через USB/APK и довести прошивку до нормального режима canbox + logger.

## Текущее Состояние На 2026-05-06

Адаптер:

- UID: `37 FF DA 05 42 47 30 38 59 41 22 43`.
- Штатный USB CDC: обычно `/dev/cu.usbmodemKIA1`.
- Штатный VID/PID: `0483:5740`.
- APK/USB serial: `19200 8N1`.
- Профиль в прошивке: `HYK-RZ-10-0001-VK`.
- Скорости по подсказке программиста и дизассемблеру:
  - `C-CAN = 500 kbit/s`.
  - `M-CAN = 100 kbit/s`.

Что уже доказано:

- APK-compatible update по USB работает: команда `0x55`, блоки по 16 байт, после финиша адаптер сам перезапускается.
- Версия программиста `04 35 00 04` рассчитана именно на наш UID.
- В машине был снят реальный двухканальный GS USB лог: `logs/car_can_cleanjump_20260506_220618.txt`.
- В этом логе канал `ch0` был запущен на `100000`, канал `ch1` на `500000`.
- Наш экспериментальный CAN-log комплект добавлен в `firmware/canlog/`.
- В normal mode прошивка программиста работает, но есть баг: адаптер сам отдает в CAN fallback-событие `Музыка USB`.
- Патч `skipMediaState0` гасит этот спам, но ломает музыку/навигацию. Значит это не финальный фикс, а только временная отсечка источника.
- Патч `skipMediaState3` спам не убрал.

Главный вывод: завтра не продолжаем слепо резать таблицу состояний. Сначала снимаем чистые парные логи `до/во время/после` и доказываем, какой именно CAN-пакет отвечает за `Музыка USB`, навигацию, треки и источники.

## Структура Репозитория

```text
docs/
  REVERSE_SPORTAGE_2CAN35.md      подробный reverse APK/update/firmware
  HYUNDAI_KIA_MCAN_MEDIA_RESEARCH.md  публичные DBC-кандидаты для media/source на приборку
  RECOVERY_STLINK_SEQUENCE.md     порядок восстановления через ST-Link
firmware/
  MANIFEST.md                     хэши и статусы локальных прошивок
  canlog/                         наш экспериментальный CAN-log firmware комплект
logs/
  car_can_cleanjump_20260506_220618.txt  пример реального двухшинного лога
samples/
  stations.txt                    таблица FM-станций из APK
tools/
  usb_update_2can35.py            штатная USB-прошивка как APK
  usb_mode_2can35.py              программный переход normal/update/logger
  stockusb_canlog_2can35.py       CDC CAN logger протокол `0x70`
  gsusb_2can35_logger.py          GS USB logger для VID/PID `1d50:606f`
  send_usb_display_demo.py        повторный тест FM/music/track/nav по USB
  analyze_can_log.py              быстрый summary ID/rate/changing bytes
  verify_2can35_modes.py          эмуляция выбора режимов
  decode_2can35_update.py         декодер/энкодер update package
  patch_mode1_skip_selected_media_states.py
  apk_scan_sportage.py
```

## Режимы Адаптера

Целевая схема:

| Режим | Звук | Назначение | USB | Комментарий |
|---|---:|---|---|---|
| Mode 1 | 1 писк | Normal canbox | CDC `0483:5740` | Штатная прошивка программиста: читает CAN, физический reverse, UART/USB, отдает данные в машину. |
| Mode 2 | 2 писка | Update loader | CDC `0483:5740` | APK-compatible загрузчик. Принимает `.bin` update package блоками по 16 байт. |
| Mode 3 | 3 писка | CAN logger | CDC logger или GS USB | Нужен для снятия двух шин. Самый надежный вариант для Mac был GS USB `1d50:606f`; CDC logger нужен как запасной путь. |

Программные команды переключения через штатный CDC:

```text
BB 41 A1 07 55 01 CS  -> update mode
BB 41 A1 07 55 03 CS  -> logger mode
BB 41 A1 07 55 04 CS  -> normal/reset
```

`CS = sum(all previous bytes) & 0xff`.

Команды:

```bash
python3 tools/usb_mode_2can35.py /dev/cu.usbmodemKIA1 update
python3 tools/usb_mode_2can35.py /dev/cu.usbmodemKIA1 canlog
python3 tools/usb_mode_2can35.py /dev/cu.usbmodemKIA1 normal
```

## Как Работает Штатное USB Обновление

Кадр:

```text
BB dst src len cmd payload... checksum
```

Направление Android/Mac -> adapter:

```text
BB 41 A1 ...
```

Направление adapter -> Android/Mac:

```text
BB A1 41 ...
```

Старт:

```text
BB 41 A1 07 55 01 CS
```

Блок:

```text
BB 41 A1 19 55 off_hi off_mid off_lo 16_bytes CS
```

Финиш:

```text
BB 41 A1 07 55 00 CS
```

Ответы:

```text
BB A1 41 07 55 01 CS  update mode entered
BB A1 41 07 55 02 CS  block accepted
BB A1 41 07 55 00 CS  finish
```

Прошить update package:

```bash
python3 tools/usb_update_2can35.py /dev/cu.usbmodemKIA1 firmware/local/37FFDA054247303859412243_04350004.bin
```

Скрипт проверяет UID из файла и UID адаптера. `--force` использовать только если точно понятно, почему UID не совпадает.

## Как Работает Logger

Есть два рабочих направления.

### GS USB / budgetcan

Когда режим 3 поднимается как GS USB:

- VID/PID: `1d50:606f`.
- Интерфейс не serial CDC, а USB bulk/control.
- На Mac читаем через `pyusb` + `libusb`.
- Каналы в нашем реальном логе:
  - `ch0 = 100000` предположительно M-CAN.
  - `ch1 = 500000` предположительно C-CAN.
- Лог пишется в listen-only, чтобы не вмешиваться в машину.

Команда:

```bash
python3 tools/gsusb_2can35_logger.py \
  --bitrate0 100000 \
  --bitrate1 500000 \
  --seconds 120 \
  --outfile logs/live_baseline_ign_on.txt
```

С выходом обратно в normal mode после таймера:

```bash
python3 tools/gsusb_2can35_logger.py \
  --bitrate0 100000 \
  --bitrate1 500000 \
  --seconds 120 \
  --outfile logs/live_test.txt \
  --exit-on-complete
```

### CDC Logger

Если режим 3 остается на штатном CDC:

- VID/PID: `0483:5740`.
- Порт: `/dev/cu.usbmodemKIA1`.
- Старт логирования: команда `0x70 0x01`.
- Стоп: команда `0x70 0x00`.
- Формат одного CAN frame от адаптера:

```text
BB A1 41 16 70 02 bus flags can_id_le[4] dlc data[8] CS
```

Команда:

```bash
python3 tools/stockusb_canlog_2can35.py /dev/cu.usbmodemKIA1 --seconds 120 > logs/live_cdc_test.txt
```

## Что Поддерживает Sportage.apk / Прошивка V04

Источник: reverse `Sportage.apk` (`com.sorento.navi`, version `2.1`) и decoded update `04 35 00 04`.

### USB команды APK <-> adapter

| Cmd | Направление | Длина/формат | Назначение |
|---:|---|---|---|
| `0x20` | adapter -> APK, APK -> adapter | adapter шлет частоту, APK возвращает UTF-16LE текст | FM station. APK читает `stations.txt`, режет имя станции до 16 символов. |
| `0x21` | APK -> adapter | UTF-16LE, максимум 16 символов по APK | Media/radio/music text. |
| `0x22` | adapter -> APK, APK -> adapter | adapter может слать номер трека; APK может вернуть UTF-16LE | Track text / track number. Если нет metadata, APK строит `Track № N`. |
| `0x30` | both | request/apply/report | Штатный усилитель: volume, balance, fader, bass, mid, treble, mode. |
| `0x44` | APK -> adapter | `01 speed` | Speed limit. В v03 программист писал, что speed-limit убран из постоянной отправки из-за возможных ошибок на комплектациях без этой функции. |
| `0x45` | APK -> adapter | 8 байт payload | Navigation maneuver / TBT. |
| `0x47` | APK -> adapter | 6 байт payload | Distance/ETA до финиша. |
| `0x48` | APK -> adapter | `00/01` | Navigation on/off. |
| `0x51` | both | frame bytes | TPMS request/report. |
| `0x55` | both | update flow | Firmware update / mode switching. |
| `0x56` | both | UID/version | UID и версия прошивки. |
| `0x60` | both | settings | Настройки: speed/TBT mode, SAS delay, engine temp enable. |

### Android источники данных

| Источник | Где найдено | Что дает |
|---|---|---|
| `NaviReceiver` | `com.sorento.navi.NaviReceiver` | Принимает broadcast actions: maneuver, exceeded, speed, ETA, navi_on. |
| SMOD/Yandex navigation bridge | action names `com.sorento.navi.ACTION_*` | Маневр, дистанция, единицы, улица, ограничение скорости, признак навигации. |
| FYT/SYU media service | bind к `com.syu.ms.toolkit`, package `com.syu.ms` | Metadata музыки/радио/Bluetooth на FYT/TEYES-платформе. |
| `stations.txt` | asset APK, копируется в `Android/data/com.sorento.navi/files` | Сопоставление FM частоты с названием станции. Можно редактировать. |
| `AmpActivity` | UI activity APK | Управление штатным усилителем. |
| `SettingsActivity` | UI activity APK | TBT/speed mode, SAS delay 10..50, engine temp enable, firmware update. |

### Ограничения текста

В APK явно стоит обрезка:

```text
if text.length() > 16:
    text = text.substring(0, 16)
```

Это найдено для FM station, media text и track text. Кодирует строки как UTF-16LE.

Практически завтра проверяем так:

- ASCII 16 символов: `ABCDEFGHIJKLMNOP`.
- ASCII 17+ символов: должно обрезаться до 16.
- Кириллица 16 символов: проверить, не режется ли плохо на приборке.
- Смешанная строка: `USB TEST 123456`.
- Пустая строка: проверить, скрывает ли поле или оставляет старое.

## Данные, Которые Надо Проверить По M-CAN И C-CAN

Из описания программиста и reverse:

### M-CAN, 100 kbit/s

Проверить:

- FM station name.
- AM station.
- Music text.
- Music over Bluetooth.
- Bluetooth call.
- Navigation street names.
- Navigation TBT maneuver.
- Distance to finish.
- Time to finish.
- Climate display.
- Stock amplifier control.
- Возможно source state, который сейчас вызывает `Музыка USB` spam.

### C-CAN, 500 kbit/s

Проверить:

- Driver door.
- Passenger door.
- Rear left door.
- Rear right door.
- Trunk.
- Hood.
- Reverse gear / physical reverse input correlation.
- Dynamic reverse lines.
- Parking sensors / SPAS.
- Outside temperature.
- Engine temperature.
- Left/right turn indicators.
- Hazard.
- Brake pedal.
- Parking brake.
- ACC/IGN/engine running.
- Gear selector P/R/N/D.
- Vehicle speed.
- RPM.
- Steering angle.
- Steering wheel buttons.
- Lights: low beam, high beam, fog, DRL.
- Wipers if visible.
- Lock/unlock.

## Завтрашний План Снятия Логов

Главное правило: одно действие - один лог или один четкий marker. Не смешивать двери, поворотники, климат и музыку в одном хаосе.

### 0. Подготовка

1. Поставить адаптер в known-good normal mode.
2. Проверить, что машина не ругается ошибками.
3. Подключить USB напрямую к Mac, без нестабильного хаба.
4. Подключить обе шины:
   - M-CAN 100 kbit/s.
   - C-CAN 500 kbit/s.
5. Проверить, что логер видит оба канала.
6. В начале каждого теста вслух/в чат писать marker: `START door_driver_open`.
7. После теста писать marker: `END door_driver_open`.

### 1. Baseline

Снимаем четыре базовых состояния:

| Лог | Длительность | Машина |
|---|---:|---|
| `baseline_power_off` | 60 сек | Машина закрыта/спокойна, адаптер запитан как получится от схемы. |
| `baseline_acc` | 60 сек | ACC on, двигатель не запущен. |
| `baseline_ign_on` | 60 сек | Зажигание on, двигатель не запущен. |
| `baseline_engine_idle` | 120 сек | Двигатель заведен, стоим. |

Команда:

```bash
python3 tools/gsusb_2can35_logger.py --bitrate0 100000 --bitrate1 500000 --seconds 120 --outfile logs/live_YYYYMMDD_HHMM_baseline_engine_idle.txt
```

### 2. Двери/Кузов

Для каждого события:

1. 10 секунд покой.
2. Сделать действие.
3. Держать состояние 10 секунд.
4. Вернуть обратно.
5. Подождать 10 секунд.
6. Повторить 3 раза.

События:

- `door_driver_open_close`
- `door_passenger_open_close`
- `door_rear_left_open_close`
- `door_rear_right_open_close`
- `trunk_open_close`
- `hood_open_close`
- `lock_unlock_key`
- `lock_unlock_button`

### 3. Свет/Поворотники

События:

- `turn_left`
- `turn_right`
- `hazard`
- `low_beam_on_off`
- `high_beam_on_off`
- `fog_front_on_off`
- `fog_rear_on_off`
- `drl_state`
- `brake_pedal_press_release`
- `parking_brake_on_off`

Каждое действие держать минимум 5 секунд, повторить 3 раза.

### 4. Reverse / Камера / Парктроники

Сначала без движения, на тормозе:

- `gear_r_on_off`
- `reverse_physical_input_vs_can`
- `steering_reverse_left_center_right`
- `parking_sensor_near_far` если можно безопасно поставить препятствие.
- `spas_state` если есть.

Важно: для dynamic lines обязательно логировать steering angle при включенном R.

### 5. Климат

Каждый пункт менять по одному:

- fan speed 0 -> max -> 0.
- driver temp min -> 22 -> max.
- passenger temp min -> 22 -> max.
- mode face/feet/windshield.
- AC on/off.
- auto on/off.
- front defrost.
- rear defrost.
- recirculation.

### 6. Руль/Кнопки/Магнитола

Проверить отдельно:

- volume up/down.
- next/prev track.
- mode/source.
- phone answer/hangup.
- voice.
- mute.
- seek.

### 7. Двигатель/Движение

Только безопасно:

- idle 2 минуты.
- RPM 1000/1500/2000 на месте, если безопасно.
- скорость 0/5/20/40 км/ч на закрытой безопасной дороге.
- плавное торможение.

### 8. Android -> Adapter -> CAN Тесты

После чистых CAN-логов проверяем, что можно отправить в машину через USB:

- FM station: 16 chars.
- FM station: long >16 chars.
- media text: 16 chars.
- track text: 16 chars.
- nav on.
- maneuver icon 0/1/2/3/finish.
- distance 80 m, 150 m, 1.2 km, 12.3 km.
- ETA/distance to finish.
- speed limit 40/60/90 только если машина не ругается.
- amp request/apply только если штатный усилитель есть и это безопасно.

Пакеты надо слать повтором 5 секунд, не одиночным кадром.

Готовый тестовый скрипт:

```bash
python3 tools/send_usb_display_demo.py /dev/cu.usbmodemKIA1 --seconds 5
```

## Как Разбирать Логи

Для каждого события делаем три окна:

- `before`: 10 секунд до действия.
- `during`: время действия.
- `after`: 10 секунд после возврата.

Алгоритм:

1. Разделить по channel: `ch0` и `ch1`.
2. Посчитать частоты ID и DLC.
3. Найти ID, которые появились только during.
4. Для постоянных ID найти байты, которые менялись синхронно с действием.
5. Проверить повторяемость на 3 циклах.
6. Записать кандидата в таблицу:

```text
event, channel, bitrate, can_id, dlc, byte, bit/mask, off_value, on_value, confidence, notes
```

7. Сравнить с поведением прошивки программиста: если firmware уже реагирует, найти такой же ID в дизассемблере.
8. Только после этого писать патч/код.

Быстрый summary по логу:

```bash
python3 tools/analyze_can_log.py logs/live_YYYYMMDD_HHMM_baseline_engine_idle.txt --top 80
```

## Что Ищем По Багу `Музыка USB`

Надо доказать не "строку", а CAN-событие.

Вероятные варианты:

1. Firmware видит пустой/нулевой media state и сама отправляет fallback `Музыка USB`.
2. Firmware получает source state по UART/FYT и транслирует его, но user test показал, что при физически отключенном UART spam оставался.
3. Firmware держит internal scheduler, который периодически обновляет media source даже без APK.
4. Один из M-CAN source frames заставляет приборку показывать USB.

Завтра снимаем:

- normal programmer firmware, Android disconnected from adapter USB.
- normal programmer firmware, Android connected.
- physical UART disconnected.
- physical UART connected.
- source на магнитоле: USB выбран.
- source на магнитоле: не USB выбран.
- сразу после пропадания spam и сразу после возврата spam.

Для каждого состояния нужен лог 60 секунд.

Потом ищем CAN ID, который меняет только source/media display, а не всю навигацию.

## Безопасность

- До завершения разбора логер держим listen-only.
- Не включать active injection в C-CAN без отдельного решения.
- Не шить экспериментальные full ST-Link images в машине.
- Если USB update завис, сначала power cycle 3-5 секунд, потом проверка порта, потом только повтор.
- ST-Link recovery использовать только по `docs/RECOVERY_STLINK_SEQUENCE.md`.

## Внешние Источники Для Дальнейшей Работы

- CAN basics / подборки: `github.com/iDoka/awesome-canbus`.
- GS USB firmware reference: `github.com/candle-usb/candleLight_fw`.
- Lawicel/SLCAN parser: `github.com/tixiv/lib-slcan`.
- Cangaroo: `github.com/normaldotcom/cangaroo`.
- SavvyCAN: `github.com/collin80/SavvyCAN`.
- STM32F1 analysis / H3 method references: `github.com/JohannesObermaier/f103-analysis`, `github.com/CTXz/stm32f1-picopwner`.

Практически для нашего адаптера важнее всего не общий CAN софт, а точная карта двух шин Kia/Hyundai и совместимость с протоколом APK `com.sorento.navi`.
