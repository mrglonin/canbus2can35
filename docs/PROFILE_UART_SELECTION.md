# Выбор CANBUS-профиля и UART-списки

## Что именно вытащено

Raise/RZC вынесен отдельно в `data/raise_uart_full_worklist.csv`.
Simple/XP/2E вынесен отдельно в `data/simple_soft_uart_full_worklist.csv`.

Это не один смешанный список. В рабочих CSV нет поля `note`: только категория, протокол, направление, команда, кадр, декодер, статус, контрольная сумма и совпадение с источником.

## Raise

Мы не вытаскивали «все Kia подряд». Вытянут рабочий слой для RZC/Raise Korea и Sportage-профилей из TEYES:

- `Raise/RZC Sportage R 2018`: profile_id `65898`, hex `0x01016A`, slots `149`, named `89`.
- `Raise/RZC Sportage 2016`: profile_id `1442154`, hex `0x16016A`, slots `149`, named `89`.

Для твоей машины сейчас самый близкий профиль: `Raise/RZC Sportage R 2018`, потому что он совпадает с выбором TEYES Raise Kia/Hyundai Sportage 17-18 и имеет тот же RZC/Raise callback-семейство.

## Simple Soft

Simple Soft вынесен отдельно как `XP/Simple`/`2E` кандидат. В APK Sportage-релевантные профили:

- `XP/Simple Sportage R 2019 LOW`: profile_id `2097507`, hex `0x200163`, slots `108`, named `99`.
- `XP/Simple Sportage R 2019 MID`: profile_id `1900899`, hex `0x1D0163`, slots `108`, named `99`.
- `XP/Simple Sportage R 2019 HIGH`: profile_id `1966435`, hex `0x1E0163`, slots `108`, named `99`.

Их стоит проверять только если на магнитоле выбран Simple Soft/XP-профиль. Для текущего Raise-профиля эти 2E-команды не являются подтвержденным проводным протоколом.

## У кого больше параметров

- Больше всего slot-поверхность: `Raise/RZC Sportage 2016; Raise/RZC Sportage R 2018` — `149` slots.
- Больше всего подписанных параметров: `Hiworld/WC2 Sportage R 2018` — `101` named slots.

Практически для нас важнее не максимум строк, а совпадение семейства протокола. Поэтому текущий основной профиль: `Raise/RZC Sportage R 2018`.

## Сверка с прошивкой прогера

Сверка вынесена в `data/programmer_firmware_uart_check.csv`.
Кузовные события, задний ход и часть HU media/time совпадают с уже найденными Raise-кадрами. Температуры, скорость и часть климата в прошивке прогера идут как CAN-декодеры/внутренние CAN-сообщения, а не как готовая внешняя Raise UART-команда.
