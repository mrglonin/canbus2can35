# TEYES Profile UART Surfaces

Задача: получить рабочую карту профилей без гадания. Файл показывает, что реально извлечено из APK/исходников, а что требует live UART.

## Важное разделение

- `teyes_profile_slot` - внутренние slots `DataCanbus.DATA[]`, которые выбранный профиль регистрирует в TEYES. Это говорит, какие функции профиль поддерживает.
- `raise_rzc_korea_uart` - реальные внешние UART `FD ...` кадры Raise/RZC Korea, которые можно отправлять/читать mode3.
- `external_canbox_uart_candidate` - другой внешний canbox-протокол `2E ...` из SmartGauges. Он полезен для Simple/других профилей как кандидат, но для Sportage/TEYES не подтвержден без live UART.

CSV: `data/apk_canbus_12072024/teyes_profile_uart_surfaces.csv`.

## Профили

| profile | id | family | callback | registered slots |
|---|---:|---|---|---|
| Raise/RZC Sportage 2016 | `1442154` | RZC/Raise | `Callback_0362_RZC3_16_QiYaK5` | 0-5 door, 10-92 air, 500-559 vendor/service |
| Raise/RZC Sportage R 2018 | `65898` | RZC/Raise | `Callback_0362_RZC3_16_QiYaK5` | 0-5 door, 10-92 air, 500-559 vendor/service |
| XP/Simple Sportage R 2019 LOW | `2097507` | XP/Simple | `Callback_0355_XP_QiYaK5` | 0-5 door, 10-92 air, 93-111 xp/extra |
| XP/Simple Sportage R 2019 MID | `1900899` | XP/Simple | `Callback_0355_XP_QiYaK5` | 0-5 door, 10-92 air, 93-111 xp/extra |
| XP/Simple Sportage R 2019 HIGH | `1966435` | XP/Simple | `Callback_0355_XP_QiYaK5` | 0-5 door, 10-92 air, 93-111 xp/extra |
| Hiworld/WC2 Sportage R 2018 | `3408315` | Hiworld/WC2 | `Callback_0443_WC2_Xiandai_All` | 0-5 door, 10-92 air, 93-117 wc2/extra |

## Подтвержденные/кандидатные внешние UART команды

| layer | direction | cmd | name | wire frame | status |
|---|---|---|---|---|---|
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x01` | external_temperature | `FD LL 0x01 TT CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x02` | steering_panel_key | `FD LL 0x02 KEY STATUS CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x03` | air_conditioning | `FD LL 0x03 L_TEMP R_TEMP FAN FLAGS CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x04` | radar | `FD LL 0x04 FRONT_PACKED REAR_PACKED CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x05` | vehicle_doors | `FD LL 0x05 FLAGS CS16` | `public_and_local_confirmed` |
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x05` | vehicle_sunroof_candidate | `FD LL 0x05 FLAGS bit6 CS16` | `local_experiment` |
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x06` | ac_climate_status | `FD LL 0x06 STATUS CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x07` | backlight | `FD LL 0x07 LEVEL CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x7F` | protocol_version | `FD LL 0x7F ASCII_BYTES CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `canbox_to_hu` | `0x7D` | reverse_local | `FD LL 0x7D 06 00/06 02 CS16` | `local_confirmed_not_public` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x03` | setting | `FD LL 0x03 UNKNOWN CS16` | `public_named_unknown_payload` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x04` | power | `FD LL 0x04 00/01 CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x05` | volume_adjust | `FD LL 0x05 VOL CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x06` | time_info | `FD LL 0x06 MIN HOUR MODE CS16` | `public_and_local_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x07` | audio_balance | `FD LL 0x07 FADE+7 BALANCE+7 CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x08` | sound_effects | `FD LL 0x08 BASS+10 MID+10 TREBLE+10 CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x09` | host_status_radio | `FD LL 0x09 02 BAND FREQ_H FREQ_L CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x09` | host_status_usb | `FD LL 0x09 16 TRACK_H TRACK_L HOUR MIN SEC CS16` | `public_and_local_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x09` | host_status_other | `FD LL 0x09 83 00 CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x09` | host_status_aux | `FD LL 0x09 12 00 CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x09` | host_status_off | `FD LL 0x09 80 00 CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x09` | host_status_navigation | `FD LL 0x09 06 00 CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x09` | host_status_bt_music | `FD LL 0x09 11 00 CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x09` | host_status_bt_phone | `FD LL 0x09 07 STATUS CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `hu_to_canbox` | `0x09` | host_status_bt_connection | `FD LL 0x09 0B 04/05 CS16` | `public_confirmed` |
| `raise_rzc_korea_uart` | `unknown` | `0xEE` | valid_unknown | `FD LL 0xEE 20 01 00 CS16` | `needs_live_capture` |
| `external_canbox_uart_candidate` | `canbox_to_hu` | `0x20` | keys | `2E 20 02 KEY STATUS CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |
| `external_canbox_uart_candidate` | `canbox_to_hu` | `0x21` | ac | `2E 21 LEN DATA CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |
| `external_canbox_uart_candidate` | `canbox_to_hu` | `0x22` | rear_radar | `2E 22 04 RR RRM RLM RL CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |
| `external_canbox_uart_candidate` | `canbox_to_hu` | `0x23` | front_radar | `2E 23 04 FR FRM FLM FL CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |
| `external_canbox_uart_candidate` | `canbox_to_hu` | `0x24` | vehicle_state | `2E 24 LEN DATA CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |
| `external_canbox_uart_candidate` | `canbox_to_hu` | `0x25` | parking_on | `2E 25 01 STATE CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |
| `external_canbox_uart_candidate` | `hu_to_canbox` | `0x81` | start_stop | `2E 81 LEN DATA CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |
| `external_canbox_uart_candidate` | `hu_to_canbox` | `0x90` | request_id | `2E 90 LEN DATA CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |
| `external_canbox_uart_candidate` | `hu_to_canbox` | `0xA0` | amplifier | `2E A0 LEN DATA CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |
| `external_canbox_uart_candidate` | `hu_to_canbox` | `0xA6` | time | `2E A6 LEN DATA CRC8_XORFF` | `candidate_protocol_not_confirmed_for_teyes_sportage` |

## Что это дает для завтрашнего теста

1. Для Raise/RZC сначала проверяем все строки `raise_rzc_korea_uart`: двери, температура, климат, парктроники, кнопки, media source.
2. Для Simple/XP выбираем профиль `XP/Simple Sportage R 2019 LOW/MID/HIGH` и снимаем live UART. APK показывает, что профиль поддерживает двери и климат slots, но не раскрывает внешний UART payload.
3. Если после переключения на Simple в UART появятся кадры `2E ...`, dashboard уже декодирует их отдельным `canbox_2e` parser: команда, payload, CRC и тип (`key`, `climate`, `radar`, `vehicle_state`, `hu_power`).
4. Таблица slots нужна как фильтр и чеклист: если slot есть в профиле, функция ожидается TEYES; если внешнего UART нет, добиваем live capture.
5. Полный список без live UART невозможен только из APK: serial/JNI слой передает байты, но внешний payload часто живет в MCU/canbox firmware, а не в Java/DB.

## Практический механизм получения списка

1. Выбрать в TEYES профиль Raise/RZC, XP/Simple или Hiworld/WC2 из таблицы выше.
2. В dashboard включить `Live CAN+UART`.
3. Сначала 10 секунд покоя: это база шума.
4. Нажимать одну функцию за раз: дверь, климат, подогрев, источник, кнопку.
5. Dashboard раскладывает UART:
   - `FD ...` как `raise_rzc_fd`;
   - `2E ...` как `canbox_2e`;
   - неизвестный prefix сохраняется raw, чтобы добавить третий parser.
6. После подтверждения сохранять связку `CAN событие -> UART frame` в обучении. Так получается таблица, которую можно передать программисту или перенести в APK.

Источник `2E`-семейства: [smartgauges/canbox](https://github.com/smartgauges/canbox). Это не доказательство, что XP/Simple Sportage точно использует `2E`, но это реальный открытый canbox UART-протокол и теперь он проверяется live, а не вручную по фото.
