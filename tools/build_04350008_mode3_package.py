#!/usr/bin/env python3
"""Build the Sportage 04.35.00.08 package with the preserved mode3 logger.

This is intentionally narrow: it ports the already-tested v4 mode2/mode3
wrapper from the current 04.35.00.06 lab package onto the programmer's
04.35.00.08 mode1 application.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
from pathlib import Path

from decode_2can35_update import (
    APP_FLASH_OFF,
    FLASH_BASE,
    HEADER_LEN,
    make_stlink_app_image,
    transform_package,
)


APP_BASE = FLASH_BASE + APP_FLASH_OFF

HOOK_A_ADDR = 0x08005478
HOOK_B_ADDR = 0x08005486
DISPATCH_A_ADDR = 0x08008C21
DISPATCH_B_ADDR = 0x08008C51
RESET_HOOK_ADDR = 0x08008CE1

EXPECTED_HOOK_A = bytes.fromhex("43 79 01 2B 98 D1 BD E8 10 40 00 F0 8D BC")
EXPECTED_HOOK_B = bytes.fromhex("43 79 01 2B 91 D1 BD E8 10 40 00 F0 88 BC")

PATCH_A = bytes.fromhex("00 4B 18 47 21 8C 00 08 00 BF 00 BF 00 BF")
PATCH_B = bytes.fromhex("00 4B 18 47 51 8C 00 08 00 BF 00 BF 00 BF")

LITERAL_DEFAULT_RETURN = 0x08008D60
LITERAL_UPDATE_A = 0x08008D64
LITERAL_UPDATE_B = 0x08008D68
LITERAL_ORIGINAL_RESET = 0x08008D80

V08_DEFAULT_RETURN = 0x080053B1
V08_UPDATE_A = 0x08005DA1
V08_UPDATE_B = 0x08005DA5

MODE3_BRANCH_ADDR = 0x08008C8C
MODE3_RESET_STUB_ADDR = 0x08008DB0
EXPECTED_MODE3_DIRECT_BRANCH = bytes.fromhex("37 E0")

# Runs backup_enable, writes 0x4C33 to BKP_DR2, then requests a system reset.
# The normal reset hook at 0x08008CE0 sees that magic value and enters the
# gs_usb logger from a cleaner reset context instead of jumping directly from
# the USB command handler.
MODE3_RESET_STUB = bytes.fromhex(
    "05 4B 98 47"
    "05 4B 44 F6 33 42 1A 80"
    "04 4B 05 4A 1A 60 BF F3 4F 8F FE E7"
    "AB 8C 00 08"
    "04 6C 00 40"
    "0C ED 00 E0"
    "04 00 FA 05"
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def u32le(data: bytes, off: int) -> int:
    return struct.unpack_from("<I", data, off)[0]


def put_u32le(buf: bytearray, off: int, value: int) -> None:
    struct.pack_into("<I", buf, off, value)


def addr_to_pkg_off(addr: int) -> int:
    if addr < APP_BASE:
        raise ValueError(f"address below app base: 0x{addr:08x}")
    return HEADER_LEN + (addr - APP_BASE)


def slice_at(data: bytes | bytearray, addr: int, size: int) -> bytes:
    off = addr_to_pkg_off(addr)
    return bytes(data[off : off + size])


def patch_at(buf: bytearray, addr: int, expected: bytes, new: bytes) -> dict[str, str]:
    off = addr_to_pkg_off(addr)
    old = bytes(buf[off : off + len(expected)])
    if old != expected:
        raise ValueError(
            f"unexpected bytes at 0x{addr:08x}: {old.hex(' ').upper()} "
            f"!= {expected.hex(' ').upper()}"
        )
    buf[off : off + len(new)] = new
    return {
        "address": f"0x{addr:08x}",
        "old": old.hex(" ").upper(),
        "new": new.hex(" ").upper(),
    }


def vector_report(decoded_package: bytes) -> dict[str, str]:
    payload = decoded_package[HEADER_LEN:]
    return {
        "sp": f"0x{u32le(payload, 0):08x}",
        "reset": f"0x{u32le(payload, 4):08x}",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--programmer-v08",
        type=Path,
        default=Path("/Users/legion/Downloads/37FFDA054247303859412243_04350008.bin"),
    )
    parser.add_argument(
        "--base-canlog",
        type=Path,
        default=Path("firmware/canlog/2can35_04350006_canlog_v4_mode3_preserve_beeps_usb.bin"),
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=Path("firmware/canlog/2can35_04350008_canlog_v4_mode3_preserve_beeps_usb.bin"),
    )
    parser.add_argument("--stlink-out", type=Path)
    parser.add_argument("--report", type=Path)
    parser.add_argument(
        "--mode3-app-bin",
        type=Path,
        help="Optional raw app binary linked at 0x08009000. Replaces the preserved gs_usb logger slot.",
    )
    parser.add_argument("--key-a", type=lambda s: int(s, 0), default=0x04)
    parser.add_argument("--key-b", type=lambda s: int(s, 0), default=0x5B)
    parser.add_argument(
        "--mode3-entry",
        choices=("direct", "reset-reboot"),
        default="direct",
        help="mode3 command path: direct logger jump or BKP magic plus SYSRESETREQ",
    )
    args = parser.parse_args()

    programmer_v08 = args.programmer_v08.read_bytes()
    base_canlog = args.base_canlog.read_bytes()

    decoded_v08 = transform_package(programmer_v08, args.key_a, args.key_b, encode=False)
    decoded_base = transform_package(base_canlog, args.key_a, args.key_b, encode=False)

    if len(decoded_v08) >= len(decoded_base):
        raise ValueError("v08 package would overwrite the preserved wrapper/logger area")

    combined = bytearray(decoded_base)
    combined[: len(decoded_v08)] = decoded_v08

    original_reset = u32le(decoded_v08, HEADER_LEN + 4)
    put_u32le(combined, HEADER_LEN + 4, RESET_HOOK_ADDR)

    patches = [
        patch_at(combined, HOOK_A_ADDR, EXPECTED_HOOK_A, PATCH_A),
        patch_at(combined, HOOK_B_ADDR, EXPECTED_HOOK_B, PATCH_B),
    ]

    literal_changes = {}
    for addr, value in (
        (LITERAL_DEFAULT_RETURN, V08_DEFAULT_RETURN),
        (LITERAL_UPDATE_A, V08_UPDATE_A),
        (LITERAL_UPDATE_B, V08_UPDATE_B),
        (LITERAL_ORIGINAL_RESET, original_reset),
    ):
        off = addr_to_pkg_off(addr)
        old = u32le(combined, off)
        put_u32le(combined, off, value)
        literal_changes[f"0x{addr:08x}"] = {
            "old": f"0x{old:08x}",
            "new": f"0x{value:08x}",
        }

    mode3_replacement = None
    if args.mode3_app_bin is not None:
        mode3_replacement = args.mode3_app_bin.read_bytes()
        mode3_slot_start = addr_to_pkg_off(0x08009000)
        required_len = mode3_slot_start + len(mode3_replacement)
        if required_len > len(combined):
            # Mode 3 is isolated above 0x08009000. Extending the package here
            # preserves mode 1/mode 2 bytes and only appends flash data for the
            # logger slot.
            combined.extend(b"\xff" * (required_len - len(combined)))
        mode3_slot_len = len(combined) - mode3_slot_start
        combined[mode3_slot_start:] = b"\xff" * mode3_slot_len
        combined[mode3_slot_start : mode3_slot_start + len(mode3_replacement)] = mode3_replacement

    mode3_branch_off = addr_to_pkg_off(MODE3_BRANCH_ADDR)
    old_mode3_branch = bytes(combined[mode3_branch_off : mode3_branch_off + 2])
    if old_mode3_branch != EXPECTED_MODE3_DIRECT_BRANCH:
        raise ValueError(
            f"unexpected mode3 branch at 0x{MODE3_BRANCH_ADDR:08x}: "
            f"{old_mode3_branch.hex(' ').upper()}"
        )
    if args.mode3_entry == "reset-reboot":
        # Keep reset_request intact by using a 16-bit branch; a 32-bit branch
        # here would overwrite the first halfword of the reset_request path.
        combined[mode3_branch_off : mode3_branch_off + 2] = bytes.fromhex("90 E0")

        reset_stub_off = addr_to_pkg_off(MODE3_RESET_STUB_ADDR)
        old_reset_stub = bytes(combined[reset_stub_off : reset_stub_off + len(MODE3_RESET_STUB)])
        if old_reset_stub != b"\xFF" * len(MODE3_RESET_STUB):
            raise ValueError(
                f"mode3 reset stub area is not empty at 0x{MODE3_RESET_STUB_ADDR:08x}: "
                f"{old_reset_stub.hex(' ').upper()}"
            )
        combined[reset_stub_off : reset_stub_off + len(MODE3_RESET_STUB)] = MODE3_RESET_STUB

        patches.append(
            {
                "name": "mode3_reset_reboot_branch",
                "address": f"0x{MODE3_BRANCH_ADDR:08x}",
                "old": old_mode3_branch.hex(" ").upper(),
                "new": "90 E0",
                "target": f"0x{MODE3_RESET_STUB_ADDR:08x}",
            }
        )
        patches.append(
            {
                "name": "mode3_reset_reboot_stub",
                "address": f"0x{MODE3_RESET_STUB_ADDR:08x}",
                "size": len(MODE3_RESET_STUB),
                "behavior": "write BKP mode3 magic and trigger SYSRESETREQ before starting gs_usb",
            }
        )
    else:
        patches.append(
            {
                "name": "mode3_direct_branch",
                "address": f"0x{MODE3_BRANCH_ADDR:08x}",
                "old": old_mode3_branch.hex(" ").upper(),
                "new": old_mode3_branch.hex(" ").upper(),
                "target": "0x08008cfe",
            }
        )

    encoded = transform_package(bytes(combined), args.key_a, args.key_b, encode=True)
    roundtrip_ok = transform_package(encoded, args.key_a, args.key_b, encode=False) == bytes(combined)
    if not roundtrip_ok:
        raise ValueError("encoded package does not decode back to the combined image")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_bytes(encoded)

    stlink_path = args.stlink_out
    if stlink_path is not None:
        stlink_path.parent.mkdir(parents=True, exist_ok=True)
        stlink_path.write_bytes(make_stlink_app_image(bytes(combined)))

    mode3_off = addr_to_pkg_off(0x08009000)
    report = {
        "source_programmer_v08": str(args.programmer_v08),
        "source_programmer_v08_sha256": sha256(programmer_v08),
        "base_preserved_wrapper_logger_from": str(args.base_canlog),
        "base_preserved_wrapper_logger_sha256": sha256(base_canlog),
        "output_update": str(args.out),
        "output_update_size": len(encoded),
        "output_update_sha256": sha256(encoded),
        "output_stlink64k": str(stlink_path) if stlink_path else None,
        "output_stlink64k_sha256": sha256(stlink_path.read_bytes()) if stlink_path else None,
        "uid": programmer_v08[:12].hex(" ").upper(),
        "version": programmer_v08[12:16].hex(" ").upper(),
        "v08_payload_end": f"0x{APP_BASE + len(decoded_v08) - HEADER_LEN:08x}",
        "stub_base": "0x08008c20",
        "logger_slot": "0x08009000",
        "mode1_vector": vector_report(bytes(combined)),
        "mode3_vector": {
            "sp": f"0x{u32le(combined, mode3_off):08x}",
            "reset": f"0x{u32le(combined, mode3_off + 4):08x}",
        },
        "patches": patches
        + [
            {
                "name": "mode1_reset_vector",
                "address": "0x08004004",
                "old": f"0x{original_reset:08x}",
                "new": f"0x{RESET_HOOK_ADDR:08x}",
            }
        ],
        "literal_changes": literal_changes,
        "software_modes": {
            "mode1": "programmer 04.35.00.08 canbox application",
            "mode2": "stock update path, value 0x01 through CMD 0x55",
            "mode3": (
                "replacement mode3 app at 0x08009000, "
                f"value 0x03 through CMD 0x51; entry={args.mode3_entry}"
                if args.mode3_app_bin
                else f"preserved gs_usb/budgetcan CAN logger at 0x08009000, value 0x03 through CMD 0x51; entry={args.mode3_entry}"
            ),
            "reset": "value 0x04 through CMD 0x55",
        },
        "mode3_replacement": (
            {
                "source": str(args.mode3_app_bin),
                "size": len(mode3_replacement),
                "sha256": sha256(mode3_replacement),
                "slot_size": len(combined) - mode3_off,
            }
            if args.mode3_app_bin
            else None
        ),
        "roundtrip_ok": roundtrip_ok,
    }

    report_path = args.report or args.out.with_suffix(args.out.suffix + ".report.json")
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
