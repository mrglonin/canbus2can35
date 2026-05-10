#!/usr/bin/env python3
"""Build profile-oriented command surfaces from TEYES APK reverse artifacts.

This deliberately separates:
- TEYES profile IDs and DataCanbus slots from APK callbacks;
- external UART protocol frames already known for Raise/RZC;
- candidate/non-Korea 0x2E canbox protocol from SmartGauges sources.

The goal is to avoid guessing: every row has a source and status.
"""

from __future__ import annotations

import csv
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "apk_canbus_12072024"
STATIC_CONSTANTS = DATA / "teyes_static_constants_new.csv"
RAISE_MATRIX = ROOT / "data" / "raise_rzc_korea_uart_matrix.csv"
CSV_OUT = DATA / "teyes_profile_uart_surfaces.csv"
MD_OUT = ROOT / "docs" / "TEYES_PROFILE_UART_SURFACES.md"


PROFILES = [
    {
        "profile": "Raise/RZC Sportage 2016",
        "profile_id": "1442154",
        "hex": "0x16016A",
        "family": "RZC/Raise",
        "callback": "Callback_0362_RZC3_16_QiYaK5",
        "source": "CANBUS FinalCanbus CAR_RZC3_XianDai_SPORTAGE_16; handler low16=362",
        "ranges": [(0, 5, "door"), (10, 92, "air"), (500, 559, "vendor/service")],
    },
    {
        "profile": "Raise/RZC Sportage R 2018",
        "profile_id": "65898",
        "hex": "0x01016A",
        "family": "RZC/Raise",
        "callback": "Callback_0362_RZC3_16_QiYaK5",
        "source": "UPDATE canbus.db Sportage R 2018; same low16=362 family",
        "ranges": [(0, 5, "door"), (10, 92, "air"), (500, 559, "vendor/service")],
    },
    {
        "profile": "XP/Simple Sportage R 2019 LOW",
        "profile_id": "2097507",
        "hex": "0x200163",
        "family": "XP/Simple",
        "callback": "Callback_0355_XP_QiYaK5",
        "source": "CANBUS FinalCanbus CAR_XP_19QiYa_Sportage_L; handler low16=355",
        "ranges": [(0, 5, "door"), (10, 92, "air"), (93, 111, "xp/extra")],
    },
    {
        "profile": "XP/Simple Sportage R 2019 MID",
        "profile_id": "1900899",
        "hex": "0x1D0163",
        "family": "XP/Simple",
        "callback": "Callback_0355_XP_QiYaK5",
        "source": "CANBUS FinalCanbus CAR_XP_19QiYa_Sportage_M; handler low16=355",
        "ranges": [(0, 5, "door"), (10, 92, "air"), (93, 111, "xp/extra")],
    },
    {
        "profile": "XP/Simple Sportage R 2019 HIGH",
        "profile_id": "1966435",
        "hex": "0x1E0163",
        "family": "XP/Simple",
        "callback": "Callback_0355_XP_QiYaK5",
        "source": "CANBUS FinalCanbus CAR_XP_19QiYa_Sportage_H; handler low16=355",
        "ranges": [(0, 5, "door"), (10, 92, "air"), (93, 111, "xp/extra")],
    },
    {
        "profile": "Hiworld/WC2 Sportage R 2018",
        "profile_id": "3408315",
        "hex": "0x3401BB",
        "family": "Hiworld/WC2",
        "callback": "Callback_0443_WC2_Xiandai_All",
        "source": "CANBUS FinalCanbus CAR_443_WC2_XianDai_All_18Sportage; handler low16=443",
        "ranges": [(0, 5, "door"), (10, 92, "air"), (93, 117, "wc2/extra")],
    },
]


SLOT_FALLBACKS = {
    0: "U_DOOR_ENGINE",
    1: "U_DOOR_FL",
    2: "U_DOOR_FR",
    3: "U_DOOR_RL",
    4: "U_DOOR_RR",
    5: "U_DOOR_BACK",
}


def load_slots() -> dict[int, list[str]]:
    slots: dict[int, list[str]] = {}
    if STATIC_CONSTANTS.exists():
        with STATIC_CONSTANTS.open(encoding="utf-8") as handle:
            for row in csv.DictReader(handle):
                name = row["constant"]
                if not name.startswith("U_"):
                    continue
                try:
                    value = int(row["value"])
                except ValueError:
                    continue
                slots.setdefault(value, []).append(name)
    for value, name in SLOT_FALLBACKS.items():
        slots.setdefault(value, []).insert(0, name)
    return slots


def iter_profile_rows() -> list[dict[str, str]]:
    slots = load_slots()
    rows: list[dict[str, str]] = []
    for profile in PROFILES:
        for start, end, group in profile["ranges"]:
            for slot in range(start, end + 1):
                names = slots.get(slot, [])
                status = "slot_named" if names else "slot_registered_unknown_name"
                if group == "vendor/service":
                    status = "registered_vendor_unknown"
                elif group.endswith("/extra") and not names:
                    status = "registered_extra_unknown"
                rows.append(
                    {
                        "layer": "teyes_profile_slot",
                        "profile": profile["profile"],
                        "profile_id": profile["profile_id"],
                        "profile_hex": profile["hex"],
                        "family": profile["family"],
                        "callback": profile["callback"],
                        "direction": "mcu_to_android_ui",
                        "slot_or_cmd": str(slot),
                        "name": " / ".join(names) if names else f"{group} slot {slot}",
                        "wire_frame": "",
                        "status": status,
                        "source": profile["source"],
                        "note": "DataCanbus.DATA slot from APK callback; not an external UART byte by itself",
                    }
                )
    return rows


def iter_raise_rows() -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    if not RAISE_MATRIX.exists():
        return rows
    with RAISE_MATRIX.open(encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            rows.append(
                {
                    "layer": "raise_rzc_korea_uart",
                    "profile": "Raise/RZC Korea family",
                    "profile_id": "",
                    "profile_hex": "",
                    "family": "RZC/Raise",
                    "callback": "",
                    "direction": row["direction"],
                    "slot_or_cmd": row["cmd_hex"],
                    "name": row["name"],
                    "wire_frame": f"FD LL {row['cmd_hex']} {row['payload']} CS16",
                    "status": row["status"],
                    "source": row["source"],
                    "note": row["meaning"],
                }
            )
    return rows


def iter_protocol_candidate_rows() -> list[dict[str, str]]:
    # SmartGauges canbox source documents a 0x2E-family frame used by some
    # Raise/Oudi/Hiworld profiles. It is useful as a candidate when switching
    # away from Korea RZC FD protocol, but it is not confirmed for this Sportage.
    base = {
        "layer": "external_canbox_uart_candidate",
        "profile": "Non-Korea 0x2E canbox family",
        "profile_id": "",
        "profile_hex": "",
        "family": "Raise/Oudi/Hiworld candidate, not confirmed Simple",
        "callback": "",
        "source": "external/smartgauges_canbox/canbox.c",
        "status": "candidate_protocol_not_confirmed_for_teyes_sportage",
    }
    commands = [
        ("canbox_to_hu", "0x20", "keys", "2E 20 02 KEY STATUS CRC8_XORFF", "button/key events in SmartGauges VW profile"),
        ("canbox_to_hu", "0x21", "ac", "2E 21 LEN DATA CRC8_XORFF", "AC/climate in SmartGauges VW profile"),
        ("canbox_to_hu", "0x22", "rear_radar", "2E 22 04 RR RRM RLM RL CRC8_XORFF", "rear radar zones"),
        ("canbox_to_hu", "0x23", "front_radar", "2E 23 04 FR FRM FLM FL CRC8_XORFF", "front radar zones"),
        ("canbox_to_hu", "0x24", "vehicle_state", "2E 24 LEN DATA CRC8_XORFF", "reverse/park/light state depending profile"),
        ("canbox_to_hu", "0x25", "parking_on", "2E 25 01 STATE CRC8_XORFF", "parking UI state"),
        ("hu_to_canbox", "0x81", "start_stop", "2E 81 LEN DATA CRC8_XORFF", "start/stop communication"),
        ("hu_to_canbox", "0x90", "request_id", "2E 90 LEN DATA CRC8_XORFF", "request canbox ID"),
        ("hu_to_canbox", "0xA0", "amplifier", "2E A0 LEN DATA CRC8_XORFF", "amplifier command"),
        ("hu_to_canbox", "0xA6", "time", "2E A6 LEN DATA CRC8_XORFF", "set time"),
    ]
    return [
        {
            **base,
            "direction": direction,
            "slot_or_cmd": cmd,
            "name": name,
            "wire_frame": frame,
            "note": note,
        }
        for direction, cmd, name, frame, note in commands
    ]


def rows() -> list[dict[str, str]]:
    return iter_profile_rows() + iter_raise_rows() + iter_protocol_candidate_rows()


def write_csv(all_rows: list[dict[str, str]]) -> None:
    DATA.mkdir(parents=True, exist_ok=True)
    fields = [
        "layer",
        "profile",
        "profile_id",
        "profile_hex",
        "family",
        "callback",
        "direction",
        "slot_or_cmd",
        "name",
        "wire_frame",
        "status",
        "source",
        "note",
    ]
    with CSV_OUT.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(all_rows)


def write_md(all_rows: list[dict[str, str]]) -> None:
    MD_OUT.parent.mkdir(parents=True, exist_ok=True)
    with MD_OUT.open("w", encoding="utf-8") as handle:
        handle.write("# TEYES Profile UART Surfaces\n\n")
        handle.write("Задача: получить рабочую карту профилей без гадания. Файл показывает, что реально извлечено из APK/исходников, а что требует live UART.\n\n")
        handle.write("## Важное разделение\n\n")
        handle.write("- `teyes_profile_slot` - внутренние slots `DataCanbus.DATA[]`, которые выбранный профиль регистрирует в TEYES. Это говорит, какие функции профиль поддерживает.\n")
        handle.write("- `raise_rzc_korea_uart` - реальные внешние UART `FD ...` кадры Raise/RZC Korea, которые можно отправлять/читать mode3.\n")
        handle.write("- `external_canbox_uart_candidate` - другой внешний canbox-протокол `2E ...` из SmartGauges. Он полезен для Simple/других профилей как кандидат, но для Sportage/TEYES не подтвержден без live UART.\n\n")
        handle.write("CSV: `data/apk_canbus_12072024/teyes_profile_uart_surfaces.csv`.\n\n")
        handle.write("## Профили\n\n")
        handle.write("| profile | id | family | callback | registered slots |\n")
        handle.write("|---|---:|---|---|---|\n")
        for profile in PROFILES:
            ranges = ", ".join(f"{a}-{b} {name}" for a, b, name in profile["ranges"])
            handle.write(f"| {profile['profile']} | `{profile['profile_id']}` | {profile['family']} | `{profile['callback']}` | {ranges} |\n")
        handle.write("\n## Подтвержденные/кандидатные внешние UART команды\n\n")
        handle.write("| layer | direction | cmd | name | wire frame | status |\n")
        handle.write("|---|---|---|---|---|---|\n")
        for row in all_rows:
            if row["layer"] == "teyes_profile_slot":
                continue
            handle.write(
                f"| `{row['layer']}` | `{row['direction']}` | `{row['slot_or_cmd']}` | {row['name']} | "
                f"`{row['wire_frame']}` | `{row['status']}` |\n"
            )
        handle.write("\n## Что это дает для завтрашнего теста\n\n")
        handle.write("1. Для Raise/RZC сначала проверяем все строки `raise_rzc_korea_uart`: двери, температура, климат, парктроники, кнопки, media source.\n")
        handle.write("2. Для Simple/XP выбираем профиль `XP/Simple Sportage R 2019 LOW/MID/HIGH` и снимаем live UART. APK показывает, что профиль поддерживает двери и климат slots, но не раскрывает внешний UART payload.\n")
        handle.write("3. Если после переключения на Simple в UART появятся кадры `2E ...`, dashboard уже декодирует их отдельным `canbox_2e` parser: команда, payload, CRC и тип (`key`, `climate`, `radar`, `vehicle_state`, `hu_power`).\n")
        handle.write("4. Таблица slots нужна как фильтр и чеклист: если slot есть в профиле, функция ожидается TEYES; если внешнего UART нет, добиваем live capture.\n")
        handle.write("5. Полный список без live UART невозможен только из APK: serial/JNI слой передает байты, но внешний payload часто живет в MCU/canbox firmware, а не в Java/DB.\n")
        handle.write("\n## Практический механизм получения списка\n\n")
        handle.write("1. Выбрать в TEYES профиль Raise/RZC, XP/Simple или Hiworld/WC2 из таблицы выше.\n")
        handle.write("2. В dashboard включить `Live CAN+UART`.\n")
        handle.write("3. Сначала 10 секунд покоя: это база шума.\n")
        handle.write("4. Нажимать одну функцию за раз: дверь, климат, подогрев, источник, кнопку.\n")
        handle.write("5. Dashboard раскладывает UART:\n")
        handle.write("   - `FD ...` как `raise_rzc_fd`;\n")
        handle.write("   - `2E ...` как `canbox_2e`;\n")
        handle.write("   - неизвестный prefix сохраняется raw, чтобы добавить третий parser.\n")
        handle.write("6. После подтверждения сохранять связку `CAN событие -> UART frame` в обучении. Так получается таблица, которую можно передать программисту или перенести в APK.\n\n")
        handle.write("Источник `2E`-семейства: [smartgauges/canbox](https://github.com/smartgauges/canbox). Это не доказательство, что XP/Simple Sportage точно использует `2E`, но это реальный открытый canbox UART-протокол и теперь он проверяется live, а не вручную по фото.\n")


def main() -> int:
    all_rows = rows()
    write_csv(all_rows)
    write_md(all_rows)
    print(f"wrote {len(all_rows)} rows to {CSV_OUT}")
    print(f"wrote {MD_OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
