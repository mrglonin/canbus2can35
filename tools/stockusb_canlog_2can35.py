#!/usr/bin/env python3
from __future__ import annotations

import argparse
import glob
import struct
import sys
import time
from pathlib import Path

import serial
from serial.tools import list_ports


REQ_DST = 0x41
REQ_SRC = 0xA1
RESP_DST = 0xA1
RESP_SRC = 0x41
CMD_CANLOG = 0x70
CMD_MODE = 0x55


def checksum(frame: bytes | bytearray) -> int:
    return sum(frame[:-1]) & 0xFF


def make_frame(cmd: int, payload: bytes = b"") -> bytes:
    length = 6 + len(payload)
    frame = bytearray([0xBB, REQ_DST, REQ_SRC, length, cmd])
    frame.extend(payload)
    frame.append(0)
    frame[-1] = checksum(frame)
    return bytes(frame)


def valid_frame(frame: bytes) -> bool:
    return len(frame) >= 6 and frame[0] == 0xBB and frame[3] == len(frame) and checksum(frame) == frame[-1]


def read_frame(ser: serial.Serial, timeout: float) -> bytes | None:
    deadline = time.monotonic() + timeout
    buf = bytearray()
    while time.monotonic() < deadline:
        chunk = ser.read(1)
        if not chunk:
            continue
        byte = chunk[0]
        if not buf:
            if byte != 0xBB:
                continue
        buf.append(byte)
        if len(buf) == 4 and not (6 <= buf[3] <= 64):
            buf.clear()
            continue
        if len(buf) >= 4 and len(buf) == buf[3]:
            frame = bytes(buf)
            if valid_frame(frame):
                return frame
            buf.clear()
    return None


def auto_port() -> str:
    candidates = []
    for port in list_ports.comports():
        dev = port.device
        text = " ".join(filter(None, [port.description, port.manufacturer, port.product, port.vid and f"{port.vid:04x}", port.pid and f"{port.pid:04x}"]))
        if "STM" in text or "CDC" in text or "0483" in text or "5740" in text or "usbmodem" in dev:
            candidates.append(dev)
    candidates.extend(glob.glob("/dev/cu.usbmodem*"))
    seen = []
    for dev in candidates:
        if dev not in seen:
            seen.append(dev)
    if not seen:
        raise SystemExit("no 2CAN35 CDC port found")
    return seen[0]


def decode_raw(frame: bytes) -> str | None:
    if len(frame) != 22 or frame[1] != RESP_DST or frame[2] != RESP_SRC or frame[4] != CMD_CANLOG:
        return None
    if frame[5] != 2:
        return None
    bus = frame[6]
    flags = frame[7]
    can_id = struct.unpack_from("<I", frame, 8)[0]
    dlc = min(frame[12], 8)
    data = frame[13 : 13 + dlc].hex().upper()
    ext = "x" if flags & 1 else "s"
    rtr = "rtr" if flags & 2 else "data"
    return f"{time.time():.6f} bus={bus} id=0x{can_id:X} {ext} {rtr} dlc={dlc} data={data}"


def main() -> int:
    parser = argparse.ArgumentParser(description="Read CAN logs from the stock-USB 2CAN35 canlog patch")
    parser.add_argument("port", nargs="?", help="serial port, default: auto-detect /dev/cu.usbmodem*")
    parser.add_argument("--baud", type=int, default=19200)
    parser.add_argument("--seconds", type=float, default=0, help="stop after N seconds, default: run until Ctrl+C")
    parser.add_argument("--no-start", action="store_true", help="do not send start command")
    parser.add_argument("--stop", action="store_true", help="send stop command and exit")
    parser.add_argument("--mode-cmd", action="store_true", help="use CMD 0x55 values instead of CMD 0x70")
    args = parser.parse_args()

    port = args.port or auto_port()
    cmd = CMD_MODE if args.mode_cmd else CMD_CANLOG
    start_value = b"\x03" if args.mode_cmd else b"\x01"
    stop_value = b"\x04" if args.mode_cmd else b"\x00"
    with serial.Serial(port, args.baud, timeout=0.05, write_timeout=1) as ser:
        ser.reset_input_buffer()
        ser.reset_output_buffer()
        if args.stop:
            ser.write(make_frame(cmd, stop_value))
            ser.flush()
            ack = read_frame(ser, 1.0)
            print(ack.hex(" ") if ack else "no ack")
            return 0
        if not args.no_start:
            ser.write(make_frame(cmd, start_value))
            ser.flush()
            ack = read_frame(ser, 1.0)
            print(f"ack: {ack.hex(' ') if ack else 'none'}")
        deadline = time.monotonic() + args.seconds if args.seconds else None
        while deadline is None or time.monotonic() < deadline:
            frame = read_frame(ser, 0.2)
            if not frame:
                continue
            decoded = decode_raw(frame)
            print(decoded if decoded else frame.hex(" "))
            sys.stdout.flush()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
