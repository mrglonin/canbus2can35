# Сверка UART/USB-команд с прошивкой автора

Файл содержит только проверенные строки. Неподтвержденные команды люка,
подогрева руля и прочие кандидаты перенесены в архив.

## Body -> Raise/HU совпадения

| Функция | CAN источник | Формула | UART/протокол | Статус |
|---|---|---|---|---|
| LF door | `C-CAN 0x541 DATA[1] bit0` | `1=open` | `FD 05 05 01 00 0B` | match |
| RF door | `C-CAN 0x541 DATA[4] bit3` | `1=open` | `FD 05 05 02 00 0C` | match |
| LR door | `C-CAN 0x553 DATA[3] bit0` | `1=open` | `FD 05 05 04 00 0E` | match |
| RR door | `C-CAN 0x553 DATA[2] bit7` | `1=open` | `FD 05 05 08 00 12` | match |
| trunk | `C-CAN 0x541 DATA[1] bit4` | `1=open` | `FD 05 05 10 00 1A` | match |
| hood | `C-CAN 0x541 DATA[2] bit1` | `1=open` | `FD 05 05 20 00 2A` | match |
| all body closed | `C-CAN 0x541/0x553` | body bits clear | `FD 05 05 00 00 0A` | match |
| reverse on | `C-CAN 0x169 DATA[0] & 0x0F` | `0x07=R` | `FD 06 7D 06 02 00 8B` | local extension tested |
| reverse off | `C-CAN 0x169 DATA[0] & 0x0F` | `!=0x07` | `FD 06 7D 06 00 00 89` | local extension tested |

## CAN декодеры без внешнего UART-байта

| Функция | CAN источник | Формула | Статус |
|---|---|---|---|
| speed to HU | `C-CAN 0x316 DATA[6]` | `DATA[6] * 100` в выходном кадре автора | подтверждено |
| engine temp | `C-CAN 0x329 DATA[1]` | `(raw - 0x40) * 0.75` | подтверждено |
| outside temp | `C-CAN 0x044 DATA[3]` | `(raw - 0x52) / 2` | подтверждено |

## HU/media вход в прошивку

| Функция | Команда | Статус |
|---|---|---|
| USB media status | `FD 0A 09 16 00 00 00 00 02 00 2B` | match user photo / programmer APK media path |
| time sync | `FD 07 06 2C 06 01 00 40` | match user photo |

Вывод: двери, багажник, капот, задний ход, скорость и температуры можно держать
как подтвержденный слой. Медиа и навигацию надо отправлять через штатный
USB/API/Raise контракт прошивки, а не через угаданные raw CAN кадры.
