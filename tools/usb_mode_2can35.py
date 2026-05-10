#!/usr/bin/env python3
import argparse
import time

import serial

from usb_update_2can35 import CMD_UPDATE, make_short, open_adapter_port, read_frame, wait_for_port_cycle


MODES = {
    "update": (CMD_UPDATE, 0x01),
    "loader": (CMD_UPDATE, 0x01),
    "log": (0x51, 0x03),
    "logger": (0x51, 0x03),
    "canlog": (0x51, 0x03),
    "reset": (CMD_UPDATE, 0x04),
    "normal": (CMD_UPDATE, 0x04),
}


def switch_mode(port: str, mode: str, wait: bool) -> None:
    command, value = MODES[mode]
    frame = make_short(command, value)
    print(f"sending CMD 0x{command:02x} value 0x{value:02x} to {port}")
    with open_adapter_port(port) as ser:
        time.sleep(0.15)
        ser.reset_input_buffer()
        ser.reset_output_buffer()
        ser.write(frame)
        ser.flush()
        try:
            ack = read_frame(ser, 0.6)
            if ack:
                print(f"ack/frame: {ack.hex(' ')}")
            else:
                print("no ack before USB reset; this is normal for direct mode switch")
        except (serial.SerialException, OSError) as exc:
            print(f"USB reset while switching mode: {exc}")
    if wait and command == CMD_UPDATE:
        wait_for_port_cycle(port, timeout=8.0)
        print(f"port cycled: {port}")
    elif wait:
        print("mode switch command sent; mode3 USB identity depends on the flashed mode3 payload")


def main() -> int:
    parser = argparse.ArgumentParser(description="Switch 2CAN35 v4 firmware modes over USB")
    parser.add_argument("port")
    parser.add_argument("mode", choices=sorted(MODES))
    parser.add_argument("--no-wait", action="store_true")
    args = parser.parse_args()
    switch_mode(args.port, args.mode, wait=not args.no_wait)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
