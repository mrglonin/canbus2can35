#!/usr/bin/env python3
import argparse
import math
import sys
import termios
import time
from pathlib import Path
from typing import Optional

import serial
import serial.tools.list_ports


REQ_DST = 0x41
REQ_SRC = 0xA1
CMD_UPDATE = 0x55
CMD_UID = 0x56
USB_IO_ERRORS = (serial.SerialException, OSError, termios.error)


def add_checksum(frame: bytearray) -> bytearray:
    length = frame[3]
    if length != len(frame):
        raise ValueError(f"bad frame length byte {length}, actual {len(frame)}")
    frame[length - 1] = sum(frame[: length - 1]) & 0xFF
    return frame


def make_short(command: int, value: int) -> bytes:
    return bytes(add_checksum(bytearray([0xBB, REQ_DST, REQ_SRC, 0x07, command, value, 0x00])))


def make_block(offset: int, chunk: bytes) -> bytes:
    if len(chunk) > 16:
        raise ValueError("block chunk is larger than 16 bytes")
    chunk = chunk.ljust(16, b"\xFF")
    frame = bytearray(25)
    frame[0:5] = bytes([0xBB, REQ_DST, REQ_SRC, 0x19, CMD_UPDATE])
    frame[5] = (offset >> 16) & 0xFF
    frame[6] = (offset >> 8) & 0xFF
    frame[7] = offset & 0xFF
    frame[8:24] = chunk
    return bytes(add_checksum(frame))


def checksum_ok(frame: bytes) -> bool:
    return len(frame) >= 5 and ((sum(frame[:-1]) & 0xFF) == frame[-1])


def read_frame(ser: serial.Serial, timeout: float) -> Optional[bytes]:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        ser.timeout = max(0.02, min(0.2, deadline - time.monotonic()))
        b = ser.read(1)
        if not b:
            continue
        if b[0] != 0xBB:
            continue

        rest = ser.read(3)
        if len(rest) != 3:
            continue
        frame = bytes([0xBB]) + rest
        length = frame[3]
        if length < 5 or length > 64:
            continue
        tail = ser.read(length - 4)
        if len(tail) != length - 4:
            continue
        frame += tail
        if checksum_ok(frame):
            return frame
        print(f"warn: bad checksum frame {frame.hex(' ')}", file=sys.stderr)
    return None


def wait_update_ack(ser: serial.Serial, wanted: int, timeout: float, label: str) -> bytes:
    deadline = time.monotonic() + timeout
    last = None
    while time.monotonic() < deadline:
        frame = read_frame(ser, max(0.05, deadline - time.monotonic()))
        if frame is None:
            break
        last = frame
        if len(frame) >= 7 and frame[4] == CMD_UPDATE and frame[5] == wanted:
            return frame
        print(f"info: ignoring frame while waiting {label}: {frame.hex(' ')}")
    detail = f"; last={last.hex(' ') if last else 'none'}"
    raise TimeoutError(f"no {label} ack {wanted:#04x}{detail}")


def query_uid(ser: serial.Serial, tries: int = 3) -> bytes:
    request = make_short(CMD_UID, 0x01)
    for _ in range(tries):
        ser.reset_input_buffer()
        ser.write(request)
        ser.flush()
        frame = read_frame(ser, 1.0)
        if frame and len(frame) >= 18 and frame[4] == CMD_UID:
            return frame[5:17]
    raise TimeoutError("adapter UID query failed")


def send_with_retry(ser: serial.Serial, frame: bytes, wanted_ack: int, label: str, retries: int, timeout: float) -> bytes:
    last_error = None
    for attempt in range(1, retries + 1):
        try:
            ser.reset_input_buffer()
            ser.write(frame)
            ser.flush()
        except USB_IO_ERRORS as exc:
            last_error = exc
            print(f"retry {attempt}/{retries}: write failed while waiting {label}: {exc}")
            time.sleep(0.25)
            continue
        try:
            return wait_update_ack(ser, wanted_ack, timeout, label)
        except (TimeoutError, *USB_IO_ERRORS) as exc:
            last_error = exc
            print(f"retry {attempt}/{retries}: {exc}")
    raise last_error


def open_adapter_port(port: str) -> serial.Serial:
    return serial.Serial(port, 19200, bytesize=8, parity="N", stopbits=1, timeout=0.2, write_timeout=1)


def wait_for_port(port: str, timeout: float = 8.0) -> str:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        devices = {p.device for p in serial.tools.list_ports.comports()}
        if port in devices:
            return port
        time.sleep(0.1)
    raise TimeoutError(f"serial port did not reappear: {port}")


def wait_for_port_cycle(port: str, timeout: float = 8.0) -> str:
    deadline = time.monotonic() + timeout
    saw_gone = False
    while time.monotonic() < deadline:
        devices = {p.device for p in serial.tools.list_ports.comports()}
        if port not in devices:
            saw_gone = True
        elif saw_gone:
            return port
        time.sleep(0.05)

    # Some macOS USB stacks keep the same BSD device node visible while the CDC
    # interface is settling. In that case use the visible port and let the start
    # handshake below prove whether the updater is ready.
    return wait_for_port(port, 2.0)


def enter_update_mode(port: str, package_uid: bytes, force: bool) -> str:
    start_frame = make_short(CMD_UPDATE, 0x01)

    with open_adapter_port(port) as ser:
        time.sleep(0.15)
        ser.reset_input_buffer()
        ser.reset_output_buffer()

        uid = query_uid(ser)
        print(f"adapter uid: {uid.hex(' ').upper()}")
        if uid != package_uid and not force:
            raise RuntimeError("UID mismatch; use --force only if you are certain this package belongs to this adapter")

        print("requesting update mode")
        try:
            ack = send_with_retry(ser, start_frame, 0x01, "start", retries=3, timeout=0.8)
            print(f"start ack before USB reset: {ack.hex(' ')}")
        except (TimeoutError, *USB_IO_ERRORS) as exc:
            print(f"start ack before USB reset not available: {exc}")

    port = wait_for_port_cycle(port)

    # After the USB CDC interface comes back, repeat the start command. The
    # Android updater does this as part of its reconnect path; without it the
    # first data block can be ignored by the adapter bootloader.
    for attempt in range(1, 6):
        try:
            with open_adapter_port(port) as ser:
                time.sleep(0.35)
                ser.reset_input_buffer()
                ser.reset_output_buffer()
                print(f"confirming update mode, start handshake {attempt}/5")
                ack = send_with_retry(ser, start_frame, 0x01, "start-after-reconnect", retries=2, timeout=1.0)
                print(f"start ack after reconnect: {ack.hex(' ')}")
                return port
        except (TimeoutError, *USB_IO_ERRORS) as exc:
            print(f"update start handshake {attempt}/5 failed: {exc}")
            wait_for_port_cycle(port, timeout=3.0)

    raise TimeoutError("adapter did not enter update mode after USB reconnect")


def update(port: str, firmware: Path, force: bool) -> None:
    data = firmware.read_bytes()
    if len(data) < 16:
        raise ValueError("firmware file is too small")

    package_uid = data[:12]
    package_version = data[12:16]
    total_blocks = math.ceil(len(data) / 16)
    print(f"file: {firmware}")
    print(f"size: {len(data)} bytes, blocks: {total_blocks}")
    print(f"package uid: {package_uid.hex(' ').upper()}")
    print(f"package version: {package_version.hex(' ').upper()}")

    port = enter_update_mode(port, package_uid, force)
    with open_adapter_port(port) as ser:
        time.sleep(0.3)
        ser.reset_input_buffer()
        ser.reset_output_buffer()

        last_pct = -1
        for offset in range(0, len(data), 16):
            frame = make_block(offset, data[offset : offset + 16])
            send_with_retry(ser, frame, 0x02, f"block {offset // 16 + 1}/{total_blocks}", retries=3, timeout=1.2)

            pct = int(((offset // 16 + 1) * 100) / total_blocks)
            if pct >= last_pct + 5 or pct == 100:
                print(f"progress: {pct}%")
                last_pct = pct

        print("sending finish")
        finish = make_short(CMD_UPDATE, 0x00)
        ser.reset_input_buffer()
        ser.write(finish)
        ser.flush()
        try:
            ack = wait_update_ack(ser, 0x00, 1.5, "finish")
            print(f"finish ack: {ack.hex(' ')}")
        except (TimeoutError, *USB_IO_ERRORS) as exc:
            print(f"finish ack not received before USB reset, APK also does not require waiting here: {exc}")

    print("done")


def main() -> int:
    parser = argparse.ArgumentParser(description="2CAN35/Sigma10 APK-compatible USB updater")
    parser.add_argument("port")
    parser.add_argument("firmware", type=Path)
    parser.add_argument("--force", action="store_true", help="skip UID mismatch protection")
    args = parser.parse_args()

    update(args.port, args.firmware, args.force)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
