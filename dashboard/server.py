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
        STATE.set_stopped("stopped")

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

    def start_process(self, mode: str, command: list[str]) -> None:
        self.stop()
        self.stop_event.clear()
        STATE.reset_runtime(mode, " ".join(command))

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
                assert proc.stdout is not None
                for line in proc.stdout:
                    if self.stop_event.is_set():
                        break
                    frame = parse_frame_line(line)
                    if frame:
                        STATE.observe(frame)
                    else:
                        STATE.marker(line.strip())
                code = proc.wait()
                if not self.stop_event.is_set():
                    STATE.set_stopped(f"process exited: {code}")
            except Exception as exc:
                STATE.set_stopped(f"process error: {exc}")

        self.thread = threading.Thread(target=run, name=f"{mode}-logger", daemon=True)
        self.thread.start()


RUNNER = LogRunner()


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
    gap = max(0.02, min(float(payload.get("gap", 0.04)), 0.5))
    scenario = payload.get("scenario", "full")
    fm = payload.get("fm", "FM TEST 101.7")
    media = payload.get("media", "MUSIC TEST")
    track = payload.get("track", "TRACK TEST")
    meters = int(payload.get("meters", 120))
    eta = int(payload.get("eta", 12))
    icon = int(payload.get("icon", 1))

    frames: list[bytes]
    if scenario == "music":
        frames = [text_frame(0x21, media), text_frame(0x22, track)]
    elif scenario == "fm":
        frames = [text_frame(0x20, fm)]
    elif scenario == "nav":
        frames = [make_frame(0x48, b"\x01"), make_frame(0x45, nav_maneuver(meters, icon)), make_frame(0x47, eta_distance(eta))]
    else:
        frames = [
            make_frame(0x48, b"\x01"),
            make_frame(0x45, nav_maneuver(meters, icon)),
            make_frame(0x47, eta_distance(eta)),
            text_frame(0x20, fm),
            text_frame(0x21, media),
            text_frame(0x22, track),
        ]

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
                    RUNNER.start_process(
                        "gsusb",
                        [sys.executable, str(GSUSB_LOGGER), "--bitrate0", bitrate0, "--bitrate1", bitrate1],
                    )
                    json_response(self, {"ok": True, "mode": mode})
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
                STATE.marker(payload.get("name", "marker"))
                json_response(self, {"ok": True})
                return
            if parsed.path == "/api/send/display":
                result = run_send_display(payload)
                json_response(self, result)
                return
            if parsed.path == "/api/mode":
                port = payload.get("port") or auto_cdc_port()
                mode = payload.get("mode")
                if not port or mode not in {"normal", "update", "canlog", "logger", "log"}:
                    raise RuntimeError("port and valid mode are required")
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
