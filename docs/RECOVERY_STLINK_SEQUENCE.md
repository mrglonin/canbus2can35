# 2CAN35 ST-Link recovery sequence

Working recovery found on 2026-05-06.

Known-good base image:

`/Users/legion/Downloads/2CAN35_base_1uart_usb_v3_0.bin`

What worked:

1. BOOT0 disconnected.
2. ST-Link connected: GND, SWDIO, SWCLK, Vref/3.3V.
3. Manual RES/NRST to GND held before OpenOCD starts.
4. OpenOCD catches SWD under reset and reports:

   `SWD DPIDR 0x1ba01477`

5. Use VectorCatch + C_HALT before releasing reset:

   `mww 0xE000EDFC 0x01000001`

   `mww 0xE000EDF0 0xA05F0003`

6. Release RES-GND while OpenOCD is inside the sleep window.
7. Successful state:

   `halted due to breakpoint`

   then `program <bin> 0x08000000 verify`

8. Confirm success only when OpenOCD prints:

   `** Programming Finished **`

   `** Verified OK **`

Do not use BOOT0 unless the real BOOT0 pin is confirmed. In this session it pulled target voltage down to about 3.15V and did not enter DFU.
