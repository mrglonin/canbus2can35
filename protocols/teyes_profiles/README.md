# TEYES CANBUS profile references

Выжимка из APK `CANBUS_12.07.2024`, `MS_12.07.2024`, `US_12.07.2024`,
`UPDATE_12.07.2024`.

Что здесь полезно:

- `profile_parameter_summary.csv` - короткий список профилей, близких к Kia/Hyundai Sportage.
- `apk_canbus_12072024/teyes_sportage_profile_command_map.csv` - слоты TEYES по профилям.
- `apk_canbus_12072024/teyes_uart_mcu_paths.csv` - где в APK виден MCU/JNI serial слой.

Эти файлы не являются готовой таблицей raw UART байтов canbox. Они нужны как
ориентир, какой профиль TEYES выбирать и где искать дальнейшую логику.
