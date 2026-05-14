# TEYES app implementation notes

Дата: 2026-05-13.

Этот файл фиксирует рабочую модель Android-приложения после live-проверок TEYES
CC4PRO, V21 2CAN35 и Sportage CAN-C/M-CAN.

## State model

Приложение не должно считать выбранный источник музыки текущим треком.

Рабочее состояние разделяется так:

- `selectedSource` - что выбрано в TEYES widget / SourceInfoManager;
- `playingSource` - кто реально играет и держит активный playback;
- `track` - metadata только от `playingSource`;
- `navState` - active route, preview/search/failed, finish hold, off;
- `location/heading` - GPS/fused location и GPS bearing, если сигнал валиден;
- `vehicle` - compact `0x77` snapshot из прошивки; raw stream только для debug.

Если выбран Bluetooth, но он paused/stopped, а Yandex Music играет, в CAN/кластер
уходит Yandex track, не старый Bluetooth title.

## Music output

Подтвержденный путь вывода в приборку:

| Источник | Откуда читать в Android | Что слать в адаптер |
|---|---|---|
| USB/local | `com.spd.media` / `android.spd.IMediaService` | `0x7A FD 0A 09 16 ...` + `0x22 title` |
| Bluetooth audio | TEYES selected source + `com.spd.bluetooth` / Bluetooth metadata | `0x7A FD 06 09 0B 04 00` + `0x22 title` + `0x20 subtype 0x1F artist` |
| FM | TEYES selected source + `com.spd.radio` / TEYES widget text | `0x7A FD 08 09 02 ...` + `0x21 text` |
| AM | radio state | `0x7A FD 06 09 09 00 00` + `0x20 text` |
| Yandex/cloud | TEYES cloud source + `com.yf.teyesspotify` / Yandex metadata | USB-like output: `0x7A FD 0A 09 16 ...` + `0x22 title` |
| BT phone | source only | `0x7A FD 06 09 07 01 00`, no confirmed text field |

Правило отправки: в приборку уходит одно событие при смене источника или
artist/title/duration. APK не повторяет тот же music пакет каждые несколько
секунд, чтобы не спамить штатный parser. Selected-only источники BT/FM/AM
могут отправить source-only пакет один раз, если нет более приоритетного
playing source.

Source selection is TEYES-first. Generic Android `MediaSession` is not allowed
to win by itself; it is used only inside the selected TEYES source path
(Bluetooth and cloud/Yandex), because USB/FM/TEYES Radio sessions are stale or
silent on the head unit.

CarPlay / Android Auto остаются future hooks. Отдельный source-status и text path
для них пока не подтвержден, поэтому APK не должен угадывать их CAN-пакеты.

## Navigation and compass

Навигация идет штатными командами прошивки:

- `0x48` - nav on/off;
- `0x45` - maneuver/TBT/compass;
- `0x47` - ETA/route status;
- `0x4A` - street/text UTF-16LE;
- `0x44` - speed limit.

Компас отправляется только через `0x45`, не raw M-CAN `0x115/0x1E6`.

Форма кадра:

```text
BB 41 A1 0E 45 08 00 00 DD 00 78 00 A0 CS
```

`DD` берется из 12-шаговой сетки `0, 3, 6, ... 33`. Для APK используется
проверенная формула панели:

```text
sent = (36 - uiStep) % 36
```

Повтор: `350-500 ms`. GPS bearing считается валидным только при свежем location,
движении и нормальной accuracy. Route preview/search failed/navigator off не
включают активное ведение и не затирают последний нормальный маршрут мгновенно.

TEYES active navigation source is the `com.yf.navinfo` broadcast produced by
`com.yf.navinfo.NavService` / `NaviInfoManager`. The APK registers this action
both in the manifest and dynamically inside `AppService`, then reads the same
extras seen in the TEYES APK:

```text
state, app, distance_val, distance_val_str, distance_unit,
total_distance, describe, position, direction, direction_lr
```

`state=open` from a known navigation app (`ru.yandex.yandexnavi`,
`ru.dublgis.dgismobile`, Google Maps/Waze) is enough to switch the adapter to
active route and send `0x7A nav` + `0x48 on`. If detailed maneuver fields arrive
later, APK updates `0x45/0x47/0x4A/0x44` without toggling nav off/on. Preview,
search and failed states are logged but held away from the cluster until a
controlled timeout/off event.

Because `0x45` is shared by maneuver and compass, APK suppresses compass output
while route active/finish hold is active. Compass resumes only after navigation
is cleanly off.

## Vehicle and RCTA

Обычный Vehicle/RCTA режим идет через прошивочный `0x77` snapshot. Адаптер
слушает FIFO внутри прошивки, хранит последние полезные значения и отдает их
маленьким ответом без постоянного raw-потока в APK. Raw CAN stream `0x70/0x76`
включается только вручную для debug/записи.

```text
0x316 speed/rpm
0x329 coolant
0x044 outside temp
0x132 voltage
0x4F4 blind spot/RCTA
0x541 body
0x553 rear doors
0x169 reverse
```

Все остальные кадры не попадают в UI/логи, если debug CAN выключен.

`0x4F4`:

- `0000C00000000001` - idle;
- `0001...` - active warning;
- known left/right payloads классифицируются таблицей и масками;
- unknown active payload показывается как rear warning и коротко логируется.

Напряжение в V21 snapshot берется из M-CAN `0x132 DATA[0] / 10`. Старый `0x545`
остается только historical fallback.

## QA hooks

Скрытый ADB receiver:

```text
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_yandex
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_bt_selected_paused
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_bt_playing
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_usb
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario media_fm
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_active
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_preview
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario nav_failed
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario rcta_left
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario rcta_right
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario rcta_unknown
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario vehicle
adb shell am broadcast --receiver-foreground -n kia.app/.QaScenarioReceiver -a kia.app.QA_SCENARIO --es scenario compass --ei step 9
```

Generated media/nav/compass frames are logged under `KiaQa` in logcat.
