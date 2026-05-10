# TEYES Sportage Profile / UART / Slot Map

Этот файл отделяет три разных слоя, которые нельзя смешивать:

- **profile**: ID профиля CANBUS в меню TEYES.
- **raise_uart**: реальные `FD ...` кадры между canbox и магнитолой.
- **teyes_slot**: внутренние `DataCanbus.DATA[]` индексы APK/MCU, не UART байты.
- **teyes_mcu_api**: Android -> MCU команды внутри TEYES.

CSV для импорта/фильтрации: `data/apk_canbus_12072024/teyes_sportage_profile_command_map.csv`.

## Главный вывод

APK подтверждают профили, serial/JNI слой и внутренние slots для дверей, климата, подогревов, скорости и т.д. Но готовой таблицы `Raise/Simple UART byte -> функция` для всех функций внутри APK нет. Значит двери/медиа/базовый климат берем из подтвержденного Raise протокола, а подогревы/обдувы/режимы добиваем live UART/CAN обучением.

## Таблица

| layer | direction | name | TEYES id/slot | bytes/slot | status | meaning |
|---|---|---|---|---|---|---|
| `profile` | `teyes_setting` | RZC/Raise Sportage 2016 | `1442154` | `0x16016A` | `candidate_profile` | Кандидат Raise/RZC для Sportage 2016 в базе TEYES |
| `profile` | `teyes_setting` | RZC/Raise Sportage R 2018 | `65898` | `0x01016A` | `candidate_profile` | Кандидат Raise/RZC Sportage R 2018; ближе к выбранному Sportage 17-18 |
| `profile` | `teyes_setting` | XP/Simple Sportage R 2019 LOW/MID/HIGH | `2097507 / 1900899 / 1966435` | `0x200163 / 0x1D0163 / 0x1E0163` | `candidate_profile` | Simple Soft альтернативный профиль Sportage; нужен live UART если захотим эмулировать Simple |
| `raise_uart` | `canbox_to_hu` | Двери/капот/багажник | `` | `FD 05 05 FLAGS 00 CS` | `confirmed` | bit0 LF, bit1 RF, bit2 LR, bit3 RR, bit4 trunk, bit5 hood |
| `raise_uart` | `canbox_to_hu` | Люк | `` | `FD 05 05 40 00 4A` | `candidate_live_failed_once` | Наш кандидат bit6; CAN люка найден, но TEYES/Raise профиль может его игнорировать |
| `raise_uart` | `canbox_to_hu` | Наружная температура | `U_EXIST_TEMP_OUT=1012` | `FD 04 01 TT CS` | `protocol_confirmed_source_pending` | Температура в Raise, bit7 обычно минус; точный CAN источник надо сверить с прошивкой прогера/live |
| `raise_uart` | `canbox_to_hu` | Климат popup | `U_AIR_POWER=10,U_AIR_AC=11,U_AIR_AUTO=13,U_AIR_DUAL=14` | `FD 08 03 L_TEMP R_TEMP FAN FLAGS CS` | `protocol_confirmed_source_pending` | Общий popup климата; отдельные TEYES slots есть, но в Raise наружу идет компактный payload |
| `raise_uart` | `canbox_to_hu` | Парктроники | `` | `FD 06 04 FRONT_PACKED REAR_PACKED CS` | `protocol_confirmed_live_pending` | 2-bit зоны перед/зад; нужны live кадры с препятствиями |
| `raise_uart` | `canbox_to_hu` | Кнопки руля/пианино | `` | `FD 06 02 KEY STATUS CS` | `protocol_confirmed_bridge_pending` | У нас основной путь: stock Raise canbox -> UART2 -> наш адаптер -> TEYES |
| `raise_uart` | `hu_to_canbox` | USB музыка | `` | `FD 0A 09 16 TRACK_H TRACK_L HOUR MIN SEC CS` | `confirmed_by_photo` | Магнитола сообщает canbox источник USB и прогресс; это потом конвертируется в M-CAN приборки |
| `raise_uart` | `hu_to_canbox` | BT музыка | `` | `FD 06 09 11 00 CS` | `protocol_confirmed_live_pending` | HU source Bluetooth music |
| `raise_uart` | `hu_to_canbox` | Навигация source | `` | `FD 06 09 06 00 CS` | `protocol_confirmed_live_pending` | HU сообщает navigation source; TBT/улицы идут отдельными M-CAN/firmware путями |
| `teyes_slot` | `mcu_to_ui` | Подогрев руля | `U_AIR_HOT_STEER=66` | `slot 0x42` | `slot_confirmed_uart_unknown` | Внутренний TEYES DataCanbus slot, НЕ готовый Raise UART пакет |
| `teyes_slot` | `mcu_to_ui` | Подогрев сиденья водитель/пассажир | `U_AIR_SEAT_HOT_LEFT=29,U_AIR_SEAT_HOT_RIGHT=30` | `slots 0x1D/0x1E` | `slot_confirmed_uart_unknown` | Внутренние slots TEYES; external Raise UART payload не найден в APK |
| `teyes_slot` | `mcu_to_ui` | Обдув сиденья водитель/пассажир | `U_AIR_SEAT_BLOW_LEFT=31,U_AIR_SEAT_BLOW_RIGHT=32` | `slots 0x1F/0x20` | `slot_confirmed_uart_unknown` | Внутренние slots TEYES; external Raise UART payload не найден в APK |
| `teyes_slot` | `mcu_to_ui` | Лобовой/задний обогрев | `U_AIR_FRONT_DEFROST=65,U_AIR_REAR_DEFROST=16` | `slots 0x41/0x10` | `slot_confirmed_uart_unknown` | Внутренние slots TEYES; искать CAN и/или Raise climate payload |
| `teyes_mcu_api` | `android_to_mcu` | CANBUS frame to MCU | `C_CANBUS_FRAME_TO_MCU=1008` | `cmd 0x3F0` | `api_confirmed` | Android/TEYES API для передачи CANBUS frame в MCU, не внешний UART canbox пакет |

## Как этим пользоваться завтра

1. Для Raise оставляем профиль Sportage 17-18/RZC и проверяем `FD` кадры из `raise_uart`.
2. Если хотим Simple Soft, выбираем XP/Simple Sportage LOW/MID/HIGH и снимаем отдельный live UART: байты будут не обязаны совпадать с Raise.
3. Для подогрева сидений/руля не отправлять `slot 0x1D/0x42` как UART напрямую: это внутренние индексы TEYES, а не пакет canbox.
4. В dashboard обучаем событие: CAN candidate -> optional UART RX/TX -> сохраняем bridge. После подтверждения переносим строку в `confirmed`.
5. Подтвержденные строки можно использовать как шумовой фильтр: известные скорость/обороты/температуры/двери не должны засорять кандидатов новых тестов.
