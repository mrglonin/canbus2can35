# Firmware manifest

В репозитории оставлены только доверенные release-бинарники поверх прошивки
автора v08. Подробный USB/API протокол, payload-форматы, ACK-коды, hook-адреса,
generated reports и скрипты сборки не публикуются в git.

## Base firmware

Проверенная основа автора:

```text
37FFDA054247303859412243_04350008.bin
UID: 37 FF DA 05 42 47 30 38 59 41 22 43
```

Стоковые бинарники автора лежат локально в `Downloads`; в git они не
дублируются.

## Trusted variants

| File | SHA256 | Назначение |
|---|---|---|
| `trusted/v18_v19/18_v08_mode1_raw_can_stream_obd_snapshot_USB.bin` | `e1899bfe888e4cf77dfbbb90c4987d5c39dada0e5eddd8ddef13847f725e8868` | legacy debug release |
| `trusted/v18_v19/18_v08_mode1_raw_can_stream_obd_snapshot_STLINK64K.bin` | `46a3654085ea9d9251ac1d28421070a0fd25b73fbd8419d957c13d94fcc8bf1a` | legacy debug release |
| `trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx_USB.bin` | `54597f7bb9b3a416ea53bab6d26b2cbb6507246cccc15e1506d68f9274aaaff0` | legacy debug release |
| `trusted/v18_v19/19_v08_mode1_raw_can_stream_obd_snapshot_can_tx_STLINK64K.bin` | `a94208b4d5ebee90f8e1b17f276a10e8b3fbbc98832232dc2da2d7c5b03be756` | legacy debug release |
| `trusted/v20/20_v08_mode1_v20_USB.bin` | `b790f7b1077358c0bac5e6eee81f96efea2fbcf90e0b39bdaf2e3e799953f95b` | trusted release |
| `trusted/v20/20_v08_mode1_v20_STLINK64K.bin` | `10ef4cbccc7cd8802cecf9a8ef4dc1c32e38edbc9eda17b9cd80a0cbea9f22f0` | trusted release |
| `trusted/v21/21_v08_mode1_v21_USB.bin` | `1e3a6c23773f39a0c0df956799ceaecd5fb3a9d0aaebce1e83d8a01b55b1e1dc` | trusted release |
| `trusted/v21/21_v08_mode1_v21_STLINK64K.bin` | `d3f37cc733e8da2cb18e53b2111b9fbb53c82e497add07498d44bda0d1d2c90f` | trusted release |
| `trusted/v22/22_v08_mode1_v22_full_raw_USB.bin` | `b1256de76cc8c1eabaabefd9f1e77ff4f5988d24a68289514405fa849ce92c4d` | current trusted release |
| `trusted/v22/22_v08_mode1_v22_full_raw_STLINK64K.bin` | `1f8786999bd90b520955e5bd64a69379ddfb09e6060e9224a5cfc922e3554a4d` | current trusted release |

## Public boundary

This manifest intentionally does not document the adapter USB/API exchange.
Operational protocol notes and firmware build artifacts are kept outside the
public repository.
