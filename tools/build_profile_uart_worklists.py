#!/usr/bin/env python3
from __future__ import annotations

import csv
import sys
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
APK_DATA = DATA / "apk_canbus_12072024"

sys.path.insert(0, str(ROOT / "dashboard"))
import server  # noqa: E402


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open(newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def write_csv(path: Path, rows: list[dict[str, object]], fields: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def compact_ranges(values: list[int]) -> str:
    if not values:
        return ""
    values = sorted(set(values))
    ranges: list[tuple[int, int]] = []
    start = end = values[0]
    for value in values[1:]:
        if value == end + 1:
            end = value
        else:
            ranges.append((start, end))
            start = end = value
    ranges.append((start, end))
    return ", ".join(f"{a}" if a == b else f"{a}-{b}" for a, b in ranges)


def frame_cmd(protocol: str, frame: str) -> str:
    parts = frame.split()
    if protocol == "raise_rzc_fd" and len(parts) >= 3:
        return "0x" + parts[2]
    if protocol == "canbox_2e" and len(parts) >= 2:
        return "0x" + parts[1]
    return ""


def checksum_status(protocol: str, frame: str) -> str:
    parts = [int(x, 16) for x in frame.split()]
    if protocol == "raise_rzc_fd":
        if len(parts) < 5 or parts[0] != 0xFD:
            return "bad_format"
        length = parts[1]
        if len(parts) != length + 1:
            return "bad_length"
        expected = sum(parts[1:-2]) & 0xFFFF
        actual = (parts[-2] << 8) | parts[-1]
        return "ok" if expected == actual else f"bad_checksum_expected_{expected:04X}"
    if protocol == "canbox_2e":
        if len(parts) < 4 or parts[0] != 0x2E:
            return "bad_format"
        length = parts[2]
        if len(parts) != length + 4:
            return "bad_length"
        expected = ((parts[1] + parts[2] + sum(parts[3:-1])) & 0xFF) ^ 0xFF
        return "ok" if expected == parts[-1] else f"bad_checksum_expected_{expected:02X}"
    return ""


def build_profile_summary() -> list[dict[str, object]]:
    grouped: dict[str, dict[str, object]] = {}
    for row in read_csv(APK_DATA / "teyes_profile_uart_surfaces.csv"):
        if row.get("layer") != "teyes_profile_slot":
            continue
        key = row["profile"]
        item = grouped.setdefault(
            key,
            {
                "profile": key,
                "profile_id": row.get("profile_id", ""),
                "profile_hex": row.get("profile_hex", ""),
                "family": row.get("family", ""),
                "callback": row.get("callback", ""),
                "total_slots": 0,
                "named_slots": 0,
                "slots": [],
            },
        )
        item["total_slots"] = int(item["total_slots"]) + 1
        if row.get("status") == "slot_named":
            item["named_slots"] = int(item["named_slots"]) + 1
        try:
            item["slots"].append(int(row.get("slot_or_cmd", "")))
        except ValueError:
            pass

    rows: list[dict[str, object]] = []
    for item in grouped.values():
        profile = str(item["profile"])
        recommendation = ""
        if profile == "Raise/RZC Sportage R 2018":
            recommendation = "best_match_for_current_teyes_raise_kia_hyundai_sportage_17_18"
        elif profile == "Raise/RZC Sportage 2016":
            recommendation = "same_raise_surface_older_sportage_fallback"
        elif profile.startswith("XP/Simple"):
            recommendation = "simple_soft_candidate_requires_hu_profile_switch_and_live_uart"
        elif profile.startswith("Hiworld"):
            recommendation = "more_named_teyes_slots_but_different_canbox_family"
        rows.append(
            {
                "profile": profile,
                "profile_id": item["profile_id"],
                "profile_hex": item["profile_hex"],
                "family": item["family"],
                "callback": item["callback"],
                "total_slots": item["total_slots"],
                "named_slots": item["named_slots"],
                "slot_ranges": compact_ranges(item["slots"]),
                "recommendation": recommendation,
            }
        )
    return sorted(rows, key=lambda r: (-int(r["total_slots"]), -int(r["named_slots"]), str(r["profile"])))


def build_source_indexes() -> tuple[dict[tuple[str, str], list[str]], set[str], set[str]]:
    raise_cmd_sources: dict[tuple[str, str], list[str]] = defaultdict(list)
    for row in read_csv(DATA / "raise_rzc_korea_uart_matrix.csv"):
        cmd = row.get("cmd_hex", "")
        if cmd:
            raise_cmd_sources[(cmd.upper(), row.get("direction", ""))].append(row.get("name", "raise_matrix"))

    simple_cmds: set[str] = set()
    for row in read_csv(APK_DATA / "teyes_profile_uart_surfaces.csv"):
        if row.get("layer") == "external_canbox_uart_candidate" and row.get("wire_frame", "").startswith("2E"):
            parts = row["wire_frame"].split()
            if len(parts) > 1:
                simple_cmds.add(("0x" + parts[1]).upper())

    programmer_frames = {
        "FD 05 05 00 00 0A",
        "FD 05 05 01 00 0B",
        "FD 05 05 02 00 0C",
        "FD 05 05 04 00 0E",
        "FD 05 05 08 00 12",
        "FD 05 05 10 00 1A",
        "FD 05 05 20 00 2A",
        "FD 06 7D 06 02 00 8B",
        "FD 06 7D 06 00 00 89",
        "FD 0A 09 16 00 00 00 00 02 00 2B",
        "FD 07 06 2C 06 01 00 40",
        "FD 07 EE 20 01 00 01 16",
    }
    return raise_cmd_sources, simple_cmds, programmer_frames


def clean_worklists() -> tuple[list[dict[str, object]], list[dict[str, object]]]:
    raise_cmd_sources, simple_cmds, programmer_frames = build_source_indexes()
    raise_rows: list[dict[str, object]] = []
    simple_rows: list[dict[str, object]] = []
    for item in server.build_uart_candidate_tests():
        protocol = item.get("protocol", "")
        frame = item.get("frame", "")
        cmd = frame_cmd(protocol, frame)
        source_match: list[str] = []
        if protocol == "raise_rzc_fd":
            matrix_matches = raise_cmd_sources.get((cmd.upper(), item.get("direction", "")), [])
            if cmd.upper() == "0X05" and item.get("direction") == "canbox_to_hu":
                source_match.append("vehicle_sunroof_candidate" if "люк" in str(item.get("name", "")) else "vehicle_doors")
            else:
                source_match.extend(matrix_matches)
            if frame in programmer_frames:
                source_match.append("programmer_firmware_or_user_photo_match")
            dest = raise_rows
        elif protocol == "canbox_2e":
            if cmd.upper() in simple_cmds:
                source_match.append("teyes_simple_external_candidate")
            dest = simple_rows
        else:
            continue
        dest.append(
            {
                "id": item.get("id", ""),
                "category": item.get("group", ""),
                "protocol": protocol,
                "direction": item.get("direction", ""),
                "cmd": cmd,
                "name": item.get("name", ""),
                "frame": frame,
                "lab": item.get("lab", ""),
                "decoded": item.get("decoded", ""),
                "status": item.get("status", ""),
                "checksum": checksum_status(protocol, frame),
                "source_match": "; ".join(dict.fromkeys(source_match)),
            }
        )
    return raise_rows, simple_rows


def programmer_check_rows() -> list[dict[str, str]]:
    return [
        {
            "function": "LF door",
            "programmer_can_source": "C-CAN 0x541 DATA[1] bit0",
            "programmer_formula": "1=open",
            "raise_uart_candidate": "FD 05 05 01 00 0B",
            "result": "match",
        },
        {
            "function": "RF door",
            "programmer_can_source": "C-CAN 0x541 DATA[4] bit3",
            "programmer_formula": "1=open",
            "raise_uart_candidate": "FD 05 05 02 00 0C",
            "result": "match",
        },
        {
            "function": "LR door",
            "programmer_can_source": "C-CAN 0x553 DATA[3] bit0",
            "programmer_formula": "1=open",
            "raise_uart_candidate": "FD 05 05 04 00 0E",
            "result": "match",
        },
        {
            "function": "RR door",
            "programmer_can_source": "C-CAN 0x553 DATA[2] bit7",
            "programmer_formula": "1=open",
            "raise_uart_candidate": "FD 05 05 08 00 12",
            "result": "match",
        },
        {
            "function": "trunk",
            "programmer_can_source": "C-CAN 0x541 DATA[1] bit4",
            "programmer_formula": "1=open",
            "raise_uart_candidate": "FD 05 05 10 00 1A",
            "result": "match",
        },
        {
            "function": "hood",
            "programmer_can_source": "C-CAN 0x541 DATA[2] bit1",
            "programmer_formula": "1=open",
            "raise_uart_candidate": "FD 05 05 20 00 2A",
            "result": "match",
        },
        {
            "function": "all body closed",
            "programmer_can_source": "C-CAN 0x541/0x553",
            "programmer_formula": "all door/trunk/hood bits clear",
            "raise_uart_candidate": "FD 05 05 00 00 0A",
            "result": "match",
        },
        {
            "function": "reverse on",
            "programmer_can_source": "C-CAN 0x169 DATA[0] & 0x0F",
            "programmer_formula": "0x07=R",
            "raise_uart_candidate": "FD 06 7D 06 02 00 8B",
            "result": "match_local_extension",
        },
        {
            "function": "reverse off",
            "programmer_can_source": "C-CAN 0x169 DATA[0] & 0x0F",
            "programmer_formula": "!=0x07",
            "raise_uart_candidate": "FD 06 7D 06 00 00 89",
            "result": "match_local_extension",
        },
        {
            "function": "speed to HU",
            "programmer_can_source": "C-CAN 0x316 DATA[6]",
            "programmer_formula": "DATA[6] * 100 in outgoing programmer frame",
            "raise_uart_candidate": "",
            "result": "not_raise_uart_command",
        },
        {
            "function": "engine temp",
            "programmer_can_source": "C-CAN 0x329 DATA[1]",
            "programmer_formula": "(raw - 0x40) * 0.75",
            "raise_uart_candidate": "",
            "result": "confirmed_can_decoder_not_external_raise_uart",
        },
        {
            "function": "outside temp",
            "programmer_can_source": "C-CAN 0x044 DATA[3]",
            "programmer_formula": "(raw - 0x52) / 2",
            "raise_uart_candidate": "FD 04 01 TT CS_H CS_L",
            "result": "protocol_match_value_dynamic",
        },
        {
            "function": "sunroof",
            "programmer_can_source": "C-CAN 0x541 DATA[7] bit1",
            "programmer_formula": "1=open",
            "raise_uart_candidate": "FD 05 05 40 00 4A",
            "result": "can_candidate_known_uart_not_confirmed_by_teyes",
        },
        {
            "function": "steering wheel heat",
            "programmer_can_source": "C-CAN 0x559 DATA[0] bit4",
            "programmer_formula": "1=on",
            "raise_uart_candidate": "",
            "result": "can_candidate_requires_more_live_confirmation",
        },
        {
            "function": "USB media status",
            "programmer_can_source": "HU UART/photo/programmer APK media path",
            "programmer_formula": "source 0x16 + track/time fields",
            "raise_uart_candidate": "FD 0A 09 16 00 00 00 00 02 00 2B",
            "result": "match_user_photo",
        },
        {
            "function": "time sync",
            "programmer_can_source": "HU UART/photo",
            "programmer_formula": "minute hour mode",
            "raise_uart_candidate": "FD 07 06 2C 06 01 00 40",
            "result": "match_user_photo",
        },
    ]


def write_docs(profile_rows: list[dict[str, object]], raise_rows: list[dict[str, object]], simple_rows: list[dict[str, object]]) -> None:
    max_total = max((int(row["total_slots"]) for row in profile_rows), default=0)
    best_total_names = [str(row["profile"]) for row in profile_rows if int(row["total_slots"]) == max_total]
    best_named = sorted(profile_rows, key=lambda r: (-int(r["named_slots"]), -int(r["total_slots"]), str(r["profile"])))[0] if profile_rows else {}
    doc = f"""# Выбор CANBUS-профиля и UART-списки

## Что именно вытащено

Raise/RZC вынесен отдельно в `data/raise_uart_full_worklist.csv`.
Simple/XP/2E вынесен отдельно в `data/simple_soft_uart_full_worklist.csv`.

Это не один смешанный список. В рабочих CSV нет поля `note`: только категория, протокол, направление, команда, кадр, декодер, статус, контрольная сумма и совпадение с источником.

## Raise

Мы не вытаскивали «все Kia подряд». Вытянут рабочий слой для RZC/Raise Korea и Sportage-профилей из TEYES:

- `Raise/RZC Sportage R 2018`: profile_id `65898`, hex `0x01016A`, slots `149`, named `89`.
- `Raise/RZC Sportage 2016`: profile_id `1442154`, hex `0x16016A`, slots `149`, named `89`.

Для твоей машины сейчас самый близкий профиль: `Raise/RZC Sportage R 2018`, потому что он совпадает с выбором TEYES Raise Kia/Hyundai Sportage 17-18 и имеет тот же RZC/Raise callback-семейство.

## Simple Soft

Simple Soft вынесен отдельно как `XP/Simple`/`2E` кандидат. В APK Sportage-релевантные профили:

- `XP/Simple Sportage R 2019 LOW`: profile_id `2097507`, hex `0x200163`, slots `108`, named `99`.
- `XP/Simple Sportage R 2019 MID`: profile_id `1900899`, hex `0x1D0163`, slots `108`, named `99`.
- `XP/Simple Sportage R 2019 HIGH`: profile_id `1966435`, hex `0x1E0163`, slots `108`, named `99`.

Их стоит проверять только если на магнитоле выбран Simple Soft/XP-профиль. Для текущего Raise-профиля эти 2E-команды не являются подтвержденным проводным протоколом.

## У кого больше параметров

- Больше всего slot-поверхность: `{'; '.join(best_total_names) or '-'}` — `{max_total or '-'}` slots.
- Больше всего подписанных параметров: `{best_named.get('profile', '-')}` — `{best_named.get('named_slots', '-')}` named slots.

Практически для нас важнее не максимум строк, а совпадение семейства протокола. Поэтому текущий основной профиль: `Raise/RZC Sportage R 2018`.

## Сверка с прошивкой прогера

Сверка вынесена в `data/programmer_firmware_uart_check.csv`.
Кузовные события, задний ход и часть HU media/time совпадают с уже найденными Raise-кадрами. Температуры, скорость и часть климата в прошивке прогера идут как CAN-декодеры/внутренние CAN-сообщения, а не как готовая внешняя Raise UART-команда.
"""
    (ROOT / "docs" / "PROFILE_UART_SELECTION.md").write_text(doc, encoding="utf-8")

    check_rows = programmer_check_rows()
    lines = [
        "# Сверка UART-команд с прошивкой прогера",
        "",
        "| Функция | Источник в прошивке прогера | Формула | UART-кандидат | Результат |",
        "|---|---|---|---|---|",
    ]
    for row in check_rows:
        lines.append(
            f"| {row['function']} | `{row['programmer_can_source']}` | `{row['programmer_formula']}` | `{row['raise_uart_candidate']}` | {row['result']} |"
        )
    lines.extend(
        [
            "",
            "Главный вывод: двери/багажник/капот и задний ход совпадают с тем, что уже есть в наших Raise-кандидатах. Температуры и скорость надо держать как CAN-декодеры, а не искать для них внешний UART Raise-байт.",
        ]
    )
    (ROOT / "docs" / "PROGRAMMER_FIRMWARE_UART_CHECK.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    profile_rows = build_profile_summary()
    raise_rows, simple_rows = clean_worklists()
    check_rows = programmer_check_rows()

    write_csv(
        DATA / "profile_parameter_summary.csv",
        profile_rows,
        ["profile", "profile_id", "profile_hex", "family", "callback", "total_slots", "named_slots", "slot_ranges", "recommendation"],
    )
    fields = ["id", "category", "protocol", "direction", "cmd", "name", "frame", "lab", "decoded", "status", "checksum", "source_match"]
    write_csv(DATA / "raise_uart_full_worklist.csv", raise_rows, fields)
    write_csv(DATA / "simple_soft_uart_full_worklist.csv", simple_rows, fields)
    write_csv(
        DATA / "programmer_firmware_uart_check.csv",
        check_rows,
        ["function", "programmer_can_source", "programmer_formula", "raise_uart_candidate", "result"],
    )
    write_docs(profile_rows, raise_rows, simple_rows)
    print(f"profiles={len(profile_rows)} raise={len(raise_rows)} simple={len(simple_rows)} programmer_check={len(check_rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
