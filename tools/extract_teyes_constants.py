#!/usr/bin/env python3
"""Extract high-signal static constants from TEYES/SYU APK DEX files."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path
from zipfile import ZipFile

from loguru import logger

logger.disable("androguard")

from androguard.core.dex import DEX  # noqa: E402


DEFAULT_APKS = [
    Path("/Users/legion/Downloads/CANBUS_12.07.2024.apk"),
    Path("/Users/legion/Downloads/US_12.07.2024.apk"),
]

CLASS_NAMES = {
    "Lcom/syu/module/canbus/FinalCanbus;",
    "Lcom/lsec/core/util/data/FinalCanbus;",
}

PROFILE_TERMS = [
    "QiYa",
    "Qiya",
    "KIA",
    "XianDai",
    "Hyundai",
    "Sportage",
    "KX5",
    "Sonata",
    "SoNaTa",
    "IX35",
    "IX45",
    "Tusheng",
    "TuSheng",
    "Sorento",
    "SORENTO",
    "Santafe",
    "Shengda",
    "K5",
]

MCU_PREFIXES = ("C_", "G_", "U_", "MCU_", "TIP_")


def int_value(field):
    value = field.get_init_value()
    if value is None or not hasattr(value, "get_value"):
        return None
    raw = value.get_value()
    return raw if isinstance(raw, int) else None


def iter_dex(apk_path: Path):
    with ZipFile(apk_path) as zf:
        for name in sorted(n for n in zf.namelist() if n.startswith("classes") and n.endswith(".dex")):
            yield name, DEX(zf.read(name))


def classify(name: str) -> str | None:
    if name.startswith(MCU_PREFIXES) or "CANBUS_FRAME" in name or "MODULE_KEY" in name:
        return "mcu_command"
    if name.startswith("CAR_") and any(term in name for term in PROFILE_TERMS):
        return "profile"
    return None


def extract(apks: list[Path]) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    for apk in apks:
        if not apk.exists():
            continue
        for dex_name, dex in iter_dex(apk):
            for cls in dex.get_classes():
                class_name = cls.get_name()
                if class_name not in CLASS_NAMES:
                    continue
                for field in cls.get_fields():
                    name = field.get_name()
                    kind = classify(name)
                    value = int_value(field)
                    if kind is None or value is None:
                        continue
                    rows.append(
                        {
                            "kind": kind,
                            "apk": apk.name,
                            "dex": dex_name,
                            "class": class_name,
                            "constant": name,
                            "value": value,
                            "hex": f"0x{value:x}",
                            "low16": value & 0xFFFF if kind == "profile" else "",
                            "variant_index": value >> 16 if kind == "profile" else "",
                        }
                    )
    return rows


def write_csv(path: Path, rows: list[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fields = ["kind", "apk", "dex", "class", "constant", "value", "hex", "low16", "variant_index"]
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=Path("data/apk_canbus_12072024/teyes_static_constants.csv"))
    parser.add_argument("apks", nargs="*", type=Path, default=DEFAULT_APKS)
    args = parser.parse_args()

    rows = extract(args.apks)
    write_csv(args.out, rows)
    print(f"wrote {len(rows)} rows to {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
