#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path


FLASH_BASE = 0x08000000
APP_OFF = 0x4000
LOGGER_OFF = 0x9000

TBB_TABLE_ADDR = 0x08005956
STATE_TARGETS = {
    0: 0x05,
    1: 0x1A,
    2: 0x2D,
    3: 0x3A,
    4: 0x3D,
    5: 0x49,
    6: 0x4D,
    7: 0x51,
    8: 0x61,
    9: 0x63,
}
SKIP_TO_INCREMENT_ENTRY = 0x11  # target 0x08005978


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def align_up(value: int, alignment: int) -> int:
    return (value + alignment - 1) & ~(alignment - 1)


def load_builder_helpers():
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import make_2can35_base_gs_modes as builder

    return builder


def find_payload_end(image: bytes) -> int:
    end = len(image)
    while end > LOGGER_OFF and image[end - 1] == 0xFF:
        end -= 1
    return align_up(end, 16)


def parse_states(raw: str) -> list[int]:
    out: list[int] = []
    for part in raw.split(","):
        part = part.strip()
        if not part:
            continue
        state = int(part, 0)
        if state not in STATE_TARGETS:
            raise ValueError(f"invalid state {state}; expected 0..9")
        out.append(state)
    return sorted(set(out))


def patch_image(
    image_path: Path,
    out_image: Path,
    out_update: Path,
    states: list[int],
    report_path: Path | None = None,
) -> dict[str, object]:
    builder = load_builder_helpers()
    image = bytearray(image_path.read_bytes())
    original = bytes(image)
    if len(image) != 0x10000:
        raise ValueError(f"expected 64 KiB image, got {len(image)} bytes")

    patches: list[dict[str, object]] = []
    for state in states:
        addr = TBB_TABLE_ADDR + state
        off = addr - FLASH_BASE
        expected = STATE_TARGETS[state]
        current = image[off]
        if current == SKIP_TO_INCREMENT_ENTRY:
            action = "already_patched"
        elif current == expected:
            image[off] = SKIP_TO_INCREMENT_ENTRY
            action = "patched_to_skip_increment"
        else:
            raise ValueError(f"unexpected TBB state {state} at 0x{addr:08x}: 0x{current:02x}")
        patches.append(
            {
                "state": state,
                "address": f"0x{addr:08x}",
                "original_or_current": f"0x{current:02x}",
                "new": f"0x{SKIP_TO_INCREMENT_ENTRY:02x}",
                "action": action,
            }
        )

    payload_end = find_payload_end(bytes(image))
    raw_update = image[APP_OFF - 16 : APP_OFF] + image[APP_OFF:payload_end]
    encoded_update = builder.encode_update_package(raw_update)
    decoded_update = builder.decode_update_package(encoded_update)
    if decoded_update != raw_update:
        raise ValueError("encoded update package does not round-trip")

    out_image.parent.mkdir(parents=True, exist_ok=True)
    out_update.parent.mkdir(parents=True, exist_ok=True)
    out_image.write_bytes(image)
    out_update.write_bytes(encoded_update)

    report = {
        "source_image": str(image_path),
        "source_sha256": sha256(original),
        "output_image": str(out_image),
        "output_image_sha256": sha256(bytes(image)),
        "output_update": str(out_update),
        "output_update_size": len(encoded_update),
        "output_update_sha256": sha256(encoded_update),
        "uid": encoded_update[:12].hex(" ").upper(),
        "version": encoded_update[12:16].hex(" ").upper(),
        "payload_end": f"0x{FLASH_BASE + payload_end:08x}",
        "states_skipped": states,
        "patches": patches,
    }
    actual_report = report_path or out_update.with_suffix(out_update.suffix + ".report.json")
    actual_report.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description="Skip selected 0x08005948 media scheduler TBB states")
    parser.add_argument("--image", type=Path, required=True)
    parser.add_argument("--out-image", type=Path, required=True)
    parser.add_argument("--out-update", type=Path, required=True)
    parser.add_argument("--states", required=True, help="comma-separated state numbers, e.g. 1,2,4,5,6")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    states = parse_states(args.states)
    print(json.dumps(patch_image(args.image, args.out_image, args.out_update, states, args.report), indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
