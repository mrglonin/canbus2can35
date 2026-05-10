# 2CAN35 Custom C Firmware

This is the first clean-source firmware tree for the 2CAN35/Sigma10 board.
It is intentionally separated from the binary patch workflow.

## Project decision

This tree is not the current production/vehicle branch. It is a lab branch for
USB, CAN, UART and board pinout experiments. The current vehicle-safe path is
the binary wrapper package in `firmware/canlog/`, where mode1 stays the
programmer's Sportage firmware and mode3 stays the proven `gs_usb` logger.

Do not flash a clean-C package as the normal car firmware unless the test goal
is explicitly to validate this lab branch and there is a known ST-Link recovery
path ready.

## Lab Target

- STM32F1 connectivity-line app at `0x08004000`.
- USB CDC ACM device, compatible with macOS serial tools.
  - VID/PID: `0483:5740`
  - Manufacturer: `KIA CANBOX`
  - Product: `KIA CANBOX 2CAN35`
  - Serial: `37FFDA054247303859412243`
- 2CAN35 framed control protocol stubs for UID/version and mode switching.
- ASCII lab protocol for live logging and raw CAN experiments.
- CAN speeds for this Sportage setup:
  - C-CAN: 500 kbit/s (`0S6`)
  - M-CAN: 100 kbit/s (`1S3`)
- Kia profile skeleton with labelled state fields from the captured logs.
- Optional reverse +12 V output on PC14 (`ENABLE_REVERSE_OUT=1`).
- Optional TEYES/Raise UART state TX on USART2 PA2/PA3, 19200 (`ENABLE_TEYES_UART=1`).
  - Body UART command `0x05` includes experimental sunroof flag `0x40`,
    generated from C-CAN `0x541 DATA[7] & 0x02`.

Hardware pin status is tracked in `docs/BOARD_IO_MAP.md`.
Photo-based board notes are tracked in `docs/BOARD_PHOTO_PINMAP.md`.
TEYES/Raise compatibility is tracked in `docs/TEYES_RAISE_COMPAT.md`.

## Safety status

Default build uses `ENABLE_CAN_HW=0`. It enumerates over USB and accepts commands,
but does not drive the CAN transceivers. This is deliberate for the first source
milestone.

Bench/car build with CAN hardware enabled:

```sh
make clean
make ENABLE_CAN_HW=1
```

Full experimental canbox build with CAN, beeper, reverse output and TEYES UART:

```sh
make clean package ENABLE_CAN_HW=1 USE_STOCK_BEEP=1 ENABLE_REVERSE_OUT=1 ENABLE_TEYES_UART=1 TEYES_UART_BAUD=19200
```

The board uses an 8 MHz HSE crystal for the USB clock tree. `CANBOX_HSE_MHZ`
defaults to `8`; do not build the USB test firmware with `16` unless a specific
board revision has been measured differently.

## Build

```sh
cd /Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/canbus2can35_repo/firmware/custom_c
make
make sim
```

Outputs:

- `build/2can35_custom.elf`
- `build/2can35_custom.bin`
- `build/2can35_custom_<version>_update.bin`
- `build/2can35_custom_<version>_report.json`

## USB ASCII commands

- `?` - status
- `O` - open CAN logging
- `C` - close CAN logging
- `I` - initialize CAN again
- `0S6` - set C-CAN to 500 kbit/s
- `1S3` - set M-CAN to 100 kbit/s
- `mode1` - logical canbox mode, one beep if stock beep calls are enabled
- `mode3` - logical lab mode, three beeps if stock beep calls are enabled
- `0t1238AABBCCDDEEFF0011` - send standard frame on C-CAN
- `1t1238AABBCCDDEEFF0011` - send standard frame on M-CAN
- `uFD050500000A` - send raw bytes to the TEYES/Raise UART when enabled
- TEYES/Raise UART RX frames are printed back to USB as `U FD...`

Media, navigation, climate and warning tests should be generated outside the
firmware by Python/web tooling through raw CAN TX. The firmware should not carry
one-off test scenarios; once a frame is verified it can be promoted into a real
feature handler.

## 2CAN35 binary frame subset

Request header is `BB 41 A1`, response header is `BB A1 41`.
The length byte is the total frame size including header, command, payload and
checksum. This matches the stock 2CAN35/APK protocol. Checksum is
`sum(frame_without_checksum) & 0xff`.

- `0x56` replies with UID, firmware version, current logical mode, and CAN-HW flag.
- `0x55 01` writes the stock update-loader magic to BKP DR1 and resets.
- `0x51 03` switches logical mode 3, matching the tested v08 binary wrapper.
- `0x55 03` also switches logical mode 3 as a lab compatibility path.
- `0x55 04` switches back to logical mode 1.
- `0x55 02` currently replies `E2`: the clean firmware does not yet know the stock loader's persistent update flag.
- `0x70` sends a raw standard CAN frame: `bus id_hi id_lo len data...`.

## Important limitation

The firmware is not a replacement for the programmer's Sportage logic. The
previous firmware behavior was reconstructed enough to organize fields and
transport, but not enough to claim a clean-room one-to-one implementation of all
dashboard/media/navigation/parking/climate behavior. Next work is to fill the
Kia profile with verified frames from labelled captures and only then enable
automatic CAN output.

Reverse +12 V output and basic TEYES/Raise UART TX are now implemented behind
compile-time flags. The firmware still is not a full replacement for the
programmer's logic: transparent Raise UART bridging for analog steering/piano
buttons, climate, parking sensors, media/nav named generators and TEYES command
parsing still need verified packet mappings.
