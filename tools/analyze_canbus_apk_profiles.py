#!/usr/bin/env python3
"""Extract CANBUS profile IDs from FYT/SYU CANBUS update APK databases."""

from __future__ import annotations

import argparse
import csv
import sqlite3
from pathlib import Path


SEARCH_TERMS = [
    "raise",
    "rzc",
    "hyk",
    "hy-k",
    "hiworld",
    "xp(simple)",
    "simple",
    "xp",
    "kia",
    "qiya",
    "hyundai",
    "xiandai",
    "sportage",
    "sonata",
    "sonata8",
    "tusheng",
    "tucson",
    "ix35",
    "ix45",
    "k5",
    "sorento",
    "santafe",
    "shengda",
]


CSV_FIELDS = [
    "db",
    "profile_id",
    "canbox_row_id",
    "company_id",
    "company_en",
    "company_ch",
    "company_name",
    "carset_id",
    "carset_en",
    "carset_ch",
    "carset_name",
    "cartype_id",
    "cartype_en",
    "cartype_ch",
    "cartype_name",
    "canbox_en",
    "canbox_ch",
    "canbox_name",
    "disp",
    "limit_text",
    "limit_byid",
    "horv",
    "cartype_msg",
    "hits",
]


def table_columns(cur: sqlite3.Cursor, table: str) -> list[str]:
    return [row[1] for row in cur.execute(f"PRAGMA table_info({table})")]


def qcol(column: str | None) -> str:
    if not column:
        return "NULL"
    return f'cb."{column.replace(chr(34), chr(34) * 2)}"'


def row_text(values) -> str:
    return " ".join("" if value is None else str(value) for value in values).lower()


def extract_rows(db_path: Path) -> list[dict[str, object]]:
    con = sqlite3.connect(db_path)
    con.row_factory = sqlite3.Row
    cur = con.cursor()
    cb_cols = table_columns(cur, "canbus_canbox")
    limit_col = "limit" if "limit" in cb_cols else ("limitt" if "limitt" in cb_cols else None)
    limit_byid = "limit_byid" if "limit_byid" in cb_cols else None
    horv = "horv" if "horv" in cb_cols else None

    query = f"""
    SELECT
      cb.id AS canbox_row_id,
      cb.id_value AS profile_id,
      cb.name AS canbox_name,
      cb.canbus_canbox_ch AS canbox_ch,
      cb.canbus_canbox_en AS canbox_en,
      cb.canbus_canbox_tw AS canbox_tw,
      cb.company_id,
      c.name AS company_name,
      c.canbus_company_name_ch AS company_ch,
      c.canbus_company_name_en AS company_en,
      cb.carset_id,
      s.name AS carset_name,
      s.canbus_carset_name_ch AS carset_ch,
      s.canbus_carset_name_en AS carset_en,
      cb.cartype_id,
      t.name AS cartype_name,
      t.canbus_cartype_ch AS cartype_ch,
      t.canbus_cartype_en AS cartype_en,
      t.msg AS cartype_msg,
      cb.disp AS disp,
      {qcol(limit_col)} AS limit_text,
      {qcol(limit_byid)} AS limit_byid,
      {qcol(horv)} AS horv
    FROM canbus_canbox cb
    LEFT JOIN canbus_company c ON cb.company_id = c.id
    LEFT JOIN canbus_carset s ON cb.carset_id = s.id
    LEFT JOIN canbus_cartype t ON cb.cartype_id = t.id
    ORDER BY cb.id
    """
    rows: list[dict[str, object]] = []
    for raw in cur.execute(query):
        row = dict(raw)
        text = row_text(row.values())
        hits = [term for term in SEARCH_TERMS if term in text]
        if not hits:
            continue
        row["db"] = db_path.name
        row["hits"] = ";".join(hits)
        rows.append(row)
    return rows


def clean(value: object, limit: int = 140) -> str:
    text = "" if value is None else str(value)
    return text.replace("|", "/").replace("\n", " ")[:limit]


def pick(row: dict[str, object], *keys: str) -> str:
    for key in keys:
        value = row.get(key)
        if value not in (None, ""):
            return clean(value)
    return ""


def write_csv(path: Path, rows: list[dict[str, object]]) -> None:
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_FIELDS, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def write_markdown(path: Path, rows: list[dict[str, object]]) -> None:
    groups = [
        ("Raise / RZC Hyundai-Kia", ["raise", "rzc"], ["hyundai", "kia"]),
        ("XP(Simple) Hyundai-Kia", ["xp(simple)", "simple"], ["hyundai", "kia"]),
        ("Sportage candidates", ["sportage"], []),
        ("Sonata 8 candidates", ["sonata8", "sonata 8"], []),
    ]
    with path.open("w", encoding="utf-8") as handle:
        handle.write("# Анализ CANBUS APK 12.07.2024\n\n")
        handle.write(
            "Источник: `UPDATE_12.07.2024.apk`, базы `assets/canbus.db`, "
            "`assets/canbus_hsp.db`, `assets/canbus_sp.db`.\n\n"
        )
        handle.write(
            "`profile_id` это ID профиля, который выбирается магнитолой в меню CANBUS. "
            "Эти базы не содержат raw UART-пакеты; байты протокола нужно снимать live "
            "или отдельно разбирать из DEX/MCU-слоя.\n\n"
        )
        for title, required, also in groups:
            matched = []
            for row in rows:
                text = row_text(row.values())
                if all(any(term in text for term in required_group) for required_group in [required, also] if required_group):
                    matched.append(row)
            handle.write(f"## {title}\n\n")
            handle.write(f"Найдено строк: {len(matched)}\n\n")
            handle.write("| db | profile_id | company | carset | cartype | canbox | limit |\n")
            handle.write("|---|---:|---|---|---|---|---|\n")
            for row in matched[:250]:
                handle.write(
                    "| {db} | {profile_id} | {company} | {carset} | {cartype} | {canbox} | {limit} |\n".format(
                        db=clean(row.get("db")),
                        profile_id=clean(row.get("profile_id")),
                        company=pick(row, "company_en", "company_ch", "company_name"),
                        carset=pick(row, "carset_en", "carset_ch", "carset_name"),
                        cartype=pick(row, "cartype_en", "cartype_ch", "cartype_name"),
                        canbox=pick(row, "canbox_en", "canbox_ch", "canbox_name"),
                        limit=pick(row, "limit_text", "limit_byid"),
                    )
                )
            handle.write("\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--db-dir", type=Path, required=True, help="Directory with canbus*.db files extracted from UPDATE APK")
    parser.add_argument("--out-dir", type=Path, required=True)
    args = parser.parse_args()

    args.out_dir.mkdir(parents=True, exist_ok=True)
    rows: list[dict[str, object]] = []
    for db_path in sorted(args.db_dir.glob("canbus*.db")):
        rows.extend(extract_rows(db_path))

    write_csv(args.out_dir / "canbus_12072024_profiles_relevant.csv", rows)
    write_markdown(args.out_dir / "CANBUS_12072024_PROFILE_ANALYSIS.md", rows)
    print(f"extracted {len(rows)} relevant rows into {args.out_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
