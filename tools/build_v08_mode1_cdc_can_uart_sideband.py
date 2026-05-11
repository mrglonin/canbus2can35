#!/usr/bin/env python3
"""Build v08 mode1 canbox with stock-USB CAN/UART sideband.

This variant deliberately keeps the programmer's normal mode1 application and
USB CDC identity. It does not install gs_usb and it does not require switching
to mode3. New lab features are exposed as extra stock CDC commands:

  0x70  CAN log start/stop and CAN RX events
  0x71  UART mirror status
  0x72  UART mirror read
  0x73  UART raw TX
  0x74  raw CAN TX

UART RX is mirrored inside the stock USART2 RX path; it does not drain USART_DR
from the USB command path, so the normal canbox UART parser still receives the
same bytes.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import struct
import subprocess
import tempfile
from pathlib import Path

from decode_2can35_update import (
    APP_FLASH_OFF,
    FLASH_BASE,
    FLASH_SIZE,
    HEADER_LEN,
    make_stlink_app_image,
    transform_package,
)


APP_BASE = FLASH_BASE + APP_FLASH_OFF
STUB_ADDR = 0x08008BF8

DEFAULT_V08 = Path(
    "/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/"
    "incoming/downloads_root_20260509/firmware/37FFDA054247303859412243_04350008.bin"
)
TOOLCHAIN = Path(
    "/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/"
    "toolchains/arm-gnu-toolchain-15.2.rel1-darwin-arm64-arm-none-eabi/bin"
)

PATCH_ASM = r"""
.syntax unified
.cpu cortex-m3
.thumb

.equ LOG_FLAG,        0x20000f00
.equ RESP_BUF,        0x20000f08
.equ UART_HEAD,       0x20007f00
.equ UART_TAIL,       0x20007f02
.equ UART_DROPPED,    0x20007f04
.equ UART_INIT_MAGIC, 0x20007f08
.equ UART_BUF,        0x20008000

.equ USART2_SR,       0x40004400
.equ USART2_DR,       0x40004404
.equ CAN1_BASE,       0x40006400
.equ CAN2_BASE,       0x40006800

.equ ORIG_USB_DISPATCH, 0x08005395
.equ ORIG_CAN_POP,      0x08007399
.equ ORIG_USB_SEND,     0x08005fa9
.equ ORIG_UART_RX_PUT,  0x0800430f
.equ UART_RX_CONTINUE,  0x08004cd5

.global dispatcher
.global canlog0
.global canlog1
.global uart_rx_mirror

.thumb_func
dispatcher:
    push {r4-r7, lr}
    bl uart_init_once
    mov r4, r0
    ldrb r2, [r4, #4]
    cmp r2, #0x70
    beq cmd_canlog
    cmp r2, #0x71
    beq cmd_uart_status
    cmp r2, #0x72
    beq cmd_uart_read
    cmp r2, #0x73
    beq cmd_uart_write
    cmp r2, #0x74
    beq cmd_can_tx
    mov r0, r4
    pop {r4-r7, lr}
    ldr r3, =ORIG_USB_DISPATCH
    bx r3

cmd_canlog:
    ldrb r5, [r4, #5]
    cmp r5, #0
    beq 1f
    cmp r5, #4
    beq 1f
    cmp r5, #1
    beq 2f
    cmp r5, #3
    beq 2f
    b ack_ff
1:
    ldr r6, =LOG_FLAG
    movs r5, #0
    strb r5, [r6]
    movs r0, #0x70
    movs r1, #0
    bl send_ack
    b handled
2:
    ldr r6, =LOG_FLAG
    movs r5, #1
    strb r5, [r6]
    movs r0, #0x70
    movs r1, #1
    bl send_ack
    b handled

cmd_uart_status:
    bl uart_available
    ldr r5, =RESP_BUF
    ldr r6, =0x54524155
    str r6, [r5, #5]
    strh r0, [r5, #9]
    ldr r6, =UART_DROPPED
    ldr r6, [r6]
    str r6, [r5, #11]
    movs r0, #0x71
    movs r1, #10
    bl send_response
    b handled

cmd_uart_read:
    ldrb r6, [r4, #5]
    cmp r6, #48
    bls 1f
    movs r6, #48
1:
    ldr r5, =RESP_BUF
    adds r5, #6
    movs r7, #0
2:
    cmp r7, r6
    bhs 4f
    bl uart_pop
    cmp r1, #0
    beq 4f
    strb r0, [r5, r7]
    adds r7, #1
    b 2b
4:
    ldr r5, =RESP_BUF
    strb r7, [r5, #5]
    movs r0, #0x72
    adds r1, r7, #1
    bl send_response
    b handled

cmd_uart_write:
    ldrb r6, [r4, #3]
    subs r6, #6
    cmp r6, #48
    bls 1f
    movs r6, #48
1:
    movs r7, #0
2:
    cmp r7, r6
    bhs 3f
    adds r3, r4, #5
    ldrb r0, [r3, r7]
    bl uart_write_byte
    adds r7, #1
    b 2b
3:
    movs r0, #0x73
    movs r1, #0
    bl send_ack
    b handled

cmd_can_tx:
    mov r0, r4
    bl can_send_from_frame
    mov r1, r0
    movs r0, #0x74
    bl send_ack
    b handled

ack_ff:
    mov r0, r2
    movs r1, #0xff
    bl send_ack

handled:
    pop {r4-r7, pc}

.thumb_func
send_ack:
    ldr r2, =RESP_BUF
    strb r1, [r2, #5]
    movs r1, #1
    b send_response

.thumb_func
send_response:
    push {r4-r7, lr}
    mov r4, r0
    mov r5, r1
    ldr r6, =RESP_BUF
    movs r3, #0xbb
    strb r3, [r6, #0]
    movs r3, #0xa1
    strb r3, [r6, #1]
    movs r3, #0x41
    strb r3, [r6, #2]
    adds r3, r5, #6
    strb r3, [r6, #3]
    strb r4, [r6, #4]
    movs r0, #0
    movs r1, #0
1:
    adds r2, r5, #5
    cmp r1, r2
    bhs 2f
    ldrb r3, [r6, r1]
    adds r0, r0, r3
    uxtb r0, r0
    adds r1, #1
    b 1b
2:
    strb r0, [r6, r1]
    mov r0, r6
    adds r1, r5, #6
    ldr r3, =ORIG_USB_SEND
    blx r3
    pop {r4-r7, pc}

.thumb_func
canlog0:
    push {lr}
    ldr r3, =ORIG_CAN_POP
    blx r3
    push {r0-r3}
    ldr r3, =LOG_FLAG
    ldrb r3, [r3]
    cbnz r3, 1f
    pop {r0-r3}
    pop {pc}
1:
    add r1, sp, #20
    movs r0, #0
    b canlog_common

.thumb_func
canlog1:
    push {lr}
    ldr r3, =ORIG_CAN_POP
    blx r3
    push {r0-r3}
    ldr r3, =LOG_FLAG
    ldrb r3, [r3]
    cbnz r3, 1f
    pop {r0-r3}
    pop {pc}
1:
    add r1, sp, #20
    movs r0, #1
    b canlog_common

.thumb_func
canlog_common:
    push {r4-r7, lr}
    mov r4, r1
    mov r6, r0
    ldr r5, =RESP_BUF
    movs r3, #0xbb
    strb r3, [r5, #0]
    movs r3, #0xa1
    strb r3, [r5, #1]
    movs r3, #0x41
    strb r3, [r5, #2]
    movs r3, #22
    strb r3, [r5, #3]
    movs r3, #0x70
    strb r3, [r5, #4]
    movs r3, #2
    strb r3, [r5, #5]
    strb r6, [r5, #6]
    ldr r2, [r4, #0]
    ldrb r2, [r2, #0]
    ldr r3, [r4, #4]
    ldrb r3, [r3, #0]
    lsls r3, r3, #1
    orrs r2, r3
    strb r2, [r5, #7]
    ldr r2, [r4, #36]
    str r2, [r5, #8]
    ldr r2, [r4, #12]
    ldrb r2, [r2, #0]
    cmp r2, #8
    it hi
    movhi r2, #8
    strb r2, [r5, #12]
    ldr r0, [r4, #16]
    adds r1, r5, #13
    movs r3, #0
1:
    cmp r3, #8
    bge 4f
    cmp r3, r2
    bge 2f
    ldrb r7, [r0, r3]
    b 3f
2:
    movs r7, #0
3:
    strb r7, [r1, r3]
    adds r3, #1
    b 1b
4:
    mov r0, r5
    movs r1, #22
    bl checksum
    mov r0, r5
    movs r1, #22
    ldr r3, =ORIG_USB_SEND
    blx r3
    pop {r4-r7, lr}
    pop {r0-r3}
    pop {pc}

.thumb_func
checksum:
    push {r4, lr}
    movs r2, #0
    movs r3, #0
    subs r4, r1, #1
1:
    cmp r3, r4
    bge 2f
    ldrb r4, [r0, r3]
    adds r2, r2, r4
    uxtb r2, r2
    adds r3, #1
    b 1b
2:
    strb r2, [r0, r3]
    pop {r4, pc}

.thumb_func
uart_rx_mirror:
    push {r0-r3, lr}
    bl uart_init_once
    mov r0, r1
    bl uart_ring_put
    pop {r0-r3, lr}
    add.w r0, r4, #56
    ldr r3, =ORIG_UART_RX_PUT
    blx r3
    ldr r3, =UART_RX_CONTINUE
    bx r3

.thumb_func
uart_init_once:
    push {r0-r3, lr}
    ldr r0, =UART_INIT_MAGIC
    ldr r1, [r0]
    ldr r2, =0x49545255
    cmp r1, r2
    beq 1f
    str r2, [r0]
    movs r1, #0
    ldr r0, =UART_HEAD
    strh r1, [r0]
    ldr r0, =UART_TAIL
    strh r1, [r0]
    ldr r0, =UART_DROPPED
    str r1, [r0]
1:
    pop {r0-r3, pc}

.thumb_func
uart_available:
    ldr r0, =UART_HEAD
    ldrh r0, [r0]
    ldr r1, =UART_TAIL
    ldrh r1, [r1]
    subs r0, r0, r1
    uxtb r0, r0
    bx lr

.thumb_func
uart_pop:
    ldr r2, =UART_HEAD
    ldrh r2, [r2]
    ldr r3, =UART_TAIL
    ldrh r1, [r3]
    cmp r1, r2
    beq 1f
    ldr r0, =UART_BUF
    ldrb r0, [r0, r1]
    adds r1, #1
    uxtb r1, r1
    strh r1, [r3]
    movs r1, #1
    bx lr
1:
    movs r0, #0
    movs r1, #0
    bx lr

.thumb_func
uart_ring_put:
    push {r1-r4, lr}
    ldr r1, =UART_HEAD
    ldrh r2, [r1]
    adds r3, r2, #1
    uxtb r3, r3
    ldr r4, =UART_TAIL
    ldrh r4, [r4]
    cmp r3, r4
    beq 1f
    ldr r4, =UART_BUF
    strb r0, [r4, r2]
    strh r3, [r1]
    pop {r1-r4, pc}
1:
    ldr r1, =UART_DROPPED
    ldr r2, [r1]
    adds r2, #1
    str r2, [r1]
    pop {r1-r4, pc}

.thumb_func
uart_write_byte:
    push {r1-r3, lr}
    ldr r1, =USART2_SR
    ldr r2, =100000
1:
    ldr r3, [r1]
    tst r3, #0x80
    bne 2f
    subs r2, #1
    bne 1b
    b 3f
2:
    ldr r1, =USART2_DR
    str r0, [r1]
3:
    pop {r1-r3, pc}

.thumb_func
can_send_from_frame:
    push {r4-r7, lr}
    mov r4, r0
    ldrb r0, [r4, #5]
    cmp r0, #1
    beq 1f
    ldr r5, =CAN1_BASE
    b 2f
1:
    ldr r5, =CAN2_BASE
2:
    ldr r6, [r5, #8]
    lsrs r6, r6, #26
    ands r6, r6, #7
    cbz r6, can_fail
    movs r7, #0
    tst r6, #1
    bne 3f
    movs r7, #1
    tst r6, #2
    bne 3f
    movs r7, #2
3:
    movs r6, #0x10
    muls r7, r6, r7
    add.w r5, r5, r7
    add.w r5, r5, #0x180
    ldrb r1, [r4, #6]
    ldr r2, [r4, #7]
    ldrb r3, [r4, #11]
    cmp r3, #8
    it hi
    movhi r3, #8
    movs r0, #0
    tst r1, #1
    bne ext_id
    lsls r0, r2, #21
    b id_done
ext_id:
    lsls r0, r2, #3
    orr r0, r0, #4
id_done:
    tst r1, #2
    beq 4f
    orr r0, r0, #2
4:
    str r0, [r5, #0]
    str r3, [r5, #4]
    ldr r0, [r4, #12]
    str r0, [r5, #8]
    ldr r0, [r4, #16]
    str r0, [r5, #12]
    ldr r0, [r5, #0]
    orr r0, r0, #1
    str r0, [r5, #0]
    movs r0, #0
    pop {r4-r7, pc}
can_fail:
    movs r0, #1
    pop {r4-r7, pc}

.balign 4
.pool
"""


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def u32(data: bytes | bytearray, off: int) -> int:
    return struct.unpack_from("<I", data, off)[0]


def put_u32(buf: bytearray, off: int, value: int) -> None:
    struct.pack_into("<I", buf, off, value)


def encode_thumb_b_w(addr: int, target: int) -> bytes:
    target &= ~1
    offset = target - (addr + 4)
    if offset & 1:
        raise ValueError("Thumb B.W target offset must be halfword aligned")
    imm = offset & 0x01FFFFFF
    s = (imm >> 24) & 1
    i1 = (imm >> 23) & 1
    i2 = (imm >> 22) & 1
    imm10 = (imm >> 12) & 0x03FF
    imm11 = (imm >> 1) & 0x07FF
    j1 = (~(i1 ^ s)) & 1
    j2 = (~(i2 ^ s)) & 1
    return struct.pack("<HH", 0xF000 | (s << 10) | imm10, 0x9000 | (j1 << 13) | (j2 << 11) | imm11)


def encode_thumb_bl(addr: int, target: int) -> bytes:
    target &= ~1
    offset = target - (addr + 4)
    if offset & 1:
        raise ValueError("Thumb BL target offset must be halfword aligned")
    imm = offset & 0x01FFFFFF
    s = (imm >> 24) & 1
    i1 = (imm >> 23) & 1
    i2 = (imm >> 22) & 1
    imm10 = (imm >> 12) & 0x03FF
    imm11 = (imm >> 1) & 0x07FF
    j1 = (~(i1 ^ s)) & 1
    j2 = (~(i2 ^ s)) & 1
    return struct.pack("<HH", 0xF000 | (s << 10) | imm10, 0xD000 | (j1 << 13) | (j2 << 11) | imm11)


def assemble_patch(toolchain: Path) -> tuple[bytes, dict[str, int], str]:
    as_bin = toolchain / "arm-none-eabi-as"
    objcopy_bin = toolchain / "arm-none-eabi-objcopy"
    nm_bin = toolchain / "arm-none-eabi-nm"
    if not as_bin.exists():
        as_bin = Path(shutil.which("arm-none-eabi-as") or "")
        objcopy_bin = Path(shutil.which("arm-none-eabi-objcopy") or "")
        nm_bin = Path(shutil.which("arm-none-eabi-nm") or "")
    if not as_bin or not as_bin.exists():
        raise FileNotFoundError("arm-none-eabi toolchain not found")

    with tempfile.TemporaryDirectory(prefix="mode1-cdc-sideband-") as td:
        tdp = Path(td)
        asm = tdp / "patch.S"
        obj = tdp / "patch.o"
        raw = tdp / "patch.bin"
        asm.write_text(PATCH_ASM, encoding="utf-8")
        subprocess.run([str(as_bin), "-mcpu=cortex-m3", "-mthumb", str(asm), "-o", str(obj)], check=True)
        subprocess.run([str(objcopy_bin), "-O", "binary", "-j", ".text", str(obj), str(raw)], check=True)
        nm_out = subprocess.check_output([str(nm_bin), "-n", str(obj)], text=True)
        symbols: dict[str, int] = {}
        wanted = {"dispatcher", "canlog0", "canlog1", "uart_rx_mirror"}
        for line in nm_out.splitlines():
            parts = line.split()
            if len(parts) >= 3 and parts[2] in wanted:
                symbols[parts[2]] = int(parts[0], 16)
        missing = wanted - set(symbols)
        if missing:
            raise RuntimeError(f"missing symbols: {', '.join(sorted(missing))}")
        return raw.read_bytes(), symbols, nm_out


def stlink_off(addr: int) -> int:
    return addr - FLASH_BASE


def pkg_off(addr: int) -> int:
    return HEADER_LEN + (addr - APP_BASE)


def patch_exact(buf: bytearray, addr: int, expected: bytes, new: bytes) -> dict[str, str]:
    off = pkg_off(addr)
    old = bytes(buf[off : off + len(expected)])
    if old != expected:
        raise ValueError(
            f"unexpected bytes at 0x{addr:08x}: {old.hex(' ').upper()} != {expected.hex(' ').upper()}"
        )
    buf[off : off + len(new)] = new
    return {"address": f"0x{addr:08x}", "old": old.hex(" ").upper(), "new": new.hex(" ").upper()}


def ensure_pkg_len(buf: bytearray, addr: int, size: int) -> None:
    required = pkg_off(addr) + size
    if len(buf) < required:
        buf.extend(b"\xff" * (required - len(buf)))


def build(args: argparse.Namespace) -> dict[str, object]:
    source = args.programmer_v08
    encoded_source = source.read_bytes()
    decoded = bytearray(transform_package(encoded_source, args.key_a, args.key_b, encode=False))
    patch_blob, symbols, nm_out = assemble_patch(args.toolchain)
    ensure_pkg_len(decoded, STUB_ADDR, len(patch_blob))

    stub_off = pkg_off(STUB_ADDR)
    old_stub = bytes(decoded[stub_off : stub_off + len(patch_blob)])
    if old_stub != b"\xff" * len(patch_blob):
        raise ValueError(f"stub area at 0x{STUB_ADDR:08x} is not empty")
    decoded[stub_off : stub_off + len(patch_blob)] = patch_blob

    sym_addr = {name: STUB_ADDR + off for name, off in symbols.items()}
    patches = [
        patch_exact(decoded, 0x08005F2E, bytes.fromhex("FF F7 31 BA"), encode_thumb_b_w(0x08005F2E, sym_addr["dispatcher"])),
        patch_exact(decoded, 0x08004826, bytes.fromhex("02 F0 B7 FD"), encode_thumb_bl(0x08004826, sym_addr["canlog0"])),
        patch_exact(decoded, 0x08004946, bytes.fromhex("02 F0 27 FD"), encode_thumb_bl(0x08004946, sym_addr["canlog0"])),
        patch_exact(decoded, 0x08004A70, bytes.fromhex("02 F0 92 FC"), encode_thumb_bl(0x08004A70, sym_addr["canlog1"])),
        patch_exact(decoded, 0x08004B7C, bytes.fromhex("02 F0 0C FC"), encode_thumb_bl(0x08004B7C, sym_addr["canlog1"])),
        patch_exact(decoded, 0x08004CCC, bytes.fromhex("04 F1 38 00 FF F7 1D FB"), encode_thumb_b_w(0x08004CCC, sym_addr["uart_rx_mirror"]) + b"\x00\xbf\x00\xbf"),
    ]

    encoded = transform_package(bytes(decoded), args.key_a, args.key_b, encode=True)
    roundtrip_ok = transform_package(encoded, args.key_a, args.key_b, encode=False) == bytes(decoded)
    if not roundtrip_ok:
        raise ValueError("encoded package does not roundtrip")

    app_image = make_stlink_app_image(bytes(decoded))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_bytes(encoded)
    if args.stlink_out:
        args.stlink_out.parent.mkdir(parents=True, exist_ok=True)
        args.stlink_out.write_bytes(app_image)

    report = {
        "name": "v08 mode1 stock canbox + stock CDC CAN/UART sideband",
        "source": str(source),
        "source_size": len(encoded_source),
        "source_sha256": sha256(encoded_source),
        "output_usb": str(args.out),
        "output_usb_size": len(encoded),
        "output_usb_sha256": sha256(encoded),
        "output_stlink": str(args.stlink_out) if args.stlink_out else None,
        "output_stlink_sha256": sha256(app_image) if args.stlink_out else None,
        "vectors": {
            "sp": f"0x{u32(app_image, APP_FLASH_OFF):08x}",
            "reset": f"0x{u32(app_image, APP_FLASH_OFF + 4):08x}",
            "unchanged_reset_expected": "0x08006e3d",
        },
        "usb_identity": "unchanged stock CDC/proprietary canbox interface; no gs_usb",
        "mode1_policy": "programmer canbox logic remains default and runs immediately",
        "sideband_commands": {
            "0x70": "CAN log start/stop; events returned as BB A1 41 16 70 02 bus flags id_le dlc data checksum",
            "0x71": "UART mirrored RX status",
            "0x72": "UART mirrored RX read",
            "0x73": "UART raw TX through USART2 PA2",
            "0x74": "CAN raw TX on CAN1/C-CAN or CAN2/M-CAN without changing USB interface",
        },
        "safety_notes": [
            "UART RX is mirrored after stock USART2 RX byte read, so stock parser still receives bytes.",
            "CAN RX hooks call the original CAN pop function before emitting USB copies.",
            "CAN TX assumes stock firmware already initialized CAN1/CAN2 timings.",
        ],
        "stub": {"address": f"0x{STUB_ADDR:08x}", "size": len(patch_blob), "symbols": {k: f"0x{v:08x}" for k, v in sym_addr.items()}},
        "patches": patches,
        "nm": nm_out,
        "roundtrip_ok": roundtrip_ok,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--programmer-v08", type=Path, default=DEFAULT_V08)
    parser.add_argument("--out", type=Path, default=Path("firmware/variants_20260511/07_v08_mode1_canbox_cdc_can_uart_USB.bin"))
    parser.add_argument("--stlink-out", type=Path, default=Path("firmware/variants_20260511/07_v08_mode1_canbox_cdc_can_uart_STLINK64K.bin"))
    parser.add_argument("--report", type=Path, default=Path("firmware/variants_20260511/07_v08_mode1_canbox_cdc_can_uart.report.json"))
    parser.add_argument("--toolchain", type=Path, default=TOOLCHAIN)
    parser.add_argument("--key-a", type=lambda s: int(s, 0), default=0x04)
    parser.add_argument("--key-b", type=lambda s: int(s, 0), default=0x5B)
    args = parser.parse_args()
    report = build(args)
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
