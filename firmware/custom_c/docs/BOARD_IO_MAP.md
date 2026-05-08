# 2CAN35 Board I/O Map

This file tracks what is known about the custom 2CAN35 board. Do not promote an
I/O to firmware output until the pin and polarity are confirmed.

Photo-based board notes and STM32F105RBT6 LQFP64 pin numbers are in
`docs/BOARD_PHOTO_PINMAP.md`.

## Confirmed / Used In Custom C Firmware

| Function | MCU pins / path | Status | Notes |
|---|---|---|---|
| C-CAN | CAN1 remap PB8 RX, PB9 TX | implemented | Car C-CAN is 500 kbit/s. |
| M-CAN | CAN2 PB12 RX, PB13 TX | implemented | Car M-CAN is 100 kbit/s. |
| CAN transceiver wake/silent | PB15, PB11 | candidate implemented | Inferred from the working logger; polarity kept as in previous code. |
| USB device | OTG FS, CDC ACM | implemented | One visible serial device; USB internally has CDC control + data interfaces. |
| Mode button | PB14 input | confirmed from emulator notes, not used in clean C yet | Previous mode selector used PB14 low = pressed. |
| Beeper | stock ROM calls `0x0800057d`, `0x0800064d`, `0x08000629` | implemented through stock calls | Count-based beep only. Tone/PWM pin is not confirmed. |
| Reverse +12 V output | PC14, active high | implemented behind `ENABLE_REVERSE_OUT` | Confirmed from v06 function that calls GPIO set/reset after reverse state changes. |
| TEYES/Raise UART | USART2 PA2 TX, PA3 RX, 19200 8N1 | implemented behind `ENABLE_TEYES_UART` | Confirmed from v06 USART object at RAM `0x200000ac` copied from flash table `0x080089f4`. |

## Required But Not Yet Implemented

| Function | Status | Why not enabled yet |
|---|---|---|
| Full UART bridge to HU/SimpleSoft | partially implemented | Basic TEYES state TX and raw UART TX exist; transparent SimpleSoft pass-through/filter is not implemented yet. |
| Second UART | not implemented | USART1 exists in firmware references, but routing and purpose are not mapped. |
| Physical reverse input | not implemented | User said physical reverse exists on the board path, but exact MCU input pin is not confirmed. |
| Other GPIO outputs | unknown | Need board probing or reverse-engineering of programmer firmware GPIO init/table. |

## CAN Features Already Parsed From Logs

| Feature | CAN source | Current firmware state |
|---|---|---|
| Front doors / trunk / hood / sunroof / ignition | C-CAN `0x541` | read-only parsed |
| Rear doors | C-CAN `0x553` | parsed with v06 firmware bit mapping |
| Reverse gear | C-CAN `0x111`, byte4 `0x64`; fallback `0x169` low nibble `7` | parsed; can drive PC14 when `ENABLE_REVERSE_OUT=1` |
| Speed | C-CAN `0x316` | read-only parsed candidate |
| Outside temperature | C-CAN `0x383` | read-only parsed candidate |
| Heated steering wheel status | C-CAN `0x559` | read-only parsed |
| Parking/SPAS | C-CAN `0x436`, `0x390`, `0x4F4`, `0x58B` | logged candidates only; exact display/action mapping not finalized |

## Media / Navigation / Climate Output Policy

Keep the firmware generic while we are searching:

- firmware exposes raw CAN TX/RX;
- Python/web tooling sends candidate frames and repeats them when needed;
- once a frame is verified on the car, promote it to a named firmware feature.

This avoids hardcoding one-off test commands into flash and keeps mode1 small.

## Next Pin-Mapping Work

1. Verify PC14 reverse output on the bench/car with a meter before using it as the only reverse trigger.
2. Add a transparent UART bridge only after deciding whether SimpleSoft stays physically in line.
3. Identify USART1 routing and whether it is the second HU/SimpleSoft path.
4. Promote more decoded TEYES/Raise packets: steering keys, parking sensors, climate, outside temp.
5. Keep raw CAN/UART TX available for testing before hardcoding named actions.
