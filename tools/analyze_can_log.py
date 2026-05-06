#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from collections import Counter, defaultdict
from pathlib import Path


GSUSB_RE = re.compile(r"(?P<ts>\d+\.\d+)\s+ch(?P<ch>\d+)\s+(?P<type>STD|EXT)\s+(?P<id>[0-9A-Fa-f]{8})\s+dlc=(?P<dlc>\d+)\s+(?P<data>[0-9A-Fa-f ]*)")
CDC_RE = re.compile(r"(?P<ts>\d+\.\d+)\s+bus=(?P<ch>\d+)\s+id=0x(?P<id>[0-9A-Fa-f]+).*dlc=(?P<dlc>\d+)\s+data=(?P<data>[0-9A-Fa-f]*)")


def parse_line(line: str):
    match = GSUSB_RE.search(line)
    if match:
        data = match.group("data").strip().replace(" ", "").upper()
        return (
            float(match.group("ts")),
            int(match.group("ch")),
            int(match.group("id"), 16),
            int(match.group("dlc")),
            bytes.fromhex(data) if data else b"",
        )
    match = CDC_RE.search(line)
    if match:
        data = match.group("data").strip().replace(" ", "").upper()
        return (
            float(match.group("ts")),
            int(match.group("ch")),
            int(match.group("id"), 16),
            int(match.group("dlc")),
            bytes.fromhex(data) if data else b"",
        )
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description="Summarize 2CAN35 CAN log IDs, rates and changing bytes")
    parser.add_argument("log", type=Path)
    parser.add_argument("--top", type=int, default=80)
    args = parser.parse_args()

    counts: Counter[tuple[int, int, int]] = Counter()
    samples: dict[tuple[int, int, int], bytes] = {}
    byte_values: dict[tuple[int, int, int], list[set[int]]] = defaultdict(lambda: [set() for _ in range(8)])
    first_ts = None
    last_ts = None
    total = 0

    with args.log.open("r", encoding="utf-8", errors="ignore") as f:
        for line in f:
            parsed = parse_line(line)
            if not parsed:
                continue
            ts, ch, can_id, dlc, data = parsed
            first_ts = ts if first_ts is None else min(first_ts, ts)
            last_ts = ts if last_ts is None else max(last_ts, ts)
            key = (ch, can_id, dlc)
            counts[key] += 1
            samples.setdefault(key, data)
            for i, value in enumerate(data[:8]):
                byte_values[key][i].add(value)
            total += 1

    duration = (last_ts - first_ts) if first_ts is not None and last_ts is not None else 0.0
    print(f"log={args.log}")
    print(f"frames={total} duration={duration:.3f}s")
    print()
    print("count   hz      ch  id       dlc  changing_bytes  sample")
    for key, count in counts.most_common(args.top):
        ch, can_id, dlc = key
        hz = count / duration if duration > 0 else 0.0
        changing = [str(i) for i, values in enumerate(byte_values[key]) if len(values) > 1]
        sample = " ".join(f"{b:02X}" for b in samples[key])
        print(f"{count:6d} {hz:7.2f}  {ch:<2d}  0x{can_id:03X}  {dlc:<3d}  {','.join(changing) or '-':<14s}  {sample}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
