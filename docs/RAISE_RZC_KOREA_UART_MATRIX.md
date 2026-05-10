# Raise/RZC Korea UART Matrix

Дата: 2026-05-09

Цель: отделить **UART-протокол canbox Raise/RZC** от трех других слоев:

- APK/USB команды приложения программиста;
- CAN автомобиля (`C-CAN 500 kbit/s`, `M-CAN 100 kbit/s`);
- M-CAN пакеты, которые выводят музыку/навигацию на штатную приборку.

В режиме TEYES `Raise -> Hyundai/Kia -> Sportage` магнитола ожидает не CAN
кадры, а UART кадры canbox. Наш адаптер должен выглядеть для магнитолы как
Raise/RZC canbox: принимать команды магнитолы по UART, отдавать события машины
по UART и отдельно конвертировать часть данных в CAN.

## Источники

Подтвержденные локальные источники:

- прошивка программиста `37FFDA054247303859412243_04350008.bin`;
- наши UART/CAN кадры, снятые с машины;
- фото WR-лога магнитолы с кадрами `FD 0A 09 ...`, `FD 07 06 ...`,
  `FD 07 EE ...`;
- `firmware/custom_c/docs/TEYES_RAISE_COMPAT.md`.

Публичные источники:

- `RZC_KoreaSeriesProtocol.java`:
  https://raw.githubusercontent.com/darkspr1te/Work/master/MctCoreServices/src/com/mct/carmodels/RZC_KoreaSeriesProtocol.java
- `RZC_KoreaSeriesManager.java`:
  https://raw.githubusercontent.com/darkspr1te/Work/master/MctCoreServices/src/com/mct/carmodels/RZC_KoreaSeriesManager.java
- `CanBoxProtocol.java`:
  https://raw.githubusercontent.com/darkspr1te/Work/master/MctCoreServices/src/com/mct/coreservices/CanBoxProtocol.java
- `CanManager.java` pack/unpack:
  https://raw.githubusercontent.com/darkspr1te/Work/master/MctCoreServices/src/com/mct/coreservices/CanManager.java
- `CarModelDefine.java`:
  https://raw.githubusercontent.com/darkspr1te/Work/master/MctCoreServices/src/com/mct/carmodels/CarModelDefine.java

Публичный код не является именно прошивкой нашего 2CAN35, но он описывает
профиль `RZC` / `Korea` со стороны Android-headunit и поэтому полезен для
того, какие UART команды магнитола умеет понимать и отправлять.

## Формат Кадра

Для `CAN_BOX_RZC`, модели `300..349` (`Korea`: IX35, Sorento, K5, KX5 и т.д.)
публичный `CanBoxProtocol.java` задает:

| Поле | Значение |
|---|---|
| Prefix | `0xFD` |
| UART | `0x4B00` = 19200 baud, 8N1 |
| Порядок | `FD LL CMD PAYLOAD... CS_H CS_L` |
| `LL` | длина всех байт после `FD`, включая `LL`, `CMD`, payload и 2 байта checksum |
| Checksum | `sum(LL + CMD + payload) & 0xFFFF`, high byte first |
| ACK | не требуется |

Это совпадает с нашими кадрами:

```text
FD 05 05 00 00 0A
FD 05 05 3F 00 49
FD 0A 09 16 00 00 00 00 02 00 2B
FD 07 06 2C 06 01 00 40
```

## Canbox -> Head Unit

Эти команды отправляет canbox в магнитолу. Наш адаптер должен формировать их
из CAN машины и/или прозрачного UART bridge от внешнего Raise canbox.

| Cmd | Название из RZC Korea | Payload | Что делает магнитола | Статус у нас |
|---|---|---|---|---|
| `0x01` | external temperature | `TT` | наружная температура, `bit7` = минус | публично подтверждено, CAN источник еще привязать |
| `0x02` | steering wheel / panel key | `KEY STATUS` | кнопки руля и панели | публично подтверждено; у нас кнопки идут аналогом через stock Raise -> UART bridge |
| `0x03` | air conditioning info | `L_TEMP R_TEMP FAN FLAGS` | всплывающее окно климата | публично подтверждено; CAN климат частично в таблице |
| `0x04` | radar info | `FRONT_PACKED REAR_PACKED` | передние/задние парктроники | публично подтверждено; реальные препятствия еще плохо пойманы |
| `0x05` | vehicle door info | `FLAGS` | двери/багажник/капот | подтверждено публично и локально |
| `0x06` | AC climate status | `STATUS` | показать панель климата; в коде отмечен KX5 2017 | публично подтверждено |
| `0x07` | backlight info | `LEVEL` | яркость/подсветка | публично подтверждено |
| `0x7F` | protocol version | ASCII/bytes | версия canbox | публично подтверждено |
| `0x7D` | reverse local extension | `06 00` / `06 02` | задний ход / камера | подтверждено локальной прошивкой программиста, в публичном RZC Korea не найдено |

### Команда `0x05`: двери/капот

Публичный `RZC_KoreaSeriesManager` разбирает только биты `0..5`:

| Bit | Значение |
|---|---|
| `0x01` | водительская дверь |
| `0x02` | пассажирская передняя дверь |
| `0x04` | задняя левая дверь |
| `0x08` | задняя правая дверь |
| `0x10` | багажник |
| `0x20` | капот |

Локальные тестовые кадры:

| Событие | UART frame |
|---|---|
| все закрыто | `FD 05 05 00 00 0A` |
| водительская дверь | `FD 05 05 01 00 0B` |
| пассажирская передняя | `FD 05 05 02 00 0C` |
| задняя левая | `FD 05 05 04 00 0E` |
| задняя правая | `FD 05 05 08 00 12` |
| багажник | `FD 05 05 10 00 1A` |
| капот | `FD 05 05 20 00 2A` |
| все 6 штатных битов | `FD 05 05 3F 00 49` |

### Люк

CAN люка на нашей машине подтвержден:

```text
C-CAN ch1 500000 STD ID 0x541 DLC 8
closed: 00 00 41 00 C0 00 00 01
closed with IGN: 03 00 41 00 C0 00 00 01
open:   03 00 41 00 C0 00 00 03
```

Декод: `DATA[7] & 0x02`.

Но в публичном `RZC_KoreaSeriesManager` для команды `0x05` нет обработки
`bit6/0x40`. Поэтому кадр:

```text
FD 05 05 40 00 4A
```

нужно считать **экспериментом**, а не подтвержденной штатной функцией TEYES.
Если TEYES/Raise профиль не знает люк, возможны варианты:

- добавлять люк в нашу Android-утилиту/оверлей;
- отображать через отдельный кастомный канал, если найдем его в других Raise
  профилях;
- не смешивать люк с дверью/капотом, чтобы не ломать штатную индикацию.

## Head Unit -> Canbox

Эти команды отправляет магнитола в canbox. Наш адаптер должен их принимать и
конвертировать в M-CAN/служебные действия, если хотим вывод на приборку.

| Cmd | Название из RZC Korea | Payload | Назначение |
|---|---|---|---|
| `0x03` | setting | unknown per public code | настройки |
| `0x04` | power | `00` start, `01` end | старт/окончание canbox session |
| `0x05` | volume adjust | `VOL` | управление штатным усилителем |
| `0x06` | time info | `MIN HOUR MODE` | синхронизация времени |
| `0x07` | audio balance | `FADE+7 BALANCE+7` | баланс/фейдер усилителя |
| `0x08` | sound effects | `BASS+10 MID+10 TREBLE+10` | тембры усилителя |
| `0x09` | host/media status | см. ниже | источник, радио, USB, BT, нави |

### Команда `0x09`: источники/медиа

Это объясняет фото с WR-логом магнитолы.

| Source byte | Payload | Значение |
|---|---|---|
| `0x02` | `02 BAND FREQ_H FREQ_L` | радио FM/AM; `BAND 00` FM, `03` AM в публичном коде |
| `0x16` | `16 TRACK_H TRACK_L HOUR MIN SEC` | USB media numeric status |
| `0x83` | `83 00` | other media |
| `0x12` | `12 00` | AUX |
| `0x80` | `80 00` | media off |
| `0x06` | `06 00` | navigation source |
| `0x11` | `11 00` | Bluetooth music |
| `0x07` | `07 STATUS` | Bluetooth phone call state |
| `0x0B` | `0B 04/05` | Bluetooth connected/disconnected |

Фото WR-лога:

```text
FD 0A 09 16 00 00 00 00 02 00 2B
```

Расшифровка: `cmd=0x09`, source `0x16` = USB media, `track=0`,
`play_time=2s`. Последний байт payload на фото часто растет как секунды
воспроизведения, это не обязательно номер трека.

Еще один кадр с фото:

```text
FD 07 06 2C 06 01 00 40
```

Расшифровка по публичному RZC Korea: `cmd=0x06` = time info,
`minute=44`, `hour=6`, `mode=1`.

Кадр:

```text
FD 07 EE 20 01 00 01 16
```

имеет правильный checksum, но `cmd=0xEE` не найден в публичном
`RZC_KoreaSeriesProtocol`. Пока статус: `valid_unknown`. Его надо ловить в live
UART и смотреть, всегда ли он идет рядом с кнопками/ACK/сервисом.

## Кнопки Руля И Панели

Публичные key codes:

| Key | Значение |
|---|---|
| `0x10` | mute |
| `0x11` | mode/source |
| `0x12` | seek up |
| `0x13` | seek down |
| `0x14` | volume up |
| `0x15` | volume down |
| `0x16` | phone accept |
| `0x17` | phone hangup |
| `0x18` | panel power |
| `0x19` | panel volume up |
| `0x1A` | panel volume down |
| `0x1B` | panel FM/AM |
| `0x1C` | panel media |
| `0x1D` | panel phone |
| `0x1E` | panel display |
| `0x1F` | panel seek up |
| `0x20` | panel seek down |
| `0x21` | panel map |
| `0x22` | panel dest |
| `0x23` | panel route |
| `0x24` | panel setup |
| `0x25` | panel enter |
| `0x26` | panel tuner up |
| `0x27` | panel tuner down |
| `0x28` | panel UVO |
| `0x29` | panel home |
| `0x84` | scroll up |
| `0x85` | scroll down |

У нас физическая архитектура другая: кнопки руля и “пианино” аналоговые, их
логичнее оставить на внешнем штатном Raise canbox, а наш адаптер должен сделать
прозрачный UART bridge:

```text
analog buttons -> stock Raise canbox -> our UART2 RX/TX -> TEYES UART
```

Так магнитола получает родные RZC key frames, а мы можем дополнительно логировать
или фильтровать их.

## Что Пока Не Найдено В RZC Korea

В публичном RZC Korea коде не найдено штатных UART событий для:

- люка;
- поворотников;
- ближнего/дальнего света как отдельной UI-функции;
- подогрева руля;
- подогрева/вентиляции сидений;
- Auto Hold / Drive Mode / Lock / Hill Descent;
- RCTA/LCA/слепые зоны;
- текстового названия трека/исполнителя.

Поиск по соседним публичным RZC профилям показал, что seat heater/fan события
есть у некоторых VW/Toyota/GM/Trumpchi профилей, а не у найденного Korea/KX5
профиля. `sunroof` как отдельная функция в этих исходниках не найден; есть
только VW `roof light brightness`, это не люк.

Это не значит, что TEYES никогда не умеет это показывать. Это значит, что в
найденном публичном RZC Korea профиле этих команд нет. Для них нужны:

1. live UART лог между реальным Raise/TEYES;
2. сравнение с новыми APK/progger бинарниками;
3. поиск в других RZC профилях;
4. отдельная Android-утилита/оверлей, если штатный TEYES UI это не поддерживает.

## Практические Команды Для Mode3 Lab

Через наш mode3 CDC lab можно отправлять raw UART командой `u...`.

Безопасные canbox->HU тесты:

```text
uFD050500000A  # all body closed
uFD050501000B  # driver door open
uFD050520002A  # hood open
uFD050540004A  # sunroof candidate; may be ignored by TEYES
uFD067D0602008B  # reverse on, local/progger extension
uFD067D06000089  # reverse off, local/progger extension
```

HU->canbox кадры вроде `FD0A0916...` обычно приходят со стороны магнитолы; их
лучше не “слать в магнитолу”, а принимать, декодировать и конвертировать в
M-CAN для приборки.

## Вывод Для Прошивки

Минимальный правильный слой Raise/RZC в нашей чистой прошивке:

1. UART2 `PA2/PA3`, 19200 8N1, frame `FD LL CMD ... CS`.
2. Canbox->HU:
   - двери/багажник/капот `0x05`;
   - климат `0x03/0x06`;
   - парктроники `0x04`;
   - наружная температура `0x01`;
   - backlight `0x07`;
   - version `0x7F`;
   - reverse `0x7D` как локальная совместимость с прошивкой программиста.
3. HU->canbox:
   - parse `0x09` media/source and convert to M-CAN cluster frames;
   - parse `0x06` time;
   - parse amplifier controls `0x05/0x07/0x08`, если включаем штатный усилитель;
   - pass/observe unknown `0xEE`.
4. Transparent bridge для внешнего Raise canbox, чтобы аналоговые кнопки руля и
   панели работали штатно.
