#!/usr/bin/env python3
from pathlib import Path
import struct
import sys

from unicorn import Uc, UcError, UC_ARCH_ARM, UC_MODE_THUMB, UC_HOOK_CODE
from unicorn.arm_const import UC_ARM_REG_CPSR, UC_ARM_REG_SP


FLASH = 0x08000000
RAM = 0x20000000
PERIPH = 0x40000000
SCB = 0xE000E000
GPIOB_IDR = 0x40010C08
BKP_DR1 = 0x40006C04
MODE3_MAGIC = 0x4C33
UPDATE_MAGIC = 0xBEEF
RESET_MAGIC = 0xCAFE
SELECTOR = 0x08003400

DEFAULT_IMAGE = Path("/Users/legion/Downloads/canbox-fw-lab/final_2can35_prog0435_update_logger3_STLINK.bin")


def write16(image: bytearray, addr: int, halfword: int) -> None:
    image[addr - FLASH:addr - FLASH + 2] = struct.pack("<H", halfword)


def gpio_state(pressed: bool) -> bytes:
    # PB14 high means released, low means pressed.
    return struct.pack("<I", 0xBFFF if pressed else 0xFFFF)


def emulate(image_path: Path, mode: str) -> str:
    image = bytearray(image_path.read_bytes())

    # Skip real tone delays and buzzer functions in emulation only.
    for addr in (
        0x0800057C,
        0x0800064C,
        0x08000628,
        SELECTOR + 0xBC,
        SELECTOR + 0xC2,
        SELECTOR + 0xC8,
    ):
        write16(image, addr, 0x4770)  # bx lr

    # Unicorn does not reliably emulate MSR MSP here. The branch target check
    # is what matters for selector validation.
    for addr in (0x0800016C, SELECTOR + 0x106):
        image[addr - FLASH:addr - FLASH + 4] = b"\x00\xbf\x00\xbf"

    mu = Uc(UC_ARCH_ARM, UC_MODE_THUMB)
    mu.mem_map(FLASH, 0x20000)
    mu.mem_write(FLASH, bytes(image).ljust(0x20000, b"\xff"))
    mu.mem_map(RAM, 0x10000)
    mu.mem_map(PERIPH, 0x200000)
    mu.mem_map(SCB, 0x10000)

    initially_pressed = mode in {"short", "long"}
    mu.mem_write(GPIOB_IDR, gpio_state(initially_pressed))
    if mode == "magic":
        mu.mem_write(BKP_DR1, struct.pack("<H", MODE3_MAGIC))
    if mode in {"update_magic", "boot_update_magic"}:
        mu.mem_write(BKP_DR1, struct.pack("<H", UPDATE_MAGIC))
    if mode in {"reset_magic", "boot_reset_magic"}:
        mu.mem_write(BKP_DR1, struct.pack("<H", RESET_MAGIC))
    mu.reg_write(UC_ARM_REG_SP, 0x20010000)
    mu.reg_write(UC_ARM_REG_CPSR, mu.reg_read(UC_ARM_REG_CPSR) | 0x20)

    result = {"slot": "none"}

    def on_code(mu: Uc, addr: int, size: int, _user_data) -> None:
        if mode == "short" and addr == SELECTOR + 0x68:
            mu.mem_write(GPIOB_IDR, gpio_state(False))
        if addr == 0x080001D4:
            result["slot"] = "update"
            mu.emu_stop()
        elif 0x08004000 <= addr < 0x08009000:
            result["slot"] = "normal"
            mu.emu_stop()
        elif 0x08009000 <= addr < 0x08010000:
            result["slot"] = "logger"
            mu.emu_stop()

    mu.hook_add(UC_HOOK_CODE, on_code)
    try:
        start = 0x080001C4 | 1 if mode in {"boot_update_magic", "boot_reset_magic"} else SELECTOR | 1
        mu.emu_start(start, FLASH + 0x20000, count=20000)
    except UcError as exc:
        return f"error:{exc}"
    return result["slot"]


def main() -> int:
    image_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_IMAGE
    expected = {
        "released": "normal",
        "short": "update",
        "long": "logger",
        "magic": "logger",
        "update_magic": "update",
        "boot_update_magic": "update",
        "reset_magic": "normal",
        "boot_reset_magic": "normal",
    }
    failed = False
    for mode, want in expected.items():
        got = emulate(image_path, mode)
        ok = got == want
        print(f"{mode}: {got} expected={want} {'OK' if ok else 'FAIL'}")
        failed |= not ok
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
