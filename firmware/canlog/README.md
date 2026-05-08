# 2CAN35 CAN Logger Firmware

These are the current experimental CAN-log artifacts for the adapter UID:

```text
37 FF DA 05 42 47 30 38 59 41 22 43
```

They are not the final daily-driver canbox firmware. Use them when the goal is
to capture CAN traffic from the car and then return to the known-good canbox
firmware.

## Files

| File | Size | SHA256 | Use |
|---|---:|---|---|
| `2can35_canlog_v1_usb_update.bin` | 19616 | `81245cc636644c86128b8d0f7d4c17fd4b3c1a44b127f2de7bf55709be8b61dd` | Flash through the normal USB update loader. |
| `2can35_canlog_v1_stlink_full.bin` | 65536 | `d8cbec300e75c264babbc98488e4c75a452ab2b3b37bb2af0d25b8deaa202813` | Full image for ST-Link recovery/programming. |
| `2can35_04350004_canlog_v4_final_mode3_mediafix_usb.bin` | 32176 | `93e197f3e9c1839daa52925cacbe70267552288e5fba2f908941b455946b84c0` | Previous working v04 + mode3 package. It skips selected media fallback states but may still allow spontaneous source/media display paths. |
| `2can35_04350004_canlog_v4_no_auto_media_usb.bin` | 32176 | `339a35e3c46ce7b906bb05046b2a97cc27ebe41edb98a74e46518d8d780864f0` | Current car-test package. It keeps UART and explicit APK/USB display commands, but disables internal mode1 media/source fallback schedulers so media should not appear unless sent. |
| `2can35_04350004_canlog_v4_mode3_original_tbb_no_fallback_usb.bin` | 32176 | `6fc42aa40c26e7009bd327aff1637d56c5707616a6a51de7483e43bf125fcea8` | Current candidate. It restores source/compass scheduler states and disables only fallback branches to static text senders. |
| `2can35_04350006_canlog_v4_mode3_preserve_beeps_usb.bin` | 32176 | `a1caf625e9070be6c9da520336751982238c314b9ac3c90140839c370f4867e6` | Programmer v06 mode1, plus preserved software mode2/mode3 switching and GS USB logger slot. |

## Current Three-Mode Package

Use `2can35_04350006_canlog_v4_mode3_preserve_beeps_usb.bin` for current v06
car tests:

- mode1: normal programmer v06 canbox behavior, CDC `/dev/cu.usbmodemKIA1`.
- mode2: stock USB update loader.
- mode3: GS USB CAN logger, VID/PID `1d50:606f`.
- mode1 -> mode3: USB command `0x55` value `0x03`.
- mode3 -> mode1: patched GS USB exit request from the dashboard.
- mode1 -> reset/normal: USB command `0x55` value `0x04`.
- media/BL/parking logic: programmer v06 behavior, without the old v04 media
  NOP patch.
- direct commands still enabled: FM `0x20`, media/source `0x21`, track `0x22`,
  navigation maneuver `0x45`, ETA/distance `0x47`, nav on/off `0x48`, update
  `0x55`, UID/version `0x56`, settings `0x60`, amp `0x30`.

The broader `no_auto_media` build is a diagnostic only. It removed spontaneous
USB music, but also removed source switching and the default compass display
path in car testing.

## Expected USB Logger Protocol

The CDC logger path uses the same serial transport as the normal APK protocol:

```text
port:  /dev/cu.usbmodemKIA1
baud:  19200 8N1
frame: BB dst src len cmd payload... checksum
```

Start logging:

```text
BB 41 A1 07 70 01 CS
```

Stop logging:

```text
BB 41 A1 07 70 00 CS
```

CAN frame from adapter:

```text
BB A1 41 16 70 02 bus flags can_id_le[4] dlc data[8] CS
```

Fields:

- `bus`: adapter CAN channel number.
- `flags bit0`: extended CAN ID.
- `flags bit1`: RTR frame.
- `can_id_le`: little-endian 11-bit or 29-bit CAN ID.
- `dlc`: 0..8.
- `data`: up to 8 bytes, padded in the USB frame.

Capture command:

```bash
python3 ../../tools/stockusb_canlog_2can35.py /dev/cu.usbmodemKIA1 --seconds 120 > ../../logs/live_cdc_canlog_v1.txt
```

## Flash Through USB Loader

From repository root:

```bash
python3 tools/usb_update_2can35.py \
  /dev/cu.usbmodemKIA1 \
  firmware/canlog/2can35_canlog_v1_usb_update.bin
```

If USB disappears after flashing, power-cycle the adapter for 3-5 seconds and
check the port again:

```bash
ls /dev/cu.usbmodem*
```

## Flash Through ST-Link

Use only when USB update is not available or the board needs recovery. Follow:

```text
docs/RECOVERY_STLINK_SEQUENCE.md
```

The short version:

1. Connect SWDIO, SWCLK, GND, VTref/3.3V sense.
2. Use connect-under-reset if readout protection blocks attach.
3. Release reset only when the tool is actually connecting.
4. Program the full image `2can35_canlog_v1_stlink_full.bin`.
5. Power-cycle after a full flash.

## Known Caveats

- This is not the final three-mode firmware.
- It should be used for capture sessions, not for final car integration.
- If it does not raise CDC on Mac after flashing, do not keep reflashing blindly:
  power-cycle, confirm USB enumeration, then recover through ST-Link if needed.
- For two-bus capture the preferred stable path remains the GS USB mode when it
  enumerates as `1d50:606f`; this CDC logger is kept as our own APK-compatible
  logging path.
