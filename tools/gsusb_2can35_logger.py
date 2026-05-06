#!/usr/bin/env python3
import argparse
import signal
import struct
import sys
import time
from pathlib import Path
from typing import Optional

import usb.core
import usb.util
import usb.backend.libusb1


VID = 0x1D50
PID = 0x606F
EP_IN = 0x81
EP_OUT = 0x02

GS_USB_BREQ_HOST_FORMAT = 0
GS_USB_BREQ_BITTIMING = 1
GS_USB_BREQ_MODE = 2
GS_USB_BREQ_BT_CONST = 4
GS_USB_BREQ_DEVICE_CONFIG = 5
GS_USB_BREQ_2CAN35_EXIT_MODE1 = 0x7F

GS_CAN_MODE_RESET = 0
GS_CAN_MODE_START = 1

GS_CAN_FEATURE_LISTEN_ONLY = 1 << 0
GS_CAN_FEATURE_HW_TIMESTAMP = 1 << 4

CAN_EFF_FLAG = 0x80000000
CAN_RTR_FLAG = 0x40000000
CAN_ERR_FLAG = 0x20000000

DEFAULT_LIBUSB = Path("/Users/legion/Downloads/canbox-fw-lab/local-libusb/libusb-1.0.dylib")


def gsusb_bittiming(fclk_hz: int, bitrate: int) -> tuple[int, int, int, int, int]:
    # bxCAN timing used by the known-good 2CAN35 firmware: 18 time quanta/bit,
    # sample point at 14/18, SJW 2. For fclk=36 MHz this gives exact
    # 500/250/125/100 kbit/s with integer BRP values.
    tq = 18
    brp = fclk_hz // (bitrate * tq)
    if brp < 1 or fclk_hz != bitrate * tq * brp:
        raise ValueError(f"cannot make exact timing for {bitrate} bit/s from {fclk_hz} Hz")
    return (1, 12, 4, 2, brp)


def load_backend(path: Path):
    return usb.backend.libusb1.get_backend(find_library=lambda _name: str(path))


def ctrl_out(dev, request: int, value: int, data: bytes, index: int = 0):
    return dev.ctrl_transfer(0x41, request, value, index, data, timeout=1000)


def ctrl_in(dev, request: int, value: int, length: int, index: int = 0) -> bytes:
    return bytes(dev.ctrl_transfer(0xC1, request, value, index, length, timeout=1000))


def open_device(libusb_path: Path):
    backend = load_backend(libusb_path)
    if backend is None:
        raise RuntimeError(f"libusb backend not found: {libusb_path}")

    dev = usb.core.find(idVendor=VID, idProduct=PID, backend=backend)
    if dev is None:
        raise RuntimeError("budgetcan gs_usb 1d50:606f not found")

    dev.set_configuration()
    usb.util.claim_interface(dev, 0)
    return dev


def init_gsusb(dev, bitrate0: int, bitrate1: int):
    ctrl_out(dev, GS_USB_BREQ_HOST_FORMAT, 1, struct.pack("<I", 0x0000BEEF))

    raw_config = ctrl_in(dev, GS_USB_BREQ_DEVICE_CONFIG, 1, 12)
    reserved1, reserved2, reserved3, icount, sw_version, hw_version = struct.unpack("<BBBBII", raw_config)
    channels = icount + 1

    bt = ctrl_in(dev, GS_USB_BREQ_BT_CONST, 0, 40)
    feature, fclk, tseg1_min, tseg1_max, tseg2_min, tseg2_max, sjw_max, brp_min, brp_max, brp_inc = struct.unpack("<IIIIIIIIII", bt)

    if channels < 2:
        raise RuntimeError(f"expected 2 CAN channels, got {channels}")

    speeds = [bitrate0, bitrate1]
    for channel, bitrate in enumerate(speeds):
        ctrl_out(dev, GS_USB_BREQ_MODE, channel, struct.pack("<II", GS_CAN_MODE_RESET, 0))
        timing = gsusb_bittiming(fclk, bitrate)
        ctrl_out(dev, GS_USB_BREQ_BITTIMING, channel, struct.pack("<IIIII", *timing))
        mode_features = GS_CAN_FEATURE_LISTEN_ONLY | GS_CAN_FEATURE_HW_TIMESTAMP
        ctrl_out(dev, GS_USB_BREQ_MODE, channel, struct.pack("<II", GS_CAN_MODE_START, mode_features))

    return {
        "channels": channels,
        "sw_version": sw_version,
        "hw_version": hw_version,
        "feature": feature,
        "fclk": fclk,
        "speeds": speeds,
    }


def request_mode1_exit(dev) -> None:
    try:
        ctrl_out(dev, GS_USB_BREQ_2CAN35_EXIT_MODE1, 0, b"")
    except usb.core.USBError:
        # The patched logger resets immediately, so the control transfer often
        # fails from the host side because the device disconnects mid-request.
        pass


def format_frame(packet: bytes, monotonic_start: float) -> Optional[str]:
    if len(packet) < 20:
        return None

    echo_id, can_id, dlc, channel, flags, _reserved = struct.unpack_from("<IIBBBB", packet, 0)
    data = packet[12:12 + min(dlc, 8)]
    timestamp_us = None
    if len(packet) >= 24:
        timestamp_us = struct.unpack_from("<I", packet, 20)[0]

    frame_type = "ERR" if can_id & CAN_ERR_FLAG else ("EXT" if can_id & CAN_EFF_FLAG else "STD")
    rtr = bool(can_id & CAN_RTR_FLAG)
    clean_id = can_id & (0x1FFFFFFF if can_id & CAN_EFF_FLAG else 0x7FF)
    now = time.monotonic() - monotonic_start
    ts = f"{timestamp_us / 1000000:.6f}" if timestamp_us is not None else f"{now:.6f}"
    payload = "RTR" if rtr else data.hex(" ").upper()
    return f"{ts} ch{channel} {frame_type} {clean_id:08X} dlc={dlc} {payload}"


def main() -> int:
    parser = argparse.ArgumentParser(description="Read CAN logs from 2CAN35 gs_usb scanner firmware")
    parser.add_argument("--libusb", type=Path, default=DEFAULT_LIBUSB)
    parser.add_argument("--bitrate0", type=int, default=500000, help="channel 0 bitrate, default C-CAN 500 kbit/s")
    parser.add_argument("--bitrate1", type=int, default=100000, help="channel 1 bitrate, default M-CAN 100 kbit/s")
    parser.add_argument("--seconds", type=float, default=0, help="stop after N seconds, 0 means forever")
    parser.add_argument("--outfile", type=Path)
    parser.add_argument("--exit-to-mode1", action="store_true", help="send the patched logger exit request and stop")
    parser.add_argument("--exit-on-complete", action="store_true", help="return to mode 1 after timed logging or Ctrl-C")
    args = parser.parse_args()

    stop = False

    def on_signal(_signum, _frame):
        nonlocal stop
        stop = True

    signal.signal(signal.SIGINT, on_signal)
    signal.signal(signal.SIGTERM, on_signal)

    out = args.outfile.open("a", encoding="utf-8") if args.outfile else None
    dev = None
    try:
        dev = open_device(args.libusb)
        if args.exit_to_mode1:
            request_mode1_exit(dev)
            print("mode1 exit requested", flush=True)
            return 0

        info = init_gsusb(dev, args.bitrate0, args.bitrate1)
        header = (
            f"gs_usb ready channels={info['channels']} fclk={info['fclk']} "
            f"speeds=ch0:{info['speeds'][0]} ch1:{info['speeds'][1]} "
            f"feature=0x{info['feature']:X} sw={info['sw_version']} hw={info['hw_version']}"
        )
        print(header, flush=True)
        if out:
            print(header, file=out, flush=True)

        started = time.monotonic()
        while not stop:
            if args.seconds and time.monotonic() - started >= args.seconds:
                break
            try:
                packet = bytes(dev.read(EP_IN, 64, timeout=500))
            except usb.core.USBTimeoutError:
                continue
            line = format_frame(packet, started)
            if line:
                print(line, flush=True)
                if out:
                    print(line, file=out, flush=True)
    finally:
        if args.exit_on_complete and dev is not None:
            request_mode1_exit(dev)
        if out:
            out.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
