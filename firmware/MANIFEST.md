# Firmware Manifest

This repository intentionally stores firmware metadata and tooling, not the
third-party APK/update binaries themselves. The GitHub repo shown during the
session is public, so binary redistribution should only be enabled after the
owner confirms publishing rights.

Put local binaries in `firmware/local/` when working on the Mac. That directory
is ignored by git.

## Known Local Files

| File | Size | SHA256 | Status |
|---|---:|---|---|
| `/Users/legion/Downloads/2CAN35_base_1uart_usb_v3_0.bin` | 30488 | `9c6cf03a1179244fa44635a7b56ea20801f879af0d46b19c8beae0c826fba5c9` | Base full image / stock loader + app v3.0. |
| `/Users/legion/Downloads/37FFDA054247303859412243_04350004.bin` | 19168 | `a3cb8fc7edd3fd95083eb5f3332ca662a17ac686e4e543d9ce94b30fd046aa3d` | Programmer update for adapter UID `37 FF DA 05 42 47 30 38 59 41 22 43`, version `04 35 00 04`. Normal mode works, but observed fallback media spam `Музыка USB`. |
| `/Users/legion/Downloads/30FFD3054747353756551343_04100005.bin` | 19400 | `4fba0d32af864979a1945ed2528f21ddfa3ee6d66ac78940f0482ea381e7a2c8` | Other-car programmer update, UID `30 FF D3 05 47 47 35 37 56 55 13 43`, version `04 10 00 05`. Useful as v05 reference for media cleanup, reverse, and speed logic. Not directly flashable to the user adapter through stock USB loader. |
| `/Users/legion/Downloads/37FFDA054247303859412243_04350004_skipMediaState0_min.bin` | 19168 | `10c7d53447ee024e3bc92c8e12c57f7bbaf9e92197babd921162a67534f458f8` | Temporary one-byte patch: `0x08005956: 05 -> 11`. It suppressed `Музыка USB`, but also killed normal media/navigation display in testing. Use only as temporary "music off" experiment. |
| `/Users/legion/Downloads/37FFDA054247303859412243_04350004_skipMediaState3_min.bin` | 19168 | `e3296f6a7a32a60ce054cee976799420327d56b56841a70868ed9047080ceb29` | One-byte patch: `0x08005959: 3A -> 11`. It did not remove the spam in car test. |
| `/Users/legion/Downloads/gs_2can35.bin` | 11264 | `44ec1b79f7dc49a12beae622c33ac8cefb1f9b211419ee5e41451b208bad5609` | GS USB / budgetcan-style logger reference. Mac sees this class as VID/PID `1d50:606f` when the logger slot boots correctly. |
| `/Users/legion/Downloads/Sportage.apk` | 2351349 | `fb41a1f38c4f2a020aed3003c27301e527e24c01fc83c94512a2357e9e26898e` | Android app `com.sorento.navi`, version `2.1`; used only for reverse engineering. |
| `firmware/canlog/2can35_canlog_v1_usb_update.bin` | 19616 | `81245cc636644c86128b8d0f7d4c17fd4b3c1a44b127f2de7bf55709be8b61dd` | Our experimental CAN-log USB update package. |
| `firmware/canlog/2can35_canlog_v1_stlink_full.bin` | 65536 | `d8cbec300e75c264babbc98488e4c75a452ab2b3b37bb2af0d25b8deaa202813` | Our experimental CAN-log full ST-Link image. |

## Update Package Header

Programmer update files begin with 16 clear bytes:

```text
37 FF DA 05 42 47 30 38 59 41 22 43 04 35 00 04
|------------- STM32 UID -------------| | version |
```

Payload starts after byte 16 and is written by the adapter bootloader around
`0x08004000`. The app header is mirrored at `0x08003FF0`.

## Current Practical Rule

For tomorrow, do not start by flashing experimental combined images. Start from
known-good programmer firmware, prove normal mode, then switch to logger only
when we are ready to capture.
