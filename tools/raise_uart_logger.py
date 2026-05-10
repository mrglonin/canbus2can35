#!/usr/bin/env python3
from __future__ import annotations

import argparse
import glob
import sys
import time
from pathlib import Path
from typing import Iterable

import serial
from serial.tools import list_ports


DEFAULT_BAUD = 19200
MAX_FRAME = 96


BODY_BITS = [
    (0x01, "LF door"),
    (0x02, "RF door"),
    (0x04, "LR door"),
    (0x08, "RR door"),
    (0x10, "trunk"),
    (0x20, "hood"),
    (0x40, "sunroof candidate"),
]


RZC_KOREA_CH_COMMANDS = {
    0x01: "CH external temperature",
    0x02: "CH steering/panel key",
    0x03: "CH air conditioning",
    0x04: "CH radar",
    0x05: "CH vehicle doors",
    0x06: "CH climate key/status",
    0x07: "CH backlight",
    0x7F: "CH protocol version",
}


RZC_KOREA_HC_COMMANDS = {
    0x03: "HC setting",
    0x04: "HC power",
    0x05: "HC volume adjust",
    0x06: "HC time info",
    0x07: "HC audio balance",
    0x08: "HC sound effects",
    0x09: "HC host/media status",
}


RZC_KOREA_KEYS = {
    0x00: "none/release",
    0x10: "mute",
    0x11: "mode/source",
    0x12: "seek up",
    0x13: "seek down",
    0x14: "volume up",
    0x15: "volume down",
    0x16: "phone accept",
    0x17: "phone hangup",
    0x18: "panel power",
    0x19: "panel volume up",
    0x1A: "panel volume down",
    0x1B: "panel FM/AM",
    0x1C: "panel media",
    0x1D: "panel phone",
    0x1E: "panel display",
    0x1F: "panel seek up",
    0x20: "panel seek down",
    0x21: "panel map",
    0x22: "panel dest",
    0x23: "panel route",
    0x24: "panel setup",
    0x25: "panel enter",
    0x26: "panel tuner up",
    0x27: "panel tuner down",
    0x28: "panel UVO",
    0x29: "panel home",
    0x84: "scroll up",
    0x85: "scroll down",
}


RZC_KOREA_HOST_SOURCES = {
    0x02: "tuner/FM/AM",
    0x06: "navigation",
    0x07: "Bluetooth phone",
    0x0B: "Bluetooth connection",
    0x11: "Bluetooth music",
    0x12: "AUX",
    0x16: "USB media",
    0x80: "media off",
    0x83: "other media",
}


def now() -> str:
    return f"{time.time():.6f}"


def hex_bytes(data: bytes | bytearray) -> str:
    return data.hex(" ").upper()


def checksum(frame: bytes | bytearray) -> int:
    # Raise frame: FD LL CC PP... HH LL. Sum length, command and payload.
    if len(frame) < 5:
        return -1
    return sum(frame[1:-2]) & 0xFFFF


def decode_payload_text(payload: bytes) -> str:
    if not payload:
        return ""
    parts: list[str] = []
    ascii_text = "".join(chr(b) if 32 <= b <= 126 else "." for b in payload)
    if any(ch != "." for ch in ascii_text):
        parts.append(f"ascii={ascii_text!r}")
    if len(payload) >= 2 and len(payload) % 2 == 0:
        try:
            text = payload.decode("utf-16le").rstrip("\x00")
            if text and any(ch.isprintable() and ch != "\x00" for ch in text):
                parts.append(f"utf16le={text!r}")
        except UnicodeDecodeError:
            pass
    return " ".join(parts)


def decode_frame(frame: bytes) -> str:
    if len(frame) < 5 or frame[0] != 0xFD:
        return f"RAW {hex_bytes(frame)}"

    length = frame[1]
    cmd = frame[2]
    payload = frame[3:-2]
    got = (frame[-2] << 8) | frame[-1]
    want = checksum(frame)
    ok = got == want
    names = []
    if cmd in RZC_KOREA_CH_COMMANDS:
        names.append(RZC_KOREA_CH_COMMANDS[cmd])
    if cmd in RZC_KOREA_HC_COMMANDS:
        names.append(RZC_KOREA_HC_COMMANDS[cmd])
    name_suffix = f" name={'/'.join(names)}" if names else ""
    base = (
        f"FRAME cmd=0x{cmd:02X} len={length} payload={hex_bytes(payload) or '-'} "
        f"checksum={'ok' if ok else f'bad got=0x{got:04X} want=0x{want:04X}'}{name_suffix}"
    )

    if cmd == 0x05 and payload:
        flags = payload[0]
        active = [name for bit, name in BODY_BITS if flags & bit]
        meaning = "all closed" if not active else ", ".join(active)
        return f"{base} body_flags=0x{flags:02X} [{meaning}]"

    if cmd == 0x7D and len(payload) >= 2 and payload[0] == 0x06:
        reverse = payload[1] == 0x02
        return f"{base} reverse={'on' if reverse else 'off'}"

    if cmd == 0x01 and payload:
        temp = payload[0] & 0x7F
        if payload[0] & 0x80:
            temp = -temp
        return f"{base} outside_temp_c={temp}"

    if cmd == 0x02 and len(payload) >= 2:
        key = payload[0]
        status = payload[1]
        key_name = RZC_KOREA_KEYS.get(key, f"unknown 0x{key:02X}")
        return f"{base} key={key_name} status={status}"

    if cmd == 0x03 and len(payload) >= 4:
        left, right, fan, flags = payload[:4]
        active = []
        if flags & 0x01:
            active.append("AC")
        if flags & 0x02:
            active.append("dual")
        if flags & 0x04:
            active.append("auto")
        if flags & 0x08:
            active.append("face")
        if flags & 0x10:
            active.append("feet")
        if flags & 0x20:
            active.append("rear_defog")
        if flags & 0x40:
            active.append("front_defog")
        if flags & 0x80:
            active.append("recirc")
        return f"{base} climate left_raw={left} right_raw={right} fan={fan} flags=[{', '.join(active) or 'none'}]"

    if cmd == 0x04 and len(payload) >= 2:
        front = [(payload[0] >> shift) & 0x03 for shift in (6, 4, 2, 0)]
        rear = [(payload[1] >> shift) & 0x03 for shift in (6, 4, 2, 0)]
        return f"{base} radar_front={front} radar_rear={rear}"

    if cmd == 0x06:
        if len(payload) == 1:
            return f"{base} climate_popup_status={payload[0]}"
        if len(payload) >= 3:
            return f"{base} hu_time minute={payload[0]} hour={payload[1]} hour_mode={payload[2]}"

    if cmd == 0x07 and len(payload) == 1:
        return f"{base} backlight_or_balance_value={payload[0]}"

    if cmd == 0x09 and payload:
        source = payload[0]
        source_name = RZC_KOREA_HOST_SOURCES.get(source, f"unknown 0x{source:02X}")
        detail = ""
        if source == 0x02 and len(payload) >= 4:
            detail = f" band={payload[1]} freq={payload[2] * 100 + payload[3]}"
        elif source == 0x16 and len(payload) >= 6:
            track = (payload[1] << 8) | payload[2]
            seconds = payload[3] * 3600 + payload[4] * 60 + payload[5]
            detail = f" track={track} play_time_s={seconds}"
        elif source in (0x07, 0x0B, 0x11, 0x12, 0x80, 0x83, 0x06) and len(payload) >= 2:
            detail = f" state={payload[1]}"
        return f"{base} hu_source={source_name}{detail}"

    if cmd == 0x7F and payload:
        version = decode_payload_text(payload)
        return f"{base} version {version}".rstrip()

    text = decode_payload_text(payload)
    if text:
        return f"{base} {text}"
    return base


class RaiseParser:
    def __init__(self) -> None:
        self.buf = bytearray()

    def feed(self, data: bytes) -> Iterable[bytes]:
        self.buf.extend(data)
        frames: list[bytes] = []

        while self.buf:
            if self.buf[0] != 0xFD:
                # Drop noise until next frame prefix.
                next_fd = self.buf.find(0xFD)
                if next_fd < 0:
                    self.buf.clear()
                    break
                del self.buf[:next_fd]

            if len(self.buf) < 2:
                break
            length = self.buf[1]
            total = length + 1
            if length < 4 or total > MAX_FRAME:
                del self.buf[0]
                continue
            if len(self.buf) < total:
                break
            frames.append(bytes(self.buf[:total]))
            del self.buf[:total]

        return frames


def list_candidate_ports() -> list[str]:
    ports: list[str] = []
    for port in list_ports.comports():
        dev = port.device
        text = " ".join(str(x or "") for x in [dev, port.description, port.manufacturer, port.product]).lower()
        if "bluetooth" in text:
            continue
        # The 2CAN35 CDC port is not the external Raise UART tap. Keep it out of
        # auto mode so we do not confuse USB protocol with head-unit UART.
        if "usbmodemkia" in text or "0483" in text:
            continue
        if any(key in text for key in ["usb", "uart", "serial", "ch340", "wch", "cp210", "ftdi", "slab"]):
            ports.append(dev)

    for pattern in ["/dev/cu.usbserial*", "/dev/cu.wchusbserial*", "/dev/cu.SLAB_USBtoUART*", "/dev/cu.usbUART*"]:
        for path in glob.glob(pattern):
            if path not in ports:
                ports.append(path)
    return ports


def run_logger(port: str, baud: int, seconds: float, outfile: Path | None, raw: bool) -> int:
    parser = RaiseParser()
    deadline = time.monotonic() + seconds if seconds > 0 else None
    frame_count = 0
    byte_count = 0

    fh = outfile.open("a", encoding="utf-8") if outfile else None
    try:
        with serial.Serial(port, baud, bytesize=8, parity="N", stopbits=1, timeout=0.1) as ser:
            line = f"# raise_uart_logger port={port} baud={baud} started={now()}"
            print(line, flush=True)
            if fh:
                print(line, file=fh, flush=True)
            while deadline is None or time.monotonic() < deadline:
                chunk = ser.read(256)
                if not chunk:
                    continue
                byte_count += len(chunk)
                if raw:
                    line = f"{now()} RAW {hex_bytes(chunk)}"
                    print(line, flush=True)
                    if fh:
                        print(line, file=fh, flush=True)
                for frame in parser.feed(chunk):
                    frame_count += 1
                    line = f"{now()} {decode_frame(frame)}"
                    print(line, flush=True)
                    if fh:
                        print(line, file=fh, flush=True)
    finally:
        if fh:
            fh.close()

    print(f"# done bytes={byte_count} frames={frame_count}", flush=True)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Log TEYES/Raise canbox UART FD frames")
    parser.add_argument("port", nargs="?", help="USB-UART port connected to the Raise/TEYES UART line")
    parser.add_argument("--baud", type=int, default=DEFAULT_BAUD)
    parser.add_argument("--seconds", type=float, default=0, help="0 means run until Ctrl-C")
    parser.add_argument("--outfile", type=Path)
    parser.add_argument("--raw", action="store_true", help="also print raw byte chunks")
    parser.add_argument("--list", action="store_true", help="list likely USB-UART ports and exit")
    parser.add_argument("--auto", action="store_true", help="use the first likely USB-UART port")
    args = parser.parse_args()

    candidates = list_candidate_ports()
    if args.list:
        if candidates:
            print("\n".join(candidates))
        else:
            print("no likely USB-UART ports found")
        return 0

    port = args.port
    if args.auto:
        if not candidates:
            print("no likely USB-UART ports found; connect a USB-UART tap to Raise TX/RX", file=sys.stderr)
            return 2
        port = candidates[0]
    if not port:
        parser.error("port is required unless --auto or --list is used")

    try:
        return run_logger(port, args.baud, args.seconds, args.outfile, args.raw)
    except KeyboardInterrupt:
        return 130


if __name__ == "__main__":
    raise SystemExit(main())
