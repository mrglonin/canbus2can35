# UART command verification

Цель: получить не теоретический, а реально рабочий список UART-команд для выбранного профиля TEYES/canbox.

## Файлы

- `data/uart_command_candidates.csv` - полный текущий список кандидатов для проверки.
- `data/uart_command_verifications.jsonl` - результаты проверки: `works`, `no_effect`, `bad`, `unsafe`, `unknown`.
- `data/raise_rzc_korea_uart_matrix.csv` - расшифрованная база Raise/RZC `FD ...`.
- `data/apk_canbus_12072024/teyes_profile_uart_surfaces.csv` - профили TEYES, slots и UART surfaces.

## Как проверять завтра

1. Подключить адаптер в `mode3 lab`.
2. В dashboard открыть `UART Raise`.
3. Запустить `Live CAN+UART`.
4. В блоке `Проверка UART команд` искать нужное: `FD`, `2E`, `USB`, `люк`, `Simple`, `климат`.
5. Жать `TX` только по одной команде и смотреть на магнитолу/приборку.
6. Если сработало - нажать `работает`.
7. Если реакции нет - нажать `нет`.
8. После проверки экспортировать `/api/export` или файл `data/uart_command_verifications.jsonl`.

## Статусы

- `works` - команда фактически сработала на машине/магнитоле.
- `no_effect` - команда валидная по формату, но визуальной реакции нет.
- `bad` - команда вызывает неправильный эффект.
- `unsafe` - не использовать в обычном тесте.
- `candidate` - еще не проверено.

## Протоколы

- `raise_rzc_fd`: кадры `FD LL CMD PAYLOAD CS16`; это основной подтвержденный Raise/RZC Korea слой.
- `canbox_2e`: кадры `2E CMD LEN PAYLOAD CRC8_XORFF`; это кандидат для Simple/Hiworld/других canbox-профилей, найденный в открытой реализации SmartGauges и добавленный в parser для live-проверки.

Полностью рабочим считается только то, что имеет verdict `works`.
