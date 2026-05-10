# Current Firmware Branch Decision

Date: 2026-05-08

## Decision

The current clean-C firmware branch is not suitable as the working car firmware.
It remains a lab branch only.

The stable practical branch is:

```text
programmer Sportage firmware in mode1
+ stock update path in mode2
+ proven gs_usb/budgetcan logger in mode3
```

Current practical package:

```text
firmware/canlog/2can35_04350008_canlog_v4_mode3_preserve_beeps_usb.bin
```

When UART lab access is required, use the separate lab package:

```text
firmware/canlog/2can35_04350008_mode3_lab_can_uart_usb.bin
```

That package keeps the same rule for mode1 and mode2, but replaces mode3 with a
CDC serial lab app:

```text
mode1 = programmer Sportage firmware
mode2 = stock update path
mode3 = CAN RX/TX + TEYES/Raise UART RX/TX lab mode
```

## Reason

The clean-C branch can compile and simulate USB/CAN/UART framing, but it does not
yet reproduce the full Sportage behavior from the programmer firmware:

- media/source/navigation behavior is incomplete;
- TEYES/Raise startup and identity flow is incomplete;
- transparent Raise UART bridge is not finished;
- parking/SPAS/RCTA behavior is not mapped fully;
- climate behavior is not mapped fully;
- stock update-loader handoff is not proven as a complete replacement path.

Because of that, using clean-C as mode1 risks losing working car functions.

## Working Rule

For real car testing:

- mode1 must remain the programmer firmware;
- mode2 must remain the known update path;
- mode3 may be our logger/lab mode;
- any new feature must first be verified from logs or controlled TX;
- only after verification should it be promoted into a future clean implementation.

For clean-C:

- keep it buildable;
- keep simulations passing;
- use it to document hardware and protocols;
- do not call it the active vehicle branch.
