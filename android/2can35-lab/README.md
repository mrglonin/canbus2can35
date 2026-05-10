# 2CAN35 Lab Android

Офлайн APK-версия локального dashboard.

## Что внутри

- WebView открывает локальный сервер `http://127.0.0.1:8765/`.
- HTML/JS/CSS берутся прямо из `dashboard/static`.
- CSV-таблицы берутся прямо из `data`.
- Android HTTP backend заменяет Python `dashboard/server.py`.
- USB CDC слой умеет искать `0483:5740` и `1d50:606f`, открывать bulk IN/OUT,
  читать строки mode3 lab и отправлять строки TX.

Интернет во время работы APK не нужен.

## Текущий статус

Это первый MVP:

- работает офлайн UI;
- работает локальное API `/api/status`, `/api/commands`, `/api/events`,
  `/api/export`;
- есть старт/стоп live-сессии;
- есть базовый USB CDC open/read/write;
- есть UART TX быстрых команд;
- есть прямой CAN TX через mode3 CDC строки;
- есть первичная отправка bundle для приборки: `0x114`, `0x197`, `0x1E6`,
  `0x115`, `0x4BB`, `0x49B`, `0x490`.

Еще не parity с Python:

- обучение пока возвращает пустой результат;
- фильтр шума и закрепление CAN↔UART надо переносить следующим этапом;
- gs_usb bulk-протокол не реализован, основной Android-путь сейчас CDC lab mode;
- реальные media/nav пакеты надо добивать по логам и таблице
  `data/cluster_media_nav_tx_plan.csv`.

## Сборка

На машине должен быть JDK и Android SDK/Android Studio.

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
  -> UsbManager / USB CDC
  -> 2CAN35 mode3 lab
  -> C-CAN / M-CAN / UART Raise
```

Для полноценной работы адаптер надо переводить в mode3 lab, где CDC принимает:

```text
0 t1238AABBCCDDEEFF0011   CAN TX ch0
1 t1238AABBCCDDEEFF0011   CAN TX ch1
U FD 05 05 01 00 0B       UART TX
```

