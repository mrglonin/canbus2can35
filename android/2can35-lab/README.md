# 2CAN35 Lab Android

Офлайн APK-версия локального dashboard.

## Что внутри

- WebView открывает локальный сервер `http://127.0.0.1:8765/`.
- HTML/JS/CSS берутся прямо из `dashboard/static`.
- CSV-таблицы берутся прямо из `data`.
- Android HTTP backend заменяет Python `dashboard/server.py`.
- USB слой умеет два режима: CDC `0483:5740` для старого lab mode и gs_usb
  `1d50:606f` для mode3 CAN logger.

Интернет во время работы APK не нужен.

## Текущий статус

Это первый MVP:

- работает офлайн UI;
- работает локальное API `/api/status`, `/api/commands`, `/api/events`,
  `/api/export`;
- есть старт/стоп live-сессии;
- есть базовый USB CDC open/read/write;
- есть gs_usb init/read/write: ch0 M-CAN 100 kbit/s, ch1 C-CAN 500 kbit/s;
- есть UART TX/RX быстрых команд через sideband control-transfer mode3;
- есть прямой CAN TX через CDC строки или gs_usb host frames;
- есть первичная отправка bundle для приборки: `0x114`, `0x197`, `0x1E6`,
  `0x115`, `0x4BB`, `0x49B`, `0x490`.

Еще не parity с Python:

- обучение пока возвращает пустой результат;
- фильтр шума и закрепление CAN↔UART надо переносить следующим этапом;
- обучение пока не parity с Python-анализатором;
- реальные media/nav пакеты надо добивать по логам и таблице
  `data/cluster_media_nav_tx_plan.csv`.

## Сборка

На машине должен быть JDK и Android SDK/Android Studio.

Можно поставить локальный toolchain в корневую `.tools/` папку:

```bash
cd android/2can35-lab
./scripts/install_toolchain.sh
```

Скрипт ставит локально, без системных изменений:

- Temurin JDK 17;
- Gradle 8.10.2;
- Android command-line tools;
- `platform-tools`;
- `platforms;android-35`;
- `build-tools;35.0.0`.

```bash
cd android/2can35-lab
./scripts/build_apk.sh
```

APK после сборки:

```text
android/2can35-lab/app/build/outputs/apk/debug/app-debug.apk
```

## Runtime схема

```text
Android WebView
  -> 127.0.0.1:8765 local API
  -> UsbManager / USB CDC или gs_usb
  -> 2CAN35 mode3 lab
  -> C-CAN / M-CAN / UART Raise
```

Для полноценной работы адаптер надо переводить в mode3 lab. В gs_usb режиме:

- CAN RX читается из bulk endpoint `0x81`;
- CAN TX отправляется host-frame в bulk endpoint `0x02`;
- UART RX/TX идет через 2CAN35 sideband requests `0x70..0x73`.

CDC fallback принимает:

```text
0 t1238AABBCCDDEEFF0011   CAN TX ch0
1 t1238AABBCCDDEEFF0011   CAN TX ch1
U FD 05 05 01 00 0B       UART TX
```
