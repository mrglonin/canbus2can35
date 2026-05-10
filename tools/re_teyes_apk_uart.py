#!/usr/bin/env python3
"""Extract TEYES/SYU APK CANBUS and UART clues from DEX files.

The goal is not to decompile the whole APK into Java. We need a repeatable
reverse-engineering report that shows where Android talks to MCU/CANBUS and
which methods contain serial/CANBUS command plumbing.
"""

from __future__ import annotations

import argparse
import csv
import re
import textwrap
from dataclasses import dataclass
from pathlib import Path
from zipfile import ZipFile

from loguru import logger

logger.disable("androguard")

from androguard.core.dex import DEX  # noqa: E402


DEFAULT_APKS = [
    Path("/Users/legion/Downloads/CANBUS_12.07.2024.apk"),
    Path("/Users/legion/Downloads/MS_12.07.2024.apk"),
    Path("/Users/legion/Downloads/US_12.07.2024.apk"),
    Path("/Users/legion/Downloads/UPDATE_12.07.2024.apk"),
]

TERMS = [
    "/dev/goc_serial",
    "/dev/BT_serial",
    "JniSerial",
    "SerialNative",
    "SyuJniNative",
    "ToolsJni",
    "cmd_149_write_data",
    "CANBUS DEV PATH",
    "Send to Mcu Data",
    "MCU Canbus ID",
    "Canbus ID",
    "DataCanbus",
    "FinalCanbus",
    "FinalCanbusID",
    "G_MCU_CANBUS_SUPPORT",
    "C_CANBUS_FRAME_TO_MCU",
    "MODULE_KEY_TO_MCU",
    "handleCanbusSendTouch",
    "ARM_UI_SETCANBUS_ID",
    "ARM_MCU_UPDATE_FLAG",
    "ACTION_CANBUSID_CHANGE",
    "Raise",
    "RZC",
    "ADJUST_RAISE",
    "Simple",
    "XP",
    "Hiworld",
    "KIA",
    "QiYa",
    "Hyundai",
    "Xiandai",
    "Sportage",
    "KX5",
    "Zhipao",
    "Tusheng",
    "Sonata",
    "SoNaTa",
    "K5",
]

TARGET_CLASS_RE = re.compile(
    r"(com/syu/jni|com/lsec/core/ipc/module/main/Canbus|com/lsec/core/util/data/FinalCanbus|"
    r"com/syu/data/FinalCanbus|com/syu/module/canbus|com/syu/canbus|"
    r"Hiworld|Canbus|DataCanbus|FinalCanbus|Serial|Jni|Syu|ToolsJni|Lc/d;)"
)


@dataclass
class DexResult:
    apk: Path
    dex_name: str
    dex_index: int
    class_count: int
    string_count: int
    interesting_strings: list[str]
    interesting_classes: list[str]
    method_hit_count: int


def clean_filename(value: str, limit: int = 150) -> str:
    value = value.strip("L;").replace("/", "_").replace("$", "_")
    value = re.sub(r"[^A-Za-z0-9_.-]+", "_", value)
    return value[:limit] or "item"


def term_hits(text: str) -> list[str]:
    lower = text.lower()
    return [term for term in TERMS if term.lower() in lower]


def iter_dex_payloads(apk_path: Path):
    with ZipFile(apk_path) as zf:
        for index, name in enumerate(sorted(n for n in zf.namelist() if re.fullmatch(r"classes\d*\.dex", n))):
            yield index, name, zf.read(name)


def instruction_lines(method) -> list[str]:
    code = method.get_code()
    if not code:
        return []
    lines: list[str] = []
    offset = 0
    for ins in code.get_bc().get_instructions():
        lines.append(f"{offset:04x}: {ins.get_name():24s} {ins.get_output()}")
        try:
            offset += ins.get_length()
        except Exception:
            offset += 0
    return lines


def method_signature(class_name: str, method) -> str:
    return f"{class_name}->{method.get_name()}{method.get_descriptor()}"


def write_method_dump(path: Path, apk: Path, dex_name: str, class_name: str, method, lines: list[str], hits: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        handle.write(f"APK: {apk}\n")
        handle.write(f"DEX: {dex_name}\n")
        handle.write(f"METHOD: {method_signature(class_name, method)}\n")
        handle.write(f"HITS: {', '.join(hits)}\n\n")
        handle.write("\n".join(lines))
        handle.write("\n")


def analyze_dex(apk: Path, dex_index: int, dex_name: str, payload: bytes, out_dir: Path) -> DexResult:
    dex = DEX(payload)
    strings = sorted({str(value) for value in dex.get_strings()})
    interesting_strings = [value for value in strings if term_hits(value)]

    classes = list(dex.get_classes())
    interesting_classes = sorted(
        c.get_name()
        for c in classes
        if TARGET_CLASS_RE.search(c.get_name()) or term_hits(c.get_name())
    )

    apk_tag = apk.stem.replace(".", "_")
    method_dir = out_dir / "methods" / apk_tag / dex_name.replace(".", "_")
    method_index_rows: list[dict[str, str]] = []
    dumped = 0

    for cls in classes:
        class_name = cls.get_name()
        class_is_target = class_name in interesting_classes
        for method in cls.get_methods():
            lines = instruction_lines(method)
            if not lines and not class_is_target:
                continue
            joined = "\n".join(lines)
            hits = sorted(set(term_hits(class_name) + term_hits(method.get_name()) + term_hits(joined)))
            if not hits and not class_is_target:
                continue

            sig = method_signature(class_name, method)
            filename = (
                f"{dumped:04d}_"
                f"{clean_filename(class_name, 90)}__{clean_filename(method.get_name(), 45)}.txt"
            )
            dump_path = method_dir / filename
            write_method_dump(dump_path, apk, dex_name, class_name, method, lines, hits or ["target_class"])
            method_index_rows.append(
                {
                    "apk": apk.name,
                    "dex": dex_name,
                    "class": class_name,
                    "method": method.get_name(),
                    "descriptor": str(method.get_descriptor()),
                    "hits": ";".join(hits or ["target_class"]),
                    "dump": str(dump_path.relative_to(out_dir)),
                }
            )
            dumped += 1

    with (out_dir / f"{apk_tag}_{dex_name}_interesting_strings.txt").open("w", encoding="utf-8") as handle:
        handle.write("\n".join(interesting_strings))
        handle.write("\n")

    with (out_dir / f"{apk_tag}_{dex_name}_interesting_classes.txt").open("w", encoding="utf-8") as handle:
        handle.write("\n".join(interesting_classes))
        handle.write("\n")

    if method_index_rows:
        with (out_dir / f"{apk_tag}_{dex_name}_method_index.csv").open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(
                handle,
                fieldnames=["apk", "dex", "class", "method", "descriptor", "hits", "dump"],
            )
            writer.writeheader()
            writer.writerows(method_index_rows)

    return DexResult(
        apk=apk,
        dex_name=dex_name,
        dex_index=dex_index,
        class_count=len(classes),
        string_count=len(strings),
        interesting_strings=interesting_strings,
        interesting_classes=interesting_classes,
        method_hit_count=dumped,
    )


def write_summary(out_dir: Path, results: list[DexResult]) -> None:
    summary = out_dir / "TEYES_12072024_UART_RE_REPORT.md"
    with summary.open("w", encoding="utf-8") as handle:
        handle.write("# TEYES 12.07.2024 APK UART/CANBUS reverse report\n\n")
        handle.write("Задача: найти слой, через который Android-приложения TEYES/SYU общаются с MCU/CANBUS, и отделить его от баз профилей.\n\n")
        handle.write("## APK/DEX inventory\n\n")
        handle.write("| APK | DEX | classes | strings | interesting strings | method dumps |\n")
        handle.write("|---|---|---:|---:|---:|---:|\n")
        for result in results:
            handle.write(
                f"| `{result.apk.name}` | `{result.dex_name}` | {result.class_count} | {result.string_count} | "
                f"{len(result.interesting_strings)} | {result.method_hit_count} |\n"
            )
        handle.write("\n")

        handle.write("## Main findings so far\n\n")
        handle.write(
            "- `UPDATE_12.07.2024.apk` is treated as profile/database layer: CANBUS profile IDs and names, not raw UART packet logic.\n"
        )
        handle.write(
            "- `MS_12.07.2024.apk` contains the serial transport layer: `JniSerial`, `SerialNative`, `/dev/goc_serial`, and wrapper calls that open/setup/read/write bytes.\n"
        )
        handle.write(
            "- `US_12.07.2024.apk` contains MCU/CANBUS command routing names such as `C_CANBUS_FRAME_TO_MCU`, `MODULE_KEY_TO_MCU`, and `handleCanbusSendTouch`.\n"
        )
        handle.write(
            "- `CANBUS_12.07.2024.apk` contains the large canbus UI/profile/data layer: `DataCanbus`, Kia/Hyundai/RZC/Hiworld class and string references.\n"
        )
        handle.write(
            "- По текущим признакам APK чаще описывают Android->MCU протокол, а не напрямую внешний Raise UART canbox. Это все равно важно: мы можем понять, какие ID/события магнитола ожидает от MCU/CANBUS слоя.\n\n"
        )

        handle.write("## Files to inspect next\n\n")
        handle.write("- `*_method_index.csv`: индекс методов и путь к дампу инструкций.\n")
        handle.write("- `methods/MS_12_07_2024/classes_dex/*JniSerial*`, `*SerialNative*`, `*c_d*`: serial open/read/write.\n")
        handle.write("- `methods/US_12_07_2024/classes_dex/*Canbus*`: MCU/CANBUS frame routing.\n")
        handle.write("- `methods/CANBUS_12_07_2024/classes_dex/*DataCanbus*`, `*RZC*`, `*QiYa*`: профильные события и отображение.\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out-dir", type=Path, default=Path("re/teyes_12072024"))
    parser.add_argument("apks", nargs="*", type=Path, default=DEFAULT_APKS)
    args = parser.parse_args()

    args.out_dir.mkdir(parents=True, exist_ok=True)
    results: list[DexResult] = []
    for apk in args.apks:
        if not apk.exists():
            print(f"skip missing {apk}")
            continue
        for dex_index, dex_name, payload in iter_dex_payloads(apk):
            print(f"analyze {apk.name} {dex_name}")
            results.append(analyze_dex(apk, dex_index, dex_name, payload, args.out_dir))

    write_summary(args.out_dir, results)
    print(f"wrote {args.out_dir / 'TEYES_12072024_UART_RE_REPORT.md'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
