# FM UART -> M-CAN trace in programmer firmware

Source firmware: `37FFDA054247303859412243_04350008.bin`, decoded app
`re/04350008/37FFDA054247303859412243_04350008.decoded_app_stlink64k.bin`.

## Input from TEYES / Raise UART

The head unit reports source and radio state with Raise/RZC command `0x09`.

Confirmed FM example:

```text
FD 08 09 02 00 65 46 00 BE
```

Decode:

- `FD` - Raise/RZC frame prefix;
- `08` - length field;
- `09` - HU/media status command;
- `02` - radio source;
- `00` - FM band;
- `65 46` - frequency bytes, decoded by our tooling as FM `101.70`;
- `00 BE` - checksum.

The same command family carries USB/BT/navigation state:

```text
FD 0A 09 16 00 00 00 00 02 00 2B  # USB music, play time 2s
FD 06 09 11 00 00 20              # Bluetooth music
FD 06 09 06 00 00 15              # navigation source
```

## Firmware path

The media/source parser starts at `0x080057EC`.

Important code points from the decoded v08 firmware:

| Address | Meaning |
|---:|---|
| `0x080057F0` | reads incoming source byte from the received packet |
| `0x080057F6` | compares it with cached previous source |
| `0x080057FE..0x08005804` | if previous source was USB `0x16`, clears/sends M-CAN `0x490` |
| `0x08005842..0x0800584C` | if previous source was radio `0x02`, clears/sends M-CAN `0x4E8` |
| `0x08005908..0x08005976` | active radio/FM branch: builds internal FM/source payload and schedules display update |

The direct USB/API parser has these confirmed command mappings:

| USB/API command | Firmware address | M-CAN ID | Role |
|---:|---:|---:|---|
| `0x20` | `0x08005458` | `0x4E8` | FM/station text transport |
| `0x21` | `0x08005464..0x0800546C` | `0x4F6` | media/source text transport in this firmware |
| `0x22` | `0x0800546E..0x08005476` | `0x490` | USB/track text transport |

This corrects the older dashboard assumption that generic media text should be
sent to `0x4E6`. Public DBCs still name `0x4E6` as a multimedia TP candidate,
but this programmer firmware uses `0x4F6` for command `0x21`.

## Practical conclusion

FM working in the programmer firmware is a two-layer flow:

1. TEYES sends UART `FD .. 09 02 ...` to the adapter. The adapter stores source
   state and frequency.
2. The adapter sends source-specific M-CAN transport frames. FM text uses
   `0x4E8`; USB/track uses `0x490`; firmware v08 maps media/source text command
   `0x21` to `0x4F6`.

For tests, prefer the stock USB/API path (`0x20`, `0x21`, `0x22`) or replay the
firmware-confirmed M-CAN IDs above. Sending random `0x114/0x197` payloads alone
is not enough to reproduce the working FM behavior.
