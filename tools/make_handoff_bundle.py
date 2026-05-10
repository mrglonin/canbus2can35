#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import time
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

DEFAULT_ITEMS = [
    "README.md",
    "dashboard/README.md",
    "docs/PROJECT_STATUS_FOR_AUTHOR.md",
    "docs/ARCHITECTURE_DECISION_CURRENT_BRANCH.md",
    "docs/HARDWARE_WIRING_MOD_GUIDE.md",
    "docs/RAISE_RZC_KOREA_UART_MATRIX.md",
    "docs/CAN_FUNCTION_MATRIX.md",
    "docs/HYUNDAI_KIA_MCAN_MEDIA_RESEARCH.md",
    "docs/END_TO_END_RUNBOOK.md",
    "data/can_function_matrix.csv",
    "data/raise_rzc_korea_uart_matrix.csv",
    "logs/analysis_20260509/ANALYSIS_SUMMARY_20260509.md",
    "logs/analysis_20260509/known_id_segment_candidates.csv",
    "logs/analysis_20260509/important_function_rows.csv",
    "logs/analysis_20260509/firmware_build_summary.csv",
    "firmware/MANIFEST.md",
    "firmware/canlog/README.md",
    "firmware/canlog/2can35_04350008_mode3_lab_can_uart_usb.bin",
    "firmware/canlog/2can35_04350008_mode3_lab_can_uart_usb.bin.report.json",
]


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def copy_item(src: Path, dst_root: Path) -> dict:
    rel = src.relative_to(ROOT)
    dst = dst_root / rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    return {
        "path": str(rel),
        "size": src.stat().st_size,
        "sha256": sha256(src),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Create a clean handoff bundle for the 2CAN35 Sportage project")
    parser.add_argument("--out", type=Path, default=ROOT / "dist")
    parser.add_argument("--name", default=f"2can35_sportage_handoff_{time.strftime('%Y%m%d_%H%M%S')}")
    args = parser.parse_args()

    bundle = args.out / args.name
    if bundle.exists():
        raise SystemExit(f"bundle already exists: {bundle}")
    bundle.mkdir(parents=True)

    manifest = {
        "created_at": time.strftime("%Y-%m-%d %H:%M:%S %z"),
        "root": str(ROOT),
        "bundle": str(bundle),
        "items": [],
        "missing": [],
    }

    for item in DEFAULT_ITEMS:
        src = ROOT / item
        if src.exists() and src.is_file():
            manifest["items"].append(copy_item(src, bundle))
        else:
            manifest["missing"].append(item)

    (bundle / "HANDOFF_MANIFEST.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    readme = [
        "# 2CAN35 Sportage Handoff Bundle",
        "",
        "В этом пакете только чистые артефакты для обсуждения, проверки и продолжения работы.",
        "",
        "Главные точки входа:",
        "",
        "- `README.md` - обзор проекта.",
        "- `docs/END_TO_END_RUNBOOK.md` - пошаговый сценарий теста в машине.",
        "- `dashboard/README.md` - как запустить локальный пульт.",
        "- `logs/analysis_20260509/ANALYSIS_SUMMARY_20260509.md` - выводы по снятым логам.",
        "- `data/can_function_matrix.csv` - рабочая таблица функций CAN.",
        "- `data/raise_rzc_korea_uart_matrix.csv` - Raise/RZC UART команды.",
        "- `firmware/canlog/2can35_04350008_mode3_lab_can_uart_usb.bin` - текущий update-пакет mode3 CAN+UART lab.",
        "- `HANDOFF_MANIFEST.json` - SHA256 каждого файла.",
        "",
    ]
    if manifest["missing"]:
        readme += ["Не найдено при сборке:", ""]
        readme += [f"- `{item}`" for item in manifest["missing"]]
        readme += [""]
    (bundle / "README.md").write_text("\n".join(readme), encoding="utf-8")

    print(bundle)
    print(f"items={len(manifest['items'])} missing={len(manifest['missing'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
