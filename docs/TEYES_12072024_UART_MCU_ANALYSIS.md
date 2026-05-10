# TEYES 12.07.2024 UART/MCU анализ

Цель: понять, где в комплекте TEYES лежит логика UART/CANBUS, чтобы наш mode3 мог не только читать CAN/UART, но и эмулировать нужный слой для Raise/Simple Soft.

## Итог коротко

UART-логика в комплекте есть, но она разнесена по APK:

- `UPDATE_12.07.2024.apk` - база профилей, ID выбора CANBUS в магнитоле. Raw UART-команд там не найдено.
- `MS_12.07.2024.apk` - реальный транспорт: serial device, open/setup/read/write, JNI-команды в MCU.
- `US_12.07.2024.apk` - слой команд Android UI -> MCU/CANBUS, константы `C_*`, `U_*`, `Canbus.PROXY.cmd(...)`.
- `CANBUS_12.07.2024.apk` - большой слой профилей/виджетов/состояний CANBUS: `DataCanbus`, `FinalCanbus`, Hyundai/Kia/RZC/Hiworld классы.

Это означает: готовой таблицы вида `Raise UART byte -> функция` в `UPDATE` нет. Но найден путь, через который TEYES передает данные, и найдено, как кодирует выбранный профиль CANBUS.

## Проверенные артефакты

| Файл | Назначение |
|---|---|
| `re/teyes_12072024/TEYES_12072024_UART_RE_REPORT.md` | общий индекс DEX/методов/строк |
| `data/apk_canbus_12072024/teyes_static_constants.csv` | выгрузка статических `FinalCanbus` констант из `CANBUS` и `US` |
| `data/apk_canbus_12072024/teyes_uart_mcu_paths.csv` | проверенные методы, где открывается serial, пишутся байты и уходят команды в MCU |
| `data/apk_canbus_12072024/canbus_12072024_profiles_relevant.csv` | профили из `UPDATE` по Kia/Hyundai/Raise/Simple/Hiworld |
| `data/apk_canbus_12072024/finalcanbus_hyundai_kia_constants.csv` | профильные ID из `CANBUS` по Hyundai/Kia |

## Serial transport в MS

Главный низкоуровневый wrapper: `Lc/d;`.

| Метод | Что делает |
|---|---|
| `Lc/d;->b()I` | `JniSerial.open(path)`, fd кешируется по пути |
| `Lc/d;->e(int fd, int baud)I` | `JniSerial.setup(fd, baud, 8, 78, 1)` |
| `Lc/d;->c()` | `JniSerial.read(fd, 512, 2)` |
| `Lc/d;->f(int[])I` | `int[] -> byte[] -> JniSerial.write(fd, bytes, 0, len)` |

Подтверждение: `re/teyes_12072024/methods/MS_12_07_2024/classes_dex/0375_c_d__b.txt`, `0378_c_d__e.txt`, `0376_c_d__c.txt`, `0379_c_d__f.txt`.

Есть отдельный CANBUS serial opener:

- `Ly/k;->f(String path, int baud, Lj1/q handler)V`
- лог-строка: `CANBUS DEV PATH = %s FD = %d BAUD = %d`
- подтверждение: `re/teyes_12072024/methods/MS_12_07_2024/classes_dex/0626_y_k__f.txt`

Отдельно найден BT/GOC serial:

- `Ly/k;->e()V`
- пути: `/dev/goc_serial`, `/dev/BT_serial`
- скорость: `9600`
- подтверждение: `re/teyes_12072024/methods/MS_12_07_2024/classes_dex/0625_y_k__e.txt`

В `chip/Chip` есть стандартные tty-пути:

- `/dev/ttyS0`
- `/dev/ttyS1`
- `/dev/ttyS2`

Подтверждение: `re/teyes_12072024/methods/MS_12_07_2024/classes_dex/0920_chip_Chip___init_.txt`.

## Отправка байтов в MCU

Есть два важных пути:

1. Прямой serial-worker:
   - `Ly/k;->t(int[])V`
   - логирует `Send to Mcu Data: ...`
   - затем преобразует пакет и пишет в serial worker.
   - подтверждение: `re/teyes_12072024/methods/MS_12_07_2024/classes_dex/0627_y_k__t.txt`

2. JNI raw write:
   - `ToolsJni.cmd_149_write_data(byte[] data, int offset)`
   - кладет `writedata` и `offset` в `Bundle`
   - вызывает `SyuJniNative.syu_jni_command(149, bundle, null)`
   - подтверждение: `re/teyes_12072024/methods/MS_12_07_2024/classes_dex/0545_com_syu_jni_ToolsJni__cmd_149_write_data.txt`
   - такой же wrapper есть в `US`: `re/teyes_12072024/methods/US_12_07_2024/classes_dex/0208_com_syu_jni_ToolsJni__cmd_149_write_data.txt`

Это полезно для Android-приложения: если когда-нибудь будем делать APK, API-модель должна быть близка к `Canbus.PROXY.cmd(...)` и/или raw-byte write, а не к случайным кнопкам.

## CANBUS command IDs в US

В `US_12.07.2024.apk` класс `Lcom/lsec/core/util/data/FinalCanbus;` дает ID команд и DATA-слотов.

Ключевые значения:

| Константа | Значение | Смысл |
|---|---:|---|
| `C_CANBUS_ID` | `1000` | команда выбора CANBUS ID |
| `C_CANBUS_FRAME_TO_MCU` | `1008` | кадр/команда в MCU |
| `C_CANBUS_FRAME_TO_MCU_0X10` | `1013` | отдельный тип кадра в MCU |
| `C_CANBUS_FRAME_TO_MTU` | `1012` | кадр в MTU |
| `C_CANBUS_KEY_FUNC` | `1014` | key-функции CANBUS |
| `C_CANBUS_BACKCAR_FUNC` | `1015` | backcar/reverse функции |
| `C_CMD_360_TOUCH` | `1029` | touch для 360 |
| `C_CAMERA_MODE` | `1005` | режим камеры |
| `U_CANBUS_FRAME_TO_UI` | `1019` | кадр из CANBUS в UI |
| `U_CUR_SPEED` | `1031` | скорость |
| `U_ENGINE_SPEED` | `1032` | обороты |
| `U_CAR_BACKCAR` | `101` | состояние заднего хода |
| `U_CNT_MAX` | `1200` | размер массива DATA |

Полная выгрузка: `data/apk_canbus_12072024/teyes_static_constants.csv`.

В этой же выгрузке есть полезные UI/DATA слоты из `CANBUS_12.07.2024.apk`, которые нужны для нашего dashboard и будущего APK:

| Группа | Примеры |
|---|---|
| Двери | `U_DOOR_FL=1`, `U_DOOR_FR=2`, `U_DOOR_RL=3`, `U_DOOR_RR=4`, `U_DOOR_BACK=5` |
| Климат | `U_AIR_POWER=10`, `U_AIR_AC=11`, `U_AIR_CYCLE=12`, `U_AIR_AUTO=13`, `U_AIR_DUAL=14` |
| Обдув | `U_AIR_BLOW_UP_LEFT=18`, `U_AIR_BLOW_BODY_LEFT=19`, `U_AIR_BLOW_FOOT_LEFT=20` |
| Температура | `U_AIR_TEMP_LEFT=27`, `U_AIR_TEMP_RIGHT=28`, `U_AIR_TEMP_TYPE=75` |
| Подогрев/обдув | `U_AIR_SEAT_HOT_LEFT=29`, `U_AIR_SEAT_HOT_RIGHT=30`, `U_AIR_SEAT_BLOW_LEFT=31`, `U_AIR_SEAT_BLOW_RIGHT=32`, `U_AIR_HOT_STEER=66` |
| Заднее/лобовое | `U_AIR_REAR_DEFROST=16`, `U_AIR_FRONT_DEFROST=65`, `U_AIR_REARVIEW_HOT=59` |

Это не CAN ID автомобиля, а внутренние индексы TEYES/SYU слоя. Их надо использовать как справочник интерфейса, а реальные `CAN C/M` и `UART Raise` байты заполнять по live-логам.

## Профиль CANBUS и кодирование ID

`MS` применяет выбранный профиль так:

- `Lg0/ur;->D(int)` читает/применяет полный CANBUS ID.
- low16 = protocol id / canbox family.
- high16 = car/profile variant.
- `Lg0/ur;->b0(int)` выбирает конкретный handler по `low16 + high16`.

Подтверждения:

- `re/teyes_12072024/methods/MS_12_07_2024/classes_dex/0604_g0_ur__D.txt`
- `re/teyes_12072024/methods/MS_12_07_2024/classes_dex/0605_g0_ur__b0.txt`

Примеры из `CANBUS_12.07.2024.apk`:

| Константа | value | low16 | variant |
|---|---:|---:|---:|
| `CAR_443_WC2_XianDai_All_18Sportage` | `3408315` / `0x3401bb` | `443` | `52` |
| `CAR_443_WC2_XianDai_All_KX5` | `9961915` / `0x9801bb` | `443` | `152` |
| `CAR_443_WC2_XianDai_All_Sonata8` | `2228667` / `0x2201bb` | `443` | `34` |
| `CAR_RZC_16_QiYaKX5` | `393` / `0x189` | `393` | `0` |
| `CAR_RZC_16_QiYaKX5_M` | `65929` / `0x10189` | `393` | `1` |
| `CAR_RZC_16_QiYaKX5_H` | `131465` / `0x20189` | `393` | `2` |
| `CAR_XP_19QiYa_Sportage_L` | `2097507` / `0x200163` | `355` | `32` |
| `CAR_XP_19QiYa_Sportage_M` | `1900899` / `0x1d0163` | `355` | `29` |
| `CAR_XP_19QiYa_Sportage_H` | `1966435` / `0x1e0163` | `355` | `30` |

## Что это дает нашему mode3

Mode3 должен оставаться простым:

- CAN read/write по двум каналам.
- UART RX/TX sideband без изменения gs_usb.
- Команды TX должны принимать сырой пакет: канал `mcan/ccan/uart`, bytes, repeat, delay.
- В UI/сервере мы уже строим связку `CAN событие -> UART событие -> TX replay`.

Теперь можно добавить справочник TEYES:

- профиль выбранного canbox (`low16`, `variant`);
- MCU command ID (`C_*`);
- DATA slot (`U_*`);
- найденные UART-пакеты из live-логов;
- статус: `confirmed_by_firmware`, `confirmed_by_live`, `candidate`.

## Что еще надо раскопать

Raw Raise/Simple Soft UART для конкретных событий лучше добивать live-логом, потому что в APK он скрыт за MCU/serial plugin:

1. Включить mode3 `CAN+UART`.
2. На магнитоле выбрать нужный профиль Raise/Simple.
3. Делать по одному действию: источник, трек, климат, дверь, люк, парктроники.
4. Сохранять отдельно:
   - `CAN M`
   - `CAN C`
   - `UART RX`
   - `UART TX`
   - таблицу связки `действие -> CAN -> UART`.

Если понадобится Android-приложение, его API нужно строить вокруг:

- `Canbus.PROXY.cmd(id, int[], float[], String[])` как логическая модель;
- raw UART/CAN TX как нижний уровень;
- экспорт таблиц в CSV/JSON для прошивки.
