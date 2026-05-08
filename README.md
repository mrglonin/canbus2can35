# canbus2can35

Лаборатория и исходники для кастомного 2CAN35/Sigma10 canbox на базе
`STM32F105RBT6` под Kia/Hyundai Sportage-family setup.

Цель проекта - максимально раскрыть функции автомобиля по двум CAN-шинам
`C-CAN` и `M-CAN`, собрать воспроизводимую карту сигналов и постепенно
перенести подтвержденную логику в свою прошивку. Репозиторий не является
публичной раздачей чужих APK/прошивок: сторонние бинарники описаны через имена,
версии и SHA256, а исходная разработка ведется в `firmware/custom_c/`.

## Hardware

| Item | Current knowledge |
|---|---|
| Board | StarLine / 2CAN35-like board, PCB `StarLine 200-00002 REV.A` |
| MCU | `STM32F105RBT6`, LQFP64 |
| Adapter UID | `37 FF DA 05 42 47 30 38 59 41 22 43` |
| USB CDC | `0483:5740`, usually `/dev/cu.usbmodemKIA1` |
| C-CAN | `PB8/PB9`, 500 kbit/s |
| M-CAN | `PB12/PB13`, 100 kbit/s |
| TEYES/Raise UART2 | `PA2/PA3`, 19200 8N1 |
| Reverse output control | `PC14`, active high logic output to external driver |
| Profile string seen in firmware | `HYK-RZ-10-0001-VK` |

Board notes:

- wiring/rework guide: `docs/HARDWARE_WIRING_MOD_GUIDE.md`;
- photo/pin map: `firmware/custom_c/docs/BOARD_PHOTO_PINMAP.md`;
- board I/O map: `firmware/custom_c/docs/BOARD_IO_MAP.md`;
- TEYES/Raise UART notes: `firmware/custom_c/docs/TEYES_RAISE_COMPAT.md`;
- ST-Link recovery sequence: `docs/RECOVERY_STLINK_SEQUENCE.md`.

## Architecture

The project separates four layers:

| Layer | Purpose |
|---|---|
| Car CAN parser | Read body, reverse, parking, climate, speed, temperatures and other states from `C-CAN` / `M-CAN`. |
| TEYES/Raise UART bridge | Talk to the Android head unit through the selected Raise Hyundai/Kia canbox protocol. |
| M-CAN sender | Send confirmed media/navigation/cluster frames to the factory cluster. |
| Lab tooling | Log both buses, replay captures, send controlled raw CAN/USB/UART tests, and update firmware. |

### Analog Buttons / External Raise Bridge

Steering wheel buttons and the piano buttons above the climate panel are treated
as analog button hardware, not as a primary CAN-decoding target.

Planned practical wiring:

```text
analog buttons -> stock Raise canbox -> UART2 -> our 2CAN35 adapter -> TEYES HU
```

Meaning:

- the stock Raise box can keep reading analog buttons exactly as intended;
- its UART output goes into our adapter UART2;
- our adapter can transparently bridge, log, filter or later extend that stream;
- because the head unit already expects the Raise protocol, no button protocol
  conversion is needed for the basic path.

Firmware implication: the clean firmware needs a reliable transparent
Raise-UART bridge before it tries to replace button handling. CAN-side button
candidates are kept only as secondary research, not as the main plan for these
analog controls.

## Current Status

### Works / Confirmed

| Area | Status |
|---|---|
| Stock USB update protocol | Works. `0x55` update flow, 16-byte blocks, UID check. |
| Three-mode practical packages | Works as experimental binary workflow: mode1 canbox, mode2 update, mode3 logger. |
| GS USB logger mode | Works when mode3 enumerates as `1d50:606f`; used for two-bus logging. |
| Mac CDC port | Works as `/dev/cu.usbmodemKIA1` for stock/update CDC mode. |
| CAN speeds | Confirmed working setup: `M-CAN=100000`, `C-CAN=500000`. |
| Dashboard | Local web dashboard for replay, live logger, matrix view and controlled TX. |
| Clean C firmware build | Builds and packages from source in `firmware/custom_c/`. |
| Clean C USB CDC | Implemented as `KIA CANBOX 2CAN35`, VID/PID `0483:5740`. |
| Clean C raw CAN TX/RX | Implemented for lab use. |
| Clean C basic TEYES UART TX | Implemented behind `ENABLE_TEYES_UART=1`. |
| Clean C reverse output | Implemented on `PC14` behind `ENABLE_REVERSE_OUT=1`. |
| Logs and candidate tables | Labelled capture data and function matrix are in `logs/session_20260507/` and `data/can_function_matrix.csv`. |

### Experimental

| Area | Current state |
|---|---|
| `firmware/canlog/` packages | Useful for car testing, but still binary-wrapper workflow. |
| `firmware/custom_c/` daily firmware | Real C source exists, but not yet a full replacement for the programmer firmware. |
| TEYES/Raise semantic parser | Frame format and basic body/reverse packets are decoded; full identity/startup and all commands are not complete. |
| M-CAN media/navigation | Public DBC candidates and APK commands are mapped; named generators are not finalized. |
| Parking/SPAS/RCTA | Candidate IDs exist, exact byte/zone mapping still needs stronger captures. |
| Climate display/control | Candidate IDs exist, exact mapping is incomplete. |
| Raw CAN TX from dashboard | Available for controlled discovery; must be used carefully. |
| UART2 board passives | PA2/PA3 are known, but the bottom-side wire and missing resistor footprints still need continuity checks. |

### Not Implemented Yet

| Area | Missing work |
|---|---|
| Full clean-room canbox replacement | Need to promote verified CAN/UART/media/climate/parking functions into `firmware/custom_c`. |
| Full mode2 handoff from clean C | Clean C currently replies to mode commands but does not fully hand off to the stock loader. |
| Transparent Raise UART bridge | Needed for the external Raise button-chain plan. |
| TEYES/Raise identity exchange | Need exact startup/profile exchange for `HYK-RZ-10-0001-VK`. |
| Named M-CAN media/nav senders | Need confirmed frames for FM/AM/USB/BT/CarPlay/Android Auto/navigation/default compass. |
| Exact parking sensor/RCTA mapping | Need safer, isolated obstacle captures. |
| Exact climate mapping | Need isolated captures for each climate control state. |
| Second UART route | USART1 is referenced in reverse work, but board routing/purpose is not confirmed. |
| Physical reverse input pin | Reverse output is mapped; separate physical reverse input still needs pin confirmation. |

## Repository Layout

```text
dashboard/                  Local browser dashboard for replay, live logging and TX tests.
data/can_function_matrix.csv Working CAN/feature matrix.
docs/
  HARDWARE_WIRING_MOD_GUIDE.md      Wiring/rework guide adapted from Drive2.
  PROJECT_STATUS_FOR_AUTHOR.md      Short status file to send to the firmware author.
  CAN_FUNCTION_MATRIX.md            Human-readable feature matrix.
  HYUNDAI_KIA_MCAN_MEDIA_RESEARCH.md M-CAN media/navigation DBC candidates.
  REVERSE_SPORTAGE_2CAN35.md        APK/update/firmware reverse notes.
  FIRMWARE_V05_COMPARISON.md        Comparison of related programmer builds.
  SPORTAGE_0705_APK_04100007_ANALYSIS.md APK 07.05 / update 04100007 notes.
  RECOVERY_STLINK_SEQUENCE.md       ST-Link recovery notes.
firmware/
  MANIFEST.md                       Local firmware file hashes and status.
  canlog/                           Practical experimental binary packages.
  custom_c/                         Clean C firmware source.
logs/
  car_can_cleanjump_20260506_220618.txt   Small example capture.
  session_20260507/                 Curated capture summaries/tables.
samples/
  stations.txt                      FM station mapping extracted from APK work.
tools/
  usb_update_2can35.py              Stock USB update flow.
  usb_mode_2can35.py                Mode switching helper.
  gsusb_2can35_logger.py            GS USB logger.
  stockusb_canlog_2can35.py         CDC logger protocol.
  send_usb_display_demo.py          USB media/nav/FM test sender.
  analyze_can_log.py                CAN log summary tool.
  decode_2can35_update.py           Update package decoder/encoder helper.
```

Ignored local data:

- `firmware/local/`;
- raw APK/bin files without explicit publishing rights;
- `firmware/custom_c/build/`;
- `firmware/custom_c/dist/`;
- large raw full logs such as `logs/session_*/full_*.txt`.

## Build Clean C Firmware

Default build keeps CAN hardware disabled:

```sh
cd firmware/custom_c
make clean package
make sim
```

Full experimental build used for current source validation:

```sh
cd firmware/custom_c
make clean package \
  ENABLE_CAN_HW=1 \
  USE_STOCK_BEEP=1 \
  ENABLE_REVERSE_OUT=1 \
  ENABLE_TEYES_UART=1 \
  TEYES_UART_BAUD=19200 \
  VERSION_HEX=04351002
make sim
```

Expected generated package:

```text
build/2can35_custom_04351002_update.bin
```

The locally generated package hash from the last validation:

```text
a4662920bdd7de908bb4c31c1c0c612c99433daacc91f0f128a034e02489e743
```

## Use The Dashboard

```sh
python3 dashboard/server.py --port 8765
```

Open:

```text
http://127.0.0.1:8765
```

Dashboard roles:

- replay curated logs;
- start live GS USB capture;
- inspect function matrix;
- queue controlled raw CAN TX tests;
- send USB display/navigation/media experiments;
- switch modes where the current firmware supports it.

## USB Update / Mode Tools

Mode switch:

```sh
python3 tools/usb_mode_2can35.py /dev/cu.usbmodemKIA1 update
python3 tools/usb_mode_2can35.py /dev/cu.usbmodemKIA1 canlog
python3 tools/usb_mode_2can35.py /dev/cu.usbmodemKIA1 normal
```

Flash an update package:

```sh
python3 tools/usb_update_2can35.py /dev/cu.usbmodemKIA1 firmware/local/<update>.bin
```

Two-bus GS USB capture:

```sh
python3 tools/gsusb_2can35_logger.py \
  --bitrate0 100000 \
  --bitrate1 500000 \
  --seconds 120 \
  --outfile logs/live_test.txt
```

CDC logger fallback:

```sh
python3 tools/stockusb_canlog_2can35.py /dev/cu.usbmodemKIA1 --seconds 120
```

## CAN Mapping Workflow

Every new function should be promoted only after evidence:

1. Capture baseline.
2. Mark `START <event>` and `END <event>`.
3. Repeat the same action 3-5 times.
4. Compare `before / during / after`.
5. Identify channel, ID, byte, mask, off value and on value.
6. Add it to `data/can_function_matrix.csv`.
7. Test controlled TX only if the frame is safe.
8. Promote to a named firmware feature only after repeatable behavior.

Priority function groups:

- body: doors, trunk, hood, sunroof, locks, ignition;
- reverse: CAN reverse, physical input, +12 V output, dynamic lines;
- parking: front/rear sensors, SPAS, side warning, rear cross-traffic alert;
- climate: fan, driver/passenger temperature, AC, auto, defrost, recirculation,
  seat heat/ventilation;
- analog controls through Raise UART: steering wheel and piano buttons;
- media/source: FM, AM, USB, Bluetooth, CarPlay, Android Auto, default compass;
- navigation: street text, TBT, distance, ETA, speed limit if supported;
- diagnostics/status: outside temp, engine temp, speed, RPM, errors if visible.

## Safety Rules

- Keep logger sessions listen-only unless a test explicitly needs TX.
- Do not sweep active-control C-CAN frames blindly.
- Repeat display/media/navigation test frames with short bounded intervals, not
  unbounded loops.
- Do not flash third-party profile binaries to this UID unless the UID/header and
  target are understood.
- For ST-Link recovery, use `docs/RECOVERY_STLINK_SEQUENCE.md`.
- Treat `firmware/custom_c/` as experimental until daily-driver functions are
  confirmed on the car.

## External Sources

CAN and tooling:

- https://github.com/iDoka/awesome-canbus
- https://github.com/candle-usb/candleLight_fw
- https://www.can232.com/docs/can232_v3.pdf
- https://github.com/tixiv/lib-slcan
- https://github.com/homewsn/candleLight_fw-SavvyCAN-Windows-plugin
- https://github.com/normaldotcom/cangaroo
- https://github.com/collin80/SavvyCAN
- https://github.com/TOSUN-Shanghai/TSMaster

STM32 / firmware research:

- https://www.st.com/resource/en/datasheet/stm32f105rb.pdf
- https://github.com/CTXz/stm32f1-picopwner
- https://github.com/JohannesObermaier/f103-analysis/tree/master/h3

2CAN35 practical references:

- https://www.drive2.ru/l/717368666034802531/
- https://www.drive2.ru/l/717580596901055496/

Local project docs:

- `docs/PROJECT_STATUS_FOR_AUTHOR.md`
- `docs/REVERSE_SPORTAGE_2CAN35.md`
- `docs/CAN_FUNCTION_MATRIX.md`
- `firmware/custom_c/README.md`
