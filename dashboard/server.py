#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import glob
import json
import os
import queue
import re
import signal
import subprocess
import sys
import threading
import time
from collections import Counter, defaultdict, deque
from http import HTTPStatus
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

try:
    import serial
    from serial.tools import list_ports
except Exception:  # pragma: no cover - dashboard still serves sample logs
    serial = None
    list_ports = None

try:
    import usb.core
    import usb.backend.libusb1
except Exception:  # pragma: no cover - gs_usb status becomes unavailable
    usb = None


ROOT = Path(__file__).resolve().parents[1]
STATIC = Path(__file__).resolve().parent / "static"
MATRIX = ROOT / "data" / "can_function_matrix.csv"
DEFAULT_LOG = ROOT / "logs" / "car_can_cleanjump_20260506_220618.txt"
LIVE_LOG_DIR = ROOT / "logs" / "live"
TOOLS = ROOT / "tools"
GSUSB_LOGGER = TOOLS / "gsusb_2can35_logger.py"
CDC_LOGGER = TOOLS / "stockusb_canlog_2can35.py"
USB_MODE = TOOLS / "usb_mode_2can35.py"

REQ_DST = 0x41
REQ_SRC = 0xA1

GSUSB_RE = re.compile(
    r"(?P<ts>\d+\.\d+)\s+ch(?P<ch>\d+)\s+(?P<type>STD|EXT)\s+"
    r"(?P<id>[0-9A-Fa-f]{8})\s+dlc=(?P<dlc>\d+)\s+(?P<data>[0-9A-Fa-f ]*)"
)
CDC_RE = re.compile(
    r"(?P<ts>\d+\.\d+)\s+bus=(?P<ch>\d+)\s+id=0x(?P<id>[0-9A-Fa-f]+).*"
    r"dlc=(?P<dlc>\d+)\s+data=(?P<data>[0-9A-Fa-f]*)"
)


def now_ms() -> int:
    return int(time.time() * 1000)


def checksum(frame: bytes | bytearray) -> int:
    return sum(frame[:-1]) & 0xFF


def make_frame(cmd: int, payload: bytes = b"") -> bytes:
    out = bytearray([0xBB, REQ_DST, REQ_SRC, 6 + len(payload), cmd])
    out.extend(payload)
    out.append(0)
    out[-1] = checksum(out)
    return bytes(out)


def text_frame(cmd: int, value: str) -> bytes:
    return make_frame(cmd, value[:16].encode("utf-16le"))


def nav_maneuver(meters: int, icon: int) -> bytes:
    meters = max(0, min(9999, int(meters)))
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
    tenths_km = max(0, min(9999, int(tenths_km)))
    whole = tenths_km // 10
    dec = tenths_km % 10
    return bytes([0x00, (whole >> 8) & 0xFF, whole & 0xFF, dec & 0xFF, 0x01])


def display_frames(scenario: str, fm: str, media: str, track: str, meters: int, eta: int, icon: int) -> list[bytes]:
    nav_on = make_frame(0x48, b"\x01")
    nav_off = make_frame(0x48, b"\x00")
    nav_payload = [nav_on, make_frame(0x45, nav_maneuver(meters, icon)), make_frame(0x47, eta_distance(eta))]
    media_payload = [text_frame(0x21, media), text_frame(0x22, track)]

    scenarios: dict[str, list[bytes]] = {
        "full": nav_payload + [text_frame(0x20, fm)] + media_payload,
        "music": media_payload,
        "source": [text_frame(0x21, media)],
        "track": [text_frame(0x22, track)],
        "fm": [text_frame(0x20, fm)],
        "nav": nav_payload,
        "nav-on": [nav_on],
        "nav-off": [nav_off],
        "clear": [nav_off, text_frame(0x20, ""), text_frame(0x21, ""), text_frame(0x22, "")],
    }
    if scenario not in scenarios:
        raise ValueError(f"unknown display scenario: {scenario}")
    return scenarios[scenario]


def auto_cdc_port() -> str | None:
    candidates: list[str] = []
    if list_ports is not None:
        for port in list_ports.comports():
            text = " ".join(
                str(x)
                for x in [port.description, port.manufacturer, port.product, port.vid, port.pid]
                if x
            )
            if "STM" in text or "CDC" in text or "0483" in text or "5740" in text or "usbmodem" in port.device:
                candidates.append(port.device)
    candidates.extend(glob.glob("/dev/cu.usbmodem*"))
    seen: list[str] = []
    for item in candidates:
        if item not in seen:
            seen.append(item)
    return seen[0] if seen else None


def parse_frame_line(line: str) -> dict | None:
    match = GSUSB_RE.search(line)
    if match:
        data = match.group("data").strip().replace(" ", "").upper()
        can_id = int(match.group("id"), 16)
        return {
            "ts": float(match.group("ts")),
            "ch": int(match.group("ch")),
            "type": match.group("type"),
            "id": can_id,
            "id_hex": f"0x{can_id:03X}",
            "dlc": int(match.group("dlc")),
            "data": data,
            "data_spaced": " ".join(data[i : i + 2] for i in range(0, len(data), 2)),
            "source": "gs_usb",
        }
    match = CDC_RE.search(line)
    if match:
        data = match.group("data").strip().replace(" ", "").upper()
        can_id = int(match.group("id"), 16)
        return {
            "ts": float(match.group("ts")),
            "ch": int(match.group("ch")),
            "type": "STD",
            "id": can_id,
            "id_hex": f"0x{can_id:03X}",
            "dlc": int(match.group("dlc")),
            "data": data,
            "data_spaced": " ".join(data[i : i + 2] for i in range(0, len(data), 2)),
            "source": "cdc",
        }
    return None


def frame_bytes(frame: dict) -> bytes:
    try:
        return bytes.fromhex(frame.get("data", ""))
    except ValueError:
        return b""


class DashboardState:
    def __init__(self) -> None:
        self.lock = threading.Lock()
        self.recent: deque[dict] = deque(maxlen=500)
        self.counts: Counter[str] = Counter()
        self.latest_by_id: dict[int, dict] = {}
        self.byte_values: dict[str, list[set[int]]] = defaultdict(lambda: [set() for _ in range(8)])
        self.frames_total = 0
        self.started_ms = now_ms()
        self.last_frame_ms = 0
        self.session = {"running": False, "mode": "idle", "note": "stopped"}
        self.markers: deque[dict] = deque(maxlen=80)
        self._clients: list[queue.Queue] = []
        self._last_emit = 0.0

    def reset_runtime(self, mode: str, note: str) -> None:
        with self.lock:
            self.recent.clear()
            self.counts.clear()
            self.latest_by_id.clear()
            self.byte_values.clear()
            self.frames_total = 0
            self.started_ms = now_ms()
            self.last_frame_ms = 0
            self.session = {"running": True, "mode": mode, "note": note}
        self.broadcast({"type": "summary", "summary": self.summary()})

    def set_stopped(self, note: str = "stopped") -> None:
        with self.lock:
            self.session = {"running": False, "mode": self.session.get("mode", "idle"), "note": note}
        self.broadcast({"type": "summary", "summary": self.summary()})

    def add_client(self) -> queue.Queue:
        q: queue.Queue = queue.Queue(maxsize=100)
        with self.lock:
            self._clients.append(q)
        return q

    def remove_client(self, q: queue.Queue) -> None:
        with self.lock:
            if q in self._clients:
                self._clients.remove(q)

    def broadcast(self, event: dict) -> None:
        dead: list[queue.Queue] = []
        with self.lock:
            clients = list(self._clients)
        for client in clients:
            try:
                client.put_nowait(event)
            except queue.Full:
                dead.append(client)
        for client in dead:
            self.remove_client(client)

    def observe(self, frame: dict) -> None:
        key = f"ch{frame['ch']}:0x{frame['id']:03X}:{frame['dlc']}"
        data = frame_bytes(frame)
        with self.lock:
            frame["seen_ms"] = now_ms()
            self.recent.appendleft(frame)
            self.counts[key] += 1
            self.latest_by_id[frame["id"]] = frame
            self.frames_total += 1
            self.last_frame_ms = frame["seen_ms"]
            for idx, value in enumerate(data[:8]):
                self.byte_values[key][idx].add(value)

        current = time.monotonic()
        if current - self._last_emit >= 0.06:
            self._last_emit = current
            self.broadcast({"type": "frame", "frame": frame, "summary": self.summary()})

    def marker(self, name: str) -> None:
        item = {"name": name.strip()[:80] or "marker", "ms": now_ms()}
        with self.lock:
            self.markers.appendleft(item)
        self.broadcast({"type": "marker", "marker": item})

    def _latest(self, can_id: int) -> dict | None:
        return self.latest_by_id.get(can_id)

    def _raw_value(self, can_id: int) -> str:
        item = self._latest(can_id)
        return item["data_spaced"] if item else "no data"

    def _age(self, can_id: int) -> str:
        item = self._latest(can_id)
        if not item:
            return "-"
        return f"{max(0, (now_ms() - item['seen_ms']) / 1000):.1f}s"

    def derived_states_locked(self) -> list[dict]:
        speed = "-"
        f316 = self._latest(0x316)
        if f316:
            data = frame_bytes(f316)
            if len(data) >= 7:
                speed = f"{data[6]} km/h candidate"
            else:
                speed = self._raw_value(0x316)

        return [
            {
                "group": "Body",
                "items": [
                    {"label": "Doors / trunk / hood / sunroof", "source": "0x541 CGW1", "value": self._raw_value(0x541), "age": self._age(0x541), "status": "DBC candidate"},
                    {"label": "Rear doors / CGW2", "source": "0x553 CGW2", "value": self._raw_value(0x553), "age": self._age(0x553), "status": "DBC candidate"},
                    {"label": "Windows / heated wheel", "source": "0x559 CGW4", "value": self._raw_value(0x559), "age": self._age(0x559), "status": "DBC candidate"},
                    {"label": "Steering buttons", "source": "0x523 GW_SWRC_PE", "value": self._raw_value(0x523), "age": self._age(0x523), "status": "DBC candidate"},
                ],
            },
            {
                "group": "Motion",
                "items": [
                    {"label": "Speed", "source": "0x316 EMS11", "value": speed, "age": self._age(0x316), "status": "v05 uses 0x316"},
                    {"label": "Wheel speed", "source": "0x386 WHL_SPD11", "value": self._raw_value(0x386), "age": self._age(0x386), "status": "DBC candidate"},
                    {"label": "Gear / reverse", "source": "0x111 / 0x354 / 0x169", "value": f"{self._raw_value(0x111)} | {self._raw_value(0x354)} | {self._raw_value(0x169)}", "age": f"{self._age(0x111)} / {self._age(0x354)} / {self._age(0x169)}", "status": "needs log"},
                    {"label": "Steering angle", "source": "0x2B0 / 0x381 / 0x390", "value": f"{self._raw_value(0x2B0)} | {self._raw_value(0x381)} | {self._raw_value(0x390)}", "age": f"{self._age(0x2B0)} / {self._age(0x381)} / {self._age(0x390)}", "status": "DBC candidate"},
                ],
            },
            {
                "group": "Media / Navigation",
                "items": [
                    {"label": "HU source state", "source": "0x114 HU_CLU_PE_01", "value": self._raw_value(0x114), "age": self._age(0x114), "status": "seen"},
                    {"label": "HU nav state", "source": "0x197 HU_CLU_PE_05", "value": self._raw_value(0x197), "age": self._age(0x197), "status": "seen"},
                    {"label": "USB text transport", "source": "0x490 TP_HU_USB_CLU", "value": self._raw_value(0x490), "age": self._age(0x490), "status": "spam candidate"},
                    {"label": "FM / Navi text", "source": "0x4E8 / 0x49B / 0x4BB", "value": f"{self._raw_value(0x4E8)} | {self._raw_value(0x49B)} | {self._raw_value(0x4BB)}", "age": f"{self._age(0x4E8)} / {self._age(0x49B)} / {self._age(0x4BB)}", "status": "DBC candidate"},
                ],
            },
            {
                "group": "Parking / Safety",
                "items": [
                    {"label": "Parking sensors", "source": "0x436 PAS11", "value": self._raw_value(0x436), "age": self._age(0x436), "status": "seen"},
                    {"label": "SPAS / dynamic lines", "source": "0x390 SPAS11", "value": self._raw_value(0x390), "age": self._age(0x390), "status": "seen"},
                    {"label": "RCTA / blind spot", "source": "0x58B LCA11", "value": self._raw_value(0x58B), "age": self._age(0x58B), "status": "seen"},
                    {"label": "Climate", "source": "0x034 / 0x134 / 0x383", "value": f"{self._raw_value(0x034)} | {self._raw_value(0x134)} | {self._raw_value(0x383)}", "age": f"{self._age(0x034)} / {self._age(0x134)} / {self._age(0x383)}", "status": "DBC candidate"},
                ],
            },
        ]

    def summary(self) -> dict:
        with self.lock:
            elapsed = max(0.001, (now_ms() - self.started_ms) / 1000)
            top = []
            for key, count in self.counts.most_common(16):
                parts = key.split(":")
                changed = []
                for idx, values in enumerate(self.byte_values[key]):
                    if len(values) > 1:
                        changed.append(idx)
                top.append({
                    "key": key,
                    "channel": parts[0],
                    "id": parts[1],
                    "dlc": parts[2],
                    "count": count,
                    "hz": count / elapsed,
                    "changed": changed,
                })
            return {
                "session": dict(self.session),
                "frames_total": self.frames_total,
                "fps": self.frames_total / elapsed,
                "last_frame_age": (now_ms() - self.last_frame_ms) / 1000 if self.last_frame_ms else None,
                "recent": list(self.recent)[:120],
                "top": top,
                "states": self.derived_states_locked(),
                "markers": list(self.markers),
            }


STATE = DashboardState()


class LogRunner:
    def __init__(self) -> None:
        self.lock = threading.Lock()
        self.stop_event = threading.Event()
        self.thread: threading.Thread | None = None
        self.proc: subprocess.Popen | None = None
        self.raw_log_path: Path | None = None
        self.tx_control_path: Path | None = None
        self.raw_log_fh = None

    def stop(self) -> None:
        with self.lock:
            self.stop_event.set()
            if self.proc and self.proc.poll() is None:
                self.proc.terminate()
                try:
                    self.proc.wait(timeout=2)
                except subprocess.TimeoutExpired:
                    self.proc.kill()
            self.proc = None
            self.tx_control_path = None
            if self.raw_log_fh:
                self.raw_log_fh.flush()
                self.raw_log_fh.close()
                self.raw_log_fh = None
        STATE.set_stopped("stopped")

    def write_raw_marker(self, name: str) -> None:
        with self.lock:
            if not self.raw_log_fh:
                return
            stamp = time.strftime("%Y-%m-%d %H:%M:%S %z")
            self.raw_log_fh.write(f"# MARK {stamp} {name.strip()[:80] or 'marker'}\n")
            self.raw_log_fh.flush()

    def start_sample(self, path: Path, speed: float) -> None:
        self.stop()
        self.stop_event.clear()
        STATE.reset_runtime("sample", str(path))

        def run() -> None:
            previous_ts = None
            try:
                with path.open("r", encoding="utf-8", errors="ignore") as fh:
                    for line in fh:
                        if self.stop_event.is_set():
                            break
                        frame = parse_frame_line(line)
                        if not frame:
                            continue
                        if previous_ts is not None and speed > 0:
                            delay = max(0.0, min(0.25, (frame["ts"] - previous_ts) / speed))
                            time.sleep(delay)
                        previous_ts = frame["ts"]
                        STATE.observe(frame)
                STATE.set_stopped("sample complete")
            except Exception as exc:
                STATE.set_stopped(f"sample error: {exc}")

        self.thread = threading.Thread(target=run, name="sample-log", daemon=True)
        self.thread.start()

    def start_process(self, mode: str, command: list[str], *, tx_control_path: Path | None = None) -> None:
        self.stop()
        self.stop_event.clear()
        LIVE_LOG_DIR.mkdir(parents=True, exist_ok=True)
        raw_log_path = LIVE_LOG_DIR / f"{mode}_{time.strftime('%Y%m%d_%H%M%S')}.txt"
        STATE.reset_runtime(mode, f"{' '.join(command)} | raw={raw_log_path}")

        def run() -> None:
            try:
                with self.lock:
                    self.proc = subprocess.Popen(
                        command,
                        cwd=str(ROOT),
                        stdout=subprocess.PIPE,
                        stderr=subprocess.STDOUT,
                        text=True,
                        bufsize=1,
                    )
                    proc = self.proc
                    self.raw_log_path = raw_log_path
                    self.tx_control_path = tx_control_path
                    if self.tx_control_path is not None:
                        self.tx_control_path.parent.mkdir(parents=True, exist_ok=True)
                        self.tx_control_path.write_text("", encoding="utf-8")
                    self.raw_log_fh = raw_log_path.open("w", encoding="utf-8")
                    self.raw_log_fh.write(f"# COMMAND {' '.join(command)}\n")
                    if self.tx_control_path is not None:
                        self.raw_log_fh.write(f"# TX_CONTROL {self.tx_control_path}\n")
                    self.raw_log_fh.flush()
                assert proc.stdout is not None
                for line in proc.stdout:
                    if self.stop_event.is_set():
                        break
                    with self.lock:
                        if self.raw_log_fh:
                            self.raw_log_fh.write(line)
                    frame = parse_frame_line(line)
                    if frame:
                        STATE.observe(frame)
                    else:
                        STATE.marker(line.strip())
                code = proc.wait()
                with self.lock:
                    if self.raw_log_fh:
                        self.raw_log_fh.flush()
                        self.raw_log_fh.close()
                        self.raw_log_fh = None
                if not self.stop_event.is_set():
                    STATE.set_stopped(f"process exited: {code}; raw={raw_log_path}")
            except Exception as exc:
                STATE.set_stopped(f"process error: {exc}")

        self.thread = threading.Thread(target=run, name=f"{mode}-logger", daemon=True)
        self.thread.start()


RUNNER = LogRunner()


def clean_hex(value: str) -> str:
    text = str(value or "").replace(" ", "").replace(":", "").replace("-", "").strip()
    if len(text) % 2:
        raise ValueError("hex data must contain full bytes")
    if text and not re.fullmatch(r"[0-9A-Fa-f]+", text):
        raise ValueError("hex data contains non-hex characters")
    if len(text) > 16:
        raise ValueError("classic CAN data is limited to 8 bytes")
    return text.upper()


def parse_can_id(value) -> int:
    can_id = int(str(value or "0"), 0)
    if not 0 <= can_id <= 0x1FFFFFFF:
        raise ValueError("CAN id is out of range")
    return can_id


def append_tx_request(request: dict) -> Path:
    with RUNNER.lock:
        path = RUNNER.tx_control_path
        running = RUNNER.proc is not None and RUNNER.proc.poll() is None
    if path is None or not running:
        raise RuntimeError("start Live GS USB first; TX is sent through the active mode3 gs_usb session")
    with path.open("a", encoding="utf-8") as fh:
        fh.write(json.dumps(request, ensure_ascii=False, separators=(",", ":")) + "\n")
        fh.flush()
    return path


def build_tx_frame(payload: dict) -> dict:
    channel = int(payload.get("channel", 0))
    if channel not in {0, 1}:
        raise ValueError("channel must be 0 or 1")
    can_id = parse_can_id(payload.get("id", payload.get("can_id", "0")))
    extended = bool(payload.get("extended", False))
    if not extended and can_id > 0x7FF:
        raise ValueError("standard CAN id must be <= 0x7FF; enable extended for 29-bit ids")
    data = clean_hex(payload.get("data", ""))
    count = max(1, min(int(payload.get("count", 1)), 200))
    interval = max(0.0, min(float(payload.get("interval", 0.05)), 2.0))
    return {
        "channel": channel,
        "id": f"0x{can_id:X}",
        "data": data,
        "count": count,
        "interval": interval,
        "extended": extended,
        "rtr": bool(payload.get("rtr", False)),
    }


def run_send_can(payload: dict) -> dict:
    if payload.get("confirm") is not True:
        raise RuntimeError("TX confirm flag is required")
    frame = build_tx_frame(payload)
    path = append_tx_request(frame)
    STATE.marker(f"tx ch{frame['channel']} {frame['id']} {frame['data']} x{frame['count']}")
    return {"ok": True, "queued": 1, "frames": frame["count"], "tx_control": str(path)}


def run_sweep_can(payload: dict) -> dict:
    if payload.get("confirm") is not True:
        raise RuntimeError("TX confirm flag is required")
    base = build_tx_frame(payload)
    data = bytearray.fromhex(base["data"])
    data.extend(b"\x00" * (8 - len(data)))
    byte_index = int(payload.get("byte_index", 0))
    if not 0 <= byte_index <= 7:
        raise ValueError("byte index must be 0..7")
    start = int(str(payload.get("start", "0")), 0)
    end = int(str(payload.get("end", "0")), 0)
    if not 0 <= start <= 255 or not 0 <= end <= 255:
        raise ValueError("sweep values must be 0..255")
    step = 1 if end >= start else -1
    values = list(range(start, end + step, step))
    if len(values) > 64:
        raise ValueError("sweep is limited to 64 values per request")
    count_each = max(1, min(int(payload.get("count_each", 3)), 50))
    frames = []
    for value in values:
        item = dict(base)
        data[byte_index] = value
        item["data"] = data.hex().upper()
        item["count"] = count_each
        frames.append(item)
    path = append_tx_request({"frames": frames})
    STATE.marker(f"sweep ch{base['channel']} {base['id']} byte{byte_index} {start}->{end} frames={len(frames) * count_each}")
    return {"ok": True, "queued": len(frames), "frames": len(frames) * count_each, "tx_control": str(path)}


def read_json(handler: SimpleHTTPRequestHandler) -> dict:
    length = int(handler.headers.get("content-length", "0"))
    raw = handler.rfile.read(length) if length else b"{}"
    return json.loads(raw.decode("utf-8") or "{}")


def json_response(handler: SimpleHTTPRequestHandler, payload: dict | list, status: int = 200) -> None:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(body)))
    handler.end_headers()
    handler.wfile.write(body)


def list_serial_ports() -> list[dict]:
    ports = []
    if list_ports is not None:
        for port in list_ports.comports():
            ports.append({
                "device": port.device,
                "description": port.description,
                "manufacturer": port.manufacturer,
                "vid": f"{port.vid:04x}" if port.vid is not None else None,
                "pid": f"{port.pid:04x}" if port.pid is not None else None,
            })
    for path in glob.glob("/dev/cu.usbmodem*"):
        if not any(item["device"] == path for item in ports):
            ports.append({"device": path, "description": "usbmodem", "manufacturer": None, "vid": None, "pid": None})
    return ports


def gsusb_present() -> bool | None:
    if usb is None:
        return None
    try:
        backend_path = Path("/Users/legion/Downloads/canbox-fw-lab/local-libusb/libusb-1.0.dylib")
        backend = usb.backend.libusb1.get_backend(find_library=lambda _name: str(backend_path))
        return usb.core.find(idVendor=0x1D50, idProduct=0x606F, backend=backend) is not None
    except Exception:
        return None


def load_matrix() -> list[dict]:
    if not MATRIX.exists():
        return []
    with MATRIX.open("r", encoding="utf-8", newline="") as fh:
        return list(csv.DictReader(fh))


def run_send_display(payload: dict) -> dict:
    if serial is None:
        raise RuntimeError("pyserial is not available")
    port = payload.get("port") or auto_cdc_port()
    if not port:
        raise RuntimeError("no CDC port found")
    seconds = max(0.2, min(float(payload.get("seconds", 5.0)), 30.0))
    scenario = payload.get("scenario", "full")
    default_gap = 0.08 if str(scenario).startswith("nav") else 0.04
    gap = max(0.02, min(float(payload.get("gap", default_gap)), 0.5))
    fm = payload.get("fm", "FM TEST 101.7")
    media = payload.get("media", "MUSIC TEST")
    track = payload.get("track", "TRACK TEST")
    meters = int(payload.get("meters", 120))
    eta = int(payload.get("eta", 12))
    icon = int(payload.get("icon", 1))

    frames = display_frames(scenario, fm, media, track, meters, eta, icon)

    sent = 0
    with serial.Serial(port, 19200, timeout=0.05, write_timeout=1) as ser:
        time.sleep(0.15)
        ser.reset_input_buffer()
        ser.reset_output_buffer()
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            for packet in frames:
                ser.write(packet)
                ser.flush()
                sent += 1
                time.sleep(gap)
    STATE.marker(f"sent {scenario}: {sent} frames to {port}")
    return {"ok": True, "port": port, "scenario": scenario, "sent": sent}


class DashboardHandler(SimpleHTTPRequestHandler):
    def translate_path(self, path: str) -> str:
        parsed = urlparse(path)
        if parsed.path == "/":
            return str(STATIC / "index.html")
        return str(STATIC / parsed.path.lstrip("/"))

    def log_message(self, fmt: str, *args) -> None:
        sys.stderr.write(f"[dashboard] {fmt % args}\n")

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path == "/api/events":
            self.handle_events()
            return
        if parsed.path == "/api/status":
            json_response(self, {
                "ports": list_serial_ports(),
                "auto_port": auto_cdc_port(),
                "gsusb_present": gsusb_present(),
                "summary": STATE.summary(),
            })
            return
        if parsed.path == "/api/matrix":
            json_response(self, load_matrix())
            return
        if parsed.path == "/api/summary":
            json_response(self, STATE.summary())
            return
        super().do_GET()

    def do_POST(self) -> None:
        parsed = urlparse(self.path)
        try:
            payload = read_json(self)
            if parsed.path == "/api/log/start":
                mode = payload.get("mode", "sample")
                if mode == "sample":
                    path = Path(payload.get("path") or DEFAULT_LOG)
                    speed = float(payload.get("speed", 12.0))
                    RUNNER.start_sample(path, speed)
                    json_response(self, {"ok": True, "mode": mode, "path": str(path)})
                elif mode == "gsusb":
                    bitrate0 = str(int(payload.get("bitrate0", 100000)))
                    bitrate1 = str(int(payload.get("bitrate1", 500000)))
                    tx_control = LIVE_LOG_DIR / f"gsusb_tx_{time.strftime('%Y%m%d_%H%M%S')}.jsonl"
                    RUNNER.start_process(
                        "gsusb",
                        [
                            sys.executable,
                            str(GSUSB_LOGGER),
                            "--bitrate0",
                            bitrate0,
                            "--bitrate1",
                            bitrate1,
                            "--tx-control",
                            str(tx_control),
                        ],
                        tx_control_path=tx_control,
                    )
                    json_response(self, {"ok": True, "mode": mode, "tx_control": str(tx_control)})
                elif mode == "cdc":
                    port = payload.get("port") or auto_cdc_port()
                    if not port:
                        raise RuntimeError("no CDC port found")
                    RUNNER.start_process("cdc", [sys.executable, str(CDC_LOGGER), port])
                    json_response(self, {"ok": True, "mode": mode, "port": port})
                else:
                    json_response(self, {"ok": False, "error": f"unknown mode {mode}"}, HTTPStatus.BAD_REQUEST)
                return
            if parsed.path == "/api/log/stop":
                RUNNER.stop()
                json_response(self, {"ok": True})
                return
            if parsed.path == "/api/marker":
                name = payload.get("name", "marker")
                STATE.marker(name)
                RUNNER.write_raw_marker(name)
                json_response(self, {"ok": True})
                return
            if parsed.path == "/api/send/display":
                result = run_send_display(payload)
                json_response(self, result)
                return
            if parsed.path == "/api/can/send":
                result = run_send_can(payload)
                json_response(self, result)
                return
            if parsed.path == "/api/can/sweep":
                result = run_sweep_can(payload)
                json_response(self, result)
                return
            if parsed.path == "/api/mode":
                port = payload.get("port") or auto_cdc_port()
                mode = payload.get("mode")
                if mode not in {"normal", "update", "canlog", "logger", "log"}:
                    raise RuntimeError("valid mode is required")
                if mode == "normal" and not port and gsusb_present():
                    cmd = [sys.executable, str(GSUSB_LOGGER), "--exit-to-mode1"]
                else:
                    if not port:
                        raise RuntimeError("CDC port is required for this mode")
                    cmd = [sys.executable, str(USB_MODE), port, mode, "--no-wait"]
                result = subprocess.run(cmd, cwd=str(ROOT), text=True, capture_output=True, timeout=8)
                STATE.marker(f"mode {mode}: rc={result.returncode}")
                json_response(self, {"ok": result.returncode == 0, "stdout": result.stdout, "stderr": result.stderr})
                return
            json_response(self, {"ok": False, "error": "not found"}, HTTPStatus.NOT_FOUND)
        except Exception as exc:
            json_response(self, {"ok": False, "error": str(exc)}, HTTPStatus.BAD_REQUEST)

    def handle_events(self) -> None:
        q = STATE.add_client()
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream; charset=utf-8")
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Connection", "keep-alive")
        self.end_headers()

        def send(event: dict) -> None:
            payload = json.dumps(event, ensure_ascii=False)
            self.wfile.write(f"data: {payload}\n\n".encode("utf-8"))
            self.wfile.flush()

        try:
            send({"type": "summary", "summary": STATE.summary()})
            while True:
                try:
                    event = q.get(timeout=10)
                    send(event)
                except queue.Empty:
                    send({"type": "ping", "ms": now_ms()})
        except (BrokenPipeError, ConnectionResetError):
            pass
        finally:
            STATE.remove_client(q)


def main() -> int:
    parser = argparse.ArgumentParser(description="2CAN35 CAN Lab local dashboard")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()

    server = ThreadingHTTPServer((args.host, args.port), DashboardHandler)

    def stop(_signum, _frame) -> None:
        RUNNER.stop()
        server.shutdown()

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)
    print(f"2CAN35 dashboard: http://{args.host}:{args.port}", flush=True)
    server.serve_forever()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
