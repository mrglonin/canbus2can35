# Firmware

Рабочая ветка прошивок после очистки:

- `trusted/v18_v19/` - legacy debug releases.
- `trusted/v20/` - previous trusted release.
- `trusted/v21/` - stable trusted release.
- `trusted/v22/` - current trusted release.
- `MANIFEST.md` - краткий список release-файлов и SHA256.

Публичная граница:

- release-бинарники остаются в git для OTA/update;
- подробный USB/API протокол, payload-форматы, ACK-коды и hook-адреса не
  документируются в публичной ветке;
- firmware build scripts и generated reports хранятся только локально.

Удалено из рабочей ветки:

- `custom_c` - чистая C-прошивка не стала рабочей заменой canbox.
- `canlog` и старые `mode3`/`gs_usb` сборки.
- `variants_20260511` - серия нерабочих/переходных экспериментов.
- firmware generation scripts and reports.
