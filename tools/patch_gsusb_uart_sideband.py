#!/usr/bin/env python3
"""Patch the known-good 2CAN35 gs_usb logger with a UART sideband.

The important constraint is that the USB identity and gs_usb endpoints stay
byte-for-byte compatible with the working logger. This patch only replaces the
existing "request > 7" vendor-control fallback with a small dispatcher:

  0x70 IN   UART status
  0x71 IN   UART read
  0x72 OUT  UART raw write
  0x73 OUT  UART init
  0x7f OUT  reboot back to mode 1

The patched standalone image is then relocated from 0x08000000 to the 2CAN35
mode-3 slot at 0x08009000 so it can be inserted into the normal package.
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


FLASH_BASE = 0x08000000
LOGGER_SLOT_OFF = 0x9000
LOGGER_SLOT_BASE = FLASH_BASE + LOGGER_SLOT_OFF
EXIT_HOOK_OFF = 0x0CF2
USART2_VECTOR_OFF = (16 + 38) * 4
PACKED_DESCRIPTOR_PTR_OFF = 0x2AD7
BSS_END_LITERAL_OFF = 0x12DC
UART_BSS_END = 0x20008100

TOOLCHAIN = Path(
    "/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/"
    "toolchains/arm-gnu-toolchain-15.2.rel1-darwin-arm64-arm-none-eabi/bin"
)

PATCH_ASM = r"""
.syntax unified
.cpu cortex-m3
.thumb

.equ RCC_APB2ENR,     0x40021018
.equ RCC_APB1ENR,     0x4002101c
.equ GPIOA_CRL,       0x40010800
.equ USART2_SR,       0x40004400
.equ USART2_DR,       0x40004404
.equ USART2_BRR,      0x40004408
.equ USART2_CR1,      0x4000440c
.equ USART2_CR2,      0x40004410
.equ USART2_CR3,      0x40004414
.equ NVIC_ISER1,      0xe000e104
.equ SCB_AIRCR,       0xe000ed0c
.equ SYSRESETREQ,     0x05fa0004

.equ UART_HEAD,       0x20007f00
.equ UART_TAIL,       0x20007f02
.equ UART_DROPPED,    0x20007f04
.equ UART_INIT,       0x20007f08
.equ UART_BUF,        0x20008000
.equ UART_MAX_CTRL,   64

.global dispatcher
.global usart2_irq

.thumb_func
dispatcher:
    push {r0-r3, r7, lr}
    cmp r4, #0x7f
    beq do_reset
    cmp r4, #0x70
    beq uart_status
    cmp r4, #0x71
    beq uart_read
    cmp r4, #0x72
    beq uart_write
    cmp r4, #0x73
    beq uart_init_request

unknown_request:
    pop {r0-r3, r7, lr}
    movs r4, #0
    b return_to_gsusb

handled_request:
    pop {r0-r3, r7, lr}
    movs r4, #1

return_to_gsusb:
    ldr r7, =0x08000cad
    bx r7

do_reset:
    cpsid i
    ldr r3, =SCB_AIRCR
    ldr r2, =SYSRESETREQ
    str r2, [r3]
    dsb
1:
    b 1b

uart_init_request:
    bl uart_init
    b handled_request

uart_status:
    bl uart_init
    bl uart_drain
    ldr r7, [r6]
    ldr r0, =0x54524155
    str r0, [r7, #0]
    ldr r0, =UART_INIT
    ldrb r0, [r0]
    strb r0, [r7, #4]
    movs r0, #0
    strb r0, [r7, #5]
    bl uart_available
    strh r0, [r7, #6]
    ldr r0, =UART_DROPPED
    ldr r0, [r0]
    str r0, [r7, #8]
    ldr r3, [sp, #12]
    movs r2, #12
    strh r2, [r3]
    b handled_request

uart_read:
    bl uart_init
    bl uart_drain
    ldr r3, [sp, #12]
    ldrh r2, [r3]
    cmp r2, #UART_MAX_CTRL
    bls 1f
    movs r2, #UART_MAX_CTRL
1:
    ldr r7, [r6]
    movs r1, #0
2:
    cmp r1, r2
    bhs 4f
    ldr r0, =UART_HEAD
    ldrh r0, [r0]
    ldr r3, =UART_TAIL
    ldrh r4, [r3]
    cmp r4, r0
    beq 4f
    ldr r0, =UART_BUF
    ldrb r0, [r0, r4]
    strb r0, [r7, r1]
    adds r4, #1
    uxtb r4, r4
    strh r4, [r3]
    adds r1, #1
    b 2b
4:
    ldr r3, [sp, #12]
    strh r1, [r3]
    b handled_request

uart_write:
    bl uart_init
    ldr r3, [sp, #12]
    ldrh r2, [r3]
    cmp r2, #UART_MAX_CTRL
    bls 1f
    movs r2, #UART_MAX_CTRL
1:
    ldr r7, [r6]
    movs r1, #0
2:
    cmp r1, r2
    bhs 3f
    ldrb r0, [r7, r1]
    bl uart_write_byte
    adds r1, #1
    b 2b
3:
    ldr r3, [sp, #12]
    movs r0, #0
    strh r0, [r3]
    b handled_request

.thumb_func
uart_init:
    push {r0-r3, lr}
    ldr r0, =UART_INIT
    ldrb r1, [r0]
    cbnz r1, 1f

    movs r1, #0
    ldr r2, =UART_HEAD
    strh r1, [r2]
    ldr r2, =UART_TAIL
    strh r1, [r2]
    ldr r2, =UART_DROPPED
    str r1, [r2]

1:
    ldr r2, =RCC_APB2ENR
    ldr r3, [r2]
    orr r3, r3, #0x00000005
    str r3, [r2]

    ldr r2, =RCC_APB1ENR
    ldr r3, [r2]
    orr r3, r3, #0x00020000
    str r3, [r2]

    ldr r2, =GPIOA_CRL
    ldr r3, [r2]
    bic r3, r3, #0x0000ff00
    orr r3, r3, #0x00004b00
    str r3, [r2]

    ldr r2, =USART2_CR1
    movs r3, #0
    str r3, [r2]
    ldr r2, =USART2_CR2
    str r3, [r2]
    ldr r2, =USART2_CR3
    str r3, [r2]
    ldr r2, =USART2_BRR
    ldr r3, =0x00000753
    str r3, [r2]
    ldr r2, =USART2_CR1
    ldr r3, =0x0000202c
    str r3, [r2]

    ldr r2, =NVIC_ISER1
    movs r3, #0x40
    str r3, [r2]

    movs r1, #1
    strb r1, [r0]
    bl uart_drain
9:
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
uart_drain:
    push {r0-r2, lr}
1:
    ldr r1, =USART2_SR
    ldr r0, [r1]
    tst r0, #0x20
    beq 2f
    ldr r1, =USART2_DR
    ldr r0, [r1]
    uxtb r0, r0
    bl ring_put
    b 1b
2:
    pop {r0-r2, pc}

.thumb_func
uart_write_byte:
    push {r1-r2, lr}
    ldr r1, =USART2_SR
    ldr r2, =100000
1:
    ldr r12, [r1]
    tst r12, #0x80
    bne 2f
    subs r2, #1
    bne 1b
    b 3f
2:
    ldr r1, =USART2_DR
    str r0, [r1]
3:
    pop {r1-r2, pc}

.thumb_func
ring_put:
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
usart2_irq:
    push {r0-r2, lr}
    ldr r1, =USART2_SR
    ldr r0, [r1]
    tst r0, #0x20
    beq 1f
    ldr r1, =USART2_DR
    ldr r0, [r1]
    uxtb r0, r0
    bl ring_put
1:
    pop {r0-r2, pc}

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
    offset = target - (addr + 4)
    if offset & 1:
        raise ValueError("Thumb B.W target offset must be halfword aligned")
    if not -(1 << 24) <= offset < (1 << 24):
        raise ValueError("Thumb B.W target out of range")

    imm = offset & 0x01FFFFFF
    s = (imm >> 24) & 1
    i1 = (imm >> 23) & 1
    i2 = (imm >> 22) & 1
    imm10 = (imm >> 12) & 0x03FF
    imm11 = (imm >> 1) & 0x07FF
    j1 = (~(i1 ^ s)) & 1
    j2 = (~(i2 ^ s)) & 1

    h1 = 0xF000 | (s << 10) | imm10
    h2 = 0x9000 | (j1 << 13) | (j2 << 11) | imm11
    return struct.pack("<HH", h1, h2)


def valid_vector_at(blob: bytes, off: int, start: int, end: int) -> bool:
    if off + 8 > len(blob):
        return False
    sp = u32(blob, off)
    reset = u32(blob, off + 4)
    return 0x20000000 <= sp <= 0x20010000 and start <= (reset & ~1) < end and bool(reset & 1)


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

    with tempfile.TemporaryDirectory(prefix="gsusb-uart-") as td:
        tdp = Path(td)
        asm = tdp / "uart_sideband.S"
        obj = tdp / "uart_sideband.o"
        raw = tdp / "uart_sideband.bin"
        asm.write_text(PATCH_ASM, encoding="utf-8")
        subprocess.run([str(as_bin), "-mcpu=cortex-m3", "-mthumb", str(asm), "-o", str(obj)], check=True)
        subprocess.run([str(objcopy_bin), "-O", "binary", "-j", ".text", str(obj), str(raw)], check=True)
        nm_out = subprocess.check_output([str(nm_bin), "-n", str(obj)], text=True)
        symbols: dict[str, int] = {}
        for line in nm_out.splitlines():
            parts = line.split()
            if len(parts) >= 3 and parts[2] in {"dispatcher", "usart2_irq"}:
                symbols[parts[2]] = int(parts[0], 16)
        missing = {"dispatcher", "usart2_irq"} - set(symbols)
        if missing:
            raise RuntimeError(f"missing symbols: {', '.join(sorted(missing))}")
        return raw.read_bytes(), symbols, nm_out


def relocate_standalone_flash_image(blob: bytes, slot_off: int) -> tuple[bytes, list[dict[str, str]]]:
    if not valid_vector_at(blob, 0, FLASH_BASE, FLASH_BASE + len(blob)):
        raise ValueError("logger image does not look like a standalone STM32 flash image")

    delta = slot_off
    slot_start = FLASH_BASE + slot_off
    slot_end = slot_start + len(blob)
    original_end = FLASH_BASE + len(blob)
    out = bytearray(blob)
    patches: list[dict[str, str]] = []

    i = 0
    while i <= len(out) - 4:
        value = u32(out, i)
        value_addr = value & ~1
        if FLASH_BASE <= value_addr < original_end:
            relocated = value + delta
            if not (slot_start <= (relocated & ~1) < slot_end):
                raise ValueError(f"relocated pointer out of logger slot at file offset 0x{i:x}")
            put_u32(out, i, relocated)
            patches.append({"file_offset": f"0x{i:04x}", "old": f"0x{value:08x}", "new": f"0x{relocated:08x}"})
            i += 4
        else:
            i += 4

    if PACKED_DESCRIPTOR_PTR_OFF + 4 <= len(out):
        value = u32(out, PACKED_DESCRIPTOR_PTR_OFF)
        value_addr = value & ~1
        if FLASH_BASE <= value_addr < original_end:
            relocated = value + delta
            put_u32(out, PACKED_DESCRIPTOR_PTR_OFF, relocated)
            patches.append(
                {
                    "file_offset": f"0x{PACKED_DESCRIPTOR_PTR_OFF:04x}",
                    "old": f"0x{value:08x}",
                    "new": f"0x{relocated:08x}",
                    "note": "packed USB config descriptor pointer",
                }
            )

    if not valid_vector_at(out, 0, slot_start, slot_end):
        raise ValueError("relocated logger vector is invalid")
    return bytes(out), patches


def patch_logger(source: Path, out_original: Path, out_relocated: Path, report_path: Path, toolchain: Path) -> dict[str, object]:
    image = bytearray(source.read_bytes())
    hook_original = bytes(image[EXIT_HOOK_OFF : EXIT_HOOK_OFF + 4])
    if hook_original != bytes.fromhex("00 24 da e7"):
        raise ValueError(f"unexpected unpatched gs_usb hook at 0x{EXIT_HOOK_OFF:04x}: {hook_original.hex(' ')}")

    patch_blob, symbols, nm_out = assemble_patch(toolchain)
    stub_off = (len(image) + 3) & ~3
    if len(image) < stub_off:
        image.extend(b"\xff" * (stub_off - len(image)))
    dispatcher_addr = FLASH_BASE + stub_off + symbols["dispatcher"]
    usart2_irq_addr = FLASH_BASE + stub_off + symbols["usart2_irq"] + 1

    image.extend(patch_blob)
    branch = encode_thumb_b_w(FLASH_BASE + EXIT_HOOK_OFF, dispatcher_addr)
    image[EXIT_HOOK_OFF : EXIT_HOOK_OFF + 4] = branch
    old_vector = u32(image, USART2_VECTOR_OFF)
    put_u32(image, USART2_VECTOR_OFF, usart2_irq_addr)
    old_bss_end = u32(image, BSS_END_LITERAL_OFF)
    put_u32(image, BSS_END_LITERAL_OFF, UART_BSS_END)

    original = bytes(image)
    relocated, reloc_patches = relocate_standalone_flash_image(original, LOGGER_SLOT_OFF)

    out_original.parent.mkdir(parents=True, exist_ok=True)
    out_relocated.parent.mkdir(parents=True, exist_ok=True)
    out_original.write_bytes(original)
    out_relocated.write_bytes(relocated)

    report = {
        "source": str(source),
        "source_size": source.stat().st_size,
        "source_sha256": sha256(source.read_bytes()),
        "output_original": str(out_original),
        "output_original_size": len(original),
        "output_original_sha256": sha256(original),
        "output_relocated": str(out_relocated),
        "output_relocated_size": len(relocated),
        "output_relocated_sha256": sha256(relocated),
        "hook_offset": f"0x{EXIT_HOOK_OFF:04x}",
        "hook_original": hook_original.hex(" "),
        "hook_branch": branch.hex(" "),
        "stub_offset": f"0x{stub_off:04x}",
        "stub_size": len(patch_blob),
        "dispatcher_original_addr": f"0x{dispatcher_addr:08x}",
        "dispatcher_relocated_addr": f"0x{dispatcher_addr + LOGGER_SLOT_OFF:08x}",
        "usart2_vector_offset": f"0x{USART2_VECTOR_OFF:04x}",
        "usart2_vector_old": f"0x{old_vector:08x}",
        "usart2_irq_original_addr": f"0x{usart2_irq_addr:08x}",
        "usart2_irq_relocated_addr": f"0x{usart2_irq_addr + LOGGER_SLOT_OFF:08x}",
        "bss_end_literal_offset": f"0x{BSS_END_LITERAL_OFF:04x}",
        "bss_end_old": f"0x{old_bss_end:08x}",
        "bss_end_new": f"0x{UART_BSS_END:08x}",
        "usb_identity": "unchanged gs_usb/budgetcan 1d50:606f endpoints 0x81/0x02",
        "uart": {
            "peripheral": "USART2",
            "pins": "PA2 TX, PA3 RX",
            "baud": 19200,
            "control_requests": {
                "0x70 IN": "UART status: bytes 'UART', init flag, rx_available, dropped",
                "0x71 IN": "UART read, max 64 bytes",
                "0x72 OUT": "UART raw write, max 64 bytes",
                "0x73 OUT": "UART init",
                "0x7f OUT": "system reset back to mode 1",
            },
        },
        "relocation_count": len(reloc_patches),
        "relocation_patches": reloc_patches,
        "nm": nm_out,
    }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=Path("/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/gs_2can35.bin"))
    parser.add_argument("--out-original", type=Path, default=Path("firmware/canlog/gs_2can35_uart_sideband_original.bin"))
    parser.add_argument("--out-relocated", type=Path, default=Path("firmware/canlog/gs_2can35_uart_sideband_0x08009000.bin"))
    parser.add_argument("--report", type=Path, default=Path("firmware/canlog/gs_2can35_uart_sideband_0x08009000.report.json"))
    parser.add_argument("--toolchain", type=Path, default=TOOLCHAIN)
    args = parser.parse_args()

    report = patch_logger(args.source, args.out_original, args.out_relocated, args.report, args.toolchain)
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
