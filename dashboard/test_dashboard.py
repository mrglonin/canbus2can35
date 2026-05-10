#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
STATIC = Path(__file__).resolve().parent / "static"


def read_url(url: str, timeout: float = 4.0):
    with urllib.request.urlopen(url, timeout=timeout) as response:
        raw = response.read()
    if url.endswith(".json"):
        return json.loads(raw.decode("utf-8"))
    return raw.decode("utf-8")


def post_json(url: str, payload: dict, timeout: float = 6.0) -> dict:
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def assert_static_contract() -> None:
    html = (STATIC / "index.html").read_text(encoding="utf-8")
    js = (STATIC / "app.js").read_text(encoding="utf-8")
    css = (STATIC / "styles.css").read_text(encoding="utf-8")
    ids = set(re.findall(r'id="([^"]+)"', html))
    refs = set(re.findall(r'\$\("([^"]+)"\)', js))
    missing = sorted(refs - ids)
    assert not missing, f"JS references missing HTML ids: {missing}"
    for text in [
        "2CAN35 Lab Console",
        "Динамика",
        "Live CAN+UART",
        "План сета",
        "Режимы адаптера",
        "Приборка",
        "CAN sweep",
    ]:
        assert text in html, f"missing visible copy: {text}"
    for selector in [".main-grid", ".car-stage", ".plan-list", ".quick-command-grid", "@media"]:
        assert selector in css, f"missing CSS selector: {selector}"


def assert_server_contract(base_url: str) -> None:
    status = json.loads(read_url(f"{base_url}/api/status"))
    assert "summary" in status, "status missing summary"
    commands = json.loads(read_url(f"{base_url}/api/commands"))
    assert len(commands["actions"]) >= 59, "too few learn actions"
    assert len(commands["safe_uart_tests"]) >= 23, "too few UART test commands"
    assert len(commands["uart_candidate_tests"]) >= 120, "too few UART candidate commands"
    assert len(commands["hu_to_canbox_examples"]) >= 8, "too few HU examples"
    assert len(commands["can_matrix"]) >= 100, "CAN matrix not loaded"
    assert len(commands["demo_scenarios"]) >= 5, "demo scenarios not loaded"
    for required_id in ["sunroof", "reverse_on", "version"]:
        assert any(item["id"] == required_id for item in commands["safe_uart_tests"]), f"missing UART command {required_id}"
    for required_id in ["fd_body_40", "fd_hu_usb_2s", "2e_key_14_press", "2e_90_empty"]:
        assert any(item["id"] == required_id for item in commands["uart_candidate_tests"]), f"missing UART candidate {required_id}"
    export = json.loads(read_url(f"{base_url}/api/export"))
    assert "commands" in export and "learned_assignments" in export, "export bundle incomplete"


def assert_learning_flow(base_url: str) -> None:
    start = post_json(f"{base_url}/api/log/start", {"mode": "sample", "speed": 120})
    assert start.get("ok") is True, start
    time.sleep(0.35)
    learn = post_json(f"{base_url}/api/learn/start", {"action_id": "door_driver", "name": "Дверь водительская"})
    assert learn.get("ok") is True, learn
    time.sleep(0.35)
    stopped = post_json(f"{base_url}/api/learn/stop", {})
    result = stopped["result"]
    assert result["frames"] > 0, "learning captured no frames"
    assert len(result["candidates"]) > 0, "learning returned no candidates"
    post_json(f"{base_url}/api/log/stop", {})


def main() -> int:
    parser = argparse.ArgumentParser(description="Dashboard API/static smoke tests")
    parser.add_argument("--url", default="http://127.0.0.1:8765")
    args = parser.parse_args()
    base_url = args.url.rstrip("/")

    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import server  # noqa: PLC0415

    decoded = server.decode_raise_frame(bytes.fromhex("FD0A09160000000002002B"))
    assert decoded["valid"], decoded
    assert "USB music" in decoded["text"], decoded
    decoded_2e = server.decode_canbox_uart_frame(bytes.fromhex("2E20021401C8"))
    assert decoded_2e["valid"], decoded_2e
    assert decoded_2e["fields"]["protocol"] == "canbox_2e", decoded_2e
    assert "кнопка" in decoded_2e["text"], decoded_2e

    assert_static_contract()
    assert_server_contract(base_url)
    assert_learning_flow(base_url)
    print("dashboard tests ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
