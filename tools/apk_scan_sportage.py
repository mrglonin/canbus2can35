#!/usr/bin/env python3
"""Extract high-signal protocol data from the Sportage/Sorento APK.

The APK contains case-colliding resource names, so avoid full unzip on macOS.
This scanner uses zipfile directly and optionally uses androguard for manifest
metadata when it is available.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path


PATTERNS = [
    "com.sorento",
    "UartService",
    "UsbSerial",
    "USB_PERMISSION",
    "ACTION_",
    "SETTING_DATA_RECEIVED",
    "TPMS_DATA_RECEIVED",
    "AMP_DATA_RECEIVED",
    "CONFIRMATION",
    "uid_data_bytes",
    "ver_data_bytes",
    "amp_data_bytes",
    "tpms_data_bytes",
    "BB",
    "19200",
    "stations",
    "Track",
]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def dex_strings(data: bytes, min_len: int = 4) -> list[str]:
    found = set()
    # This is not a full DEX parser, but it is robust for triage and does not
    # depend on APK extraction. UTF-8 strings in DEX are length-prefixed, but the
    # printable payload still appears contiguously.
    for m in re.finditer(rb"[\x20-\x7e]{%d,}" % min_len, data):
        s = m.group().decode("utf-8", "replace")
        if any(p in s for p in PATTERNS):
            found.add(s)
    return sorted(found)


def try_manifest(apk: Path) -> dict[str, object]:
    try:
        try:
            from loguru import logger  # type: ignore

            logger.remove()
        except Exception:
            pass
        from androguard.core.apk import APK  # type: ignore

        a = APK(str(apk))
        return {
            "package": a.get_package(),
            "app_name": a.get_app_name(),
            "version_name": a.get_androidversion_name(),
            "version_code": a.get_androidversion_code(),
            "permissions": sorted(a.get_permissions()),
            "activities": sorted(a.get_activities()),
            "services": sorted(a.get_services()),
            "receivers": sorted(a.get_receivers()),
        }
    except Exception as exc:
        return {"androguard_error": str(exc)}


def main() -> int:
    parser = argparse.ArgumentParser(description="Scan Sportage APK protocol artifacts")
    parser.add_argument("apk", type=Path)
    parser.add_argument("-o", "--out", type=Path, required=True)
    args = parser.parse_args()

    args.out.mkdir(parents=True, exist_ok=True)
    report: dict[str, object] = {
        "apk": str(args.apk),
        "apk_sha256": sha256(args.apk.read_bytes()),
        "manifest": try_manifest(args.apk),
        "entries": [],
        "dex": [],
        "assets": {},
    }

    with zipfile.ZipFile(args.apk) as zf:
        entries = []
        for info in zf.infolist():
            entries.append(
                {
                    "name": info.filename,
                    "size": info.file_size,
                    "crc": f"0x{info.CRC:08x}",
                }
            )
            if info.filename.endswith(".dex"):
                data = zf.read(info)
                out_path = args.out / info.filename.replace("/", "_")
                out_path.write_bytes(data)
                report["dex"].append(
                    {
                        "name": info.filename,
                        "size": len(data),
                        "sha256": sha256(data),
                        "strings": dex_strings(data),
                        "extracted_to": str(out_path),
                    }
                )
            if info.filename.startswith("assets/"):
                data = zf.read(info)
                asset_path = args.out / info.filename.replace("/", "_")
                asset_path.write_bytes(data)
                report["assets"][info.filename] = {
                    "size": len(data),
                    "sha256": sha256(data),
                    "extracted_to": str(asset_path),
                    "preview": data[:512].decode("utf-8", "replace") if data else "",
                }
        report["entries"] = entries

    report_path = args.out / "apk_scan_report.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")
    print(report_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
