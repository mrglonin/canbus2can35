# Firmware adapter-owned logic target

Дата: 2026-05-14.

V21 уже перенесла базовое удержание штатных nav/compass событий в адаптер:
APK не шлет постоянные повторы и не использует raw M-CAN как основной способ
работы. APK сообщает только факт изменения состояния, а адаптер держит
маленькое состояние и отдает его штатному parser.

Это production-модель. Logger и raw TX остаются, но только как явный debug.

## Главный принцип

```text
APK event
  -> adapter state holder
  -> stock parser
  -> штатный M-CAN/cluster output
```

Нормальная работа не включает `0x70` raw stream и не требует чистой отправки
M-CAN кадров из APK. Чистый M-CAN TX через `0x78` остается только для ручной
диагностики, когда штатный API еще не покрывает конкретный тест.

## Что должно быть вшито в адаптер

| Механизм | Что ждет адаптер от APK | Что делает адаптер сам | Raw/logger нужен |
|---|---|---|---|
| Компас | новый compass value через штатный `0x45` payload | V21 хранит последний compass frame и повторяет его на `0x77` tick; формула панели `sent = (36 - ui) % 36` | нет |
| Навигация | route state changes: `0x7A nav`, `0x48`, `0x45`, `0x47`, `0x4A`, `0x44` | V21 хранит route bundle, повторяет active route около `1 s`, finish hold задает APK через clean off после `5 s` | нет |
| Музыка USB/cloud/Yandex | source/track event: `0x7A FD 0A 09 16 ...`, `0x22 title` | один раз прогоняет source/text через stock parser; не спамит трек | нет |
| Bluetooth audio | source/track event: `0x7A FD 06 09 0B 04 00`, `0x20 1F artist`, `0x22 title` | один раз прогоняет BT source/text; selected-paused не перебивает playing source | нет |
| FM/AM | source/station event: FM `0x21`, AM `0x20` плюс source-status | один раз обновляет mode/station text при смене волны/режима | нет |
| RCTA/blind spot | APK ничего не отправляет; только читает snapshot | пассивно слушает `0x4F4`, хранит latest payload/bus/dlc/id/data8 для `0x77` | нет |
| Voltage | APK ничего не отправляет; только читает snapshot | пассивно слушает M-CAN `0x132 DATA[0]`, хранит mV для `0x77` | нет |
| Vehicle | APK опрашивает compact snapshot `0x77` | пассивно слушает whitelisted CAN IDs и держит speed/rpm/temp/reverse/etc | нет |
| Debug logger | явный debug switch | включает `0x70/0x76`, пишет C-CAN/M-CAN в ring/log | да, только debug |
| Debug M-CAN TX | явная debug/TX команда `0x78 bus=1` | отправляет один M-CAN frame и возвращает ACK | да, только debug |

## Что APK должен перестать делать после такой прошивки

- Повторять compass каждые `350-500 ms` самому.
- Повторять active navigation bundle каждую секунду самому.
- Держать music source/title повтором.
- Включать raw stream для Vehicle/RCTA.
- Слать production raw M-CAN вместо штатных source/nav/media команд.
- Слать любой C-CAN TX в production.

## Что APK продолжает делать

- Определять реальный источник медиа: USB, BT, FM, AM, Yandex/cloud.
- Выбирать playing source выше selected-paused source.
- Формировать artist/title/station text.
- Разбирать навигацию/Yandex/2GIS/TEYES intents и отдавать route events.
- Читать `0x77` snapshot для UI, RCTA, voltage, vehicle state.
- Показывать предупреждения RCTA/TPMS и играть warning sound.
- Запускать debug logger/TX только по явному действию пользователя.

## Минимальная команда/состояние для будущей прошивки

Существующие команды можно оставить как внешний API:

| Команда | Production роль |
|---:|---|
| `0x20/0x21/0x22` | media/radio text events |
| `0x44/0x45/0x47/0x48/0x4A` | nav/compass events |
| `0x7A` | source-status injection into stock parser |
| `0x77` | compact Vehicle/RCTA/voltage snapshot |
| `0x79` | health/capabilities |
| `0x55/0x56` | update and UID/version |

Debug-only:

| Команда | Debug роль |
|---:|---|
| `0x70/0x76` | raw logger only |
| `0x78` | one-shot M-CAN TX only, `bus=1`; C-CAN blocked |

## Acceptance

- Без включенного debug raw stream работают compass, nav, media, RCTA, voltage и Vehicle.
- При смене трека/source APK отправляет одно событие, а не поток повторов.
- При active route адаптер сам удерживает последний route bundle, preview/failed не затирают active route.
- При stale compass адаптер держит короткий timeout и прекращает repeat без мусора.
- `0x77` дает voltage из M-CAN `0x132` и latest `0x4F4`.
- Logger пишет только после explicit debug switch и ограничивается app-side лимитом `50 000` кадров.
- `0x78 bus=0` возвращает blocked/bad bus; production C-CAN TX отсутствует.
