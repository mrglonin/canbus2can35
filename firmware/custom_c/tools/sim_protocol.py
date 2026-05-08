#!/usr/bin/env python3
from __future__ import annotations


def checksum(frame: bytes) -> int:
    return sum(frame) & 0xFF


def req(cmd: int, payload: bytes = b"") -> bytes:
    frame = bytearray([0xBB, 0x41, 0xA1, len(payload) + 1, cmd])
    frame.extend(payload)
    frame.append(checksum(frame))
    return bytes(frame)


def parse_resp(frame: bytes) -> tuple[int, bytes]:
    assert frame[0:3] == bytes([0xBB, 0xA1, 0x41])
    assert checksum(frame[:-1]) == frame[-1]
    payload_len = frame[3]
    payload = frame[4 : 4 + payload_len]
    return payload[0], payload[1:]


def main() -> int:
    version_req = req(0x56)
    mode3_req = req(0x55, b"\x03")
    raw_tx_req = req(0x70, bytes([1, 0x05, 0xF0, 8, 1, 2, 3, 4, 5, 6, 7, 8]))
    assert version_req.hex(" ").upper() == "BB 41 A1 01 56 F4"
    assert mode3_req.hex(" ").upper() == "BB 41 A1 02 55 03 F7"
    assert raw_tx_req[0:5] == bytes([0xBB, 0x41, 0xA1, 0x0D, 0x70])

    sample_resp = bytes.fromhex(
        "BB A1 41 13 56 37 FF DA 05 42 47 30 38 59 41 22 43 04 35 10 00 01 00"
    )
    sample_resp += bytes([checksum(sample_resp)])
    cmd, payload = parse_resp(sample_resp)
    assert cmd == 0x56
    assert payload[0:12].hex().upper() == "37FFDA054247303859412243"
    assert payload[12:16].hex().upper() == "04351000"
    print("protocol simulation ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
