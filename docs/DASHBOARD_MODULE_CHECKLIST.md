# 2CAN35 Lab: модули, API и экспорт

Цель интерфейса: не красивая демка, а рабочая лаборатория для раздельного анализа CAN M, CAN C и UART Raise, с последующим переносом логики в Android APK или прошивку.

## Чек-лист UI

- [x] Верхняя панель без лишней шапки: запуск Live CAN, Live CAN+UART, CAN->UART bridge, сброс, стоп.
- [x] Режимы адаптера справа: 1 canbox, 2 update, 3 lab; активный режим подсвечивается.
- [x] Метрики по модулям: M-CAN ch0, C-CAN ch1, UART RX, UART TX, активный режим.
- [x] Вкладки на всю ширину: обучение, CAN log/TX, UART Raise, приборка CAN, экспорт.
- [x] Порт USB скрыт из основного UI, но поле сохранено для ручного режима и API.
- [ ] Отдельный режим CSV-экспорта для таблицы прогера.
- [ ] Больше готовых проверенных UART Raise команд после чистых логов.

## Модули

### Обучение CAN<->UART

Назначение: выбрать действие, записать чистое окно, отфильтровать фон, закрепить найденный CAN ID/байты и, если известно, UART-команду.

API:

- `POST /api/learn/start`
- `POST /api/learn/stop`
- `POST /api/learn/save`
- `GET /api/learned`
- `GET /api/export/learned-table`

### CAN log / TX

Назначение: читать CAN M/C, видеть распознанные состояния, вручную отправлять одиночные CAN кадры и sweep.

API:

- `POST /api/log/start` с `mode=gsusb`
- `POST /api/can/send`
- `POST /api/can/sweep`
- `GET /api/export/can/m`
- `GET /api/export/can/c`

Каналы:

- `ch0` = M-CAN 100k.
- `ch1` = C-CAN 500k.

### UART Raise

Назначение: читать команды магнитолы HU -> canbox и отправлять Raise/RZC UART команды canbox -> HU.

API:

- `POST /api/log/start` с `mode=lab`
- `POST /api/lab/send`
- `GET /api/export/uart`

### Приборка CAN

Назначение: отправлять тестовые кадры медиа/радио/навигации напрямую в CAN, отдельно выбирая M-CAN, C-CAN или оба канала.

API:

- `POST /api/send/display`

Поля:

- `transport`: сейчас штатно `can`.
- `bus`: `mcan`, `ccan`, `both`.
- `scenario`: `full`, `music`, `track`, `source`, `fm`, `nav`, `clear`.
- `seconds`: длительность повтора.

### Экспорт

Назначение: быстро отдать прогеру не весь шум, а разделенные данные.

API:

- `GET /api/export/can/m` - только M-CAN последние кадры.
- `GET /api/export/can/c` - только C-CAN последние кадры.
- `GET /api/export/uart` - UART RX/TX состояние и события.
- `GET /api/export/learned-table` - таблица действий CAN<->UART.
- `GET /api/export` - полный JSON-пакет.

## Таблица для прогера

`/api/export/learned-table` должен давать строки такого смысла:

- `action_id`, `action_name` - что делали в машине.
- `can_bus`, `can_id`, `can_dlc` - где найдено событие.
- `can_changed_bytes` - какие байты менялись.
- `can_on_or_last`, `can_off_or_first` - значения для ON/OFF или последнего/первого состояния.
- `uart_hint` - команда Raise/RZC, если уже известна.
- `notes` - комментарий анализа.

## Правило развития

Любую новую функцию сначала ловим в `Обучение CAN<->UART`, затем закрепляем, затем проверяем через `CAN log / TX` или `UART Raise`, затем только переносим в прошивку/APK.
