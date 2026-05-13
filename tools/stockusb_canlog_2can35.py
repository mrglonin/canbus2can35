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
CMD_CAN_TX = 0x78
CMD_CAN_CACHE = 0x75
CMD_CAN_RING_READ = 0x76
CMD_OBD_SNAPSHOT = 0x77


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


def decode_can_cache(frame: bytes) -> str | None:
    if len(frame) != 22 or frame[4] != CMD_CAN_CACHE:
        return None
    slot = frame[5]
    bus = frame[6]
    valid = frame[7]
    can_id = struct.unpack_from("<I", frame, 8)[0]
    dlc = min(frame[12], 8)
    data = frame[13 : 13 + dlc].hex().upper()
    bus_name = "C-CAN" if bus == 0 else "M-CAN"
    return f"slot={slot:02d} {bus_name} valid={valid} id=0x{can_id:X} dlc={dlc} data={data}"


def decode_can_ring(frame: bytes) -> str | None:
    if len(frame) != 23 or frame[4] != CMD_CAN_RING_READ:
        return None
    status = frame[5]
    if status == 0:
        return None
    bus = frame[6]
    flags = frame[7]
    dlc = min(frame[8], 8)
    can_id = struct.unpack_from("<I", frame, 10)[0]
    data = frame[14 : 14 + dlc].hex().upper()
    ext = "x" if flags & 1 else "s"
    rtr = "rtr" if flags & 2 else "data"
    return f"{time.time():.6f} bus={bus} id=0x{can_id:X} {ext} {rtr} dlc={dlc} data={data}"


KNOWN_FLAGS = [
    ("speed", 0),
    ("rpm", 1),
    ("coolant_c", 2),
    ("voltage_mv", 3),
    ("throttle_pct", 4),
    ("brake", 5),
    ("gear", 6),
    ("fuel_pct", 7),
    ("fuel_rate_x10", 8),
    ("odometer_km", 9),
    ("outside_c", 10),
]


def _known_names(mask: int) -> list[str]:
    return [name for name, bit in KNOWN_FLAGS if mask & (1 << bit)]


def decode_obd_snapshot(frame: bytes) -> str | None:
    if len(frame) != 36 or frame[4] != CMD_OBD_SNAPSHOT:
        return None
    payload = frame[5:-1]
    status = payload[0]
    known = payload[1] | (payload[2] << 8) | (payload[3] << 16)
    counter = struct.unpack_from("<I", payload, 4)[0]
    speed = struct.unpack_from("<H", payload, 8)[0]
    rpm = struct.unpack_from("<H", payload, 10)[0]
    coolant = struct.unpack_from("<h", payload, 12)[0]
    voltage_mv = struct.unpack_from("<H", payload, 14)[0]
    throttle = payload[16]
    brake = payload[17]
    gear = payload[18]
    fuel = payload[19]
    outside = struct.unpack_from("<h", payload, 20)[0]
    fuel_rate = struct.unpack_from("<H", payload, 22)[0]
    odometer = struct.unpack_from("<I", payload, 24)[0]
    gear_text = {0: "unknown", 1: "P", 2: "R", 3: "N", 4: "D"}.get(gear, str(gear))
    fields = {
        "status": status,
        "known": _known_names(known),
        "counter": counter,
        "speed_kmh": speed if known & 0x001 else None,
        "rpm": rpm if known & 0x002 else None,
        "coolant_c": coolant if known & 0x004 else None,
        "voltage_v": round(voltage_mv / 1000, 2) if known & 0x008 else None,
        "throttle_pct": throttle if known & 0x010 else None,
        "brake": bool(brake) if known & 0x020 else None,
        "gear": gear_text if known & 0x040 else None,
        "fuel_pct": fuel if known & 0x080 else None,
        "outside_c": outside if known & 0x400 else None,
        "fuel_rate_lph": round(fuel_rate / 10, 1) if known & 0x100 else None,
        "odometer_km": odometer if known & 0x200 else None,
    }
    return json_dumps_compact(fields)


def json_dumps_compact(value: object) -> str:
    import json

    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


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
    parser.add_argument("--can-tx", type=parse_can_tx, metavar="BUS,ID,HEX", help="send one STD CAN frame with CMD 0x78, bus 0=C-CAN 1=M-CAN")
    parser.add_argument("--cache-slot", type=int, metavar="N", help="read one stock canbox cache slot through mode1 sideband")
    parser.add_argument("--cache-all", action="store_true", help="read stock canbox cache slots 0..15 through mode1 sideband")
    parser.add_argument("--poll-raw", action="store_true", help="poll raw CAN ring events with CMD 0x76 instead of waiting for async frames")
    parser.add_argument("--snapshot", action="store_true", help="read decoded vehicle snapshot with CMD 0x77")
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
        if args.cache_slot is not None:
            slot = max(0, min(args.cache_slot, 15))
            ser.write(make_frame(CMD_CAN_CACHE, bytes([slot])))
            ser.flush()
            ack = read_frame(ser, 1.0)
            print(decode_can_cache(ack) if ack else "no ack")
            return 0
        if args.cache_all:
            for slot in range(16):
                ser.write(make_frame(CMD_CAN_CACHE, bytes([slot])))
                ser.flush()
                ack = read_frame(ser, 1.0)
                print(decode_can_cache(ack) if ack else f"slot={slot:02d} no ack")
            return 0
        if args.snapshot:
            ser.write(make_frame(CMD_OBD_SNAPSHOT))
            ser.flush()
            ack = read_frame(ser, 1.0)
            print(decode_obd_snapshot(ack) if ack else "no ack")
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
            if args.poll_raw:
                ser.write(make_frame(CMD_CAN_RING_READ))
                ser.flush()
                frame = read_frame(ser, 0.2)
                decoded = decode_can_ring(frame) if frame else None
                if decoded:
                    print(decoded)
                    sys.stdout.flush()
                continue
            frame = read_frame(ser, 0.2)
            if not frame:
                continue
            decoded = decode_raw(frame)
            print(decoded if decoded else frame.hex(" "))
            sys.stdout.flush()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
