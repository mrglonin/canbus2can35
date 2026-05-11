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
CMD_UART_STATUS = 0x71
CMD_UART_READ = 0x72
CMD_UART_WRITE = 0x73
CMD_CAN_TX = 0x74


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


def parse_hex_bytes(value: str) -> bytes:
    compact = "".join(ch for ch in value if ch not in " \t\r\n:,-_")
    if len(compact) % 2:
        raise argparse.ArgumentTypeError("hex string must contain an even number of digits")
    try:
        return bytes.fromhex(compact)
    except ValueError as exc:
        raise argparse.ArgumentTypeError(str(exc)) from exc


def decode_uart_status(frame: bytes) -> str | None:
    if len(frame) < 16 or frame[4] != CMD_UART_STATUS:
        return None
    marker = frame[5:9]
    avail = int.from_bytes(frame[9:11], "little")
    dropped = int.from_bytes(frame[11:15], "little")
    return f"uart marker={marker!r} available={avail} dropped={dropped}"


def decode_uart_read(frame: bytes) -> str | None:
    if len(frame) < 7 or frame[4] != CMD_UART_READ:
        return None
    count = min(frame[5], max(0, len(frame) - 7))
    return f"uart-rx {frame[6:6 + count].hex(' ').upper()}"


def parse_can_tx(value: str) -> bytes:
    # bus,id,data ; bus: 0=C-CAN/CAN1, 1=M-CAN/CAN2, id is hex/dec, data is hex.
    try:
        bus_s, can_id_s, data_s = value.split(",", 2)
        bus = int(bus_s, 0)
        can_id = int(can_id_s, 0)
        data = parse_hex_bytes(data_s)
    except Exception as exc:
        raise argparse.ArgumentTypeError("expected bus,id,datahex e.g. 1,0x123,010203") from exc
    if bus not in (0, 1):
        raise argparse.ArgumentTypeError("bus must be 0 or 1")
    if not 0 <= can_id <= 0x7ff:
        raise argparse.ArgumentTypeError("only standard 11-bit CAN IDs are accepted here")
    if len(data) > 8:
        raise argparse.ArgumentTypeError("CAN data must be at most 8 bytes")
    payload = bytearray([bus, 0x00])
    payload.extend(can_id.to_bytes(4, "little"))
    payload.append(len(data))
    payload.extend(data.ljust(8, b"\x00"))
    return bytes(payload)


def main() -> int:
    parser = argparse.ArgumentParser(description="Read CAN logs from the stock-USB 2CAN35 canlog patch")
    parser.add_argument("port", nargs="?", help="serial port, default: auto-detect /dev/cu.usbmodem*")
    parser.add_argument("--baud", type=int, default=19200)
    parser.add_argument("--seconds", type=float, default=0, help="stop after N seconds, default: run until Ctrl+C")
    parser.add_argument("--no-start", action="store_true", help="do not send start command")
    parser.add_argument("--stop", action="store_true", help="send stop command and exit")
    parser.add_argument("--mode-cmd", action="store_true", help="use CMD 0x55 values instead of CMD 0x70")
    parser.add_argument("--uart-status", action="store_true", help="read UART mirror status from mode1 sideband")
    parser.add_argument("--uart-read", type=int, metavar="N", help="read up to N mirrored UART bytes from mode1 sideband")
    parser.add_argument("--uart-write", type=parse_hex_bytes, metavar="HEX", help="write raw bytes to USART2 through mode1 sideband")
    parser.add_argument("--can-tx", type=parse_can_tx, metavar="BUS,ID,HEX", help="send STD CAN frame through mode1 sideband, bus 0=C-CAN 1=M-CAN")
    args = parser.parse_args()

    port = args.port or auto_port()
    cmd = CMD_MODE if args.mode_cmd else CMD_CANLOG
    start_value = b"\x03" if args.mode_cmd else b"\x01"
    stop_value = b"\x04" if args.mode_cmd else b"\x00"
    with serial.Serial(port, args.baud, timeout=0.05, write_timeout=1) as ser:
        ser.reset_input_buffer()
        ser.reset_output_buffer()
        if args.uart_status:
            ser.write(make_frame(CMD_UART_STATUS))
            ser.flush()
            ack = read_frame(ser, 1.0)
            print(decode_uart_status(ack) if ack else "no ack")
            return 0
        if args.uart_read is not None:
            ser.write(make_frame(CMD_UART_READ, bytes([max(0, min(args.uart_read, 48))])))
            ser.flush()
            ack = read_frame(ser, 1.0)
            print(decode_uart_read(ack) if ack else "no ack")
            return 0
        if args.uart_write is not None:
            ser.write(make_frame(CMD_UART_WRITE, args.uart_write[:48]))
            ser.flush()
            ack = read_frame(ser, 1.0)
            print(ack.hex(" ") if ack else "no ack")
            return 0
        if args.can_tx is not None:
            ser.write(make_frame(CMD_CAN_TX, args.can_tx))
            ser.flush()
            ack = read_frame(ser, 1.0)
            print(ack.hex(" ") if ack else "no ack")
            return 0
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
