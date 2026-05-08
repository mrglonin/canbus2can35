#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

HEADER_LEN = 16
PROTECTED_START = 0x151
PROTECTED_END = 0x350F


def iter_block_offsets(size: int):
    for off in range(0, size, 16):
        yield off


def protected_block(off: int) -> bool:
    return PROTECTED_START <= off <= PROTECTED_END


def encode_block(block: bytes, off: int, key_a: int, key_b: int) -> bytes:
    block = block.ljust(16, b"\xff")
    xor_key = key_a
    rot = key_b & 0x0F
    if ((off >> 8) & 0x08) != 0:
        xor_key = key_b
        rot = key_a & 0x0F

    out = bytearray(16)
    for i in range(16):
        out[i] = block[(rot + i) & 0x0F] ^ xor_key
    return bytes(out)


def decode_block(block: bytes, off: int, key_a: int, key_b: int) -> bytes:
    block = block.ljust(16, b"\xff")
    xor_key = key_a
    rot = key_b & 0x0F
    if ((off >> 8) & 0x08) != 0:
        xor_key = key_b
        rot = key_a & 0x0F

    tmp = bytes((b ^ xor_key) & 0xFF for b in block)
    out = bytearray(16)
    for i, b in enumerate(tmp):
        out[(rot + i) & 0x0F] = b
    return bytes(out)


def transform_package(data: bytes, key_a: int, key_b: int, *, encode: bool) -> bytes:
    out = bytearray(data)
    for off in iter_block_offsets(len(out)):
        if not protected_block(off):
            continue
        block_len = min(16, len(out) - off)
        block = bytes(out[off : off + block_len])
        new_block = encode_block(block, off, key_a, key_b) if encode else decode_block(block, off, key_a, key_b)
        out[off : off + block_len] = new_block[:block_len]
    return bytes(out)


def parse_hex_bytes(value: str, expected_len: int, name: str) -> bytes:
    clean = value.replace(" ", "").replace(":", "").replace("-", "")
    data = bytes.fromhex(clean)
    if len(data) != expected_len:
        raise SystemExit(f"{name} must be {expected_len} bytes, got {len(data)}")
    return data


def main() -> int:
    parser = argparse.ArgumentParser(description="Build APK/USB update package for the custom 2CAN35 app")
    parser.add_argument("--app-bin", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--uid", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--key-a", type=lambda s: int(s, 0), default=0x04)
    parser.add_argument("--key-b", type=lambda s: int(s, 0), default=0x5B)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--meta", action="append", default=[])
    args = parser.parse_args()

    uid = parse_hex_bytes(args.uid, 12, "uid")
    version = parse_hex_bytes(args.version, 4, "version")
    app_raw = args.app_bin.read_bytes()
    pad_len = (-len(app_raw)) % 16
    app = app_raw + (b"\xff" * pad_len)
    plain = uid + version + app
    encoded = transform_package(plain, args.key_a, args.key_b, encode=True)
    decoded = transform_package(encoded, args.key_a, args.key_b, encode=False)
    if decoded != plain:
        raise SystemExit("update package roundtrip failed")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_bytes(encoded)

    report = {
        "app_bin": str(args.app_bin),
        "out": str(args.out),
        "uid": uid.hex(" ").upper(),
        "version": version.hex(" ").upper(),
        "app_size": len(app_raw),
        "app_padded_size": len(app),
        "app_pad_len": pad_len,
        "package_size": len(encoded),
        "key_a": f"0x{args.key_a:02x}",
        "key_b": f"0x{args.key_b:02x}",
        "sha256": hashlib.sha256(encoded).hexdigest(),
        "plain_sha256": hashlib.sha256(plain).hexdigest(),
        "roundtrip_ok": True,
        "meta": args.meta,
        "warning": "CAN HW is compile-time controlled. Build with ENABLE_CAN_HW=1 only after bench validation.",
    }
    if args.report:
        args.report.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
