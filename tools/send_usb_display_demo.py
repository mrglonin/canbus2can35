#!/usr/bin/env python3
from __future__ import annotations

import argparse
import time

import serial


def checksum(frame: bytearray) -> int:
    return sum(frame[:-1]) & 0xFF


def frame(cmd: int, payload: bytes = b"") -> bytes:
    out = bytearray([0xBB, 0x41, 0xA1, 6 + len(payload), cmd])
    out.extend(payload)
    out.append(0)
    out[-1] = checksum(out)
    return bytes(out)


def text_frame(cmd: int, value: str) -> bytes:
    # Sportage.apk truncates display strings to 16 Java chars. Keep tests
    # explicit so car-side display behavior is easy to compare.
    value = value[:16]
    return frame(cmd, value.encode("utf-16le"))


def nav_maneuver(meters: int, icon: int) -> bytes:
    # Payload shape recovered from the APK's NaviReceiver / aw.B frame.
    tenths = min(9, (meters // 10) % 10)
    return bytes([
        icon & 0xFF,
        0x00,
        0x00,
        0x00,
        (meters >> 8) & 0xFF,
        meters & 0xFF,
        0x00,
        (tenths << 4) & 0xF0,
    ])


def eta_distance(tenths_km: int) -> bytes:
    whole = tenths_km // 10
    dec = tenths_km % 10
    return bytes([0x00, (whole >> 8) & 0xFF, whole & 0xFF, dec & 0xFF, 0x01])


def frames_for_scenario(scenario: str, item: dict) -> list[bytes]:
    nav_on = frame(0x48, b"\x01")
    nav_off = frame(0x48, b"\x00")
    nav_frames = [
        nav_on,
        frame(0x45, nav_maneuver(item["meters"], item["icon"])),
        frame(0x47, eta_distance(item["eta"])),
    ]
    media_frames = [text_frame(0x21, item["media"]), text_frame(0x22, item["track"])]
    scenarios = {
        "full": nav_frames + [text_frame(0x20, item["fm"])] + media_frames,
        "music": media_frames,
        "source": [text_frame(0x21, item["media"])],
        "track": [text_frame(0x22, item["track"])],
        "fm": [text_frame(0x20, item["fm"])],
        "nav": nav_frames,
        "nav-on": [nav_on],
        "nav-off": [nav_off],
        "clear": [nav_off, text_frame(0x20, ""), text_frame(0x21, ""), text_frame(0x22, "")],
    }
    if scenario not in scenarios:
        raise ValueError(f"unknown scenario: {scenario}")
    return scenarios[scenario]


def send_repeated(port: str, baud: int, seconds: float, gap: float, scenario: str) -> None:
    test_sets = [
        {
            "name": "text16",
            "fm": "FM TEST 1234567",
            "media": "MUSIC TEST 1234",
            "track": "TRACK TEST 1234",
            "meters": 80,
            "eta": 12,
            "icon": 0,
        },
        {
            "name": "long-truncate",
            "fm": "ABCDEFGHIJKLMNOPQ",
            "media": "MUSIC LONG VALUE",
            "track": "TRACK LONG VALUE",
            "meters": 150,
            "eta": 18,
            "icon": 1,
        },
        {
            "name": "nav",
            "fm": "FM NAV TEST",
            "media": "NAV MUSIC TEST",
            "track": "NAV TRACK TEST",
            "meters": 300,
            "eta": 25,
            "icon": 2,
        },
    ]

    with serial.Serial(port, baud, timeout=0.05, write_timeout=1) as ser:
        time.sleep(0.25)
        ser.reset_input_buffer()
        ser.reset_output_buffer()
        for item in test_sets:
            frames = frames_for_scenario(scenario, item)
            print(f"START {item['name']} scenario={scenario} repeat={seconds}s")
            deadline = time.monotonic() + seconds
            loops = 0
            while time.monotonic() < deadline:
                loops += 1
                for packet in frames:
                    ser.write(packet)
                    ser.flush()
                    time.sleep(gap)
            print(f"END {item['name']} loops={loops}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Send repeated Sportage display/nav/media test frames to 2CAN35 over USB CDC")
    parser.add_argument("port", nargs="?", default="/dev/cu.usbmodemKIA1")
    parser.add_argument("--baud", type=int, default=19200)
    parser.add_argument("--seconds", type=float, default=5.0)
    parser.add_argument("--gap", type=float, default=0.08)
    parser.add_argument(
        "--scenario",
        choices=["full", "music", "source", "track", "fm", "nav", "nav-on", "nav-off", "clear"],
        default="full",
    )
    args = parser.parse_args()
    send_repeated(args.port, args.baud, args.seconds, args.gap, args.scenario)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
