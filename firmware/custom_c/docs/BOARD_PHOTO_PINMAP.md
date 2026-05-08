# Board Photo Pin Map

This note connects the board photos to the firmware pin map. It is a working
map, not a soldering instruction. Pads and missing resistors must be confirmed
with continuity checks before hardware changes.

## Board Identity From Photos

| Item | Observation | Confidence |
|---|---|---|
| PCB | `StarLine 200-00002 REV.A`, top marking `0419` | high |
| MCU | `STM32F105RBT6` in LQFP64 | high |
| USB connector | Micro-USB to MCU USB OTG FS | high |
| Buzzer | round part at lower-left top side | high |
| Main connector | long top header | high |
| Service pads | bottom-side gold test pads near lower-left/center | high |
| Yellow bodge wire | bottom side, from lower-left test pad to center pad/footprint area | high |
| Missing UART2 resistors | plausible, but not proven by image alone | low until probed |

## Current USB Identity

The device currently seen on macOS before this source build was:

| Field | Value |
|---|---|
| Product | `CDC_ACM` / `CDC-ACM` |
| Manufacturer | `Home Made Technologies` |
| VID:PID | `0483:5740` |
| Serial | `KIA` |

The custom C build now advertises:

| Field | Value |
|---|---|
| Product | `KIA CANBOX 2CAN35` |
| Manufacturer | `KIA CANBOX` |
| VID:PID | `0483:5740` |
| Serial | `37FFDA054247303859412243` |

The VID/PID stays the same to keep CDC ACM behavior and the existing update
path simple.

## MCU Pin Reference

Pin numbers below are for STM32F105RBT6 LQFP64. The package pinout is from the
official ST STM32F105/107 datasheet.

| Signal | LQFP64 pin | MCU function | Firmware use |
|---|---:|---|---|
| PC14 | 3 | GPIO / OSC32_IN | reverse +12 V output control, active high |
| NRST | 7 | reset | ST-Link reset / boot control |
| PA2 | 16 | USART2_TX | TEYES/Raise UART TX |
| PA3 | 17 | USART2_RX | TEYES/Raise UART RX |
| PB12 | 33 | CAN2_RX | M-CAN RX, 100 kbit/s |
| PB13 | 34 | CAN2_TX | M-CAN TX, 100 kbit/s |
| PB14 | 35 | GPIO / SPI2_MISO | mode button candidate |
| PB15 | 36 | GPIO / SPI2_MOSI | CAN transceiver enable/silent candidate |
| PA11 | 44 | OTG_FS_DM / CAN1_RX default | USB DM |
| PA12 | 45 | OTG_FS_DP / CAN1_TX default | USB DP |
| PA13 | 46 | SWDIO | ST-Link SWDIO |
| PA14 | 49 | SWCLK | ST-Link SWCLK |
| PB11 | 30 | GPIO / USART3_RX | CAN transceiver enable/silent candidate |
| PB8 | 61 | CAN1_RX remap | C-CAN RX, 500 kbit/s |
| PB9 | 62 | CAN1_TX remap | C-CAN TX, 500 kbit/s |
| BOOT0 | 60 | boot select | system bootloader when pulled high |

Important: PC14 has limited output capability on STM32F105. It is OK as a logic
control into a transistor/driver, not as a direct load output.

## What The Yellow Wire Most Likely Is

The yellow wire is a board modification that links an unused/test pad to a pad
near an unpopulated footprint. Given the firmware traces, the most likely
purpose is to route one side of an extra serial/CANbox signal path, but this is
not confirmed by the image alone.

Do not assume the two empty resistor footprints are UART2 until these checks
pass.

## UART2 Continuity Checks

Use power off. Meter in continuity mode.

| Check | Probe A | Probe B | Expected result |
|---|---|---|---|
| UART2 TX source | STM32 PA2, pin 16 | near side of candidate resistor footprint 1 | beep/low ohms if this is TX |
| UART2 RX source | STM32 PA3, pin 17 | near side of candidate resistor footprint 2 | beep/low ohms if this is RX |
| Connector TX path | far side of footprint 1 | TEYES/Raise UART connector pin | beep/low ohms |
| Connector RX path | far side of footprint 2 | TEYES/Raise UART connector pin | beep/low ohms |
| Yellow wire net | each end of yellow wire | PA2/PA3/connector pads | identify which net it bridges |
| Isolation | candidate pads | CANH/CANL pins | should not be shorted |

If the two footprints are in series between PA2/PA3 and the connector, 47-100 ohm
series resistors are plausible. If they are only solder jumpers, 0 ohm links may
be the original intent. The photo cannot decide that; continuity and the original
trace routing decide it.

## Practical Board Test Order

1. With USB only, confirm macOS enumerates `KIA CANBOX 2CAN35`.
2. Send `?` over the CDC serial port and verify version `04 35 10 02`.
3. With CAN disconnected, verify no unexpected reverse output on PC14.
4. With CAN connected, verify C-CAN frames on bus 0 and M-CAN frames on bus 1.
5. Put reverse gear and measure the reverse +12 V output path.
6. Only after UART2 continuity is proven, test `uFD050500000A` raw UART TX.

## Open Items

| Item | Status |
|---|---|
| Exact main connector pinout | needs continuity from header pins to MCU/transceivers |
| USART1 routing | unknown |
| Second UART hardware path | unknown; likely around unpopulated passives |
| Physical reverse input | unknown |
| Exact transceiver enable polarity | copied from previous working behavior, still should be scoped |
