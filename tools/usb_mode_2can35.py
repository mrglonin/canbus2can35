#!/usr/bin/env python3
import argparse
import time

import serial

from usb_update_2can35 import CMD_UPDATE, make_short, open_adapter_port, read_frame, wait_for_port_cycle


MODES = {
    "update": 0x01,
    "loader": 0x01,
    "log": 0x03,
    "logger": 0x03,
    "canlog": 0x03,
    "reset": 0x04,
    "normal": 0x04,
}


def switch_mode(port: str, mode: str, wait: bool) -> None:
    value = MODES[mode]
    frame = make_short(CMD_UPDATE, value)
    print(f"sending CMD 0x55 value 0x{value:02x} to {port}")
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
    if wait:
        wait_for_port_cycle(port, timeout=8.0)
        print(f"port cycled: {port}")


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
