# 2CAN35 CAN Lab Dashboard

Локальный web-интерфейс для работы с адаптером:

- replay уже снятого CAN-лога без машины;
- live-чтение `gs_usb` logger режима;
- live-чтение запасного CDC logger режима;
- быстрый просмотр DBC/function matrix;
- тестовая отправка USB media/FM/navigation кадров в штатную прошивку;
- программный переход mode 1 / mode 2 / mode 3 через CDC.

Запуск:

```bash
python3 dashboard/server.py --port 8765
```

Открыть:

```text
http://127.0.0.1:8765
```

Без машины нажать `Sample replay` - интерфейс проиграет файл:

```text
logs/car_can_cleanjump_20260506_220618.txt
```

С машиной:

1. Для штатного режима с USB CDC использовать вкладку `Transmit`.
2. Для логгера mode 3 нажать `Live GS USB`.
3. Скорости по нашей текущей схеме: `ch0 = 100000`, `ch1 = 500000`.

Текущие decoded state значения являются кандидатами. Пока не заполнены точные
битовые позиции по твоему авто, интерфейс показывает raw bytes и DBC source,
чтобы быстро видеть, какой ID меняется при действии.
## Raw CAN TX

`Live GS USB` now starts the mode3 logger with a JSONL TX control file. While
that live session is running, the `Transmit -> Raw CAN TX` panel can queue:

- one classic CAN frame: channel, CAN ID, 0..8 data bytes, repeat count,
  interval;
- a one-byte sweep: keep the same frame, vary one byte over a small range.

The dashboard requires `TX enabled` to be checked before it queues a frame. This
is intentional: mode3 can now transmit onto the car CAN bus, not only listen.

Known channel mapping from the current car setup:

- `ch0 = M-CAN = 100000`
- `ch1 = C-CAN = 500000`

Use this for controlled discovery of cluster/HU display frames such as source
state, CarPlay/USB/BL music labels, navigation state, or gateway-mirrored body
states. Keep active-control frames out of sweeps unless a separate safety
decision has been made.
