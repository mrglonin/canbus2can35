# Firmware Manifest

This repository intentionally stores firmware metadata, tooling and selected
lab packages, not raw third-party APK/update binaries unless publishing rights
are clear. The GitHub repo shown during the session is public, so raw binary
redistribution should only be enabled after the owner confirms publishing
rights.

Put local binaries in `firmware/local/` when working on the Mac. That directory
is ignored by git.

## Known Local Files

| File | Size | SHA256 | Status |
|---|---:|---|---|
| `/Users/legion/Downloads/2CAN35_base_1uart_usb_v3_0.bin` | 30488 | `9c6cf03a1179244fa44635a7b56ea20801f879af0d46b19c8beae0c826fba5c9` | Base full image / stock loader + app v3.0. |
| `/Users/legion/Downloads/37FFDA054247303859412243_04350004.bin` | 19168 | `a3cb8fc7edd3fd95083eb5f3332ca662a17ac686e4e543d9ce94b30fd046aa3d` | Programmer update for adapter UID `37 FF DA 05 42 47 30 38 59 41 22 43`, version `04 35 00 04`. Historical reference for early media/source experiments. |
| `/Users/legion/Downloads/37FFDA054247303859412243_04350006.bin` | 19492 | `c38db9b6e9f8dfdee0bc0a93e69f52b62cfee8e15b9a4a98e1b8e1e964fd6d05` | Programmer update for the user adapter, version `04 35 00 06`: optimized message handling, 60s parking dynamic-line delay, media fixes, BL music display. |
| `/Users/legion/Downloads/37FFDA054247303859412243_04350008.bin` | 19464 | `6b0f718f89e1bc1955dbeb03c0874d9ec83f1bf506cb1ce66920f7bca70d186f` | Programmer update for the user adapter, version `04 35 00 08`: TPMS request disabled, BL title/artist added, USB music updates on title changes, optimization. |
| `/Users/legion/Downloads/30FFD3054747353756551343_04100005.bin` | 19400 | `4fba0d32af864979a1945ed2528f21ddfa3ee6d66ac78940f0482ea381e7a2c8` | Other-car programmer update, UID `30 FF D3 05 47 47 35 37 56 55 13 43`, version `04 10 00 05`. Useful as v05 reference for media cleanup, reverse, and speed logic. Not directly flashable to the user adapter through stock USB loader. |
| `/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/30FFD3054747353756551343_04100007.bin` | 19616 | `0a93d1eb0e0283f48c563738b78c468d2b5ddc1f4921f3b2c882bf786c48aa2d` | Other-car programmer update, UID `30 FF D3 05 47 47 35 37 56 55 13 43`, version `04 10 00 07`. Decodes with `0x04/0x58`; close v05 successor with +216 bytes and same USB protocol. Reference only, not directly flashable to the user adapter through stock USB loader. |
| `/Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08/Sportage_07_05.apk` | 2458561 | `c5a922b322558192db789ccc48d1510930cda0200cf451215cd0a2c8d9231a2b` | New Sportage APK. Manifest/protocol unchanged; DEX change is mainly FYT/BL media callback cleanup, 500 ms duplicate suppression, and fallback track title handling. |
| `/Users/legion/Downloads/Sportage_08_05.apk` | 2458369 | `476976c54c032808f5f84fb2b8da78bdf20f393f53d6d880a74939869759e227` | Sportage APK 08.05. Manifest still `com.sorento.navi` version `2.1`; protocol family unchanged by scan. |
| `/Users/legion/Downloads/37FFDA054247303859412243_04350004_skipMediaState0_min.bin` | 19168 | `10c7d53447ee024e3bc92c8e12c57f7bbaf9e92197babd921162a67534f458f8` | Historical one-byte media scheduler experiment: `0x08005956: 05 -> 11`. It killed normal media/navigation display in testing, so it is not a current strategy. |
| `/Users/legion/Downloads/37FFDA054247303859412243_04350004_skipMediaState3_min.bin` | 19168 | `e3296f6a7a32a60ce054cee976799420327d56b56841a70868ed9047080ceb29` | Historical one-byte media scheduler experiment: `0x08005959: 3A -> 11`. Not a current strategy. |
| `/Users/legion/Downloads/gs_2can35.bin` | 11644 | `44ec1b79f7dc49a12beae622c33ac8cefb1f9b211419ee5e41451b208bad5609` | GS USB / budgetcan-style logger reference. Mac sees this class as VID/PID `1d50:606f` when the logger slot boots correctly. |
| `/Users/legion/Downloads/Sportage.apk` | 2351349 | `fb41a1f38c4f2a020aed3003c27301e527e24c01fc83c94512a2357e9e26898e` | Android app `com.sorento.navi`, version `2.1`; used only for reverse engineering. |
| `firmware/canlog/2can35_canlog_v1_usb_update.bin` | 19616 | `81245cc636644c86128b8d0f7d4c17fd4b3c1a44b127f2de7bf55709be8b61dd` | Our experimental CAN-log USB update package. |
| `firmware/canlog/2can35_canlog_v1_stlink_full.bin` | 65536 | `d8cbec300e75c264babbc98488e4c75a452ab2b3b37bb2af0d25b8deaa202813` | Our experimental CAN-log full ST-Link image. |
| `firmware/canlog/2can35_04350004_canlog_v4_final_mode3_mediafix_usb.bin` | 32176 | `93e197f3e9c1839daa52925cacbe70267552288e5fba2f908941b455946b84c0` | Current USB update package flashed on 2026-05-07: programmer v04 mode1 + gs_usb mode3 slot + local mode1->mode3 command + media fallback states 4/5/6 skipped. UID `37 FF DA 05 42 47 30 38 59 41 22 43`. |
| `firmware/canlog/2can35_04350004_canlog_v4_no_auto_media_usb.bin` | 32176 | `339a35e3c46ce7b906bb05046b2a97cc27ebe41edb98a74e46518d8d780864f0` | Historical diagnostic package. Same v04+mode3 package, but disables internal mode1 media/source fallback schedulers at `0x08005948`, `0x08005A40`, `0x08005A9C`, `0x08005AF8`, `0x08005B74`. It is not the current goal because it removes useful media/source behavior. |
| `firmware/canlog/2can35_04350004_canlog_v4_mode3_original_tbb_no_fallback_usb.bin` | 32176 | `6fc42aa40c26e7009bd327aff1637d56c5707616a6a51de7483e43bf125fcea8` | Historical diagnostic package after `no_auto_media` broke source/compass. Keeps v4-final mode3/software switching, restores original media scheduler TBB states 4/5/6 (`3D 49 4D`), and NOPs selected dispatcher branches at `0x08005B7A`, `0x08005B90`, `0x08005B9A`. |
| `firmware/canlog/2can35_04350006_canlog_v4_mode3_preserve_beeps_usb.bin` | 32176 | `a1caf625e9070be6c9da520336751982238c314b9ac3c90140839c370f4867e6` | Programmer v06 mode1 plus preserved local mode2/mode3 switching. Mode1 is v06 except required hooks: `0x08005474`, `0x08005482`, reset vector `0x08004004`; mode3 `gs_usb` logger remains at `0x08009000`. |
| `firmware/canlog/2can35_04350008_canlog_v4_mode3_preserve_beeps_usb.bin` | 32176 | `846d761cd1e7c26673c40bd9e8193e142b744f9ad2d7ab2a5a152b4d42b6c053` | Current package. Programmer v08 mode1 plus preserved local mode2/mode3 switching. Mode1 is v08 except required hooks: `0x08005478`, `0x08005486`, reset vector `0x08004004`; mode3 `gs_usb` logger remains at `0x08009000`. |
| `firmware/custom_c/dist/2can35_custom_04351002_kia_canbox_reverse_teyes_uart_canlog.bin` | 11184 | `a4662920bdd7de908bb4c31c1c0c612c99433daacc91f0f128a034e02489e743` | Generated locally from `firmware/custom_c/`. Clean C experimental source build: USB name `KIA CANBOX 2CAN35`, CAN HW enabled, stock beep calls enabled, reverse output enabled, TEYES UART enabled. Build artifact is ignored by git; regenerate with the documented make command. |

## Update Package Header

Programmer update files begin with 16 clear bytes:

```text
37 FF DA 05 42 47 30 38 59 41 22 43 04 35 00 04
|------------- STM32 UID -------------| | version |
```

Payload starts after byte 16 and is written by the adapter bootloader around
`0x08004000`. The app header is mirrored at `0x08003FF0`.

## Current Practical Rule

Use `firmware/canlog/2can35_04350008_canlog_v4_mode3_preserve_beeps_usb.bin`
when testing the programmer's v08 behavior with our three-mode wrapper. It keeps
normal canbox mode as the programmer's v08 update, preserves the existing
lower bootloader/selector beeps already on the adapter, and keeps the `gs_usb`
CAN logger in mode3:

- mode1/normal: CDC `/dev/cu.usbmodemKIA1`, VID/PID `0483:5740`
- mode3/CAN log: `gs_usb` / budgetcan, VID/PID `1d50:606f`
- mode1 -> mode3: send USB command `0x51` value `0x03`
- mode3 -> mode1: send patched `gs_usb` exit request

The v08 package does not carry the old v04 media NOP patch. Media/BL/parking
logic is left to the programmer's v08 implementation; only the local mode
switch hooks and reset hook are added.

The broader `no_auto_media` package disabled useful source switching and the
default compass path, so keep it only as a diagnostic reference. Current work is
about expanding supported C-CAN/M-CAN functions, not cutting media behavior.
