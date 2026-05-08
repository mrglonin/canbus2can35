#!/usr/bin/env python3
import argparse
import json
import signal
import struct
import sys
import threading
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

def bundled_libusb_path() -> Path:
    try:
        import libusb_package

        return Path(libusb_package.get_library_path())
    except Exception:
        return Path("/Users/legion/Downloads/canbox-fw-lab/local-libusb/libusb-1.0.dylib")


DEFAULT_LIBUSB = bundled_libusb_path()


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


def init_gsusb(dev, bitrate0: int, bitrate1: int, *, listen_only: bool = True):
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
        mode_features = GS_CAN_FEATURE_HW_TIMESTAMP
        if listen_only:
            mode_features |= GS_CAN_FEATURE_LISTEN_ONLY
        ctrl_out(dev, GS_USB_BREQ_MODE, channel, struct.pack("<II", GS_CAN_MODE_START, mode_features))

    return {
        "channels": channels,
        "sw_version": sw_version,
        "hw_version": hw_version,
        "feature": feature,
        "fclk": fclk,
        "speeds": speeds,
        "listen_only": listen_only,
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


def parse_hex_data(value: str) -> bytes:
    clean = value.replace(" ", "").replace(":", "").replace("-", "").strip()
    if len(clean) % 2:
        raise ValueError("CAN data hex must contain full bytes")
    data = bytes.fromhex(clean) if clean else b""
    if len(data) > 8:
        raise ValueError("classic CAN data cannot be longer than 8 bytes")
    return data


def pack_host_frame(
    channel: int,
    can_id: int,
    data: bytes,
    *,
    echo_id: int,
    extended: bool = False,
    rtr: bool = False,
) -> bytes:
    if not 0 <= channel <= 1:
        raise ValueError("channel must be 0 or 1")
    if extended:
        if not 0 <= can_id <= 0x1FFFFFFF:
            raise ValueError("extended CAN id must fit 29 bits")
        wire_id = can_id | CAN_EFF_FLAG
    else:
        if not 0 <= can_id <= 0x7FF:
            raise ValueError("standard CAN id must fit 11 bits")
        wire_id = can_id
    if rtr:
        wire_id |= CAN_RTR_FLAG
    payload = data[:8].ljust(8, b"\x00")
    return struct.pack("<IIBBBB8sI", echo_id & 0xFFFFFFFF, wire_id, len(data), channel, 0, 0, payload, 0)


def send_can_frame(
    dev,
    write_lock: threading.Lock,
    *,
    channel: int,
    can_id: int,
    data: bytes,
    count: int = 1,
    interval: float = 0.02,
    extended: bool = False,
    rtr: bool = False,
    echo_seed: int = 1,
) -> int:
    count = max(1, min(int(count), 1000))
    interval = max(0.0, min(float(interval), 2.0))
    sent = 0
    for idx in range(count):
        packet = pack_host_frame(channel, can_id, data, echo_id=echo_seed + idx, extended=extended, rtr=rtr)
        with write_lock:
            dev.write(EP_OUT, packet, timeout=1000)
        sent += 1
        if interval and idx + 1 < count:
            time.sleep(interval)
    return sent


def send_tx_request(dev, write_lock: threading.Lock, request: dict, echo_seed: int) -> int:
    frames = request.get("frames")
    if frames is None:
        frames = [request]
    total = 0
    for offset, item in enumerate(frames):
        channel = int(item.get("channel", request.get("channel", 0)))
        can_id_raw = item.get("id", item.get("can_id", request.get("id", "0")))
        can_id = int(str(can_id_raw), 0)
        data = parse_hex_data(str(item.get("data", request.get("data", ""))))
        count = int(item.get("count", request.get("count", 1)))
        interval = float(item.get("interval", request.get("interval", 0.02)))
        extended = bool(item.get("extended", request.get("extended", False)))
        rtr = bool(item.get("rtr", request.get("rtr", False)))
        total += send_can_frame(
            dev,
            write_lock,
            channel=channel,
            can_id=can_id,
            data=data,
            count=count,
            interval=interval,
            extended=extended,
            rtr=rtr,
            echo_seed=echo_seed + offset * 0x1000,
        )
    return total


def start_tx_control_thread(
    dev,
    control_path: Path,
    write_lock: threading.Lock,
    should_stop,
) -> threading.Thread:
    control_path.parent.mkdir(parents=True, exist_ok=True)
    control_path.touch(exist_ok=True)

    def run() -> None:
        echo_seed = 1
        with control_path.open("r", encoding="utf-8") as fh:
            fh.seek(0, 2)
            while not should_stop():
                line = fh.readline()
                if not line:
                    time.sleep(0.05)
                    continue
                try:
                    request = json.loads(line)
                    sent = send_tx_request(dev, write_lock, request, echo_seed)
                    echo_seed = (echo_seed + sent + 1) & 0xFFFFFFFF
                    print(
                        f"TX sent={sent} request={json.dumps(request, ensure_ascii=False, separators=(',', ':'))}",
                        flush=True,
                    )
                except Exception as exc:
                    print(f"TX error: {exc}", flush=True)

    thread = threading.Thread(target=run, name="gsusb-tx-control", daemon=True)
    thread.start()
    return thread


def main() -> int:
    parser = argparse.ArgumentParser(description="Read CAN logs from 2CAN35 gs_usb scanner firmware")
    parser.add_argument("--libusb", type=Path, default=DEFAULT_LIBUSB)
    parser.add_argument("--bitrate0", type=int, default=500000, help="channel 0 bitrate, default C-CAN 500 kbit/s")
    parser.add_argument("--bitrate1", type=int, default=100000, help="channel 1 bitrate, default M-CAN 100 kbit/s")
    parser.add_argument("--seconds", type=float, default=0, help="stop after N seconds, 0 means forever")
    parser.add_argument("--outfile", type=Path)
    parser.add_argument("--exit-to-mode1", action="store_true", help="send the patched logger exit request and stop")
    parser.add_argument("--exit-on-complete", action="store_true", help="return to mode 1 after timed logging or Ctrl-C")
    parser.add_argument("--tx-enable", action="store_true", help="start CAN channels without listen-only")
    parser.add_argument("--tx-control", type=Path, help="tail JSONL control file and transmit requested CAN frames")
    parser.add_argument("--send-frame", action="store_true", help="send one frame request and exit")
    parser.add_argument("--send-channel", type=int, default=0)
    parser.add_argument("--send-id", default="0x000")
    parser.add_argument("--send-data", default="")
    parser.add_argument("--send-count", type=int, default=1)
    parser.add_argument("--send-interval", type=float, default=0.02)
    parser.add_argument("--send-extended", action="store_true")
    parser.add_argument("--send-rtr", action="store_true")
    args = parser.parse_args()

    stop = False

    def on_signal(_signum, _frame):
        nonlocal stop
        stop = True

    signal.signal(signal.SIGINT, on_signal)
    signal.signal(signal.SIGTERM, on_signal)

    out = args.outfile.open("a", encoding="utf-8") if args.outfile else None
    dev = None
    write_lock = threading.Lock()
    try:
        dev = open_device(args.libusb)
        if args.exit_to_mode1:
            request_mode1_exit(dev)
            print("mode1 exit requested", flush=True)
            return 0

        tx_mode = args.tx_enable or args.tx_control is not None or args.send_frame
        info = init_gsusb(dev, args.bitrate0, args.bitrate1, listen_only=not tx_mode)
        if args.send_frame:
            sent = send_can_frame(
                dev,
                write_lock,
                channel=args.send_channel,
                can_id=int(str(args.send_id), 0),
                data=parse_hex_data(args.send_data),
                count=args.send_count,
                interval=args.send_interval,
                extended=args.send_extended,
                rtr=args.send_rtr,
            )
            print(f"TX sent={sent}", flush=True)
            return 0

        if args.tx_control is not None:
            start_tx_control_thread(dev, args.tx_control, write_lock, lambda: stop)

        header = (
            f"gs_usb ready channels={info['channels']} fclk={info['fclk']} "
            f"speeds=ch0:{info['speeds'][0]} ch1:{info['speeds'][1]} "
            f"listen_only={info['listen_only']} feature=0x{info['feature']:X} "
            f"sw={info['sw_version']} hw={info['hw_version']}"
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
