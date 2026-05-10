# TEYES / Raise Compatibility Layer

## Where This Lives

TEYES does not learn the car model directly from the car CAN bus. The user
selects a CAN box family/profile in Android settings, for example:

```text
Raise -> Hyundai/Kia -> Sportage
```

That selection tells the head unit which **canbox UART protocol** to speak.
The external adapter must then talk to the head unit in that protocol.

So the firmware has three separate layers:

| Layer | Direction | Purpose |
|---|---|---|
| Car CAN parser | car C-CAN/M-CAN -> adapter | Decode doors, reverse, climate, parking, speed and other vehicle states. |
| TEYES/Raise UART protocol | adapter <-> head unit UART | Report decoded car state to Android and accept head-unit commands. |
| M-CAN cluster/media sender | adapter/USB/app -> car M-CAN | Show FM/music/navigation/cluster text in the factory cluster. |
| External Raise button bridge | analog buttons -> stock Raise canbox -> adapter UART2 -> head unit | Preserve steering wheel and piano-panel buttons without reimplementing analog decoding. |

## Known Evidence

The decoded programmer firmware contains this literal:

```text
HYK-RZ-10-0001-VK
```

This is not a vehicle CAN message. It is a canbox/profile identifier exposed by
the adapter side so the Android/app/head unit can identify the profile.

The decoded firmware also references:

```text
USART1: 0x40013800
USART2: 0x40004400
```

The v06 decoded data table now confirms the first TEYES/Raise UART path:

| Item | Evidence | Result |
|---|---|---|
| USART | RAM object at `0x200000ac`, copied from flash `0x080089f4` | `USART2` / `0x40004400` |
| Pins | same object: `GPIOA`, pin masks `0x04`, `0x08` | PA2 TX, PA3 RX |
| Baud | same object, offset `0x4c`, value `0x4b00` | 19200 8N1 |
| Parser | function around `0x08004e18` | waits for `0xfd` frame prefix |
| TX ring | functions around `0x08004c18..0x08004d2e` | USART2 interrupt/ring based TX/RX |

## UART Frame Format

The Raise/RZC Korea frame format is now confirmed by both local v06/v08
firmware work and public Android head-unit code (`CanBoxProtocol.java`,
`RZC_KoreaSeriesProtocol.java`). The detailed command matrix lives in
`../../../docs/RAISE_RZC_KOREA_UART_MATRIX.md`.

The frame format is:

```text
FD LL CC PP... HH LL
```

Where:

- byte `0`: fixed prefix `0xFD`;
- byte `1`: length field, equal to total frame bytes minus one;
- byte `2`: command;
- bytes `3..`: payload;
- final two bytes: 16-bit checksum, high byte then low byte;
- checksum is the unsigned sum of bytes `1` through the last payload byte.

Confirmed examples:

| Meaning | Bytes |
|---|---|
| all body openings closed | `FD 05 05 00 00 0A` |
| all door/trunk/hood bits set | `FD 05 05 3F 00 49` |
| reverse off | `FD 06 7D 06 00 00 89` |
| reverse on | `FD 06 7D 06 02 00 8B` |

Body flags for command `0x05`, taken from the v06 body parser:

| Bit | Meaning |
|---|---|
| `0x01` | left front door |
| `0x02` | right front door |
| `0x04` | left rear door |
| `0x08` | right rear door |
| `0x10` | trunk |
| `0x20` | hood |
| `0x40` | sunroof open, experimental; derived from C-CAN `0x541 DATA[7] & 0x02` and needs TEYES/HU confirmation |

Experimental sunroof UART frame:

```text
FD 05 05 40 00 4A
```

Important: public RZC Korea Android code decodes only door/trunk/hood bits
`0x01..0x20` for command `0x05`. `0x40` is a local experiment, not confirmed as
a stock TEYES sunroof UI event.

Public RZC Korea command IDs:

| Direction | Cmd | Meaning |
|---|---:|---|
| canbox -> HU | `0x01` | outside temperature |
| canbox -> HU | `0x02` | steering wheel / panel key |
| canbox -> HU | `0x03` | air conditioning display |
| canbox -> HU | `0x04` | radar / parking sensors |
| canbox -> HU | `0x05` | doors, trunk, hood |
| canbox -> HU | `0x06` | climate key/status popup |
| canbox -> HU | `0x07` | backlight |
| canbox -> HU | `0x7F` | protocol version |
| HU -> canbox | `0x04` | power/session start/end |
| HU -> canbox | `0x05` | amplifier volume |
| HU -> canbox | `0x06` | time sync |
| HU -> canbox | `0x07` | amplifier balance/fade |
| HU -> canbox | `0x08` | amplifier bass/mid/treble |
| HU -> canbox | `0x09` | host media/source status |

The WR-log frame from the head unit:

```text
FD 0A 09 16 00 00 00 00 02 00 2B
```

is `HU -> canbox`, command `0x09`, source `0x16` = USB media, track `0`,
play time `2s`. It should be parsed and converted to M-CAN cluster/media frames;
it is not itself an M-CAN frame.

## What We Must Implement

Create a dedicated module:

```text
src/teyes_raise_uart.c
include/teyes_raise_uart.h
```

Responsibilities:

1. Initialize USART2 PA2/PA3 at 19200.
2. Send basic decoded vehicle state:
   - ACC/ignition
   - doors
   - trunk
   - hood
   - reverse
   - parking brake / brake if needed
   - outside temperature
   - climate display state
   - parking sensor state
   - bridged Raise UART button frames from the external stock Raise canbox
4. Parse commands coming from the head unit:
   - source/media mode if the protocol sends it
   - climate control requests
   - settings requests
   - update/service commands if present

## What Is Not The Same As Cluster M-CAN

TEYES canbox UART reporting is not the same thing as sending M-CAN frames like:

```text
0x114, 0x197, 0x490, 0x4E6, 0x4E8, 0x49B, 0x4BB
```

Those M-CAN frames are for the car/cluster side. TEYES base UI widgets usually
expect the canbox UART protocol instead.

## Current Clean Firmware Status

| Feature | Status |
|---|---|
| Car CAN read | partially implemented |
| Raw CAN TX/RX over USB | implemented |
| Raw TEYES UART TX over USB (`u...`) | implemented behind `ENABLE_TEYES_UART` |
| TEYES/Raise UART identity | not implemented |
| TEYES/Raise body open packet | implemented behind `ENABLE_TEYES_UART` |
| TEYES/Raise reverse packet | implemented behind `ENABLE_TEYES_UART` |
| TEYES/Raise command parser | frame drain only; no semantic parser yet |
| M-CAN media/nav frame generator | not implemented as named features yet |

## Next Reverse-Engineering Steps

1. Find whether `HYK-RZ-10-0001-VK` is sent through UART, USB, or only embedded as profile metadata.
2. Add a transparent Raise UART bridge for analog steering wheel and piano-panel buttons.
3. Decode command IDs for parking sensors, climate display and outside temperature.
4. Add UART RX semantic parser once we know which commands TEYES sends back.
5. Confirm whether a second UART route is needed for any other in-line device.
6. Promote verified M-CAN media/nav packets into named senders.

Only after this layer exists will selecting `Raise -> Hyundai/Kia -> Sportage`
in TEYES make the head unit show the basic canbox functions from our clean
firmware.
