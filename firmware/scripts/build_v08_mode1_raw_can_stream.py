#!/usr/bin/env python3
"""Build v08 mode1 canbox with a minimal raw C-CAN/M-CAN stream sideband.

This variant keeps the programmer's normal mode1 canbox flow. The raw logger is
off by default and is controlled over the stock CDC USB protocol:

  0x70  payload 01/03 = logger on, 00/04 = logger off
  0x76  pop one raw CAN frame from the RAM ring
  0x77  read decoded vehicle snapshot for OBD-like UI
  0x78  one-shot raw CAN TX, bus 0=C-CAN/CAN1, bus 1=M-CAN/CAN2

There is deliberately no UART code in this build.
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import tempfile
from pathlib import Path

from build_v08_mode1_cdc_can_uart_sideband import (
    APP_FLASH_OFF,
    DEFAULT_V08,
    FLASH_BASE,
    HEADER_LEN,
    STUB_ADDR,
    TOOLCHAIN,
    encode_thumb_bl,
    encode_thumb_b_w,
    ensure_pkg_len,
    patch_exact,
    pkg_off,
    sha256,
    u32,
)
from decode_2can35_update import make_stlink_app_image, transform_package


REPO_ROOT = Path(__file__).resolve().parents[1]
LOCAL_V08 = REPO_ROOT.parent / "incoming/downloads_root_20260509/firmware/37FFDA054247303859412243_04350008.bin"
PROGRAMMER_V08 = LOCAL_V08 if LOCAL_V08.exists() else DEFAULT_V08
LOCAL_TOOLCHAIN = REPO_ROOT.parent / "toolchains/arm-gnu-toolchain-15.2.rel1-darwin-arm64-arm-none-eabi/bin"
BUILD_TOOLCHAIN = LOCAL_TOOLCHAIN if LOCAL_TOOLCHAIN.exists() else TOOLCHAIN


PATCH_ASM = r"""
.syntax unified
.cpu cortex-m3
.thumb

.equ LOG_FLAG,        0x20000f00
.equ RESP_BUF,        0x20000f08
.equ CAN_RING_MAGIC,  0x20006ff0
.equ CAN_RING_HEAD,   0x20006ff4
.equ CAN_RING_TAIL,   0x20006ff6
.equ CAN_RING_DROP,   0x20006ff8
.equ CAN_RING_BUF,    0x20007000
.equ STATE_MAGIC,     0x20006e00
.equ STATE_KNOWN,     0x20006e04
.equ STATE_COUNTER,   0x20006e08
.equ STATE_SPEED,     0x20006e0c
.equ STATE_RPM,       0x20006e0e
.equ STATE_COOLANT,   0x20006e10
.equ STATE_VOLTAGE,   0x20006e12
.equ STATE_THROTTLE,  0x20006e14
.equ STATE_BRAKE,     0x20006e15
.equ STATE_GEAR,      0x20006e16
.equ STATE_FUEL,      0x20006e17
.equ STATE_OUTTEMP,   0x20006e18
.equ STATE_FUEL_RATE, 0x20006e1a
.equ STATE_ODOMETER,  0x20006e1c

.equ CAN1_BASE,       0x40006400
.equ CAN2_BASE,       0x40006800

.equ ORIG_USB_DISPATCH, 0x08005395
.equ ORIG_USB_SEND,     0x08005fa9

.global dispatcher
.global can0_fifo0_after
.global can0_fifo1_after
.global can1_fifo0_after
.global can1_fifo1_after

.thumb_func
dispatcher:
    push {r4-r7, lr}
    mov r4, r0
    ldrb r2, [r4, #4]
    cmp r2, #0x70
    beq cmd_canlog
    cmp r2, #0x76
    beq cmd_can_ring_read
    cmp r2, #0x77
    beq cmd_obd_snapshot
    cmp r2, #0x78
    beq cmd_can_tx
    mov r0, r4
    pop {r4-r7, lr}
    ldr r3, =ORIG_USB_DISPATCH
    bx r3

cmd_canlog:
    ldrb r5, [r4, #5]
    cmp r5, #0
    beq log_off
    cmp r5, #4
    beq log_off
    cmp r5, #1
    beq log_on
    cmp r5, #3
    beq log_on
    mov r0, r2
    movs r1, #0xff
    bl send_ack
    b handled

log_off:
    ldr r6, =LOG_FLAG
    movs r5, #0
    strb r5, [r6]
    movs r0, #0x70
    movs r1, #0
    bl send_ack
    b handled

log_on:
    bl can_ring_reset
    ldr r6, =LOG_FLAG
    movs r5, #0xa5
    strb r5, [r6]
    movs r0, #0x70
    movs r1, #1
    bl send_ack
    b handled

cmd_can_ring_read:
    bl can_ring_init_once
    bl can_ring_pop_response
    movs r0, #0x76
    movs r1, #17
    bl send_response
    b handled

cmd_obd_snapshot:
    bl state_init_once
    bl state_snapshot_response
    movs r0, #0x77
    movs r1, #30
    bl send_response

cmd_can_tx:
    ldrb r5, [r4, #3]
    cmp r5, #21
    blo can_tx_bad_frame
    mov r0, r4
    bl can_send_from_frame
    mov r1, r0
    movs r0, #0x78
    bl send_ack
    b handled

can_tx_bad_frame:
    movs r0, #0x78
    movs r1, #0xff
    bl send_ack
    b handled

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

.macro CAN_AFTER bus fifo canbase
    push {r2-r7, lr}
    movs r0, #\bus
    add r1, sp, #28
    bl state_update_from_callsite
    ldr r3, =LOG_FLAG
    ldrb r3, [r3]
    cmp r3, #0xa5
    bne 9f
    movs r0, #\bus
    add r1, sp, #28
    bl can_ring_put_from_callsite
9:
    movs r1, #\fifo
    ldr r0, =\canbase
    pop {r2-r7, pc}
.endm

.thumb_func
can0_fifo0_after:
    CAN_AFTER 0, 0, CAN1_BASE

.thumb_func
can0_fifo1_after:
    CAN_AFTER 0, 1, CAN1_BASE

.thumb_func
can1_fifo0_after:
    CAN_AFTER 1, 0, CAN2_BASE

.thumb_func
can1_fifo1_after:
    CAN_AFTER 1, 1, CAN2_BASE

.thumb_func
can_ring_put_from_callsite:
    push {r4-r7, lr}
    mov r4, r1
    mov r6, r0
    bl can_ring_init_once
    ldr r1, =CAN_RING_HEAD
    ldrh r2, [r1]
    adds r3, r2, #1
    ands r3, r3, #0x7f
    ldr r0, =CAN_RING_TAIL
    ldrh r0, [r0]
    cmp r3, r0
    bne 1f
    ldr r0, =CAN_RING_DROP
    ldr r3, [r0]
    adds r3, #1
    str r3, [r0]
    pop {r4-r7, pc}
1:
    strh r3, [r1]
    ldr r5, =CAN_RING_BUF
    lsls r2, r2, #4
    adds r5, r5, r2
    strb r6, [r5, #0]
    ldrb r2, [r4, #33]
    ldrb r3, [r4, #32]
    lsls r3, r3, #1
    orrs r2, r3
    strb r2, [r5, #1]
    ldrb r2, [r4, #31]
    cmp r2, #8
    it hi
    movhi r2, #8
    strb r2, [r5, #2]
    movs r3, #0
    strb r3, [r5, #3]
    ldr r2, [r4, #36]
    str r2, [r5, #4]
    ldr r0, [r4, #16]
    adds r1, r5, #8
    movs r3, #0
2:
    cmp r3, #8
    bge 5f
    ldrb r7, [r0, r3]
    strb r7, [r1, r3]
    adds r3, #1
    b 2b
5:
    pop {r4-r7, pc}

.thumb_func
state_update_from_callsite:
    push {r4-r7, lr}
    mov r4, r1
    mov r6, r0
    bl state_init_once
    ldr r0, =STATE_COUNTER
    ldr r1, [r0]
    adds r1, #1
    str r1, [r0]
    cmp r6, #0
    bne state_done
    ldr r5, [r4, #36]
    ldr r7, [r4, #16]
    ldrb r6, [r4, #31]

    ldr r0, =0x316
    cmp r5, r0
    beq state_316
    ldr r0, =0x329
    cmp r5, r0
    beq state_329
    cmp r5, #0x44
    beq state_044
    ldr r0, =0x545
    cmp r5, r0
    beq state_545
    ldr r0, =0x169
    cmp r5, r0
    beq state_169
    ldr r0, =0x111
    cmp r5, r0
    beq state_111
    ldr r0, =0x394
    cmp r5, r0
    beq state_394
    b state_done

state_316:
    cmp r6, #8
    blo state_done
    ldrb r2, [r7, #6]
    ldr r0, =STATE_SPEED
    strh r2, [r0]
    ldrb r2, [r7, #2]
    ldrb r3, [r7, #3]
    lsls r3, r3, #8
    orrs r2, r3
    lsrs r2, r2, #2
    ldr r0, =STATE_RPM
    strh r2, [r0]
    ldr r0, =STATE_KNOWN
    ldr r1, [r0]
    movs r2, #0x03
    orrs r1, r2
    str r1, [r0]
    b state_done

state_329:
    cmp r6, #2
    blo state_done
    ldrb r2, [r7, #1]
    subs r2, #64
    mov r3, r2
    lsls r2, r2, #1
    adds r2, r2, r3
    asrs r2, r2, #2
    ldr r0, =STATE_COOLANT
    strh r2, [r0]
    ldr r0, =STATE_KNOWN
    ldr r1, [r0]
    movs r2, #0x04
    orrs r1, r2
    str r1, [r0]
    b state_done

state_044:
    cmp r6, #4
    blo state_done
    ldrb r2, [r7, #3]
    subs r2, #0x52
    asrs r2, r2, #1
    ldr r0, =STATE_OUTTEMP
    strh r2, [r0]
    ldr r0, =STATE_KNOWN
    ldr r1, [r0]
    movs r2, #0x80
    lsls r2, r2, #3
    orrs r1, r2
    str r1, [r0]
    b state_done

state_545:
    cmp r6, #4
    blo state_done
    ldrb r2, [r7, #3]
    movs r3, #100
    muls r2, r3
    ldr r0, =STATE_VOLTAGE
    strh r2, [r0]
    ldr r0, =STATE_KNOWN
    ldr r1, [r0]
    movs r2, #0x08
    orrs r1, r2
    str r1, [r0]
    b state_done

state_169:
    cmp r6, #1
    blo state_done
    ldrb r2, [r7, #0]
    ands r2, r2, #0x0f
    cmp r2, #7
    bne state_gear_unknown
    movs r2, #2
    ldr r0, =STATE_GEAR
    strb r2, [r0]
    ldr r0, =STATE_KNOWN
    ldr r1, [r0]
    movs r2, #0x40
    orrs r1, r2
    str r1, [r0]
    b state_done

state_111:
    cmp r6, #5
    blo state_done
    ldrb r2, [r7, #4]
    cmp r2, #0x64
    bne state_gear_unknown
    movs r2, #2
    ldr r0, =STATE_GEAR
    strb r2, [r0]
    ldr r0, =STATE_KNOWN
    ldr r1, [r0]
    movs r2, #0x40
    orrs r1, r2
    str r1, [r0]
    b state_done

state_gear_unknown:
    movs r2, #0
    ldr r0, =STATE_GEAR
    strb r2, [r0]
    b state_done

state_394:
    cmp r6, #2
    blo state_done
    ldrb r2, [r7, #1]
    ands r2, r2, #0x20
    ite ne
    movne r2, #1
    moveq r2, #0
    ldr r0, =STATE_BRAKE
    strb r2, [r0]
    ldr r0, =STATE_KNOWN
    ldr r1, [r0]
    movs r2, #0x20
    orrs r1, r2
    str r1, [r0]

state_done:
    pop {r4-r7, pc}

.thumb_func
state_init_once:
    push {r0-r3, lr}
    ldr r0, =STATE_MAGIC
    ldr r1, [r0]
    ldr r2, =0x4453424f
    cmp r1, r2
    beq 1f
    str r2, [r0]
    movs r1, #0
    ldr r0, =STATE_KNOWN
    str r1, [r0]
    ldr r0, =STATE_COUNTER
    str r1, [r0]
    ldr r0, =STATE_SPEED
    strh r1, [r0]
    ldr r0, =STATE_RPM
    strh r1, [r0]
    ldr r0, =STATE_COOLANT
    strh r1, [r0]
    ldr r0, =STATE_VOLTAGE
    strh r1, [r0]
    ldr r0, =STATE_THROTTLE
    movs r2, #0xff
    strb r2, [r0]
    ldr r0, =STATE_BRAKE
    strb r2, [r0]
    ldr r0, =STATE_GEAR
    strb r2, [r0]
    ldr r0, =STATE_FUEL
    strb r2, [r0]
    ldr r0, =STATE_OUTTEMP
    strh r1, [r0]
    ldr r0, =STATE_FUEL_RATE
    movs r2, #0xff
    lsls r2, r2, #8
    adds r2, #0xff
    strh r2, [r0]
    ldr r0, =STATE_ODOMETER
    mvns r2, r1
    str r2, [r0]
1:
    pop {r0-r3, pc}

.thumb_func
state_snapshot_response:
    push {r4-r7, lr}
    ldr r7, =RESP_BUF
    adds r7, #5
    movs r0, #1
    strb r0, [r7, #0]
    ldr r4, =STATE_KNOWN
    ldr r0, [r4]
    strb r0, [r7, #1]
    lsrs r1, r0, #8
    strb r1, [r7, #2]
    lsrs r1, r0, #16
    strb r1, [r7, #3]
    ldr r4, =STATE_COUNTER
    ldr r0, [r4]
    str r0, [r7, #4]
    ldr r4, =STATE_SPEED
    ldrh r0, [r4]
    strh r0, [r7, #8]
    ldr r4, =STATE_RPM
    ldrh r0, [r4]
    strh r0, [r7, #10]
    ldr r4, =STATE_COOLANT
    ldrh r0, [r4]
    strh r0, [r7, #12]
    ldr r4, =STATE_VOLTAGE
    ldrh r0, [r4]
    strh r0, [r7, #14]
    ldr r4, =STATE_THROTTLE
    ldrb r0, [r4]
    strb r0, [r7, #16]
    ldr r4, =STATE_BRAKE
    ldrb r0, [r4]
    strb r0, [r7, #17]
    ldr r4, =STATE_GEAR
    ldrb r0, [r4]
    strb r0, [r7, #18]
    ldr r4, =STATE_FUEL
    ldrb r0, [r4]
    strb r0, [r7, #19]
    ldr r4, =STATE_OUTTEMP
    ldrh r0, [r4]
    strh r0, [r7, #20]
    ldr r4, =STATE_FUEL_RATE
    ldrh r0, [r4]
    strh r0, [r7, #22]
    ldr r4, =STATE_ODOMETER
    ldr r0, [r4]
    str r0, [r7, #24]
    movs r0, #0
    strb r0, [r7, #28]
    strb r0, [r7, #29]
    pop {r4-r7, pc}

.thumb_func
can_send_from_frame:
    push {r4-r7, lr}
    mov r4, r0
    ldrb r0, [r4, #5]
    cmp r0, #0
    beq can_tx_can1
    cmp r0, #1
    beq can_tx_can2
    movs r0, #2
    pop {r4-r7, pc}

can_tx_can1:
    ldr r5, =CAN1_BASE
    b can_tx_have_base

can_tx_can2:
    ldr r5, =CAN2_BASE

can_tx_have_base:
    ldr r6, [r5, #8]
    lsrs r6, r6, #26
    ands r6, r6, #7
    cbz r6, can_tx_no_mailbox
    movs r7, #0
    tst r6, #1
    bne can_tx_mailbox_selected
    movs r7, #1
    tst r6, #2
    bne can_tx_mailbox_selected
    movs r7, #2

can_tx_mailbox_selected:
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
    bne can_tx_ext_id
    lsls r0, r2, #21
    b can_tx_id_done

can_tx_ext_id:
    lsls r0, r2, #3
    orr r0, r0, #4

can_tx_id_done:
    tst r1, #2
    beq can_tx_data_frame
    orr r0, r0, #2

can_tx_data_frame:
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

can_tx_no_mailbox:
    movs r0, #1
    pop {r4-r7, pc}

.thumb_func
can_ring_init_once:
    push {r0-r3, lr}
    ldr r0, =CAN_RING_MAGIC
    ldr r1, [r0]
    ldr r2, =0x474e5243
    cmp r1, r2
    beq 1f
    bl can_ring_reset
1:
    pop {r0-r3, pc}

.thumb_func
can_ring_reset:
    push {r0-r3, lr}
    ldr r0, =CAN_RING_MAGIC
    ldr r1, =0x474e5243
    str r1, [r0]
    movs r1, #0
    ldr r0, =CAN_RING_HEAD
    strh r1, [r0]
    ldr r0, =CAN_RING_TAIL
    strh r1, [r0]
    ldr r0, =CAN_RING_DROP
    str r1, [r0]
    pop {r0-r3, pc}

.thumb_func
can_ring_pop_response:
    push {r4-r7, lr}
    ldr r7, =RESP_BUF
    adds r7, #5
    movs r3, #0
    strb r3, [r7, #0]
    ldr r0, =CAN_RING_HEAD
    ldrh r0, [r0]
    ldr r1, =CAN_RING_TAIL
    ldrh r2, [r1]
    cmp r0, r2
    beq 4f
    movs r3, #1
    strb r3, [r7, #0]
    ldr r4, =CAN_RING_BUF
    lsls r5, r2, #4
    adds r4, r4, r5
    adds r5, r7, #1
    movs r6, #0
1:
    cmp r6, #16
    bge 2f
    ldrb r3, [r4, r6]
    strb r3, [r5, r6]
    adds r6, #1
    b 1b
2:
    adds r2, #1
    ands r2, r2, #0x7f
    strh r2, [r1]
    pop {r4-r7, pc}
4:
    adds r5, r7, #1
    movs r6, #0
5:
    cmp r6, #16
    bge 6f
    strb r3, [r5, r6]
    adds r6, #1
    b 5b
6:
    pop {r4-r7, pc}

.balign 4
.pool
"""


def assemble_patch(toolchain: Path) -> tuple[bytes, dict[str, int], str]:
    as_bin = toolchain / "arm-none-eabi-as"
    objcopy_bin = toolchain / "arm-none-eabi-objcopy"
    nm_bin = toolchain / "arm-none-eabi-nm"
    if not as_bin.exists():
        as_which = shutil.which("arm-none-eabi-as")
        objcopy_which = shutil.which("arm-none-eabi-objcopy")
        nm_which = shutil.which("arm-none-eabi-nm")
        if not (as_which and objcopy_which and nm_which):
            raise FileNotFoundError("arm-none-eabi toolchain not found")
        as_bin = Path(as_which)
        objcopy_bin = Path(objcopy_which)
        nm_bin = Path(nm_which)
    if not as_bin.exists() or not objcopy_bin.exists() or not nm_bin.exists():
        raise FileNotFoundError("arm-none-eabi toolchain not found")

    with tempfile.TemporaryDirectory(prefix="mode1-rawcan-") as td:
        tdp = Path(td)
        asm = tdp / "patch.S"
        obj = tdp / "patch.o"
        raw = tdp / "patch.bin"
        asm.write_text(PATCH_ASM, encoding="utf-8")
        subprocess.run([str(as_bin), "-mcpu=cortex-m3", "-mthumb", str(asm), "-o", str(obj)], check=True)
        subprocess.run([str(objcopy_bin), "-O", "binary", "-j", ".text", str(obj), str(raw)], check=True)
        nm_out = subprocess.check_output([str(nm_bin), "-n", str(obj)], text=True)
        wanted = {
            "dispatcher",
            "can0_fifo0_after",
            "can0_fifo1_after",
            "can1_fifo0_after",
            "can1_fifo1_after",
        }
        symbols: dict[str, int] = {}
        for line in nm_out.splitlines():
            parts = line.split()
            if len(parts) >= 3 and parts[2] in wanted:
                symbols[parts[2]] = int(parts[0], 16)
        missing = wanted - set(symbols)
        if missing:
            raise RuntimeError(f"missing symbols: {', '.join(sorted(missing))}")
        return raw.read_bytes(), symbols, nm_out


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
        patch_exact(decoded, 0x0800482A, bytes.fromhex("41 46 38 46"), encode_thumb_bl(0x0800482A, sym_addr["can0_fifo0_after"])),
        patch_exact(decoded, 0x0800494A, bytes.fromhex("01 21 40 46"), encode_thumb_bl(0x0800494A, sym_addr["can0_fifo1_after"])),
        patch_exact(decoded, 0x08004A74, bytes.fromhex("21 46 40 46"), encode_thumb_bl(0x08004A74, sym_addr["can1_fifo0_after"])),
        patch_exact(decoded, 0x08004B82, bytes.fromhex("01 21 30 46"), encode_thumb_bl(0x08004B82, sym_addr["can1_fifo1_after"])),
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
        "name": "v19 v08 mode1 stock canbox + raw CAN stream + decoded vehicle snapshot + raw CAN TX",
        "source": str(source),
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
            "0x70": "raw CAN stream start/stop only",
            "0x76": "pop one raw C-CAN/M-CAN frame from RAM ring",
            "0x77": "read decoded vehicle snapshot: speed/rpm/temperatures/voltage/brake/gear and reserved OBD-like fields",
            "0x78": "one-shot raw CAN TX: payload bus, flags, id_le32, dlc, data[8]",
        },
        "vehicle_snapshot_payload": (
            "status, known24, counter_le32, speed_kmh_u16, rpm_u16, coolant_c_s16, "
            "voltage_mv_u16, throttle_pct_u8, brake_u8, gear_u8, fuel_pct_u8, "
            "outside_c_s16, fuel_rate_x10_u16, odometer_km_u32, reserved2"
        ),
        "known_flags": {
            "bit0": "speed",
            "bit1": "rpm",
            "bit2": "coolant temperature",
            "bit3": "voltage",
            "bit4": "throttle position",
            "bit5": "brake",
            "bit6": "gear",
            "bit7": "fuel level",
            "bit8": "fuel rate",
            "bit9": "odometer",
            "bit10": "outside temperature",
        },
        "raw_frame_payload": "status, bus(0=C-CAN 1=M-CAN), flags(bit0=EXT bit1=RTR), dlc, reserved, id_le32, data[8]",
        "can_tx_payload": "bus(0=C-CAN/CAN1 1=M-CAN/CAN2), flags(bit0=EXT bit1=RTR), id_le32, dlc, data[8]",
        "can_tx_ack": "0=queued, 1=no free mailbox, 2=bad bus, 0xff=bad command length",
        "removed": ["UART sideband"],
        "hook_policy": "hooks run after the stock FIFO read, then return with the stock release-FIFO arguments restored",
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
    parser.add_argument("--programmer-v08", type=Path, default=PROGRAMMER_V08)
    parser.add_argument("--out", type=Path, default=Path("firmware/trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx_USB.bin"))
    parser.add_argument("--stlink-out", type=Path, default=Path("firmware/trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx_STLINK64K.bin"))
    parser.add_argument("--report", type=Path, default=Path("firmware/trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx.report.json"))
    parser.add_argument("--toolchain", type=Path, default=BUILD_TOOLCHAIN)
    parser.add_argument("--key-a", type=lambda s: int(s, 0), default=0x04)
    parser.add_argument("--key-b", type=lambda s: int(s, 0), default=0x5B)
    args = parser.parse_args()
    report = build(args)
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
