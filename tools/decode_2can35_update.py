#!/usr/bin/env python3
"""Decode and re-encode 2CAN35/Sigma10 APK firmware update packages.

The stock loader accepts a package whose first 16 bytes are cleartext:

    12 bytes STM32 UID + 4 bytes firmware version

The payload is sent in 16-byte blocks. For the Sportage 04.35 package the
loader decrypts only block offsets inside the protected window. The transform
is intentionally small: XOR every byte, then rotate/permutate the 16-byte
block. This script reproduces both directions so round-trips are testable.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import struct
import subprocess
from pathlib import Path
from typing import Iterable


HEADER_LEN = 16
FLASH_SIZE = 0x10000
APP_FLASH_OFF = 0x4000
HEADER_FLASH_OFF = APP_FLASH_OFF - HEADER_LEN
PROTECTED_START = 0x151
PROTECTED_END = 0x350F
FLASH_BASE = 0x08000000


def u32le(data: bytes, off: int) -> int:
    return struct.unpack_from("<I", data, off)[0]


def iter_block_offsets(size: int) -> Iterable[int]:
    for off in range(0, size, 16):
        yield off


def protected_block(off: int) -> bool:
    return PROTECTED_START <= off <= PROTECTED_END


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


def transform_package(data: bytes, key_a: int, key_b: int, *, encode: bool) -> bytes:
    out = bytearray(data)
    for off in iter_block_offsets(len(out)):
        if not protected_block(off):
            continue
        block_len = min(16, len(out) - off)
        block = bytes(out[off : off + block_len])
        if encode:
            new_block = encode_block(block, off, key_a, key_b)
        else:
            new_block = decode_block(block, off, key_a, key_b)
        out[off : off + block_len] = new_block[:block_len]
    return bytes(out)


def make_stlink_app_image(decoded_package: bytes) -> bytes:
    if len(decoded_package) <= HEADER_LEN:
        raise ValueError("package is too small")
    image = bytearray(b"\xff" * FLASH_SIZE)
    image[HEADER_FLASH_OFF:APP_FLASH_OFF] = decoded_package[:HEADER_LEN]
    payload = decoded_package[HEADER_LEN:]
    end = APP_FLASH_OFF + len(payload)
    if end > len(image):
        raise ValueError(f"payload does not fit 64 KiB image: end=0x{end:x}")
    image[APP_FLASH_OFF:end] = payload
    return bytes(image)


def vector_report(decoded_package: bytes) -> dict[str, str | int | bool]:
    payload = decoded_package[HEADER_LEN:]
    sp = u32le(payload, 0)
    reset = u32le(payload, 4)
    return {
        "app_flash_address": f"0x{FLASH_BASE + APP_FLASH_OFF:08x}",
        "initial_sp": f"0x{sp:08x}",
        "reset_vector": f"0x{reset:08x}",
        "sp_looks_like_ram": 0x20000000 <= sp < 0x20020000,
        "reset_looks_like_thumb_flash": (reset & 1) == 1 and FLASH_BASE <= (reset & ~1) < FLASH_BASE + FLASH_SIZE,
    }


def protected_block_summary(size: int) -> dict[str, object]:
    offsets = [off for off in iter_block_offsets(size) if protected_block(off)]
    return {
        "loader_condition": f"0x{PROTECTED_START:x}..0x{PROTECTED_END:x}",
        "block_count": len(offsets),
        "first_block_offset": f"0x{offsets[0]:x}" if offsets else None,
        "last_block_offset": f"0x{offsets[-1]:x}" if offsets else None,
    }


def disassemble(image_path: Path, out_path: Path, objdump: str | None) -> bool:
    if objdump is None:
        return False
    cmd = [
        objdump,
        "-D",
        "-b",
        "binary",
        "-marm",
        "-Mforce-thumb",
        f"--adjust-vma=0x{FLASH_BASE:08x}",
        str(image_path),
    ]
    with out_path.open("w") as fh:
        subprocess.run(cmd, check=True, stdout=fh)
    return True


def main() -> int:
    parser = argparse.ArgumentParser(description="Decode a 2CAN35/Sigma10 update package")
    parser.add_argument("package", type=Path)
    parser.add_argument("-o", "--out-dir", type=Path, default=Path.cwd())
    parser.add_argument("--key-a", type=lambda s: int(s, 0), default=0x04)
    parser.add_argument("--key-b", type=lambda s: int(s, 0), default=0x5B)
    parser.add_argument("--objdump", default=shutil.which("arm-none-eabi-objdump"))
    args = parser.parse_args()

    package = args.package.read_bytes()
    if len(package) < HEADER_LEN:
        raise SystemExit("package is shorter than 16-byte header")

    args.out_dir.mkdir(parents=True, exist_ok=True)
    stem = args.package.stem
    decoded = transform_package(package, args.key_a, args.key_b, encode=False)
    reencoded = transform_package(decoded, args.key_a, args.key_b, encode=True)
    roundtrip_ok = reencoded == package

    decoded_pkg_path = args.out_dir / f"{stem}.decoded_package.bin"
    decoded_payload_path = args.out_dir / f"{stem}.decoded_payload.bin"
    stlink_path = args.out_dir / f"{stem}.decoded_app_stlink64k.bin"
    disasm_path = args.out_dir / f"{stem}.decoded_app.thumb.S"
    report_path = args.out_dir / f"{stem}.decode_report.json"

    decoded_pkg_path.write_bytes(decoded)
    decoded_payload_path.write_bytes(decoded[HEADER_LEN:])
    stlink_path.write_bytes(make_stlink_app_image(decoded))
    disasm_written = disassemble(stlink_path, disasm_path, args.objdump)

    report = {
        "input": str(args.package),
        "input_size": len(package),
        "input_sha256": hashlib.sha256(package).hexdigest(),
        "uid": package[:12].hex(" ").upper(),
        "version_bytes": package[12:16].hex(" ").upper(),
        "key_a": f"0x{args.key_a:02x}",
        "key_b": f"0x{args.key_b:02x}",
        "protected_block_window": protected_block_summary(len(package)),
        "roundtrip_ok": roundtrip_ok,
        "decoded_package": str(decoded_pkg_path),
        "decoded_payload": str(decoded_payload_path),
        "decoded_stlink64k": str(stlink_path),
        "disassembly": str(disasm_path) if disasm_written else None,
        "vectors": vector_report(decoded),
        "decoded_sha256": hashlib.sha256(decoded).hexdigest(),
        "stlink64k_sha256": hashlib.sha256(stlink_path.read_bytes()).hexdigest(),
    }
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")

    print(json.dumps(report, indent=2, ensure_ascii=False))
    if not roundtrip_ok:
        raise SystemExit("decode/encode roundtrip failed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
