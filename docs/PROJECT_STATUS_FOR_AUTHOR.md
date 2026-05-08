# Project Status For Firmware Author

Цель этого файла - быстро показать текущее состояние проекта человеку, который
уже понимает 2CAN35/Sigma10, Kia/Hyundai CAN и TEYES/Raise canbox протокол.

## Что Это За Проект

Плата:

- StarLine / 2CAN35-like board, PCB `StarLine 200-00002 REV.A`.
- MCU: `STM32F105RBT6`, LQFP64.
- User adapter UID: `37 FF DA 05 42 47 30 38 59 41 22 43`.
- Машина: Kia/Hyundai Sportage-family setup.
- C-CAN: 500 kbit/s.
- M-CAN: 100 kbit/s.
- TEYES/Raise profile seen in firmware strings: `HYK-RZ-10-0001-VK`.

The public repository intentionally does not publish third-party APK/update
binaries. It keeps hashes, decoded observations, tools, clean source and
capture data.

## What We Have Now

### 1. Existing Binary Workflow

The working practical firmware path is still the binary/update-package workflow:

- mode1: programmer canbox firmware behavior;
- mode2: stock USB update loader, APK-compatible;
- mode3: CAN logger / lab mode.

Current practical package documented in `firmware/canlog/README.md`:

```text
firmware/canlog/2can35_04350006_canlog_v4_mode3_preserve_beeps_usb.bin
```

It keeps programmer v06 mode1 behavior and adds our mode switching / logger
wrapper.

### 2. Clean C Firmware Source

Clean-source firmware now lives here:

```text
firmware/custom_c/
```

This is a real C firmware tree, not a disassembly dump:

- `src/` and `include/` contain the firmware code;
- `Makefile` builds an app for `0x08004000`;
- `tools/package_update.py` packs an APK/USB-compatible update file;
- `tools/sim_protocol.py` checks USB protocol framing;
- `tools/sim_teyes_raise.py` checks TEYES/Raise UART frame construction.

Current source-build identity:

| Field | Value |
|---|---|
| USB VID/PID | `0483:5740` |
| USB manufacturer | `KIA CANBOX` |
| USB product | `KIA CANBOX 2CAN35` |
| USB serial | `37FFDA054247303859412243` |
| Firmware version | `04 35 10 02` |
| App base | `0x08004000` |

Full experimental build command:

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

Generated local artifact:

```text
firmware/custom_c/dist/2can35_custom_04351002_kia_canbox_reverse_teyes_uart_canlog.bin
SHA256 a4662920bdd7de908bb4c31c1c0c612c99433daacc91f0f128a034e02489e743
```

`build/` and `dist/` are ignored by git; regenerate locally.

## Clean C Firmware Features

Implemented:

| Area | Status |
|---|---|
| Clock/runtime/vector setup | implemented |
| USB CDC ACM | implemented |
| USB ASCII lab protocol | implemented |
| 2CAN35 binary subset | implemented: `0x55`, `0x56`, `0x70` |
| CAN1 remap PB8/PB9 | implemented, C-CAN 500 kbit/s |
| CAN2 PB12/PB13 | implemented, M-CAN 100 kbit/s |
| Raw CAN TX/RX over USB | implemented |
| Stock beep calls | implemented behind `USE_STOCK_BEEP` |
| Reverse output PC14 | implemented behind `ENABLE_REVERSE_OUT` |
| TEYES/Raise UART2 PA2/PA3 | implemented behind `ENABLE_TEYES_UART` |
| TEYES body-open frame | implemented |
| TEYES reverse frame | implemented |
| Raw UART TX over USB | implemented |

Not complete yet:

| Area | Status |
|---|---|
| Full mode2 stock-loader handoff from clean C | not solved |
| Full TEYES/Raise identity exchange | not solved |
| Steering wheel keys over TEYES UART | not solved |
| Parking sensors / SPAS display | not solved |
| Climate display/control packets | not solved |
| Named M-CAN media/nav generators | not solved |
| Transparent SimpleSoft UART bridge | not solved |
| Second UART routing | not confirmed |
| Physical reverse input pin | not confirmed |

## Board Pin Map We Believe Today

See `firmware/custom_c/docs/BOARD_PHOTO_PINMAP.md` for details.

Confirmed or strongly supported:

| Function | MCU pin(s) | Notes |
|---|---|---|
| USB FS | PA11 DM, PA12 DP | standard STM32F105 OTG FS pins |
| C-CAN | PB8 RX, PB9 TX | CAN1 remap, 500 kbit/s |
| M-CAN | PB12 RX, PB13 TX | CAN2, 100 kbit/s |
| TEYES/Raise UART2 | PA2 TX, PA3 RX | 19200 8N1 |
| Reverse output control | PC14 | active high, logic control only |
| ST-Link | PA13 SWDIO, PA14 SWCLK, NRST | recovery/programming |
| BOOT0 | pin 60 | system bootloader control |

Open hardware question:

- Bottom-side yellow wire and two unpopulated resistor footprints may be part of
  UART2 routing, but this needs continuity checks from PA2/PA3 to the connector.
  47-100 ohm series resistors are plausible only if the footprints are confirmed
  to sit in series on TX/RX.

## Captured CAN/DBC Work

The repository includes:

- `logs/session_20260507/` - labelled capture session and extracted candidates.
- `docs/CAN_FUNCTION_MATRIX.md` - functional table.
- `data/can_function_matrix.csv` - CSV working matrix.
- `dashboard/` - local browser UI for replay, live logging and controlled TX.

Known/candidate CAN functions:

| Feature | Candidate source |
|---|---|
| front doors, trunk, hood, sunroof, ignition | C-CAN `0x541` |
| rear doors | C-CAN `0x553` |
| reverse gear | C-CAN `0x111`, byte4 `0x64`; fallback `0x169` low nibble `7` |
| vehicle speed | C-CAN `0x316` candidate |
| outside temperature | C-CAN `0x383` candidate |
| heated steering wheel status | C-CAN `0x559` candidate |
| parking/SPAS | C-CAN `0x436`, `0x390`, `0x4F4`, `0x58B` candidates |

## USB / APK Protocol Findings

APK/package protocol:

```text
BB dst src len cmd payload... checksum
checksum = sum(frame_without_checksum) & 0xff
Android/Mac -> adapter: BB 41 A1 ...
adapter -> Android/Mac: BB A1 41 ...
```

Important commands:

| Cmd | Meaning |
|---:|---|
| `0x20` | FM station text |
| `0x21` | media/source text |
| `0x22` | track text / track number |
| `0x30` | amp settings |
| `0x45` | navigation/TBT maneuver |
| `0x47` | distance/ETA |
| `0x48` | navigation on/off |
| `0x55` | update / mode switching |
| `0x56` | UID/version |
| `0x60` | settings |
| `0x70` | lab raw CAN / logger subset |

APK text paths appear to clamp display strings to 16 UTF-16LE characters.

## What We Need Help With

Most useful hints from the original firmware author would be:

1. Exact TEYES/Raise UART identity/startup exchange for `HYK-RZ-10-0001-VK`.
2. Whether USART1 is used, and which board pins/connectors route it.
3. Physical reverse input MCU pin, if used separately from CAN reverse.
4. Exact meaning of the media/source scheduler that caused spontaneous
   `Музыка USB` display.
5. Confirmed M-CAN frames for:
   - FM/AM source,
   - USB music,
   - Bluetooth music,
   - CarPlay/Android Auto source if supported,
   - navigation street/TBT/distance/ETA,
   - compass/default no-navigation state.
6. Parking sensor/SPAS packet mapping.
7. Climate display packet mapping.
8. Whether the two missing resistor footprints near the bottom-side wire are
   UART2 TX/RX series resistors and what nominal should be used.

## Safety Notes

- Keep active CAN injection controlled and repeat-limited.
- Do not sweep active-control IDs blindly on C-CAN.
- Clean C firmware defaults to `ENABLE_CAN_HW=0` unless explicitly built for car
  testing.
- Generated binaries are not the source of truth; source and documented build
  commands are.
