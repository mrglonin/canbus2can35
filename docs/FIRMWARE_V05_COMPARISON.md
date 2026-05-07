# Firmware 04 vs 05 Comparison

Дата: 2026-05-07

## Input Files

| Role | File | SHA256 | Notes |
|---|---|---|---|
| User current update | `/Users/legion/Downloads/37FFDA054247303859412243_04350004.bin` | `a3cb8fc7edd3fd95083eb5f3332ca662a17ac686e4e543d9ce94b30fd046aa3d` | UID `37 FF DA 05 42 47 30 38 59 41 22 43`, version `04 35 00 04`. |
| Other-car v05 update | `/Users/legion/Downloads/30FFD3054747353756551343_04100005.bin` | `4fba0d32af864979a1945ed2528f21ddfa3ee6d66ac78940f0482ea381e7a2c8` | UID `30 FF D3 05 47 47 35 37 56 55 13 43`, version `04 10 00 05`. |

The v05 update is not directly flashable to the user adapter through the stock
USB loader because the clear UID header is for another STM32. It is still useful
as a code reference.

## Decode Result

The old Sportage/user v04 package decodes with:

```text
key_a = 0x04
key_b = 0x5B
```

The other-car v05 package does not decode correctly with `0x04/0x5B` inside the
protected code window. The best structural match is:

```text
key_a = 0x04
key_b = 0x58
```

Evidence:

- brute-force structural score picked `0x04/0x58` as a clear winner
  (`1476` vs next candidate `854`);
- decoded vectors are valid: SP `0x20010000`, reset `0x08006DE1`;
- v05 code/data aligns to v04 mostly with shift `0xC4`;
- v04 media scheduler table is found again in v05 only after decoding with
  `0x04/0x58`;
- shifted byte equality improves to about `50.6%`, which is plausible for a
  close firmware with inserted code and relocated addresses.

Decoded local artifacts:

```text
re/compare_v04_v05/key_04_58/30FFD3054747353756551343_04100005.decoded_package.bin
re/compare_v04_v05/key_04_58/30FFD3054747353756551343_04100005.decoded_payload.bin
re/compare_v04_v05/key_04_58/30FFD3054747353756551343_04100005.decoded_app_stlink64k.bin
```

These artifacts stay local and are not meant for public publishing.

## Media Fix Findings

The old one-byte experiment patched the scheduler table:

```text
v04 scheduler: 0x08005948
v04 table:     0x08005956 = 05 1A 2D 3A 3D 49 4D 51 61 63
```

That suppressed the fallback "Музыка USB" state but also broke normal
music/navigation because it removed a scheduler state, not the actual spam
cause.

In v05 the same scheduler still exists, shifted:

```text
v05 scheduler: 0x080059C0
v05 table:     0x080059CE = 05 1A 2D 3A 3D 49 4D 51 61 63
```

So the v05 fix is not "delete media state from the table". The useful change is
before the scheduler, in the USB media/source parser.

### Added Source-Change Cleanup

New v05 logic starts around:

```text
0x08005764
```

It keeps the previous media source byte in RAM:

```text
last_source = 0x20000763
```

When the source changes and the previous source was `0x16`, v05 sends a cleanup
frame for the USB-media path:

```text
0x08005770: previous source equals current source? skip cleanup
0x08005772: previous source == 0x16?
0x08005776: CAN/M-CAN target id 0x490
0x0800577C: send through local TX helper 0x0800541C
0x08005784: clear bytes [tx_buffer + 5] and [tx_buffer + 6]
0x08005788: last_source = current_source
```

There is also a branch that clears/sends through id `0x4E8` when mode `2` is
selected:

```text
0x080057AE..0x080057BE
```

Interpretation: v05 explicitly clears stale media state when switching away
from USB/music/radio modes. This matches the changelog line:

```text
добавлена очистка медиа при переключении режима
```

### Changed USB Music Sending Logic

The USB music body is around:

```text
0x080057DE..0x080058C0
```

Important behavior:

- builds the display payload with state byte `0x65` and marker `0xC1`;
- copies track/source bytes from the incoming USB packet;
- caches the last transmitted pair in RAM;
- only triggers the media scheduler when those bytes actually change.

This matches the changelog line:

```text
изменена логика отправки сообщений для USB музыки
```

Practical conclusion: for our firmware, port the source-change cleanup and
cache/debounce logic. Do not patch the scheduler table as a spam fix.

## Reverse Gear Addition

v05 expands the tracked CAN-id list. v04 watched:

```text
0x131, 0x132, 0x134, 0x181, 0x183
```

v05 additionally watches:

```text
0x169
```

Relevant locations:

```text
0x08004A96: detects CAN id 0x169 in the buffered CAN scan
0x08004B24: stores 0x169 into slot 0x0D
0x080051A8: parses id 0x169
```

The parser checks:

```text
if can_id == 0x169:
    state = ((data_byte_0 & 0x0F) == 7)
```

When the state changes, it calls:

```text
0x08004EC0
```

That helper builds a 7-byte command packet and then calls another output helper.
It is the likely implementation of:

```text
добавлена задняя передача (на выходе появляется 12в + команда)
```

Important: this is for the other person's car profile. For our Sportage profile
we still need a real log to confirm whether reverse is also `0x169`, or whether
our car uses the known candidates from the matrix such as `0x111`/`0x354`/other
C-CAN messages.

## Speed To Head Unit

v05 has a new speed parser path in the body/C-CAN parser:

```text
0x08004EF4: dispatch for body/C-CAN ids
0x08004F0C: detects id 0x316
0x08005046: reads one payload byte and scales it
```

The scaling block:

```text
value = data_byte * 100
low = value & 0xFF
high = value >> 8
```

It caches the last two transmitted speed bytes and only sends when they change:

```text
0x08005058..0x0800506A
```

Then it sends a 10-byte packet through the same local TX helper:

```text
0x0800506E..0x08005082
```

This matches:

```text
добавлена отправка скорости на магнитолу (максимально 10 раз/с)
```

The exact source signal has to be confirmed by log. In Hyundai/Kia public DBCs,
`0x316` is a common vehicle-speed source (`EMS11`), so this is a strong
candidate.

## Porting Plan

1. Keep the user v04 UID/profile as the base. Do not flash the v05 package as-is.
2. Decode v05 with `0x04/0x58` and v04 with `0x04/0x5B`.
3. Port only the behavior, not raw absolute addresses:
   - previous-source RAM byte;
   - cleanup when leaving source `0x16`;
   - cache/debounce for USB music bytes;
   - optional speed path from `0x316`;
   - optional reverse parser after a real Sportage log confirms the source.
4. Test in car in this order:
   - normal buttons/Simple Soft/UART still work;
   - no repeated "Музыка USB" fallback while another source is selected;
   - APK navigation still appears;
   - track name appears when APK sends real media;
   - speed appears on the head unit;
   - reverse output is tested only after confirming the CAN source from logs.
