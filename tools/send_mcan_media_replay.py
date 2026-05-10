#!/usr/bin/env python3
from __future__ import annotations

import argparse
import sys
import threading
import time
from pathlib import Path
from typing import Union

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from gsusb_2can35_logger import (  # noqa: E402
    DEFAULT_LIBUSB,
    init_gsusb,
    open_device,
    parse_hex_data,
    request_mode1_exit,
    send_can_frame,
)


FrameSpec = Union[tuple[int, str, float], tuple[int, int, str, float]]


FRAME_SETS: dict[str, list[FrameSpec]] = {
    # Conservative M-CAN replay. These are status/transport candidates from the
    # Hyundai/Kia M-CAN DBC and our captures. They are sent on ch0 by default.
    "usb": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 1.00),
        (0x490, "01001020000000", 0.10),
        (0x490, "01001040000000", 0.10),
        (0x4E6, "49C4000083838B00", 0.10),
        (0x4E6, "49C4000083838C00", 0.10),
    ],
    "fm": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 1.00),
        (0x4E8, "49C4000083838B00", 0.10),
        (0x4E8, "49C4000083838C00", 0.10),
    ],
    "bt": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 1.00),
        (0x4EE, "49C4000083838B00", 0.10),
        (0x4EE, "49C4000083838C00", 0.10),
        (0x485, "49C4000083838B00", 0.25),
    ],
    "carplay": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 1.00),
        (0x4F2, "0000C00000000001", 0.10),
        (0x4F2, "0001C00000003001", 0.10),
    ],
    "android": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 1.00),
        (0x4F4, "0000C00000000001", 0.10),
        (0x4F4, "0001C00000003001", 0.10),
    ],
    "compass": [
        (0x197, "1000000000000000", 0.50),
        (0x1E6, "0000000000000000", 0.25),
        (0x1E7, "0000000000000000", 0.50),
    ],
    "nav": [
        (0x197, "1000000000000000", 0.50),
        (0x115, "0100000000780000", 0.10),
        (0x4BB, "49C4000083838B00", 0.10),
        (0x49B, "49C4000083838C00", 0.10),
        (0x1E6, "0000010001050000", 0.25),
        (0x1E7, "007800F001680000", 0.50),
    ],
    # Mixed physical replay follows the channels observed in the current car
    # capture: HU status on ch0/100k, high-rate transport candidates on ch1/500k.
    # This is still conservative: no gear, brake, steering, EPB, door lock or
    # climate command IDs are transmitted here.
    "mixed-usb": [
        (0, 0x114, "0B21FFFFFFFFE10F", 0.20),
        (0, 0x197, "1000000000000000", 1.00),
        (1, 0x4E6, "49C4000083838B00", 0.10),
        (1, 0x4E6, "49C4000083838C00", 0.10),
        (1, 0x490, "01001020000000", 0.10),
        (1, 0x490, "01001040000000", 0.10),
    ],
    "mixed-clear": [
        (0, 0x114, "0B21FFFFFFFFE10F", 0.20),
        (0, 0x197, "1000000000000000", 1.00),
        (1, 0x4E6, "8080000080800000", 0.10),
        (1, 0x490, "00000820000000", 0.10),
    ],
    "mixed-log-window": [
        (0, 0x114, "0B21FFFFFFFFE10F", 0.20),
        (0, 0x197, "1000000000000000", 1.00),
        (1, 0x4E5, "8052000249C40000", 0.10),
        (1, 0x4E6, "49C4000084848B00", 0.10),
        (1, 0x4E6, "49C4000084848A00", 0.10),
        (1, 0x4E7, "2A74000800840000", 0.10),
        (1, 0x490, "00000820000000", 0.10),
        (1, 0x492, "00000000AD8078E4", 0.20),
        (1, 0x4F1, "20008811", 0.20),
    ],
}


def run_replay(
    *,
    scenario: str,
    channel: int,
    bitrate0: int,
    bitrate1: int,
    seconds: float,
    libusb: Path,
    exit_to_mode1: bool,
) -> int:
    frames = FRAME_SETS[scenario]
    dev = open_device(libusb)
    info = init_gsusb(dev, bitrate0, bitrate1, listen_only=False)
    print(
        f"gs_usb tx ready speeds=ch0:{info['speeds'][0]} ch1:{info['speeds'][1]} "
        f"scenario={scenario} tx_channel={channel}",
        flush=True,
    )

    write_lock = threading.Lock()
    last_sent = [0.0 for _ in frames]
    deadline = time.monotonic() + seconds
    sent = 0
    try:
        while time.monotonic() < deadline:
            now = time.monotonic()
            for idx, spec in enumerate(frames):
                if len(spec) == 4:
                    frame_channel, can_id, data_hex, period = spec
                else:
                    can_id, data_hex, period = spec
                    frame_channel = channel
                if now - last_sent[idx] < period:
                    continue
                send_can_frame(
                    dev,
                    write_lock,
                    channel=frame_channel,
                    can_id=can_id,
                    data=parse_hex_data(data_hex),
                    count=1,
                    interval=0,
                    echo_seed=(sent + 1) & 0xFFFFFFFF,
                )
                last_sent[idx] = now
                sent += 1
            time.sleep(0.005)
    finally:
        if exit_to_mode1:
            request_mode1_exit(dev)
    print(f"TX done scenario={scenario} frames={sent}", flush=True)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Replay conservative M-CAN media/nav frames through mode3 gs_usb")
    parser.add_argument("scenario", choices=sorted(FRAME_SETS), nargs="?", default="usb")
    parser.add_argument("--channel", type=int, default=0, help="default ch0 = 100k M-CAN in our wiring")
    parser.add_argument("--bitrate0", type=int, default=100000)
    parser.add_argument("--bitrate1", type=int, default=500000)
    parser.add_argument("--seconds", type=float, default=8.0)
    parser.add_argument("--libusb", type=Path, default=DEFAULT_LIBUSB)
    parser.add_argument("--exit-to-mode1", action="store_true")
    args = parser.parse_args()
    return run_replay(
        scenario=args.scenario,
        channel=args.channel,
        bitrate0=args.bitrate0,
        bitrate1=args.bitrate1,
        seconds=args.seconds,
        libusb=args.libusb,
        exit_to_mode1=args.exit_to_mode1,
    )


if __name__ == "__main__":
    raise SystemExit(main())
