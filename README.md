# canbus2can35

Лаборатория, документация и исходники для кастомного 2CAN35/Sigma10 canbox на
базе `STM32F105RBT6` под Kia/Hyundai Sportage-family setup.

Главная цель проекта - максимально раскрыть функции автомобиля по двум
CAN-шинам `C-CAN` и `M-CAN`, собрать воспроизводимую карту сигналов и
постепенно перенести подтвержденную логику в свою прошивку.

Важно: текущая рабочая прошивка для тестов в машине пока не является полностью
написанной с нуля C-прошивкой. Практический файл `04350008 + mode3` - это
интеграционная бинарная сборка: в `mode1` используется поведение прошивки автора
`04.35.00.08`, а наши изменения добавляют сохраненный режим обновления,
переключение режимов и `gs_usb` CAN logger в `mode3`. Чистая C-прошивка лежит в
`firmware/custom_c/`, собирается из исходников, но пока является лабораторной
базой и еще не заменяет полный canbox.

Репозиторий не задуман как публичная раздача чужих APK/прошивок. Сторонние
бинарники описаны через имена, версии, размер и SHA256; собственные инструменты,
документация, таблицы и исходники хранятся в git.

## Железо

| Узел | Что известно сейчас |
|---|---|
| Плата | StarLine / 2CAN35-like board, PCB `StarLine 200-00002 REV.A` |
| MCU | `STM32F105RBT6`, LQFP64 |
| UID адаптера | `37 FF DA 05 42 47 30 38 59 41 22 43` |
| USB CDC | `0483:5740`, обычно `/dev/cu.usbmodemKIA1` |
| C-CAN | `PB8/PB9`, 500 kbit/s |
| M-CAN | `PB12/PB13`, 100 kbit/s |
| TEYES/Raise UART2 | `PA2/PA3`, 19200 8N1 |
| Управление выходом заднего хода | `PC14`, логический active-high выход на внешний драйвер |
| Строка профиля из прошивки | `HYK-RZ-10-0001-VK` |

Полезные документы по плате:

- переделка и подключение платы: `docs/HARDWARE_WIRING_MOD_GUIDE.md`;
- карта по фото платы: `firmware/custom_c/docs/BOARD_PHOTO_PINMAP.md`;
- карта входов/выходов: `firmware/custom_c/docs/BOARD_IO_MAP.md`;
- TEYES/Raise UART: `firmware/custom_c/docs/TEYES_RAISE_COMPAT.md`;
- восстановление через ST-Link: `docs/RECOVERY_STLINK_SEQUENCE.md`.

## Архитектура

Проект разделен на несколько слоев:

| Слой | Назначение |
|---|---|
| Парсер CAN автомобиля | Читать двери, задний ход, парковку, климат, скорость, температуры и другие состояния из `C-CAN` / `M-CAN`. |
| TEYES/Raise UART bridge | Общаться с Android-магнитолой через выбранный Raise Hyundai/Kia canbox протокол. |
| Отправка в M-CAN | Отправлять подтвержденные кадры медиа, навигации и приборной панели в штатную шину автомобиля. |
| Лабораторные инструменты | Логировать обе шины, повторять логи, отправлять контролируемые CAN/USB/UART тесты и обновлять прошивку. |

### Аналоговые кнопки и внешний Raise

Кнопки руля и кнопки-пианино над климатом считаются аналоговыми кнопками. Их
не нужно в первую очередь искать в CAN, потому что практический вариант такой:

```text
аналоговые кнопки -> штатный Raise canbox -> UART2 -> наш 2CAN35 -> TEYES
```

Смысл:

- штатный Raise продолжает читать аналоговые кнопки как задумано;
- его UART-выход заводится в UART2 нашего адаптера;
- наш адаптер может прозрачно мостить, логировать, фильтровать или расширять
  этот поток;
- магнитола уже ожидает Raise-протокол, поэтому для базовой работы кнопок не
  нужен конвертер протокола.

Для чистой C-прошивки это означает: сначала нужен надежный прозрачный Raise
UART bridge, и только потом можно пытаться полностью заменить обработку кнопок.
CAN-кандидаты по кнопкам остаются вторичной исследовательской задачей.

## Текущее состояние

### Работает / подтверждено

| Область | Статус |
|---|---|
| Штатный USB-протокол обновления | Работает. Команда `0x55`, блоки по 16 байт, проверка UID. |
| Практическая трехрежимная сборка | Работает как бинарный процесс: текущий пакет использует mode1 автора `04.35.00.08`, mode2 обновления, mode3 логгера. |
| GS USB логгер | Работает, когда mode3 определяется как `1d50:606f`; используется для логов двух CAN-шин. |
| CDC-порт на Mac | Работает как `/dev/cu.usbmodemKIA1` для штатного CDC/update режима. |
| Скорости CAN | Подтвержденная схема: `M-CAN=100000`, `C-CAN=500000`. |
| Web-интерфейс | Локальный интерфейс для replay, live logger, таблицы функций и контролируемого TX. |
| Сборка чистой C-прошивки | Собирается и пакуется из `firmware/custom_c/`. |
| Чистая C USB CDC | Реализовано как `KIA CANBOX 2CAN35`, VID/PID `0483:5740`. |
| Чистая C raw CAN TX/RX | Реализовано для лабораторного режима. |
| Чистая C TEYES UART TX | Базово реализовано за флагом `ENABLE_TEYES_UART=1`. |
| Чистая C reverse output | Реализовано на `PC14` за флагом `ENABLE_REVERSE_OUT=1`. |
| Логи и таблицы кандидатов | Размеченные данные лежат в `logs/session_20260507/` и `data/can_function_matrix.csv`. |

### Экспериментально

| Область | Статус |
|---|---|
| `firmware/canlog/` | Полезные пакеты для тестов в машине, но это все еще бинарный wrapper-подход. |
| `firmware/custom_c/` как ежедневная прошивка | Реальный C-код есть, но это еще не полный заменитель прошивки автора. |
| TEYES/Raise semantic parser | Формат и базовые body/reverse пакеты частично разобраны; полный startup/identity и все команды еще не готовы. |
| M-CAN media/navigation | Публичные DBC-кандидаты и APK-команды сопоставлены; финальные генераторы кадров еще не утверждены. |
| Parking/SPAS/RCTA | Есть кандидаты ID, но точная карта байтов/зон требует более чистых логов. |
| Климат | Есть кандидаты ID, но точная карта байтов неполная. |
| Raw CAN TX из dashboard | Доступен для осторожного поиска команд. |
| UART2 passives на плате | `PA2/PA3` известны, но нижний провод и отсутствующие резисторные места нужно прозвонить. |

### Еще не реализовано

| Область | Что нужно сделать |
|---|---|
| Полная clean-room canbox прошивка | Перенести подтвержденные CAN/UART/media/climate/parking функции в `firmware/custom_c`. |
| Полный переход clean C в штатный loader mode2 | Чистая C-прошивка пока отвечает на команды режимов, но не передает управление штатному loader полностью. |
| Прозрачный Raise UART bridge | Нужен для цепочки кнопки -> Raise -> наш адаптер -> TEYES. |
| TEYES/Raise identity exchange | Нужен точный startup/profile exchange для `HYK-RZ-10-0001-VK`. |
| Именованные M-CAN отправители media/nav | Нужны подтвержденные кадры для FM/AM/USB/BT/CarPlay/Android Auto/навигации/компаса по умолчанию. |
| Точная карта парктроников/RCTA | Нужны безопасные изолированные логи препятствий. |
| Точная карта климата | Нужны изолированные логи каждого состояния климата. |
| Второй UART | USART1 упоминается в работах по reverse, но назначение и трассировка на плате не подтверждены. |
| Физический вход заднего хода | Выход заднего хода найден; отдельный физический вход еще нужно подтвердить. |

## Структура репозитория

```text
dashboard/                  Локальный web-интерфейс для replay, live logging и TX.
data/can_function_matrix.csv Рабочая таблица CAN-функций.
docs/
  HARDWARE_WIRING_MOD_GUIDE.md       Инструкция по переделке и подключению платы.
  PROJECT_STATUS_FOR_AUTHOR.md       Короткая сводка для автора прошивки.
  CAN_FUNCTION_MATRIX.md             Читаемая таблица функций.
  HYUNDAI_KIA_MCAN_MEDIA_RESEARCH.md M-CAN media/navigation DBC-кандидаты.
  REVERSE_SPORTAGE_2CAN35.md         Заметки по reverse APK/update/прошивки.
  FIRMWARE_V05_COMPARISON.md         Сравнение родственных сборок автора.
  SPORTAGE_0705_APK_04100007_ANALYSIS.md Анализ APK 07.05 / update 04100007.
  SPORTAGE_0805_APK_04350008_ANALYSIS.md Анализ APK 08.05 / update 04350008.
  RECOVERY_STLINK_SEQUENCE.md        Восстановление через ST-Link.
firmware/
  MANIFEST.md                        Хэши и статус локальных прошивок.
  canlog/                            Практические экспериментальные бинарные пакеты.
  custom_c/                          Чистая C-прошивка.
logs/
  car_can_cleanjump_20260506_220618.txt   Маленький пример лога.
  session_20260507/                  Размеченная сессия логов и таблицы.
samples/
  stations.txt                       Таблица FM-станций из APK.
tools/
  usb_update_2can35.py               Штатный USB-процесс обновления.
  usb_mode_2can35.py                 Переключение режимов.
  gsusb_2can35_logger.py             GS USB logger.
  stockusb_canlog_2can35.py          CDC-протокол логгера.
  send_usb_display_demo.py           Отправка тестов USB media/nav/FM.
  analyze_can_log.py                 Анализ CAN-логов.
  decode_2can35_update.py            Декодер/энкодер пакета обновления.
  build_04350008_mode3_package.py    Сборка v08 mode1 + сохраненный mode3.
```

Игнорируются git'ом:

- `firmware/local/`;
- raw APK/bin без явного права публикации;
- `firmware/custom_c/build/`;
- `firmware/custom_c/dist/`;
- большие сырые логи вида `logs/session_*/full_*.txt`.

## Текущая практическая прошивка

Текущий рабочий пакет:

```text
firmware/canlog/2can35_04350008_canlog_v4_mode3_preserve_beeps_usb.bin
```

Суть пакета:

| Режим | Что делает |
|---|---|
| mode1 | Поведение прошивки автора `04.35.00.08`. |
| mode2 | Штатный USB update loader. |
| mode3 | Сохраненный `gs_usb` / budgetcan CAN logger. |

Пакет пересобирается воспроизводимо:

```sh
python3 tools/build_04350008_mode3_package.py
```

Для `08` точки hook'ов отличаются от старой `06`:

```text
0x08005478 -> local dispatch A
0x08005486 -> local dispatch B
0x08004004 -> reset hook
```

## Сборка чистой C-прошивки

Обычная сборка держит CAN hardware выключенным:

```sh
cd firmware/custom_c
make clean package
make sim
```

Полная экспериментальная сборка для проверки исходников:

```sh
cd firmware/custom_c
make clean package \
  ENABLE_CAN_HW=1 \
  USE_STOCK_BEEP=1 \
  ENABLE_REVERSE_OUT=1 \
  ENABLE_TEYES_UART=1 \
  TEYES_UART_BAUD=19200 \
  VERSION_HEX=04351002
make sim
```

Ожидаемый локальный пакет:

```text
build/2can35_custom_04351002_update.bin
```

Хэш последней локальной проверочной сборки:

```text
a4662920bdd7de908bb4c31c1c0c612c99433daacc91f0f128a034e02489e743
```

## Web-интерфейс

Запуск:

```sh
python3 dashboard/server.py --port 8765
```

Открыть:

```text
http://127.0.0.1:8765
```

Назначение dashboard:

- replay размеченных логов;
- live GS USB capture;
- просмотр таблицы функций;
- очередь контролируемых raw CAN TX тестов;
- отправка USB display/navigation/media экспериментов;
- переключение режимов, если текущая прошивка это поддерживает.

## USB update и переключение режимов

Переключение режима:

```sh
python3 tools/usb_mode_2can35.py /dev/cu.usbmodemKIA1 update
python3 tools/usb_mode_2can35.py /dev/cu.usbmodemKIA1 canlog
python3 tools/usb_mode_2can35.py /dev/cu.usbmodemKIA1 normal
```

Заливка пакета обновления:

```sh
python3 tools/usb_update_2can35.py /dev/cu.usbmodemKIA1 firmware/local/<update>.bin
```

Лог двух шин через GS USB:

```sh
python3 tools/gsusb_2can35_logger.py \
  --bitrate0 100000 \
  --bitrate1 500000 \
  --seconds 120 \
  --outfile logs/live_test.txt
```

Fallback CDC logger:

```sh
python3 tools/stockusb_canlog_2can35.py /dev/cu.usbmodemKIA1 --seconds 120
```

## Процесс заполнения CAN-карты

Новую функцию нужно переносить в таблицу только после доказательств:

1. Снять baseline.
2. Отметить `START <event>` и `END <event>`.
3. Повторить действие 3-5 раз.
4. Сравнить `before / during / after`.
5. Найти канал, CAN ID, байт, маску, off value и on value.
6. Добавить в `data/can_function_matrix.csv`.
7. Отправлять controlled TX только если кадр безопасен.
8. Переносить в именованную функцию прошивки только после повторяемого
   поведения.

Приоритетные группы:

- кузов: двери, багажник, капот, люк, замки, зажигание;
- задний ход: CAN reverse, физический вход, +12 V output, динамические линии;
- парковка: передние/задние датчики, SPAS, боковое предупреждение, RCTA;
- климат: вентилятор, температура водитель/пассажир, AC, auto, defrost,
  рециркуляция, обогрев/вентиляция сидений;
- аналоговые кнопки через Raise UART: руль и панель-пианино;
- media/source: FM, AM, USB, Bluetooth, CarPlay, Android Auto, компас по умолчанию;
- навигация: улица, TBT, дистанция, ETA, speed limit если поддерживается;
- диагностика/статус: наружная температура, температура двигателя, скорость,
  RPM, ошибки если видны.

## Правила безопасности

- Logger-сессии по умолчанию держать listen-only, если тест явно не требует TX.
- Не перебирать активные управляющие C-CAN кадры вслепую.
- Display/media/navigation тесты повторять короткими ограниченными интервалами,
  а не бесконечными циклами.
- Не прошивать сторонние profile binaries на этот UID, если не понятны UID,
  header и target.
- Для ST-Link recovery использовать `docs/RECOVERY_STLINK_SEQUENCE.md`.
- `firmware/custom_c/` считать экспериментальной, пока ежедневные функции не
  подтверждены на машине.

## Внешние источники

CAN и инструменты:

- https://github.com/iDoka/awesome-canbus
- https://github.com/candle-usb/candleLight_fw
- https://www.can232.com/docs/can232_v3.pdf
- https://github.com/tixiv/lib-slcan
- https://github.com/homewsn/candleLight_fw-SavvyCAN-Windows-plugin
- https://github.com/normaldotcom/cangaroo
- https://github.com/collin80/SavvyCAN
- https://github.com/TOSUN-Shanghai/TSMaster

STM32 / firmware research:

- https://www.st.com/resource/en/datasheet/stm32f105rb.pdf
- https://github.com/CTXz/stm32f1-picopwner
- https://github.com/JohannesObermaier/f103-analysis/tree/master/h3

Практические материалы по 2CAN35:

- https://www.drive2.ru/l/717368666034802531/
- https://www.drive2.ru/l/717580596901055496/

Локальные документы проекта:

- `docs/PROJECT_STATUS_FOR_AUTHOR.md`
- `docs/REVERSE_SPORTAGE_2CAN35.md`
- `docs/CAN_FUNCTION_MATRIX.md`
- `firmware/custom_c/README.md`
