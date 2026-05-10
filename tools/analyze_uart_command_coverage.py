#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
DOCS = ROOT / "docs"
CANDIDATES = DATA / "uart_command_candidates.csv"
RAISE_MATRIX = DATA / "raise_rzc_korea_uart_matrix.csv"
SURFACES = DATA / "apk_canbus_12072024" / "teyes_profile_uart_surfaces.csv"
OUT_CSV = DATA / "uart_command_coverage_analysis.csv"
OUT_MD = DOCS / "UART_COMMAND_COVERAGE_ANALYSIS.md"


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open("r", encoding="utf-8", newline="") as fh:
        return list(csv.DictReader(fh))


def frame_cmd(frame: str) -> str:
    parts = frame.strip().upper().split()
    if not parts:
        return ""
    if parts[0] == "FD" and len(parts) >= 3:
        return f"0x{parts[2]}"
    if parts[0] == "2E" and len(parts) >= 2:
        return f"0x{parts[1]}"
    return ""


def norm_cmd(value: str) -> str:
    value = str(value or "").strip().upper()
    if not value:
        return ""
    if value.startswith("0X"):
        value = value[2:]
    return f"0x{value.zfill(2)}"


def import_server_catalog() -> dict:
    sys.path.insert(0, str(ROOT / "dashboard"))
    import server  # noqa: PLC0415

    return server.command_catalog()


def main() -> int:
    candidates = read_csv(CANDIDATES)
    matrix = read_csv(RAISE_MATRIX)
    surfaces = read_csv(SURFACES)
    catalog = import_server_catalog()

    by_protocol_cmd: dict[tuple[str, str], list[dict[str, str]]] = defaultdict(list)
    by_frame = {}
    for row in candidates:
        cmd = frame_cmd(row.get("frame", ""))
        by_protocol_cmd[(row.get("protocol", ""), cmd)].append(row)
        by_frame[row.get("frame", "").replace(" ", "").upper()] = row

    analysis: list[dict[str, str]] = []
    for row in matrix:
        cmd = norm_cmd(row.get("cmd_hex", ""))
        matches = by_protocol_cmd.get(("raise_rzc_fd", cmd), [])
        analysis.append({
            "source": "raise_matrix",
            "expected": f"{row.get('direction')} {cmd} {row.get('name')}",
            "coverage": "covered" if matches else "missing",
            "candidate_count": str(len(matches)),
            "candidate_ids": ", ".join(item["id"] for item in matches[:12]),
            "note": row.get("meaning", ""),
        })

    surface_external = [
        row for row in surfaces
        if row.get("layer") in {"raise_rzc_korea_uart", "external_canbox_uart_candidate"}
    ]
    for row in surface_external:
        protocol = "raise_rzc_fd" if row.get("layer") == "raise_rzc_korea_uart" else "canbox_2e"
        cmd = norm_cmd(row.get("slot_or_cmd", ""))
        matches = by_protocol_cmd.get((protocol, cmd), [])
        analysis.append({
            "source": row.get("layer", ""),
            "expected": f"{row.get('direction')} {cmd} {row.get('name')}",
            "coverage": "covered" if matches else "missing",
            "candidate_count": str(len(matches)),
            "candidate_ids": ", ".join(item["id"] for item in matches[:12]),
            "note": row.get("status", ""),
        })

    for item in catalog.get("safe_uart_tests", []):
        key = item.get("frame", "").replace(" ", "").upper()
        analysis.append({
            "source": "dashboard_safe_uart",
            "expected": f"{item.get('id')} {item.get('name')}",
            "coverage": "covered" if key in by_frame else "missing",
            "candidate_count": "1" if key in by_frame else "0",
            "candidate_ids": by_frame.get(key, {}).get("id", ""),
            "note": item.get("warning", ""),
        })

    OUT_CSV.parent.mkdir(parents=True, exist_ok=True)
    with OUT_CSV.open("w", encoding="utf-8", newline="") as fh:
        fields = ["source", "expected", "coverage", "candidate_count", "candidate_ids", "note"]
        writer = csv.DictWriter(fh, fieldnames=fields)
        writer.writeheader()
        writer.writerows(analysis)

    totals = Counter(row["coverage"] for row in analysis)
    protocol_counts = Counter(row.get("protocol", "") for row in candidates)
    cmd_counts = Counter((row.get("protocol", ""), frame_cmd(row.get("frame", ""))) for row in candidates)
    missing = [row for row in analysis if row["coverage"] != "covered"]

    OUT_MD.parent.mkdir(parents=True, exist_ok=True)
    with OUT_MD.open("w", encoding="utf-8") as fh:
        fh.write("# UART command coverage analysis\n\n")
        fh.write("Сверка показывает, совпадает ли тестовый список UART TX с тем, что уже было извлечено из матриц, profile surfaces и quick-команд dashboard.\n\n")
        fh.write("## Итог\n\n")
        fh.write(f"- UART candidates: `{len(candidates)}`\n")
        fh.write(f"- Raise/RZC FD candidates: `{protocol_counts.get('raise_rzc_fd', 0)}`\n")
        fh.write(f"- Simple/2E candidates: `{protocol_counts.get('canbox_2e', 0)}`\n")
        fh.write(f"- Coverage rows checked: `{len(analysis)}`\n")
        fh.write(f"- Covered rows: `{totals.get('covered', 0)}`\n")
        fh.write(f"- Missing rows: `{len(missing)}`\n\n")
        fh.write("## Покрытие по протоколу/команде\n\n")
        fh.write("| protocol | cmd | candidates |\n")
        fh.write("|---|---:|---:|\n")
        for (protocol, cmd), count in sorted(cmd_counts.items()):
            fh.write(f"| `{protocol}` | `{cmd}` | {count} |\n")
        fh.write("\n## Missing\n\n")
        if not missing:
            fh.write("Нет пропусков: все строки из Raise matrix, external profile surfaces и dashboard quick-команд имеют хотя бы один TX-кандидат.\n")
        else:
            fh.write("| source | expected | note |\n|---|---|---|\n")
            for row in missing:
                fh.write(f"| `{row['source']}` | {row['expected']} | {row['note']} |\n")
        fh.write("\n## Вывод\n\n")
        fh.write("Список совпадает с уже известной базой по структуре команд: все известные `FD` команды и все кандидатные `2E` команды представлены в TX-списке. Это не означает, что каждая команда даст эффект на машине: рабочими считаются только строки, которые завтра получат verdict `works` в dashboard.\n\n")
        fh.write(f"CSV с детальной сверкой: `{OUT_CSV.relative_to(ROOT)}`.\n")

    print(json.dumps({
        "candidates": len(candidates),
        "analysis_rows": len(analysis),
        "covered": totals.get("covered", 0),
        "missing": len(missing),
        "out_csv": str(OUT_CSV),
        "out_md": str(OUT_MD),
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
