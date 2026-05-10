# Сверка UART-команд с прошивкой прогера

| Функция | Источник в прошивке прогера | Формула | UART-кандидат | Результат |
|---|---|---|---|---|
| LF door | `C-CAN 0x541 DATA[1] bit0` | `1=open` | `FD 05 05 01 00 0B` | match |
| RF door | `C-CAN 0x541 DATA[4] bit3` | `1=open` | `FD 05 05 02 00 0C` | match |
| LR door | `C-CAN 0x553 DATA[3] bit0` | `1=open` | `FD 05 05 04 00 0E` | match |
| RR door | `C-CAN 0x553 DATA[2] bit7` | `1=open` | `FD 05 05 08 00 12` | match |
| trunk | `C-CAN 0x541 DATA[1] bit4` | `1=open` | `FD 05 05 10 00 1A` | match |
| hood | `C-CAN 0x541 DATA[2] bit1` | `1=open` | `FD 05 05 20 00 2A` | match |
| all body closed | `C-CAN 0x541/0x553` | `all door/trunk/hood bits clear` | `FD 05 05 00 00 0A` | match |
| reverse on | `C-CAN 0x169 DATA[0] & 0x0F` | `0x07=R` | `FD 06 7D 06 02 00 8B` | match_local_extension |
| reverse off | `C-CAN 0x169 DATA[0] & 0x0F` | `!=0x07` | `FD 06 7D 06 00 00 89` | match_local_extension |
| speed to HU | `C-CAN 0x316 DATA[6]` | `DATA[6] * 100 in outgoing programmer frame` | `` | not_raise_uart_command |
| engine temp | `C-CAN 0x329 DATA[1]` | `(raw - 0x40) * 0.75` | `` | confirmed_can_decoder_not_external_raise_uart |
| outside temp | `C-CAN 0x044 DATA[3]` | `(raw - 0x52) / 2` | `FD 04 01 TT CS_H CS_L` | protocol_match_value_dynamic |
| sunroof | `C-CAN 0x541 DATA[7] bit1` | `1=open` | `FD 05 05 40 00 4A` | can_candidate_known_uart_not_confirmed_by_teyes |
| steering wheel heat | `C-CAN 0x559 DATA[0] bit4` | `1=on` | `` | can_candidate_requires_more_live_confirmation |
| USB media status | `HU UART/photo/programmer APK media path` | `source 0x16 + track/time fields` | `FD 0A 09 16 00 00 00 00 02 00 2B` | match_user_photo |
| time sync | `HU UART/photo` | `minute hour mode` | `FD 07 06 2C 06 01 00 40` | match_user_photo |

Главный вывод: двери/багажник/капот и задний ход совпадают с тем, что уже есть в наших Raise-кандидатах. Температуры и скорость надо держать как CAN-декодеры, а не искать для них внешний UART Raise-байт.
