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
RAISE_MATRIX = ROOT / "data" / "raise_rzc_korea_uart_matrix.csv"
LEARNED_ASSIGNMENTS = ROOT / "data" / "learned_assignments.jsonl"
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
LAB_SLCAN_RE = re.compile(
    r"^(?P<ch>[01])\s+(?P<kind>[tT])(?P<id>[0-9A-Fa-f]{3}|[0-9A-Fa-f]{8})"
    r"(?P<dlc>[0-8])(?P<data>[0-9A-Fa-f]*)"
)
UART_RE = re.compile(r"^U\s+(?P<data>[0-9A-Fa-f ]+)")
GSUSB_UART_RE = re.compile(r"(?P<ts>\d+\.\d+)\s+uart\s+(?P<data>[0-9A-Fa-f ]+)")

LEARN_ACTIONS = [
    {"id": "baseline", "group": "0. База", "name": "Покой 10 секунд", "hint": "ничего не нажимай, это шумовая база"},
    {"id": "ign_acc_on_off", "group": "1. Кузов", "name": "Зажигание ACC/IGN", "hint": "ACC -> IGN -> OFF, без запуска"},
    {"id": "door_driver", "group": "1. Кузов", "name": "Дверь водительская", "hint": "открыть/закрыть 3 раза"},
    {"id": "door_passenger", "group": "1. Кузов", "name": "Дверь пассажира", "hint": "открыть/закрыть 3 раза"},
    {"id": "door_rear_left", "group": "1. Кузов", "name": "Дверь задняя левая", "hint": "открыть/закрыть 3 раза"},
    {"id": "door_rear_right", "group": "1. Кузов", "name": "Дверь задняя правая", "hint": "открыть/закрыть 3 раза"},
    {"id": "trunk", "group": "1. Кузов", "name": "Багажник", "hint": "открыть/закрыть отдельно от дверей"},
    {"id": "hood", "group": "1. Кузов", "name": "Капот", "hint": "открыть/закрыть, не смешивать с дверью"},
    {"id": "sunroof", "group": "1. Кузов", "name": "Люк", "hint": "закрыт -> открыт -> закрыт 3 раза"},
    {"id": "lock_unlock", "group": "1. Кузов", "name": "Закрытие/открытие замков", "hint": "lock/unlock с ключа и кнопки отдельно"},
    {"id": "lights_low_auto", "group": "2. Свет", "name": "AUTO / ближний свет", "hint": "AUTO -> ближний -> AUTO/OFF"},
    {"id": "turn_left", "group": "2. Свет", "name": "Левый поворотник", "hint": "3-5 циклов"},
    {"id": "turn_right", "group": "2. Свет", "name": "Правый поворотник", "hint": "3-5 циклов"},
    {"id": "hazard", "group": "2. Свет", "name": "Аварийка", "hint": "включить/выключить 3 раза"},
    {"id": "reverse", "group": "3. Движение", "name": "Задний ход", "hint": "P -> R -> P на тормозе"},
    {"id": "steering_angle", "group": "3. Движение", "name": "Руль", "hint": "центр -> лево -> центр -> право -> центр"},
    {"id": "brake", "group": "3. Движение", "name": "Педаль тормоза", "hint": "нажать/отпустить 5 раз"},
    {"id": "parking_brake", "group": "3. Движение", "name": "EPB / ручник", "hint": "включить/выключить"},
    {"id": "climate_auto", "group": "4. Климат", "name": "Климат AUTO", "hint": "AUTO on/off 3 раза"},
    {"id": "climate_power", "group": "4. Климат", "name": "Климат ON/OFF", "hint": "общая кнопка климата и OFF отдельно"},
    {"id": "driver_temp", "group": "4. Климат", "name": "Температура водитель", "hint": "ниже/выше несколько шагов"},
    {"id": "passenger_temp", "group": "4. Климат", "name": "Температура пассажир", "hint": "ниже/выше несколько шагов"},
    {"id": "fan_speed", "group": "4. Климат", "name": "Скорость вентилятора", "hint": "0 -> max -> 0"},
    {"id": "air_modes", "group": "4. Климат", "name": "Лицо/ноги/стекло", "hint": "каждый режим отдельно"},
    {"id": "ac_button", "group": "4. Климат", "name": "A/C", "hint": "включить/выключить 5 раз"},
    {"id": "front_defog", "group": "4. Климат", "name": "Обдув лобового", "hint": "включить/выключить"},
    {"id": "rear_defog", "group": "4. Климат", "name": "Обогрев заднего стекла", "hint": "включить/выключить"},
    {"id": "heated_wheel", "group": "4. Климат", "name": "Подогрев руля", "hint": "включить/выключить, потом StarLine отдельно"},
    {"id": "driver_seat_heat", "group": "5. Сиденья", "name": "Обогрев водитель", "hint": "1 -> 2 -> 3 -> off, 3 цикла"},
    {"id": "passenger_seat_heat", "group": "5. Сиденья", "name": "Обогрев пассажир", "hint": "1 -> 2 -> 3 -> off, 3 цикла"},
    {"id": "driver_seat_vent", "group": "5. Сиденья", "name": "Вентиляция водитель", "hint": "1 -> 2 -> 3 -> off, 3 цикла"},
    {"id": "passenger_seat_vent", "group": "5. Сиденья", "name": "Вентиляция пассажир", "hint": "1 -> 2 -> 3 -> off, 3 цикла"},
    {"id": "front_parking", "group": "6. Парковка", "name": "Передние парктроники", "hint": "включить, подойти к датчикам"},
    {"id": "rear_parking", "group": "6. Парковка", "name": "Задние парктроники/RCTA", "hint": "R, препятствие сзади/слева/справа"},
    {"id": "auto_hold", "group": "7. Кнопки тоннеля", "name": "Auto Hold", "hint": "нажать 5 раз"},
    {"id": "drive_mode", "group": "7. Кнопки тоннеля", "name": "Drive Mode", "hint": "переключить по кругу"},
    {"id": "hill_descent", "group": "7. Кнопки тоннеля", "name": "Спуск с горы", "hint": "on/off"},
    {"id": "lock_button", "group": "7. Кнопки тоннеля", "name": "Lock", "hint": "on/off"},
    {"id": "uart_raise_buttons", "group": "8. UART Raise", "name": "Кнопки руля/пианино через Raise", "hint": "нажимать каждую кнопку отдельно; нужен UART bridge"},
    {"id": "hu_media_usb", "group": "8. UART Raise", "name": "Магнитола USB media", "hint": "включить USB источник на TEYES"},
    {"id": "hu_media_bt", "group": "8. UART Raise", "name": "Магнитола BT music", "hint": "включить BT music"},
    {"id": "hu_radio_fm", "group": "8. UART Raise", "name": "Магнитола FM/AM", "hint": "FM, AM, смена станции"},
    {"id": "hu_navigation", "group": "8. UART Raise", "name": "Навигация", "hint": "маневр/улица/компас"},
    {"id": "seatbelt_driver", "group": "9. Доп. кузов", "name": "Ремень водителя", "hint": "пристегнуть/отстегнуть отдельно от дверей"},
    {"id": "seatbelt_passenger", "group": "9. Доп. кузов", "name": "Ремень пассажира", "hint": "пристегнуть/отстегнуть, если есть датчик пассажира"},
    {"id": "windows_each", "group": "9. Доп. кузов", "name": "Стеклоподъемники", "hint": "каждое окно отдельно: открыть/закрыть"},
    {"id": "high_beam", "group": "9. Доп. свет", "name": "Дальний / моргание", "hint": "дальний on/off и краткое моргание отдельно"},
    {"id": "front_fog", "group": "9. Доп. свет", "name": "Передние туманки", "hint": "включить/выключить"},
    {"id": "wipers_front", "group": "9. Доп. свет", "name": "Дворники передние", "hint": "mist, auto, low, high по одному режиму"},
    {"id": "wipers_rear", "group": "9. Доп. свет", "name": "Задний дворник", "hint": "on/off, если есть"},
    {"id": "rpm_idle_steps", "group": "10. Двигатель", "name": "Обороты двигателя", "hint": "холостой, 1000, 1500, 2000 без движения"},
    {"id": "speed_low", "group": "10. Двигатель", "name": "Скорость 0/5/20/40", "hint": "короткий безопасный проезд для проверки скорости"},
    {"id": "outside_temp", "group": "10. Двигатель", "name": "Наружная температура", "hint": "пассивно, отдельное действие не нужно"},
    {"id": "engine_temp", "group": "10. Двигатель", "name": "Температура двигателя", "hint": "пассивно после прогрева"},
    {"id": "blind_spot_left_right", "group": "11. Безопасность", "name": "Слепые зоны L/R", "hint": "поймать лампы/индикаторы слева и справа"},
    {"id": "rcta_left_right", "group": "11. Безопасность", "name": "RCTA задний поперечный трафик", "hint": "R, объект слева/справа сзади"},
    {"id": "tpms_warning", "group": "11. Безопасность", "name": "TPMS / давление", "hint": "пассивно, если есть предупреждение"},
    {"id": "cluster_settings", "group": "12. Настройки", "name": "Настройки приборки", "hint": "менять по одному пункту: двери, свет, единицы, ассистенты"},
    {"id": "hu_car_settings", "group": "12. Настройки", "name": "Настройки авто с TEYES", "hint": "проверить, какие команды HU отправляет в canbox"},
]

ACTION_PROFILES = {
    "ign_acc_on_off": {"ids": {0x541}, "keys": {"ignition"}},
    "door_driver": {"ids": {0x541}, "keys": {"door_lf"}},
    "door_passenger": {"ids": {0x541}, "keys": {"door_rf"}},
    "door_rear_left": {"ids": {0x553}, "keys": {"door_lr"}},
    "door_rear_right": {"ids": {0x553}, "keys": {"door_rr"}},
    "trunk": {"ids": {0x541}, "keys": {"trunk"}},
    "hood": {"ids": {0x541}, "keys": {"hood"}},
    "sunroof": {"ids": {0x541}, "keys": {"sunroof"}},
    "reverse": {"ids": {0x111}, "keys": {"reverse_111"}},
    "front_defog": {"ids": {0x043, 0x132}, "keys": {"front_defog_up_043", "front_defog_up_132"}},
    "rear_defog": {"ids": {0x541}, "labels": {"Обогрев заднего стекла"}},
    "heated_wheel": {"ids": {0x559}, "keys": {"heated_wheel"}},
    "driver_seat_heat": {"ids": {0x134, 0x4E5, 0x5CF}, "keys": {"seat_heat_vent_134", "driver_seat_heat_4e5"}},
    "passenger_seat_heat": {"ids": {0x134, 0x5CF}, "keys": {"seat_heat_vent_134"}},
    "driver_seat_vent": {"ids": {0x134, 0x5CF}, "keys": {"seat_heat_vent_134"}},
    "passenger_seat_vent": {"ids": {0x134, 0x5CF}, "keys": {"seat_heat_vent_134"}},
    "rpm_idle_steps": {"ids": {0x316}, "keys": {"rpm_316"}},
    "speed_low": {"ids": {0x316, 0x386}, "keys": {"speed_candidate"}},
    "outside_temp": {"ids": {0x383}, "keys": {"outside_temp"}},
    "engine_temp": {"ids": {0x329}, "keys": {"engine_temp"}},
    "front_parking": {"ids": {0x436, 0x390, 0x4F4}, "keys": {"parking_sensors"}},
    "rear_parking": {"ids": {0x436, 0x390, 0x4F4, 0x58B}, "keys": {"parking_sensors"}},
    "auto_hold": {"ids": {0x507}, "labels": {"Auto Hold"}},
}

LEARN_TARGET_REPEATS = 5
BODY_UART_KEYS = {"door_lf", "door_rf", "door_lr", "door_rr", "trunk", "hood", "sunroof"}
BRIDGE_UART_KEYS = BODY_UART_KEYS | {"reverse_111"}


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


def raise_checksum(length: int, cmd: int, payload: bytes) -> int:
    return (length + cmd + sum(payload)) & 0xFFFF


def raise_frame(cmd: int, payload: bytes = b"") -> str:
    length = len(payload) + 4
    checksum_value = raise_checksum(length, cmd, payload)
    frame = bytes([0xFD, length, cmd]) + payload + bytes([(checksum_value >> 8) & 0xFF, checksum_value & 0xFF])
    return frame.hex(" ").upper()


def format_hms(seconds: int) -> str:
    seconds = max(0, int(seconds))
    h = seconds // 3600
    m = (seconds % 3600) // 60
    s = seconds % 60
    return f"{h}:{m:02d}:{s:02d}" if h else f"{m}:{s:02d}"


def radio_band_name(value: int) -> str:
    return {
        0x00: "FM",
        0x01: "FM",
        0x02: "FM",
        0x03: "AM",
    }.get(value, f"band 0x{value:02X}")


def decode_utf16_payload(payload: bytes) -> str | None:
    if len(payload) < 2 or len(payload) % 2:
        return None
    try:
        text = payload.decode("utf-16le", errors="strict").replace("\x00", "").strip()
    except UnicodeDecodeError:
        return None
    return text if text and any(ch.isprintable() for ch in text) else None


def decode_raise_frame(data: bytes) -> dict:
    if len(data) < 5 or data[0] != 0xFD:
        return {"raw": data.hex(" ").upper(), "valid": False, "text": "not Raise/RZC FD frame"}
    length = data[1]
    cmd = data[2]
    payload = data[3:-2]
    got = (data[-2] << 8) | data[-1]
    want = raise_checksum(length, cmd, payload)
    text = f"cmd 0x{cmd:02X}, payload {payload.hex(' ').upper() or '-'}"
    fields: dict[str, object] = {
        "cmd_int": cmd,
        "length": length,
        "payload_len": len(payload),
    }
    if cmd == 0x01 and payload:
        temp = payload[0] & 0x7F
        if payload[0] & 0x80:
            temp = -temp
        text = f"наружная температура {temp}C"
        fields.update({"kind": "outside_temp", "temperature_c": temp})
    elif cmd == 0x02 and len(payload) >= 2:
        key_names = {
            0x00: "release",
            0x10: "mute",
            0x11: "source/mode",
            0x12: "seek up",
            0x13: "seek down",
            0x14: "volume up",
            0x15: "volume down",
            0x16: "phone accept",
            0x17: "phone hangup",
            0x18: "panel power",
            0x19: "panel volume up",
            0x1A: "panel volume down",
            0x1B: "panel FM/AM",
            0x1C: "panel media",
            0x1D: "panel phone",
            0x1E: "panel display",
            0x1F: "panel seek up",
            0x20: "panel seek down",
        }
        key_name = key_names.get(payload[0], f"key 0x{payload[0]:02X}")
        text = f"кнопка {key_name}, status {payload[1]}"
        fields.update({"kind": "key", "key_code": f"0x{payload[0]:02X}", "key_name": key_name, "status": payload[1]})
    elif cmd == 0x03 and len(payload) >= 4:
        text = f"климат L={payload[0]} R={payload[1]} fan={payload[2]} flags=0x{payload[3]:02X}"
        fields.update({"kind": "climate", "left_raw": payload[0], "right_raw": payload[1], "fan": payload[2], "flags": f"0x{payload[3]:02X}"})
    elif cmd == 0x03:
        text = f"HU setting/status: {payload.hex(' ').upper() or '-'}"
        fields.update({"kind": "hu_setting"})
    elif cmd == 0x04 and len(payload) >= 2:
        text = f"парктроники front=0x{payload[0]:02X} rear=0x{payload[1]:02X}"
        fields.update({"kind": "parking_radar", "front_raw": f"0x{payload[0]:02X}", "rear_raw": f"0x{payload[1]:02X}"})
    elif cmd == 0x04 and len(payload) == 1:
        state = "start/on" if payload[0] == 0x00 else "end/off" if payload[0] == 0x01 else f"0x{payload[0]:02X}"
        text = f"HU питание/session {state}"
        fields.update({"kind": "hu_power", "state": state})
    elif cmd == 0x05 and payload:
        labels = []
        for bit, name in [
            (0x01, "дверь водитель"),
            (0x02, "дверь пассажир"),
            (0x04, "задняя левая"),
            (0x08, "задняя правая"),
            (0x10, "багажник"),
            (0x20, "капот"),
            (0x40, "люк candidate"),
        ]:
            if payload[0] & bit:
                labels.append(name)
        text = "кузов: " + (", ".join(labels) if labels else "все закрыто")
        fields.update({"kind": "body_or_volume", "body_flags": f"0x{payload[0]:02X}", "body_labels": labels, "volume_candidate": payload[0]})
    elif cmd == 0x06 and len(payload) == 3:
        text = f"время HU {payload[1]:02d}:{payload[0]:02d}, mode {payload[2]}"
        fields.update({"kind": "hu_time", "minute": payload[0], "hour": payload[1], "mode": payload[2], "time": f"{payload[1]:02d}:{payload[0]:02d}"})
    elif cmd == 0x07 and len(payload) >= 2:
        fade = int(payload[0]) - 7
        balance = int(payload[1]) - 7
        text = f"аудио баланс fade={fade}, balance={balance}"
        fields.update({"kind": "audio_balance", "fade": fade, "balance": balance})
    elif cmd == 0x08 and len(payload) >= 3:
        bass = int(payload[0]) - 10
        mid = int(payload[1]) - 10
        treble = int(payload[2]) - 10
        text = f"эквалайзер bass={bass}, mid={mid}, treble={treble}"
        fields.update({"kind": "sound_effects", "bass": bass, "mid": mid, "treble": treble})
    elif cmd == 0x09 and payload:
        source_names = {
            0x02: "радио",
            0x06: "навигация",
            0x07: "Bluetooth phone",
            0x0B: "Bluetooth connect",
            0x11: "Bluetooth music",
            0x12: "AUX",
            0x16: "USB music",
            0x80: "media off",
            0x83: "other media",
        }
        source_name = source_names.get(payload[0], f"0x{payload[0]:02X}")
        text = f"источник HU: {source_name}"
        fields.update({"kind": "hu_media", "source_code": f"0x{payload[0]:02X}", "source_name": source_name})
        if payload[0] == 0x02 and len(payload) >= 4:
            band = radio_band_name(payload[1])
            freq_raw = payload[2] * 100 + payload[3]
            freq = f"{freq_raw / 100:.2f}" if band.startswith("FM") else str(freq_raw)
            text += f", {band} {freq}"
            fields.update({"band_code": f"0x{payload[1]:02X}", "band": band, "frequency": freq, "frequency_raw": freq_raw})
        if payload[0] == 0x16 and len(payload) >= 6:
            track = (payload[1] << 8) | payload[2]
            seconds = payload[3] * 3600 + payload[4] * 60 + payload[5]
            text += f", track={track}, time={format_hms(seconds)}"
            fields.update({"track": track, "play_time_s": seconds, "play_time": format_hms(seconds)})
        elif payload[0] in (0x06, 0x07, 0x0B, 0x11, 0x12, 0x80, 0x83) and len(payload) >= 2:
            text += f", state=0x{payload[1]:02X}"
            fields.update({"state_code": f"0x{payload[1]:02X}"})
    elif cmd == 0x7D and len(payload) >= 2 and payload[0] == 0x06:
        text = "задний ход " + ("on" if payload[1] == 0x02 else "off")
        fields.update({"kind": "reverse", "reverse": payload[1] == 0x02})
    elif cmd == 0x86 and len(payload) == 2:
        text = f"HU keepalive/status 0x86 {payload.hex(' ').upper()}"
        fields.update({"kind": "hu_keepalive", "status_raw": payload.hex(" ").upper()})
    elif cmd == 0xEE:
        text = f"HU extended/status 0xEE: {payload.hex(' ').upper() or '-'}"
        fields.update({"kind": "hu_extended"})
    else:
        utf16 = decode_utf16_payload(payload)
        if utf16:
            text = f"текст UTF-16LE: {utf16}"
            fields.update({"kind": "text", "text_value": utf16})
    checksum_ok = got == want and len(data) == length + 1
    zero_checksum_quirk = cmd == 0x86 and len(payload) == 2 and got == 0 and len(data) == length + 1
    return {
        "raw": data.hex(" ").upper(),
        "valid": checksum_ok or zero_checksum_quirk,
        "cmd": f"0x{cmd:02X}",
        "payload": payload.hex(" ").upper(),
        "checksum": f"got=0x{got:04X} want=0x{want:04X}" + (" accepted_zero" if zero_checksum_quirk else ""),
        "text": text,
        "fields": fields,
    }


def spaced_hex(value: str) -> str:
    clean = value.replace(" ", "").replace(":", "").replace("-", "").strip().upper()
    return " ".join(clean[i : i + 2] for i in range(0, len(clean), 2))


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


DISPLAY_CAN_FRAME_SETS: dict[str, list[tuple[int, str, float]]] = {
    "source": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 1.00),
        (0x490, "01001020000000", 0.10),
        (0x490, "01001040000000", 0.10),
    ],
    "music": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 1.00),
        (0x490, "01001020000000", 0.10),
        (0x490, "01001040000000", 0.10),
        (0x4E6, "49C4000083838B00", 0.10),
        (0x4E6, "49C4000083838C00", 0.10),
    ],
    "track": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 1.00),
        (0x4E6, "49C4000083838B00", 0.10),
        (0x4E6, "49C4000083838C00", 0.10),
    ],
    "fm": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 1.00),
        (0x4E8, "49C4000083838B00", 0.10),
        (0x4E8, "49C4000083838C00", 0.10),
    ],
    "nav": [
        (0x197, "1000000000000000", 0.50),
        (0x115, "0100000000780000", 0.10),
        (0x4BB, "49C4000083838B00", 0.10),
        (0x49B, "49C4000083838C00", 0.10),
        (0x1E6, "0000010001050000", 0.25),
        (0x1E7, "007800F001680000", 0.50),
    ],
    "clear": [
        (0x114, "0B21FFFFFFFFE10F", 0.20),
        (0x197, "1000000000000000", 0.50),
        (0x490, "00000820000000", 0.10),
        (0x4E6, "8080000080800000", 0.10),
    ],
}
DISPLAY_CAN_FRAME_SETS["full"] = (
    DISPLAY_CAN_FRAME_SETS["nav"]
    + DISPLAY_CAN_FRAME_SETS["fm"]
    + DISPLAY_CAN_FRAME_SETS["music"]
)


def display_can_channels(bus: str) -> list[int]:
    value = str(bus or "mcan").lower()
    if value in {"mcan", "m-can", "0", "ch0"}:
        return [0]
    if value in {"ccan", "c-can", "1", "ch1"}:
        return [1]
    if value in {"both", "all", "оба"}:
        return [0, 1]
    raise ValueError("display bus must be mcan, ccan or both")


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
    match = LAB_SLCAN_RE.search(line.strip())
    if match:
        data = match.group("data").strip().replace(" ", "").upper()
        can_id = int(match.group("id"), 16)
        frame_type = "EXT" if match.group("kind") == "T" else "STD"
        return {
            "ts": time.time(),
            "ch": int(match.group("ch")),
            "type": frame_type,
            "id": can_id,
            "id_hex": f"0x{can_id:03X}",
            "dlc": int(match.group("dlc"), 16),
            "data": data,
            "data_spaced": " ".join(data[i : i + 2] for i in range(0, len(data), 2)),
            "source": "lab_cdc",
        }
    return None


def parse_uart_line(line: str) -> dict | None:
    items = parse_uart_lines(line)
    return items[0] if items else None


class RaiseStreamParser:
    def __init__(self) -> None:
        self.buf = bytearray()

    def feed(self, data: bytes) -> list[bytes]:
        self.buf.extend(data)
        frames: list[bytes] = []
        while self.buf:
            if self.buf[0] != 0xFD:
                next_fd = self.buf.find(0xFD)
                if next_fd < 0:
                    self.buf.clear()
                    break
                del self.buf[:next_fd]
            if len(self.buf) < 2:
                break
            length = self.buf[1]
            total = length + 1
            if length < 4 or total > 128:
                del self.buf[0]
                continue
            if len(self.buf) < total:
                break
            candidate = bytes(self.buf[:total])
            got = (candidate[-2] << 8) | candidate[-1]
            want = raise_checksum(candidate[1], candidate[2], candidate[3:-2])
            if got != want:
                next_fd = self.buf.find(0xFD, 1)
                if 0 < next_fd < total:
                    del self.buf[:next_fd]
                    continue
            frames.append(candidate)
            del self.buf[:total]
        return frames


def split_raise_frames(data: bytes) -> list[bytes]:
    frames: list[bytes] = []
    i = 0
    while i < len(data):
        if data[i] != 0xFD:
            i += 1
            continue
        if i + 1 >= len(data):
            break
        end = i + data[i + 1] + 1
        if end > len(data):
            break
        frames.append(data[i:end])
        i = end
    return frames or ([data] if data else [])


def parse_uart_lines(line: str, parser: RaiseStreamParser | None = None) -> list[dict]:
    stripped = line.strip()
    match = UART_RE.search(stripped)
    source = "raise_uart"
    ts = time.time()
    if not match:
        match = GSUSB_UART_RE.search(stripped)
        source = "gs_usb_uart"
        if match:
            try:
                ts = float(match.group("ts"))
            except ValueError:
                ts = time.time()
    if not match:
        return None
    raw = match.group("data").replace(" ", "")
    try:
        data = bytes.fromhex(raw)
    except ValueError:
        return []
    out = []
    frames = parser.feed(data) if parser is not None else split_raise_frames(data)
    for frame in frames:
        decoded = decode_raise_frame(frame)
        out.append({
            "ts": ts,
            "ms": now_ms(),
            "raw": decoded["raw"],
            "text": decoded["text"],
            "valid": decoded["valid"],
            "cmd": decoded.get("cmd"),
            "payload": decoded.get("payload"),
            "checksum": decoded.get("checksum"),
            "fields": decoded.get("fields", {}),
            "source": source,
        })
    return out


def frame_bytes(frame: dict) -> bytes:
    try:
        return bytes.fromhex(frame.get("data", ""))
    except ValueError:
        return b""


def on_off(value: bool, on_text: str = "включено", off_text: str = "выключено") -> str:
    return on_text if value else off_text


def open_closed(value: bool) -> str:
    return "открыто" if value else "закрыто"


def raw_data_text(data: bytes) -> str:
    return data.hex(" ").upper()


def datc_seat_heat_vent_text(data: bytes) -> str:
    raw = raw_data_text(data)
    if len(data) < 8:
        return raw
    return (
        f"{raw} | "
        f"b0={data[0]:02X} b1={data[1]:02X} b3={data[3]:02X} "
        f"b4={data[4]:02X} b5={data[5]:02X} b6={data[6]:02X}"
    )


def driver_seat_heat_candidate_text(data: bytes, source_id: int) -> str:
    if source_id == 0x4E5 and len(data) >= 4:
        return f"candidate 0x4E5: DATA[1]={data[1]:02X}, DATA[3]={data[3]:02X}"
    return raw_data_text(data)


def temp_c_from_half_offset(raw: int) -> str:
    return f"{(raw * 0.5) - 40:.1f} C"


def temp_c_from_offset(raw: int) -> str:
    return f"{raw - 40} C"


def rpm_from_ems11(data: bytes) -> str:
    if len(data) < 4:
        return raw_data_text(data)
    raw = data[2] | (data[3] << 8)
    return f"{raw / 4:.0f} rpm"


SEMANTIC_EVENT_SUPPRESS = {
    "driver_seat_heat_4e5",
    "seat_heat_vent_134",
    "rpm_316",
    "speed_candidate",
    "outside_temp",
    "engine_temp",
    "battery_voltage",
}


def semantic_value_from_frame(frame: dict) -> list[tuple[str, str, str, str]]:
    data = frame_bytes(frame)
    can_id = frame.get("id")
    values: list[tuple[str, str, str, str]] = []

    if can_id == 0x541 and len(data) >= 8:
        values.extend([
            ("ignition", "Зажигание", on_off((data[0] & 0x03) != 0), "0x541 CGW1"),
            ("door_lf", "Левая передняя дверь", open_closed((data[1] & 0x01) != 0), "0x541 CGW1"),
            ("door_rf", "Правая передняя дверь", open_closed((data[4] & 0x08) != 0), "0x541 CGW1"),
            ("trunk", "Багажник", open_closed((data[1] & 0x10) != 0), "0x541 CGW1"),
            ("hood", "Капот", open_closed((data[2] & 0x02) != 0), "0x541 CGW1"),
            ("sunroof", "Люк", open_closed((data[7] & 0x02) != 0), "C-CAN ch1 STD 0x541 DLC8 DATA[7] bit1"),
        ])

    if can_id == 0x553 and len(data) >= 8:
        values.extend([
            ("door_lr", "Левая задняя дверь", open_closed((data[3] & 0x01) != 0), "0x553 CGW2"),
            ("door_rr", "Правая задняя дверь", open_closed((data[2] & 0x80) != 0), "0x553 CGW2"),
        ])

    if can_id == 0x111 and len(data) >= 8:
        reverse = data[4] == 0x64
        values.append(("reverse_111", "Задний ход", on_off(reverse), "0x111 TCU11"))

    if can_id == 0x316 and len(data) >= 8:
        values.append(("speed_candidate", "Скорость", f"{data[6]} км/ч", "0x316 EMS11 candidate"))
        values.append(("rpm_316", "Обороты двигателя", rpm_from_ems11(data), "C-CAN ch1 STD 0x316 EMS11 DATA[2..3]/4"))

    if can_id == 0x329 and len(data) >= 1:
        values.append(("engine_temp", "Температура двигателя", temp_c_from_offset(data[0]), "C-CAN ch1 STD 0x329 EMS12 DATA[0]-40"))

    if can_id == 0x383 and len(data) >= 3:
        values.append(("outside_temp", "Наружная температура", temp_c_from_half_offset(data[2]), "C-CAN ch1 STD 0x383 FATC11 DATA[2]*0.5-40 candidate"))

    if can_id == 0x545 and len(data) >= 4:
        values.append(("battery_voltage", "Напряжение АКБ", f"{data[3] / 10:.1f} V", "C-CAN ch1 STD 0x545 EMS14 DATA[3]/10 candidate"))

    if can_id == 0x559 and len(data) >= 8:
        values.append(("heated_wheel", "Подогрев руля", on_off((data[0] & 0x10) != 0), "0x559 CGW4"))

    if can_id == 0x134 and len(data) >= 8:
        values.append(("seat_heat_vent_134", "Обогрев/обдув сидений", datc_seat_heat_vent_text(data), "0x134 DATC_PE_05 candidate"))

    if can_id == 0x043 and len(data) >= 3:
        values.append((
            "front_defog_up_043",
            "Обдув вверх/лобовое",
            on_off((data[2] & 0x40) != 0),
            "C-CAN ch1 STD 0x043 DATA[2] bit6",
        ))

    if can_id == 0x132 and len(data) >= 3:
        values.append((
            "front_defog_up_132",
            "Обдув вверх/лобовое",
            on_off((data[2] & 0x01) != 0),
            "M-CAN ch0 STD 0x132 DATA[2] bit0",
        ))

    if can_id == 0x4E5 and len(data) >= 8:
        values.append((
            "driver_seat_heat_4e5",
            "Обогрев водителя candidate",
            driver_seat_heat_candidate_text(data, 0x4E5),
            "C-CAN ch1 STD 0x4E5 quiet candidate",
        ))

    if can_id == 0x436 and len(data) >= 4:
        values.append(("parking_sensors", "Парктроники", "нет препятствий" if data == b"\x00\x00\x00\x00" else data.hex(" ").upper(), "0x436 PAS11"))

    return values


def channel_label(frame: dict) -> str:
    source = frame.get("source")
    ch = int(frame.get("ch", 0))
    if source == "gs_usb":
        return "M-CAN 100k" if ch == 0 else "C-CAN 500k"
    if source == "lab_cdc":
        return "C-CAN 500k" if ch == 0 else "M-CAN 100k"
    return f"ch{ch}"


def analyze_learning(action_id: str, action_name: str, started_ms, frames: list[dict], uart: list[dict]) -> dict:
    profile = ACTION_PROFILES.get(str(action_id))
    action_text = f"{action_id} {action_name}".lower()
    front_defog_action = (
        "front_defog" in action_text
        or "обдув" in action_text
        or "лобов" in action_text
    )
    groups: dict[str, list[dict]] = defaultdict(list)
    for frame in frames:
        key = f"{frame.get('source')}:{frame.get('ch')}:0x{int(frame.get('id', 0)):03X}:{frame.get('dlc')}"
        groups[key].append(frame)

    candidates = []
    for key, rows in groups.items():
        if not rows:
            continue
        first = rows[0]
        last = rows[-1]
        byte_sets = [set() for _ in range(8)]
        for row in rows:
            data = frame_bytes(row)
            for idx, value in enumerate(data[:8]):
                byte_sets[idx].add(value)
        changed = [idx for idx, values in enumerate(byte_sets) if len(values) > 1]
        if not changed and len(rows) < 3:
            continue
        unique_payloads = []
        seen_payloads = set()
        for row in rows:
            value = row.get("data_spaced", "")
            if value not in seen_payloads:
                seen_payloads.add(value)
                unique_payloads.append(value)
            if len(unique_payloads) >= 5:
                break
        semantic = []
        for row in rows[-30:]:
            for state_key, label, value, source in semantic_value_from_frame(row):
                semantic.append({"key": state_key, "label": label, "value": value, "source": source})
        semantic_unique = []
        seen_semantic = set()
        for item in semantic:
            sig = (item.get("key"), item["label"], item["value"])
            if sig not in seen_semantic:
                seen_semantic.add(sig)
                semantic_unique.append(item)
        semantic_bonus = 220 if semantic_unique else 0
        first_id = int(first.get("id", 0))
        known_id_bonus = 40 if first_id in {
            0x034, 0x043, 0x111, 0x132, 0x134, 0x316, 0x390, 0x436, 0x541, 0x553, 0x559, 0x58B
        } else 0
        high_rate_penalty = max(0, len(rows) - 120) * 0.08
        action_bonus = 0
        if front_defog_action:
            semantic_text = " ".join(
                f"{item['label']} {item['value']} {item['source']}".lower()
                for item in semantic_unique
            )
            if first_id in {0x043, 0x132}:
                action_bonus += 520
            elif "обдув" in semantic_text or "лобов" in semantic_text:
                action_bonus += 360
            else:
                action_bonus -= 420
        profile_match = True
        if profile:
            allowed_ids = set(profile.get("ids") or set())
            allowed_keys = set(profile.get("keys") or set())
            allowed_labels = set(profile.get("labels") or set())
            semantic_keys = {str(item.get("key")) for item in semantic_unique}
            semantic_labels = {str(item.get("label")) for item in semantic_unique}
            profile_match = (
                first_id in allowed_ids
                or bool(allowed_keys & semantic_keys)
                or bool(allowed_labels & semantic_labels)
            )
            if profile_match:
                action_bonus += 650
            else:
                action_bonus -= 1200
        score = semantic_bonus + known_id_bonus + action_bonus + len(changed) * 30 + min(len(seen_payloads), 12) * 4 - high_rate_penalty
        candidates.append({
            "key": key,
            "bus": channel_label(first),
            "id_hex": first.get("id_hex"),
            "dlc": first.get("dlc"),
            "count": len(rows),
            "score": round(score, 2),
            "changed_bytes": changed,
            "first": first.get("data_spaced", ""),
            "last": last.get("data_spaced", ""),
            "samples": unique_payloads,
            "semantic": semantic_unique[:6],
            "why": "менялись байты " + (", ".join(str(i) for i in changed) if changed else "не менялись, но кадр был в окне"),
            "profile_match": profile_match,
        })

    candidates.sort(key=lambda item: (-item["score"], item["bus"], item["id_hex"] or ""))
    raw_count = len(candidates)
    if profile:
        matched = [item for item in candidates if item.get("profile_match")]
        candidates = matched if matched else []
    decoded_uart = []
    for item in uart[-80:]:
        decoded_uart.append({
            "raw": item.get("raw"),
            "cmd": item.get("cmd"),
            "text": item.get("text"),
            "valid": item.get("valid"),
            "checksum": item.get("checksum"),
        })

    transitions = []
    profile_keys = set((profile or {}).get("keys") or set())
    profile_ids = set((profile or {}).get("ids") or set())
    previous_values: dict[str, str] = {}
    for row in frames:
        if profile_ids and int(row.get("id", 0)) not in profile_ids:
            continue
        for state_key, label, value, source in semantic_value_from_frame(row):
            if profile_keys and state_key not in profile_keys:
                continue
            previous = previous_values.get(state_key)
            if previous is not None and previous != value:
                transitions.append({
                    "key": state_key,
                    "label": label,
                    "value": value,
                    "source": source,
                    "ms": row.get("seen_ms"),
                    "frame": row.get("data_spaced"),
                })
            previous_values[state_key] = value

    return {
        "action_id": action_id,
        "action_name": action_name,
        "started_ms": started_ms,
        "finished_ms": now_ms(),
        "frames": len(frames),
        "uart": decoded_uart,
        "candidates": candidates[:8 if profile else 40],
        "noise_hidden": max(0, raw_count - len(candidates)),
        "target_repeats": LEARN_TARGET_REPEATS,
        "detected_changes": len(transitions),
        "transitions": transitions[:LEARN_TARGET_REPEATS],
        "profile": {
            "enabled": bool(profile),
            "ids": [f"0x{item:03X}" for item in sorted((profile or {}).get("ids") or [])],
            "keys": sorted((profile or {}).get("keys") or []),
        },
    }


class DashboardState:
    def __init__(self) -> None:
        self.lock = threading.Lock()
        self.recent: deque[dict] = deque(maxlen=500)
        self.counts: Counter[str] = Counter()
        self.latest_by_id: dict[int, dict] = {}
        self.byte_values: dict[str, list[set[int]]] = defaultdict(lambda: [set() for _ in range(8)])
        self.frames_total = 0
        self.uart_rx_total = 0
        self.uart_tx_total = 0
        self.started_ms = now_ms()
        self.last_frame_ms = 0
        self.session = {"running": False, "mode": "idle", "note": "stopped"}
        self.markers: deque[dict] = deque(maxlen=80)
        self.semantic_values: dict[str, dict] = {}
        self.semantic_events: deque[dict] = deque(maxlen=160)
        self.uart_events: deque[dict] = deque(maxlen=240)
        self.uart_state: dict[str, object] = {
            "source": "нет данных",
            "source_code": "-",
            "track": "-",
            "play_time": "-",
            "radio": "-",
            "hu_time": "-",
            "nav": "-",
            "bt": "-",
            "power": "-",
            "last_valid": "-",
            "last_raw": "-",
            "valid_count": 0,
            "invalid_count": 0,
        }
        self.uart_command_counts: Counter[str] = Counter()
        self.learn = {
            "active": False,
            "action_id": None,
            "action_name": None,
            "started_ms": None,
            "frames": [],
            "uart": [],
            "result": None,
            "detected_changes": 0,
            "target_repeats": LEARN_TARGET_REPEATS,
            "capture_closed": False,
            "detected_events": [],
        }
        self.bridge = {
            "enabled": False,
            "last_body_flags": None,
            "last_sent_ms": 0,
            "sent": 0,
            "last_error": "",
        }
        self._clients: list[queue.Queue] = []
        self._last_emit = 0.0

    def reset_runtime(self, mode: str, note: str) -> None:
        with self.lock:
            self.recent.clear()
            self.counts.clear()
            self.latest_by_id.clear()
            self.byte_values.clear()
            self.frames_total = 0
            self.uart_rx_total = 0
            self.uart_tx_total = 0
            self.started_ms = now_ms()
            self.last_frame_ms = 0
            self.semantic_values.clear()
            self.semantic_events.clear()
            self.uart_events.clear()
            self.uart_state = {
                "source": "нет данных",
                "source_code": "-",
                "track": "-",
                "play_time": "-",
                "radio": "-",
                "hu_time": "-",
                "nav": "-",
                "bt": "-",
                "power": "-",
                "last_valid": "-",
                "last_raw": "-",
                "valid_count": 0,
                "invalid_count": 0,
            }
            self.uart_command_counts.clear()
            self.learn.update({
                "active": False,
                "action_id": None,
                "action_name": None,
                "started_ms": None,
                "frames": [],
                "uart": [],
                "result": None,
                "detected_changes": 0,
                "target_repeats": LEARN_TARGET_REPEATS,
                "capture_closed": False,
                "detected_events": [],
            })
            self.session = {"running": True, "mode": mode, "note": note}
        self.broadcast({"type": "summary", "summary": self.summary()})

    def clear_runtime(self) -> None:
        with self.lock:
            session = dict(self.session)
            bridge = dict(self.bridge)
            self.recent.clear()
            self.counts.clear()
            self.latest_by_id.clear()
            self.byte_values.clear()
            self.frames_total = 0
            self.uart_rx_total = 0
            self.uart_tx_total = 0
            self.started_ms = now_ms()
            self.last_frame_ms = 0
            self.markers.clear()
            self.semantic_values.clear()
            self.semantic_events.clear()
            self.uart_events.clear()
            self.uart_command_counts.clear()
            self.uart_state = {
                "source": "нет данных",
                "source_code": "-",
                "track": "-",
                "play_time": "-",
                "radio": "-",
                "hu_time": "-",
                "nav": "-",
                "bt": "-",
                "power": "-",
                "last_valid": "-",
                "last_raw": "-",
                "valid_count": 0,
                "invalid_count": 0,
            }
            self.learn.update({
                "active": False,
                "action_id": None,
                "action_name": None,
                "started_ms": None,
                "frames": [],
                "uart": [],
                "result": None,
                "detected_changes": 0,
                "target_repeats": LEARN_TARGET_REPEATS,
                "capture_closed": False,
                "detected_events": [],
            })
            self.session = session
            self.bridge = bridge
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
        semantic_updates = semantic_value_from_frame(frame)
        bridge_uart_hex = None
        bridge_flags = None
        bridge_error = None
        with self.lock:
            frame["seen_ms"] = now_ms()
            self.recent.appendleft(frame)
            self.counts[key] += 1
            self.latest_by_id[frame["id"]] = frame
            self.frames_total += 1
            self.last_frame_ms = frame["seen_ms"]
            for idx, value in enumerate(data[:8]):
                self.byte_values[key][idx].add(value)
            if self.learn.get("active") and not self.learn.get("capture_closed"):
                self.learn["frames"].append(dict(frame))
                if len(self.learn["frames"]) > 5000:
                    self.learn["frames"] = self.learn["frames"][-5000:]
            for state_key, label, value, source in semantic_updates:
                previous = self.semantic_values.get(state_key, {}).get("value")
                current = {
                    "key": state_key,
                    "label": label,
                    "value": value,
                    "source": source,
                    "ms": frame["seen_ms"],
                }
                self.semantic_values[state_key] = current
                if previous is not None and previous != value and state_key not in SEMANTIC_EVENT_SUPPRESS:
                    self.semantic_events.appendleft({
                        "label": label,
                        "value": value,
                        "source": source,
                        "ms": frame["seen_ms"],
                    })
                if self.learn.get("active") and not self.learn.get("capture_closed") and previous is not None and previous != value:
                    profile = ACTION_PROFILES.get(str(self.learn.get("action_id")))
                    profile_keys = set((profile or {}).get("keys") or set())
                    profile_labels = set((profile or {}).get("labels") or set())
                    if (not profile_keys and not profile_labels) or state_key in profile_keys or label in profile_labels:
                        event = {
                            "key": state_key,
                            "label": label,
                            "value": value,
                            "source": source,
                            "ms": frame["seen_ms"],
                        }
                        self.learn["detected_events"].append(event)
                        self.learn["detected_changes"] = int(self.learn.get("detected_changes", 0)) + 1
                        if int(self.learn.get("detected_changes", 0)) >= int(self.learn.get("target_repeats", LEARN_TARGET_REPEATS)):
                            self.learn["capture_closed"] = True
                if previous is not None and previous != value and state_key in BRIDGE_UART_KEYS and self.bridge.get("enabled"):
                    request = self.bridge_uart_request_locked(state_key, value)
                    if request:
                        bridge_key, bridge_uart_hex, bridge_flags = request
                        if (
                            bridge_key != self.bridge.get("last_key")
                            or bridge_uart_hex != self.bridge.get("last_uart_hex")
                            or frame["seen_ms"] - int(self.bridge.get("last_sent_ms") or 0) > 120
                        ):
                            self.bridge["last_key"] = bridge_key
                            self.bridge["last_uart_hex"] = bridge_uart_hex
                            self.bridge["last_body_flags"] = bridge_flags
                            self.bridge["last_sent_ms"] = frame["seen_ms"]
                            self.bridge["sent"] = int(self.bridge.get("sent", 0)) + 1

        if bridge_uart_hex:
            try:
                append_tx_request({"type": "uart", "uart_hex": bridge_uart_hex})
                self.record_uart_tx()
                label = f"0x{bridge_flags:02X}" if bridge_flags is not None else spaced_hex(bridge_uart_hex)
                self.marker(f"CAN->UART {label}")
            except Exception as exc:
                bridge_error = str(exc)
        if bridge_error:
            with self.lock:
                self.bridge["last_error"] = bridge_error

        current = time.monotonic()
        if current - self._last_emit >= 0.06:
            self._last_emit = current
            self.broadcast({"type": "frame", "frame": frame, "summary": self.summary()})

    def observe_uart(self, item: dict) -> None:
        with self.lock:
            self.uart_events.appendleft(item)
            self.uart_rx_total += 1
            self.update_uart_state_locked(item)
            if self.learn.get("active"):
                self.learn["uart"].append(dict(item))
                if len(self.learn["uart"]) > 1000:
                    self.learn["uart"] = self.learn["uart"][-1000:]
        self.broadcast({"type": "uart", "uart": item, "summary": self.summary()})

    def record_uart_tx(self, count: int = 1) -> None:
        with self.lock:
            self.uart_tx_total += max(0, int(count))

    def body_uart_flags_locked(self) -> int:
        mapping = [
            ("door_lf", 0x01),
            ("door_rf", 0x02),
            ("door_lr", 0x04),
            ("door_rr", 0x08),
            ("trunk", 0x10),
            ("hood", 0x20),
            ("sunroof", 0x40),
        ]
        flags = 0
        for key, bit in mapping:
            if self.semantic_values.get(key, {}).get("value") == "открыто":
                flags |= bit
        return flags

    def bridge_uart_request_locked(self, state_key: str, value: str) -> tuple[str, str, int | None] | None:
        if state_key in BODY_UART_KEYS:
            flags = self.body_uart_flags_locked()
            return ("body", raise_frame(0x05, bytes([flags])).replace(" ", ""), flags)
        if state_key == "reverse_111":
            payload = bytes([0x06, 0x02 if value == "включено" else 0x00])
            return ("reverse", raise_frame(0x7D, payload).replace(" ", ""), None)
        if state_key == "outside_temp":
            try:
                temp = int(round(float(str(value).split()[0])))
            except Exception:
                return None
            raw = abs(temp) & 0x7F
            if temp < 0:
                raw |= 0x80
            return ("outside_temp", raise_frame(0x01, bytes([raw])).replace(" ", ""), None)
        return None

    def set_bridge(self, enabled: bool | None = None) -> dict:
        with self.lock:
            if enabled is None:
                enabled = not bool(self.bridge.get("enabled"))
            self.bridge["enabled"] = bool(enabled)
            self.bridge["last_error"] = ""
            status = dict(self.bridge)
        self.marker(f"CAN->UART bridge {'ON' if status['enabled'] else 'OFF'}")
        self.broadcast({"type": "summary", "summary": self.summary()})
        return status

    def update_uart_state_locked(self, item: dict) -> None:
        cmd = item.get("cmd") or "-"
        self.uart_command_counts[str(cmd)] += 1
        self.uart_state["last_raw"] = item.get("raw") or "-"
        self.uart_state["last_text"] = item.get("text") or "-"
        self.uart_state["last_ms"] = item.get("ms")
        if not item.get("valid"):
            self.uart_state["invalid_count"] = int(self.uart_state.get("invalid_count", 0)) + 1
            return

        self.uart_state["valid_count"] = int(self.uart_state.get("valid_count", 0)) + 1
        self.uart_state["last_valid"] = item.get("text") or "-"
        fields = item.get("fields") or {}
        kind = fields.get("kind")

        if kind == "hu_time":
            self.uart_state["hu_time"] = fields.get("time", "-")
            return

        if kind == "hu_media":
            source_name = str(fields.get("source_name", "нет данных"))
            self.uart_state["source"] = source_name
            self.uart_state["source_code"] = fields.get("source_code", "-")
            if "track" in fields:
                self.uart_state["track"] = str(fields.get("track"))
                self.uart_state["play_time"] = fields.get("play_time", "-")
            if "frequency" in fields:
                self.uart_state["radio"] = f"{fields.get('band', '')} {fields.get('frequency', '')}".strip()
            if source_name == "навигация":
                self.uart_state["nav"] = f"active {fields.get('state_code', '')}".strip()
            if source_name.startswith("Bluetooth"):
                self.uart_state["bt"] = f"{source_name} {fields.get('state_code', '')}".strip()
            if source_name == "media off":
                self.uart_state["track"] = "-"
                self.uart_state["play_time"] = "-"
            return

        if kind == "hu_power":
            self.uart_state["power"] = fields.get("state", "-")
            return

        if kind == "audio_balance":
            self.uart_state["audio_balance"] = f"fade {fields.get('fade')}, bal {fields.get('balance')}"
            return

        if kind == "sound_effects":
            self.uart_state["sound"] = f"bass {fields.get('bass')}, mid {fields.get('mid')}, treble {fields.get('treble')}"
            return

    def marker(self, name: str) -> None:
        item = {"name": name.strip()[:80] or "marker", "ms": now_ms()}
        with self.lock:
            self.markers.appendleft(item)
        self.broadcast({"type": "marker", "marker": item})

    def start_learning(self, action_id: str, action_name: str) -> dict:
        started = now_ms()
        with self.lock:
            self.learn = {
                "active": True,
                "action_id": action_id,
                "action_name": action_name,
                "started_ms": started,
                "frames": [],
                "uart": [],
                "result": None,
                "detected_changes": 0,
                "target_repeats": LEARN_TARGET_REPEATS,
                "capture_closed": False,
                "detected_events": [],
            }
        self.marker(f"START {action_id} {action_name}")
        self.broadcast({"type": "summary", "summary": self.summary()})
        return self.learn_public()

    def stop_learning(self) -> dict:
        with self.lock:
            frames = list(self.learn.get("frames", []))
            uart = list(self.learn.get("uart", []))
            action_id = self.learn.get("action_id") or "manual"
            action_name = self.learn.get("action_name") or action_id
            started_ms = self.learn.get("started_ms")
            self.learn["active"] = False
        result = analyze_learning(action_id, action_name, started_ms, frames, uart)
        with self.lock:
            self.learn["result"] = result
        self.marker(f"STOP {action_id} candidates={len(result['candidates'])} uart={len(result['uart'])}")
        self.broadcast({"type": "summary", "summary": self.summary()})
        return result

    def learn_public(self) -> dict:
        with self.lock:
            return {
                "active": self.learn.get("active", False),
                "action_id": self.learn.get("action_id"),
                "action_name": self.learn.get("action_name"),
                "started_ms": self.learn.get("started_ms"),
                "frames_count": len(self.learn.get("frames", [])),
                "uart_count": len(self.learn.get("uart", [])),
                "result": self.learn.get("result"),
                "detected_changes": self.learn.get("detected_changes", 0),
                "target_repeats": self.learn.get("target_repeats", LEARN_TARGET_REPEATS),
                "capture_closed": self.learn.get("capture_closed", False),
                "detected_events": list(self.learn.get("detected_events", []))[-LEARN_TARGET_REPEATS:],
            }

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
                    {"label": "Seat heat / vent", "source": "0x134 / 0x4E5 / 0x4E6 / 0x5CE / 0x5CF / 0x547", "value": f"{self._raw_value(0x134)} | {self._raw_value(0x4E5)} | {self._raw_value(0x4E6)} | {self._raw_value(0x5CE)} | {self._raw_value(0x5CF)} | {self._raw_value(0x547)}", "age": f"{self._age(0x134)} / {self._age(0x4E5)} / {self._age(0x4E6)} / {self._age(0x5CE)} / {self._age(0x5CF)} / {self._age(0x547)}", "status": "DBC/raw candidate"},
                ],
            },
        ]

    def summary(self) -> dict:
        with self.lock:
            elapsed = max(0.001, (now_ms() - self.started_ms) / 1000)
            can_by_channel = {"mcan": 0, "ccan": 0}
            for key, count in self.counts.items():
                if key.startswith("ch0:"):
                    can_by_channel["mcan"] += count
                elif key.startswith("ch1:"):
                    can_by_channel["ccan"] += count
            active_mode = str(self.session.get("mode") or "idle")
            if not self.session.get("running"):
                active_mode = f"{active_mode}:stopped"
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
                "module_stats": {
                    "mcan_frames": can_by_channel["mcan"],
                    "ccan_frames": can_by_channel["ccan"],
                    "mcan_fps": can_by_channel["mcan"] / elapsed,
                    "ccan_fps": can_by_channel["ccan"] / elapsed,
                    "uart_rx": self.uart_rx_total,
                    "uart_tx": self.uart_tx_total,
                    "active_mode": active_mode,
                },
                "last_frame_age": (now_ms() - self.last_frame_ms) / 1000 if self.last_frame_ms else None,
                "recent": list(self.recent)[:120],
                "top": top,
                "states": self.derived_states_locked(),
                "semantic": list(self.semantic_values.values()),
                "semantic_events": list(self.semantic_events),
                "uart_events": list(self.uart_events),
                "uart_state": dict(self.uart_state),
                "uart_command_counts": [
                    {"cmd": cmd, "count": count}
                    for cmd, count in self.uart_command_counts.most_common(12)
                ],
                "markers": list(self.markers),
                "learn": {
                    "active": self.learn.get("active", False),
                    "action_id": self.learn.get("action_id"),
                    "action_name": self.learn.get("action_name"),
                    "started_ms": self.learn.get("started_ms"),
                    "frames_count": len(self.learn.get("frames", [])),
                    "uart_count": len(self.learn.get("uart", [])),
                    "result": self.learn.get("result"),
                    "detected_changes": self.learn.get("detected_changes", 0),
                    "target_repeats": self.learn.get("target_repeats", LEARN_TARGET_REPEATS),
                    "capture_closed": self.learn.get("capture_closed", False),
                    "detected_events": list(self.learn.get("detected_events", []))[-LEARN_TARGET_REPEATS:],
                },
                "bridge": dict(self.bridge),
            }


STATE = DashboardState()


class QuietThreadingHTTPServer(ThreadingHTTPServer):
    def handle_error(self, request, client_address) -> None:
        exc_type, exc, _tb = sys.exc_info()
        if exc_type in {ConnectionResetError, BrokenPipeError}:
            return
        super().handle_error(request, client_address)


class LogRunner:
    def __init__(self) -> None:
        self.lock = threading.Lock()
        self.stop_event = threading.Event()
        self.thread: threading.Thread | None = None
        self.proc: subprocess.Popen | None = None
        self.serial_conn = None
        self.raw_log_path: Path | None = None
        self.tx_control_path: Path | None = None
        self.raw_log_fh = None
        self.run_id = 0

    def _set_stopped_if_current(self, run_id: int, note: str) -> None:
        with self.lock:
            is_current = run_id == self.run_id
        if is_current:
            STATE.set_stopped(note)

    def stop(self) -> None:
        with self.lock:
            self.run_id += 1
            self.stop_event.set()
            if self.serial_conn is not None:
                try:
                    self.serial_conn.close()
                except Exception:
                    pass
                self.serial_conn = None
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
        with self.lock:
            self.run_id += 1
            run_id = self.run_id
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
                self._set_stopped_if_current(run_id, "sample complete")
            except Exception as exc:
                self._set_stopped_if_current(run_id, f"sample error: {exc}")

        self.thread = threading.Thread(target=run, name="sample-log", daemon=True)
        self.thread.start()

    def start_process(self, mode: str, command: list[str], *, tx_control_path: Path | None = None) -> None:
        self.stop()
        self.stop_event.clear()
        with self.lock:
            self.run_id += 1
            run_id = self.run_id
        LIVE_LOG_DIR.mkdir(parents=True, exist_ok=True)
        raw_log_path = LIVE_LOG_DIR / f"{mode}_{time.strftime('%Y%m%d_%H%M%S')}.txt"
        STATE.reset_runtime(mode, f"{' '.join(command)} | raw={raw_log_path}")

        def run() -> None:
            uart_parser = RaiseStreamParser()
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
                        uart_items = parse_uart_lines(line, uart_parser)
                        if uart_items:
                            for uart in uart_items:
                                STATE.observe_uart(uart)
                        else:
                            STATE.marker(line.strip())
                code = proc.wait()
                with self.lock:
                    if self.raw_log_fh:
                        self.raw_log_fh.flush()
                        self.raw_log_fh.close()
                        self.raw_log_fh = None
                if not self.stop_event.is_set():
                    self._set_stopped_if_current(run_id, f"process exited: {code}; raw={raw_log_path}")
            except Exception as exc:
                self._set_stopped_if_current(run_id, f"process error: {exc}")

        self.thread = threading.Thread(target=run, name=f"{mode}-logger", daemon=True)
        self.thread.start()

    def start_lab_serial(self, port: str) -> None:
        if serial is None:
            raise RuntimeError("pyserial is not available")
        self.stop()
        self.stop_event.clear()
        with self.lock:
            self.run_id += 1
            run_id = self.run_id
        LIVE_LOG_DIR.mkdir(parents=True, exist_ok=True)
        raw_log_path = LIVE_LOG_DIR / f"lab_{time.strftime('%Y%m%d_%H%M%S')}.txt"
        STATE.reset_runtime("lab", f"{port} | raw={raw_log_path}")

        def run() -> None:
            uart_parser = RaiseStreamParser()
            try:
                ser = serial.Serial(port, 115200, timeout=0.1, write_timeout=1)
                with self.lock:
                    self.serial_conn = ser
                    self.raw_log_path = raw_log_path
                    self.raw_log_fh = raw_log_path.open("w", encoding="utf-8")
                    self.raw_log_fh.write(f"# LAB_PORT {port}\n")
                    self.raw_log_fh.flush()
                for command in ["0S6", "1S3", "O", "?"]:
                    ser.write((command + "\r\n").encode("ascii"))
                    ser.flush()
                    time.sleep(0.05)
                while not self.stop_event.is_set():
                    raw = ser.readline()
                    if not raw:
                        continue
                    line = raw.decode("utf-8", errors="replace").strip()
                    if not line:
                        continue
                    with self.lock:
                        if self.raw_log_fh:
                            self.raw_log_fh.write(line + "\n")
                    frame = parse_frame_line(line)
                    if frame:
                        STATE.observe(frame)
                        continue
                    uart_items = parse_uart_lines(line, uart_parser)
                    if uart_items:
                        for uart in uart_items:
                            STATE.observe_uart(uart)
                        continue
                    STATE.marker(line)
            except Exception as exc:
                if not self.stop_event.is_set():
                    self._set_stopped_if_current(run_id, f"lab error: {exc}")
            finally:
                with self.lock:
                    if self.raw_log_fh:
                        self.raw_log_fh.flush()
                        self.raw_log_fh.close()
                        self.raw_log_fh = None
                    if self.serial_conn is not None:
                        try:
                            self.serial_conn.close()
                        except Exception:
                            pass
                        self.serial_conn = None
                if not self.stop_event.is_set():
                    self._set_stopped_if_current(run_id, f"lab stopped; raw={raw_log_path}")

        self.thread = threading.Thread(target=run, name="lab-serial", daemon=True)
        self.thread.start()

    def send_lab_line(self, line: str) -> None:
        command = line.strip()
        if not command:
            raise RuntimeError("empty lab command")
        with self.lock:
            ser = self.serial_conn
        if ser is None or not getattr(ser, "is_open", False):
            raise RuntimeError("start Lab CAN+UART first")
        ser.write((command + "\r\n").encode("ascii"))
        ser.flush()
        STATE.marker(f"lab tx: {command}")


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


def lab_command_to_uart_hex(command: str) -> str:
    text = str(command or "").strip()
    if not text:
        raise ValueError("empty UART command")
    if text[0] in {"u", "U"}:
        text = text[1:]
    text = text.replace(" ", "").replace(":", "").replace("-", "").upper()
    if text.startswith("0X"):
        text = text[2:]
    if len(text) % 2:
        raise ValueError("UART hex must contain full bytes")
    if not text or not re.fullmatch(r"[0-9A-F]+", text):
        raise ValueError("UART command must be hex, example: uFD050540004A")
    if len(text) > 128:
        raise ValueError("UART sideband write is limited to 64 bytes")
    return text


def run_send_uart(command: str) -> dict:
    uart_hex = lab_command_to_uart_hex(command)
    path = append_tx_request({"type": "uart", "uart_hex": uart_hex})
    STATE.record_uart_tx()
    decoded = decode_raise_frame(bytes.fromhex(uart_hex))
    STATE.marker(f"uart tx: {spaced_hex(uart_hex)}")
    return {
        "ok": True,
        "command": command,
        "uart_hex": spaced_hex(uart_hex),
        "decoded": decoded,
        "tx_control": str(path),
    }


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
        try:
            import libusb_package  # type: ignore
            backend_path = Path(libusb_package.get_library_path())
        except Exception:
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


def load_raise_matrix() -> list[dict]:
    if not RAISE_MATRIX.exists():
        return []
    with RAISE_MATRIX.open("r", encoding="utf-8", newline="") as fh:
        return list(csv.DictReader(fh))


def load_learned_assignments() -> list[dict]:
    if not LEARNED_ASSIGNMENTS.exists():
        return []
    items = []
    with LEARNED_ASSIGNMENTS.open("r", encoding="utf-8", errors="ignore") as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            try:
                item = json.loads(line)
                if item.get("verified") is True or item.get("manual") is True or item.get("locked") is True:
                    items.append(item)
            except json.JSONDecodeError:
                items.append({"raw": line, "error": "json_decode"})
    return items


def build_test_plan() -> list[dict]:
    assignments = load_learned_assignments()
    saved_by_action = {
        str(item.get("action_id")): item
        for item in assignments
        if item.get("action_id")
    }
    plan = []
    for action in LEARN_ACTIONS:
        saved = saved_by_action.get(action["id"])
        plan.append({
            **action,
            "status": "captured" if saved else "pending",
            "candidate": saved.get("candidate") if saved else None,
            "saved_ms": saved.get("saved_ms") if saved else None,
        })
    return plan


def command_catalog() -> dict:
    safe_uart = [
        {"id": "body_closed", "name": "Кузов: все закрыто", "frame": raise_frame(0x05, bytes([0x00])), "lab": "u" + raise_frame(0x05, bytes([0x00])).replace(" ", "")},
        {"id": "door_driver", "name": "Дверь водитель открыта", "frame": raise_frame(0x05, bytes([0x01])), "lab": "u" + raise_frame(0x05, bytes([0x01])).replace(" ", "")},
        {"id": "door_passenger", "name": "Дверь пассажир открыта", "frame": raise_frame(0x05, bytes([0x02])), "lab": "u" + raise_frame(0x05, bytes([0x02])).replace(" ", "")},
        {"id": "door_rear_left", "name": "Дверь задняя левая открыта", "frame": raise_frame(0x05, bytes([0x04])), "lab": "u" + raise_frame(0x05, bytes([0x04])).replace(" ", "")},
        {"id": "door_rear_right", "name": "Дверь задняя правая открыта", "frame": raise_frame(0x05, bytes([0x08])), "lab": "u" + raise_frame(0x05, bytes([0x08])).replace(" ", "")},
        {"id": "trunk", "name": "Багажник открыт", "frame": raise_frame(0x05, bytes([0x10])), "lab": "u" + raise_frame(0x05, bytes([0x10])).replace(" ", "")},
        {"id": "hood", "name": "Капот открыт", "frame": raise_frame(0x05, bytes([0x20])), "lab": "u" + raise_frame(0x05, bytes([0x20])).replace(" ", "")},
        {"id": "sunroof", "name": "Люк test: body bit6 0x40", "frame": raise_frame(0x05, bytes([0x40])), "lab": "u" + raise_frame(0x05, bytes([0x40])).replace(" ", ""), "warning": "Эксперимент: CAN люка подтвержден, TEYES/Raise bit 0x40 может игнорировать"},
        {"id": "sunroof_bit7", "name": "Люк test: body bit7 0x80", "frame": raise_frame(0x05, bytes([0x80])), "lab": "u" + raise_frame(0x05, bytes([0x80])).replace(" ", ""), "warning": "Экспериментальный альтернативный бит"},
        {"id": "sunroof_2byte_4000", "name": "Люк test: body 40 00", "frame": raise_frame(0x05, bytes([0x40, 0x00])), "lab": "u" + raise_frame(0x05, bytes([0x40, 0x00])).replace(" ", ""), "warning": "Экспериментальный 2-byte payload"},
        {"id": "sunroof_2byte_0040", "name": "Люк test: body 00 40", "frame": raise_frame(0x05, bytes([0x00, 0x40])), "lab": "u" + raise_frame(0x05, bytes([0x00, 0x40])).replace(" ", ""), "warning": "Экспериментальный 2-byte payload"},
        {"id": "all_doors", "name": "Кузов: все двери открыты", "frame": raise_frame(0x05, bytes([0x0F])), "lab": "u" + raise_frame(0x05, bytes([0x0F])).replace(" ", "")},
        {"id": "reverse_on", "name": "Задний ход on", "frame": raise_frame(0x7D, bytes([0x06, 0x02])), "lab": "u" + raise_frame(0x7D, bytes([0x06, 0x02])).replace(" ", "")},
        {"id": "reverse_off", "name": "Задний ход off", "frame": raise_frame(0x7D, bytes([0x06, 0x00])), "lab": "u" + raise_frame(0x7D, bytes([0x06, 0x00])).replace(" ", "")},
        {"id": "climate_popup", "name": "Климат popup demo", "frame": raise_frame(0x03, bytes([22, 22, 3, 0x24])), "lab": "u" + raise_frame(0x03, bytes([22, 22, 3, 0x24])).replace(" ", "")},
        {"id": "climate_off", "name": "Климат popup off", "frame": raise_frame(0x03, bytes([0, 0, 0, 0x00])), "lab": "u" + raise_frame(0x03, bytes([0, 0, 0, 0x00])).replace(" ", "")},
        {"id": "key_vol_up", "name": "Кнопка volume up", "frame": raise_frame(0x02, bytes([0x14, 0x01])), "lab": "u" + raise_frame(0x02, bytes([0x14, 0x01])).replace(" ", "")},
        {"id": "key_vol_down", "name": "Кнопка volume down candidate", "frame": raise_frame(0x02, bytes([0x15, 0x01])), "lab": "u" + raise_frame(0x02, bytes([0x15, 0x01])).replace(" ", "")},
        {"id": "key_source", "name": "Кнопка source/mode candidate", "frame": raise_frame(0x02, bytes([0x01, 0x01])), "lab": "u" + raise_frame(0x02, bytes([0x01, 0x01])).replace(" ", "")},
        {"id": "key_mute", "name": "Кнопка mute candidate", "frame": raise_frame(0x02, bytes([0x02, 0x01])), "lab": "u" + raise_frame(0x02, bytes([0x02, 0x01])).replace(" ", "")},
        {"id": "key_release", "name": "Кнопка release", "frame": raise_frame(0x02, bytes([0x00, 0x00])), "lab": "u" + raise_frame(0x02, bytes([0x00, 0x00])).replace(" ", "")},
        {"id": "outside_temp", "name": "Наружная температура 21C", "frame": raise_frame(0x01, bytes([21])), "lab": "u" + raise_frame(0x01, bytes([21])).replace(" ", "")},
        {"id": "backlight", "name": "Подсветка 80%", "frame": raise_frame(0x07, bytes([80])), "lab": "u" + raise_frame(0x07, bytes([80])).replace(" ", "")},
        {"id": "radar_clear", "name": "Парктроники clear", "frame": raise_frame(0x04, bytes([0x00, 0x00])), "lab": "u" + raise_frame(0x04, bytes([0x00, 0x00])).replace(" ", "")},
        {"id": "radar_demo", "name": "Парктроники demo", "frame": raise_frame(0x04, bytes([0x55, 0x55])), "lab": "u" + raise_frame(0x04, bytes([0x55, 0x55])).replace(" ", "")},
        {"id": "version", "name": "Версия canbox LAB08", "frame": raise_frame(0x7F, b"LAB08"), "lab": "u" + raise_frame(0x7F, b"LAB08").replace(" ", "")},
    ]
    hu_examples = [
        {"id": "hu_usb_2s", "name": "HU говорит: USB media 2s", "frame": raise_frame(0x09, bytes([0x16, 0x00, 0x00, 0x00, 0x00, 0x02]))},
        {"id": "hu_bt_music", "name": "HU говорит: BT music", "frame": raise_frame(0x09, bytes([0x11, 0x00]))},
        {"id": "hu_nav", "name": "HU говорит: navigation", "frame": raise_frame(0x09, bytes([0x06, 0x00]))},
        {"id": "hu_radio", "name": "HU говорит: FM 101.70", "frame": raise_frame(0x09, bytes([0x02, 0x00, 101, 70]))},
        {"id": "hu_media_off", "name": "HU говорит: media off", "frame": raise_frame(0x09, bytes([0x80, 0x00]))},
        {"id": "hu_aux", "name": "HU говорит: AUX", "frame": raise_frame(0x09, bytes([0x12, 0x00]))},
        {"id": "hu_bt_phone", "name": "HU говорит: BT call active", "frame": raise_frame(0x09, bytes([0x07, 0x01]))},
        {"id": "hu_time", "name": "HU sync time 06:44", "frame": raise_frame(0x06, bytes([44, 6, 1]))},
    ]
    can_rows = load_matrix()
    can_summary: dict[str, list[dict]] = defaultdict(list)
    for row in can_rows:
        can_summary[row.get("category") or "unknown"].append({
            "function": row.get("function"),
            "direction": row.get("direction"),
            "bus": row.get("dbc_bus"),
            "id": row.get("dbc_id_hex") or row.get("observed_id_hex"),
            "signals": row.get("dbc_signals"),
            "status": row.get("status"),
            "implementation": row.get("implementation_type"),
            "note": row.get("implementation_note"),
        })
    demo_scenarios = [
        {"id": "walkaround", "name": "Обход кузова", "description": "двери, багажник, капот, люк, свет"},
        {"id": "parking", "name": "Парковка", "description": "R, задний ход, парктроники, RCTA/LCA placeholders"},
        {"id": "climate", "name": "Климат и сиденья", "description": "AUTO, A/C, обдув, подогрев руля, сиденья"},
        {"id": "media", "name": "Медиа/навигация", "description": "USB, BT, FM, навигация, компас"},
        {"id": "stress", "name": "Полный стресс", "description": "быстрое переключение разных состояний для проверки UI"},
    ]
    return {
        "actions": LEARN_ACTIONS,
        "raise_matrix": load_raise_matrix(),
        "can_matrix": can_rows,
        "can_summary": can_summary,
        "safe_uart_tests": safe_uart,
        "hu_to_canbox_examples": hu_examples,
        "demo_scenarios": demo_scenarios,
        "test_plan": build_test_plan(),
        "learned_assignments": load_learned_assignments(),
    }


def run_send_display_usb(payload: dict) -> dict:
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


def run_send_display_can(payload: dict) -> dict:
    scenario = str(payload.get("scenario", "full"))
    if scenario not in DISPLAY_CAN_FRAME_SETS:
        raise ValueError(f"unknown display scenario: {scenario}")
    channels = display_can_channels(str(payload.get("bus", "mcan")))
    seconds = max(0.2, min(float(payload.get("seconds", 5.0)), 30.0))
    gap = max(0.01, min(float(payload.get("gap", 0.035)), 0.5))
    frames = DISPLAY_CAN_FRAME_SETS[scenario]
    deadline = time.monotonic() + seconds
    sent = 0
    while time.monotonic() < deadline:
        for can_id, data_hex, _period in frames:
            for channel in channels:
                append_tx_request({
                    "channel": channel,
                    "id": f"0x{can_id:X}",
                    "data": data_hex,
                    "count": 1,
                    "interval": 0,
                    "extended": False,
                })
                sent += 1
            time.sleep(gap)
            if time.monotonic() >= deadline:
                break
    bus = str(payload.get("bus", "mcan"))
    STATE.marker(f"display CAN {scenario} {bus}: {sent} frames")
    return {
        "ok": True,
        "transport": "can",
        "bus": bus,
        "channels": channels,
        "scenario": scenario,
        "sent": sent,
    }


def run_send_display(payload: dict) -> dict:
    if str(payload.get("transport", "can")).lower() == "usb":
        return run_send_display_usb(payload)
    return run_send_display_can(payload)


class DashboardHandler(SimpleHTTPRequestHandler):
    def end_headers(self) -> None:
        self.send_header("Cache-Control", "no-store, max-age=0")
        self.send_header("Pragma", "no-cache")
        super().end_headers()

    def translate_path(self, path: str) -> str:
        parsed = urlparse(path)
        if parsed.path == "/":
            return str(STATIC / "index.html")
        return str(STATIC / parsed.path.lstrip("/"))

    def log_message(self, fmt: str, *args) -> None:
        sys.stderr.write(f"[dashboard] {fmt % args}\n")

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path == "/favicon.ico":
            self.send_response(204)
            self.end_headers()
            return
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
        if parsed.path == "/api/commands":
            json_response(self, command_catalog())
            return
        if parsed.path == "/api/test-plan":
            json_response(self, build_test_plan())
            return
        if parsed.path == "/api/learned":
            json_response(self, load_learned_assignments())
            return
        if parsed.path == "/api/export":
            json_response(self, {
                "exported_ms": now_ms(),
                "summary": STATE.summary(),
                "commands": command_catalog(),
                "learned_assignments": load_learned_assignments(),
                "paths": {
                    "matrix": str(MATRIX),
                    "raise_matrix": str(RAISE_MATRIX),
                    "learned_assignments": str(LEARNED_ASSIGNMENTS),
                    "live_logs": str(LIVE_LOG_DIR),
                },
            })
            return
        if parsed.path == "/api/export/can/m":
            summary = STATE.summary()
            frames = [item for item in summary.get("recent", []) if int(item.get("ch", -1)) == 0]
            json_response(self, {"exported_ms": now_ms(), "bus": "M-CAN", "channel": 0, "frames": frames})
            return
        if parsed.path == "/api/export/can/c":
            summary = STATE.summary()
            frames = [item for item in summary.get("recent", []) if int(item.get("ch", -1)) == 1]
            json_response(self, {"exported_ms": now_ms(), "bus": "C-CAN", "channel": 1, "frames": frames})
            return
        if parsed.path == "/api/export/uart":
            summary = STATE.summary()
            json_response(self, {
                "exported_ms": now_ms(),
                "uart_state": summary.get("uart_state", {}),
                "uart_events": summary.get("uart_events", []),
                "uart_command_counts": summary.get("uart_command_counts", []),
            })
            return
        if parsed.path == "/api/export/learned-table":
            rows = []
            for item in load_learned_assignments():
                candidate = item.get("candidate") or {}
                rows.append({
                    "action_id": item.get("action_id"),
                    "action_name": item.get("action_name"),
                    "verified": bool(item.get("verified") or item.get("manual") or item.get("locked")),
                    "can_bus": candidate.get("bus"),
                    "can_id": candidate.get("id_hex"),
                    "can_dlc": candidate.get("dlc"),
                    "can_changed_bytes": candidate.get("changed_bytes", []),
                    "can_on_or_last": candidate.get("last"),
                    "can_off_or_first": candidate.get("first"),
                    "uart_hint": item.get("uart_hint") or item.get("uart_hex") or "",
                    "notes": item.get("notes") or candidate.get("why") or "",
                })
            json_response(self, {"exported_ms": now_ms(), "rows": rows})
            return
        if parsed.path == "/api/learn/actions":
            json_response(self, LEARN_ACTIONS)
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
                elif mode == "lab":
                    port = payload.get("port") or auto_cdc_port()
                    if gsusb_present() is not True:
                        if not port:
                            raise RuntimeError("mode3 gs_usb not found and no CDC port found for auto switch")
                        subprocess.run(
                            [sys.executable, str(USB_MODE), port, "canlog", "--no-wait"],
                            cwd=str(ROOT),
                            text=True,
                            capture_output=True,
                            timeout=8,
                        )
                        time.sleep(2.0)
                    tx_control = LIVE_LOG_DIR / f"gsusb_uart_tx_{time.strftime('%Y%m%d_%H%M%S')}.jsonl"
                    RUNNER.start_process(
                        "gsusb_uart",
                        [
                            sys.executable,
                            str(GSUSB_LOGGER),
                            "--bitrate0",
                            "100000",
                            "--bitrate1",
                            "500000",
                            "--uart-monitor",
                            "--uart-monitor-interval",
                            "0.05",
                            "--tx-control",
                            str(tx_control),
                        ],
                        tx_control_path=tx_control,
                    )
                    json_response(self, {"ok": True, "mode": mode, "port": port, "tx_control": str(tx_control)})
                else:
                    json_response(self, {"ok": False, "error": f"unknown mode {mode}"}, HTTPStatus.BAD_REQUEST)
                return
            if parsed.path == "/api/log/stop":
                RUNNER.stop()
                json_response(self, {"ok": True})
                return
            if parsed.path == "/api/reset":
                STATE.clear_runtime()
                json_response(self, {"ok": True, "summary": STATE.summary()})
                return
            if parsed.path == "/api/bridge":
                if "enabled" in payload:
                    enabled = bool(payload.get("enabled"))
                else:
                    enabled = None
                status = STATE.set_bridge(enabled)
                json_response(self, {"ok": True, "bridge": status})
                return
            if parsed.path == "/api/marker":
                name = payload.get("name", "marker")
                STATE.marker(name)
                RUNNER.write_raw_marker(name)
                json_response(self, {"ok": True})
                return
            if parsed.path == "/api/learn/start":
                action_id = str(payload.get("action_id") or "manual")
                action = next((item for item in LEARN_ACTIONS if item["id"] == action_id), None)
                action_name = str(payload.get("name") or (action and action["name"]) or action_id)
                result = STATE.start_learning(action_id, action_name)
                RUNNER.write_raw_marker(f"START {action_id} {action_name}")
                json_response(self, {"ok": True, "learn": result})
                return
            if parsed.path == "/api/learn/stop":
                result = STATE.stop_learning()
                RUNNER.write_raw_marker(f"STOP {result['action_id']} {result['action_name']}")
                json_response(self, {"ok": True, "result": result})
                return
            if parsed.path == "/api/learn/save":
                item = {
                    "saved_ms": now_ms(),
                    "action_id": payload.get("action_id"),
                    "action_name": payload.get("action_name"),
                    "candidate": payload.get("candidate"),
                    "note": payload.get("note", ""),
                    "verified": True,
                }
                LEARNED_ASSIGNMENTS.parent.mkdir(parents=True, exist_ok=True)
                with LEARNED_ASSIGNMENTS.open("a", encoding="utf-8") as fh:
                    fh.write(json.dumps(item, ensure_ascii=False, separators=(",", ":")) + "\n")
                STATE.marker(f"saved {item['action_id']} -> {item.get('candidate', {}).get('id_hex', 'candidate')}")
                json_response(self, {"ok": True, "path": str(LEARNED_ASSIGNMENTS)})
                return
            if parsed.path == "/api/lab/send":
                command = str(payload.get("command") or "")
                if RUNNER.tx_control_path is not None:
                    json_response(self, run_send_uart(command))
                else:
                    RUNNER.send_lab_line(command)
                    json_response(self, {"ok": True, "command": command})
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

    server = QuietThreadingHTTPServer((args.host, args.port), DashboardHandler)

    def stop(_signum, _frame) -> None:
        RUNNER.stop()
        threading.Thread(target=server.shutdown, daemon=True).start()

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)
    print(f"2CAN35 dashboard: http://{args.host}:{args.port}", flush=True)
    server.serve_forever()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
