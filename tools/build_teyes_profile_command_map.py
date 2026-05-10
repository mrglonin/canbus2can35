#!/usr/bin/env python3
"""Build a focused TEYES/Raise/Simple command map for the Sportage project.

The APK constants describe TEYES internal CANBUS slots and profile IDs. They do
not fully expose external Raise/Simple UART bytes, so this report intentionally
keeps those layers separate.
"""

from __future__ import annotations

import csv
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "data" / "apk_canbus_12072024"
CSV_OUT = OUT_DIR / "teyes_sportage_profile_command_map.csv"
MD_OUT = ROOT / "docs" / "TEYES_SPORTAGE_PROFILE_COMMAND_MAP.md"


ROWS = [
    {
        "layer": "profile",
        "direction": "teyes_setting",
        "name": "RZC/Raise Sportage 2016",
        "teyes_id": "1442154",
        "hex_or_slot": "0x16016A",
        "meaning": "Кандидат Raise/RZC для Sportage 2016 в базе TEYES",
        "status": "candidate_profile",
        "source": "UPDATE/CANBUS FinalCanbus CAR_RZC3_XianDai_SPORTAGE_16",
    },
    {
        "layer": "profile",
        "direction": "teyes_setting",
        "name": "RZC/Raise Sportage R 2018",
        "teyes_id": "65898",
        "hex_or_slot": "0x01016A",
        "meaning": "Кандидат Raise/RZC Sportage R 2018; ближе к выбранному Sportage 17-18",
        "status": "candidate_profile",
        "source": "UPDATE canbus.db/canbus_hsp.db",
    },
    {
        "layer": "profile",
        "direction": "teyes_setting",
        "name": "XP/Simple Sportage R 2019 LOW/MID/HIGH",
        "teyes_id": "2097507 / 1900899 / 1966435",
        "hex_or_slot": "0x200163 / 0x1D0163 / 0x1E0163",
        "meaning": "Simple Soft альтернативный профиль Sportage; нужен live UART если захотим эмулировать Simple",
        "status": "candidate_profile",
        "source": "UPDATE/CANBUS FinalCanbus CAR_XP_19QiYa_Sportage_L/M/H",
    },
    {
        "layer": "raise_uart",
        "direction": "canbox_to_hu",
        "name": "Двери/капот/багажник",
        "teyes_id": "",
        "hex_or_slot": "FD 05 05 FLAGS 00 CS",
        "meaning": "bit0 LF, bit1 RF, bit2 LR, bit3 RR, bit4 trunk, bit5 hood",
        "status": "confirmed",
        "source": "RZC Korea public code + local live tests",
    },
    {
        "layer": "raise_uart",
        "direction": "canbox_to_hu",
        "name": "Люк",
        "teyes_id": "",
        "hex_or_slot": "FD 05 05 40 00 4A",
        "meaning": "Наш кандидат bit6; CAN люка найден, но TEYES/Raise профиль может его игнорировать",
        "status": "candidate_live_failed_once",
        "source": "local CAN 0x541 DATA[7]&0x02; public RZC Korea does not decode bit6",
    },
    {
        "layer": "raise_uart",
        "direction": "canbox_to_hu",
        "name": "Наружная температура",
        "teyes_id": "U_EXIST_TEMP_OUT=1012",
        "hex_or_slot": "FD 04 01 TT CS",
        "meaning": "Температура в Raise, bit7 обычно минус; точный CAN источник надо сверить с прошивкой прогера/live",
        "status": "protocol_confirmed_source_pending",
        "source": "RZC Korea public code; TEYES US FinalCanbus",
    },
    {
        "layer": "raise_uart",
        "direction": "canbox_to_hu",
        "name": "Климат popup",
        "teyes_id": "U_AIR_POWER=10,U_AIR_AC=11,U_AIR_AUTO=13,U_AIR_DUAL=14",
        "hex_or_slot": "FD 08 03 L_TEMP R_TEMP FAN FLAGS CS",
        "meaning": "Общий popup климата; отдельные TEYES slots есть, но в Raise наружу идет компактный payload",
        "status": "protocol_confirmed_source_pending",
        "source": "RZC Korea public code + CANBUS FinalCanbus",
    },
    {
        "layer": "raise_uart",
        "direction": "canbox_to_hu",
        "name": "Парктроники",
        "teyes_id": "",
        "hex_or_slot": "FD 06 04 FRONT_PACKED REAR_PACKED CS",
        "meaning": "2-bit зоны перед/зад; нужны live кадры с препятствиями",
        "status": "protocol_confirmed_live_pending",
        "source": "RZC Korea public code",
    },
    {
        "layer": "raise_uart",
        "direction": "canbox_to_hu",
        "name": "Кнопки руля/пианино",
        "teyes_id": "",
        "hex_or_slot": "FD 06 02 KEY STATUS CS",
        "meaning": "У нас основной путь: stock Raise canbox -> UART2 -> наш адаптер -> TEYES",
        "status": "protocol_confirmed_bridge_pending",
        "source": "RZC Korea public code",
    },
    {
        "layer": "raise_uart",
        "direction": "hu_to_canbox",
        "name": "USB музыка",
        "teyes_id": "",
        "hex_or_slot": "FD 0A 09 16 TRACK_H TRACK_L HOUR MIN SEC CS",
        "meaning": "Магнитола сообщает canbox источник USB и прогресс; это потом конвертируется в M-CAN приборки",
        "status": "confirmed_by_photo",
        "source": "RZC Korea public code + WR photo",
    },
    {
        "layer": "raise_uart",
        "direction": "hu_to_canbox",
        "name": "BT музыка",
        "teyes_id": "",
        "hex_or_slot": "FD 06 09 11 00 CS",
        "meaning": "HU source Bluetooth music",
        "status": "protocol_confirmed_live_pending",
        "source": "RZC Korea public code",
    },
    {
        "layer": "raise_uart",
        "direction": "hu_to_canbox",
        "name": "Навигация source",
        "teyes_id": "",
        "hex_or_slot": "FD 06 09 06 00 CS",
        "meaning": "HU сообщает navigation source; TBT/улицы идут отдельными M-CAN/firmware путями",
        "status": "protocol_confirmed_live_pending",
        "source": "RZC Korea public code",
    },
    {
        "layer": "teyes_slot",
        "direction": "mcu_to_ui",
        "name": "Подогрев руля",
        "teyes_id": "U_AIR_HOT_STEER=66",
        "hex_or_slot": "slot 0x42",
        "meaning": "Внутренний TEYES DataCanbus slot, НЕ готовый Raise UART пакет",
        "status": "slot_confirmed_uart_unknown",
        "source": "CANBUS FinalCanbus",
    },
    {
        "layer": "teyes_slot",
        "direction": "mcu_to_ui",
        "name": "Подогрев сиденья водитель/пассажир",
        "teyes_id": "U_AIR_SEAT_HOT_LEFT=29,U_AIR_SEAT_HOT_RIGHT=30",
        "hex_or_slot": "slots 0x1D/0x1E",
        "meaning": "Внутренние slots TEYES; external Raise UART payload не найден в APK",
        "status": "slot_confirmed_uart_unknown",
        "source": "CANBUS FinalCanbus",
    },
    {
        "layer": "teyes_slot",
        "direction": "mcu_to_ui",
        "name": "Обдув сиденья водитель/пассажир",
        "teyes_id": "U_AIR_SEAT_BLOW_LEFT=31,U_AIR_SEAT_BLOW_RIGHT=32",
        "hex_or_slot": "slots 0x1F/0x20",
        "meaning": "Внутренние slots TEYES; external Raise UART payload не найден в APK",
        "status": "slot_confirmed_uart_unknown",
        "source": "CANBUS FinalCanbus",
    },
    {
        "layer": "teyes_slot",
        "direction": "mcu_to_ui",
        "name": "Лобовой/задний обогрев",
        "teyes_id": "U_AIR_FRONT_DEFROST=65,U_AIR_REAR_DEFROST=16",
        "hex_or_slot": "slots 0x41/0x10",
        "meaning": "Внутренние slots TEYES; искать CAN и/или Raise climate payload",
        "status": "slot_confirmed_uart_unknown",
        "source": "CANBUS FinalCanbus",
    },
    {
        "layer": "teyes_mcu_api",
        "direction": "android_to_mcu",
        "name": "CANBUS frame to MCU",
        "teyes_id": "C_CANBUS_FRAME_TO_MCU=1008",
        "hex_or_slot": "cmd 0x3F0",
        "meaning": "Android/TEYES API для передачи CANBUS frame в MCU, не внешний UART canbox пакет",
        "status": "api_confirmed",
        "source": "US FinalCanbus",
    },
]


def write_csv() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    fields = ["layer", "direction", "name", "teyes_id", "hex_or_slot", "meaning", "status", "source"]
    with CSV_OUT.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(ROWS)


def write_md() -> None:
    MD_OUT.parent.mkdir(parents=True, exist_ok=True)
    with MD_OUT.open("w", encoding="utf-8") as handle:
        handle.write("# TEYES Sportage Profile / UART / Slot Map\n\n")
        handle.write("Этот файл отделяет три разных слоя, которые нельзя смешивать:\n\n")
        handle.write("- **profile**: ID профиля CANBUS в меню TEYES.\n")
        handle.write("- **raise_uart**: реальные `FD ...` кадры между canbox и магнитолой.\n")
        handle.write("- **teyes_slot**: внутренние `DataCanbus.DATA[]` индексы APK/MCU, не UART байты.\n")
        handle.write("- **teyes_mcu_api**: Android -> MCU команды внутри TEYES.\n\n")
        handle.write("CSV для импорта/фильтрации: `data/apk_canbus_12072024/teyes_sportage_profile_command_map.csv`.\n\n")
        handle.write("## Главный вывод\n\n")
        handle.write(
            "APK подтверждают профили, serial/JNI слой и внутренние slots для дверей, климата, подогревов, скорости и т.д. "
            "Но готовой таблицы `Raise/Simple UART byte -> функция` для всех функций внутри APK нет. "
            "Значит двери/медиа/базовый климат берем из подтвержденного Raise протокола, а подогревы/обдувы/режимы добиваем live UART/CAN обучением.\n\n"
        )
        handle.write("## Таблица\n\n")
        handle.write("| layer | direction | name | TEYES id/slot | bytes/slot | status | meaning |\n")
        handle.write("|---|---|---|---|---|---|---|\n")
        for row in ROWS:
            handle.write(
                f"| `{row['layer']}` | `{row['direction']}` | {row['name']} | `{row['teyes_id']}` | "
                f"`{row['hex_or_slot']}` | `{row['status']}` | {row['meaning']} |\n"
            )
        handle.write("\n## Как этим пользоваться завтра\n\n")
        handle.write("1. Для Raise оставляем профиль Sportage 17-18/RZC и проверяем `FD` кадры из `raise_uart`.\n")
        handle.write("2. Если хотим Simple Soft, выбираем XP/Simple Sportage LOW/MID/HIGH и снимаем отдельный live UART: байты будут не обязаны совпадать с Raise.\n")
        handle.write("3. Для подогрева сидений/руля не отправлять `slot 0x1D/0x42` как UART напрямую: это внутренние индексы TEYES, а не пакет canbox.\n")
        handle.write("4. В dashboard обучаем событие: CAN candidate -> optional UART RX/TX -> сохраняем bridge. После подтверждения переносим строку в `confirmed`.\n")
        handle.write("5. Подтвержденные строки можно использовать как шумовой фильтр: известные скорость/обороты/температуры/двери не должны засорять кандидатов новых тестов.\n")


def main() -> int:
    write_csv()
    write_md()
    print(f"wrote {CSV_OUT}")
    print(f"wrote {MD_OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
