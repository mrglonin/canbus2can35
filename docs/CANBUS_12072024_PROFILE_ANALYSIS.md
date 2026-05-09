# Анализ CANBUS APK 12.07.2024

Источник: `UPDATE_12.07.2024.apk`, базы `assets/canbus.db`, `assets/canbus_hsp.db`, `assets/canbus_sp.db`.

`profile_id` это ID профиля, который выбирается магнитолой в меню CANBUS. Эти базы не содержат raw UART-пакеты; байты протокола нужно снимать live или отдельно разбирать из DEX/MCU-слоя.

## Короткий вывод

- В APK есть оба нужных семейства canbox: `RZC(Raise)` и `XP(Simple)`.
- Для Raise/RZC Kia/Hyundai найдено 108 строк профилей. Для Simple Soft в этой базе это обозначено как `XP(Simple)`, найдено 53 строки Hyundai/KIA.
- Для Sportage есть явные кандидаты: `RZC Sportage 2016 = 1442154`, `RZC Sportage R 2018 = 65898`, `XP(Simple) Sportage 2019 LOW/MID/HIGH = 2097507 / 1900899 / 1966435`.
- Для Sonata 8 есть отдельные кандидаты: `RZC Sonata8 2010-2015 = 983402`; также есть `HIWORLD`, `BNR`, `CYT` профили Sonata8.
- В `CANBUS_12.07.2024.apk` класс `FinalCanbus` подтверждает эти же числовые ID константами. Полная выгрузка лежит в `data/apk_canbus_12072024/finalcanbus_hyundai_kia_constants.csv`.
- Сами raw UART-команды Raise/Simple Soft эти таблицы не раскрывают: приложение работает через системный CANBUS/MCU API (`DataCanbus`, `FinalCanbus`, `sendCmd`), а не хранит профиль как готовую таблицу байтов UART. Для эмуляции протокола нужны live UART-логи или дальнейший разбор DEX/MCU-слоя.

## Raise / RZC Hyundai-Kia

Найдено строк: 108

| db | profile_id | company | carset | cartype | canbox | limit |
|---|---:|---|---|---|---|---|
| canbus.db | 137 | RZC(Raise) | Hyundai/KIA | IX35/IX45 | Low |  |
| canbus.db | 131209 | RZC(Raise) | Hyundai/KIA | IX35/IX45 | High |  |
| canbus.db | 393578 | RZC(Raise) | Hyundai/KIA | IX35 | 2010-2015.Low |  |
| canbus.db | 102 | RZC(Raise) | Hyundai/KIA | IX45 | RZC |  |
| canbus.db | 153 | RZC(Raise) | Hyundai/KIA | MISTRA | 2013-2015 |  |
| canbus.db | 131225 | RZC(Raise) | Hyundai/KIA | MISTRA | 2013-2015.HIGH |  |
| canbus.db | 249 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2015-2017.LOW |  |
| canbus.db | 131321 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2015-2017.HIGH |  |
| canbus.db | 328042 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2018(H) |  |
| canbus.db | 262506 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2018(M) |  |
| canbus.db | 196970 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2018(L) |  |
| canbus.db | 459114 | RZC(Raise) | Hyundai/KIA | FESTA | 2019 |  |
| canbus.db | 131434 | RZC(Raise) | Hyundai/KIA | ALL | LOW |  |
| canbus.db | 362 | RZC(Raise) | Hyundai/KIA | K5 | 2011-2016 |  |
| canbus.db | 393 | RZC(Raise) | Hyundai/KIA | KX5 | 2016(L) |  |
| canbus.db | 65929 | RZC(Raise) | Hyundai/KIA | KX5 | 2016(M) |  |
| canbus.db | 131465 | RZC(Raise) | Hyundai/KIA | KX5 | 2016(H) |  |
| canbus.db | 262537 | RZC(Raise) | Hyundai/KIA | KaiShen | RZC |  |
| canbus.db | 65898 | RZC(Raise) | Hyundai/KIA | Sportage R | 2018 |  |
| canbus.db | 262591 | RZC(Raise) | Hyundai/KIA | IX45 | 2019 |  |
| canbus.db | 328127 | RZC(Raise) | Hyundai/KIA | IX45 | 2019.HIGH.360View |  |
| canbus.db | 393663 | RZC(Raise) | Hyundai/KIA | IX45 | 2019.HIGH.360View.AMP |  |
| canbus.db | 524650 | RZC(Raise) | Hyundai/KIA | ALL | ALL.HIGH.AMP |  |
| canbus.db | 459199 | RZC(Raise) | Hyundai/KIA | tucson | 2019 |  |
| canbus.db | 524735 | RZC(Raise) | Hyundai/KIA | tucson | 2019.HIGH.AMP |  |
| canbus.db | 590271 | RZC(Raise) | Hyundai/KIA | KX5 | 2019.LOW |  |
| canbus.db | 655807 | RZC(Raise) | Hyundai/KIA | KX5 | 2019.HIGH.AMP |  |
| canbus.db | 721343 | RZC(Raise) | Hyundai/KIA | K3 | 2019 |  |
| canbus.db | 786879 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2018.Mixed Version |  |
| canbus.db | 852415 | RZC(Raise) | Hyundai/KIA | K3 | 2019.New energy |  |
| canbus.db | 917951 | RZC(Raise) | Hyundai/KIA | K5 | 2019.New energy |  |
| canbus.db | 983487 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2018.MID.Mixed Version |  |
| canbus.db | 1049023 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2018.HIGH.Mixed Version |  |
| canbus.db | 1114559 | RZC(Raise) | Hyundai/KIA | KX3 | 2020 |  |
| canbus.db | 1180095 | RZC(Raise) | Hyundai/KIA | KX3 | 2020.HIGH.AMP |  |
| canbus.db | 1245631 | RZC(Raise) | Hyundai/KIA | Elantra | 2021.Korea.LOW |  |
| canbus.db | 590186 | RZC(Raise) | Hyundai/KIA | Rohens | 2010 |  |
| canbus.db | 655722 | RZC(Raise) | Hyundai/KIA | FESTA | KOREA CAR(2019).LOW |  |
| canbus.db | 262537 | RZC(Raise) | Hyundai/KIA | K4 | 2017 |  |
| canbus.db | 721258 | RZC(Raise) | Hyundai/KIA | VELOSTER | 2011-2015 |  |
| canbus.db | 786794 | RZC(Raise) | Hyundai/KIA | I40 | 2012-2015 |  |
| canbus.db | 852330 | RZC(Raise) | Hyundai/KIA | CADENZA | 2011-2016 |  |
| canbus.db | 917866 | RZC(Raise) | Hyundai/KIA | AZERA | 2011-2015 |  |
| canbus.db | 983402 | RZC(Raise) | Hyundai/KIA | Sonata8 | 2010-2015 |  |
| canbus.db | 1048938 | RZC(Raise) | Hyundai/KIA | K3 | 2016 |  |
| canbus.db | 1114474 | RZC(Raise) | Hyundai/KIA | KX7 | 2017 |  |
| canbus.db | 1180010 | RZC(Raise) | Hyundai/KIA | KX CORSS | 2017 |  |
| canbus.db | 1245546 | RZC(Raise) | Hyundai/KIA | Rohens Coupe | 2009 |  |
| canbus.db | 1311082 | RZC(Raise) | Hyundai/KIA | Rohens Coupe | 2012 |  |
| canbus.db | 1376618 | RZC(Raise) | Hyundai/KIA | Rohens Coupe | 2012.HIHG.AMP |  |
| canbus.db | 1311167 | RZC(Raise) | Hyundai/KIA | sorento | 2021-2022 |  |
| canbus.db | 1376703 | RZC(Raise) | Hyundai/KIA | sorento | 2021-2022.HIGH.AMP |  |
| canbus.db | 1442239 | RZC(Raise) | Hyundai/KIA | sorento | 2021-2022.HIGH.360View |  |
| canbus.db | 1442154 | RZC(Raise) | Hyundai/KIA | Sportage | 2016 |  |
| canbus.db | 1507690 | RZC(Raise) | Hyundai/KIA | SANTAFE | 2017 |  |
| canbus.db | 1573226 | RZC(Raise) | Hyundai/KIA | OPTIMA | 2017 |  |
| canbus.db | 1638762 | RZC(Raise) | Hyundai/KIA | SOUL | 2019 |  |
| canbus.db | 1704298 | RZC(Raise) | Hyundai/KIA | CARNIVAL | 2019 |  |
| canbus.db | 1769834 | RZC(Raise) | Hyundai/KIA | IX35 | 2010-2015 |  |
| canbus.db | 1507775 | RZC(Raise) | Hyundai/KIA | Sonata | 2021.Low.Import |  |
| canbus.db | 1573311 | RZC(Raise) | Hyundai/KIA | AZERA | 2022.Low.Import |  |
| canbus.db | 1638847 | RZC(Raise) | Hyundai/KIA | CRETA | 2022.Low.Import |  |
| canbus.db | 1704383 | RZC(Raise) | Hyundai/KIA | STARGAZER | 2022.Low.Import |  |
| canbus.db | 1769919 | RZC(Raise) | Hyundai/KIA | Sonata | 2021.Mid.Import |  |
| canbus.db | 1835455 | RZC(Raise) | Hyundai/KIA | AZERA | 2022.Mid.Import |  |
| canbus.db | 1900991 | RZC(Raise) | Hyundai/KIA | CRETA | 2022.Mid.Import |  |
| canbus.db | 1966527 | RZC(Raise) | Hyundai/KIA | STARGAZER | 2022.Mid.Import |  |
| canbus.db | 2032063 | RZC(Raise) | Hyundai/KIA | Sonata | 2021.High.Import |  |
| canbus.db | 2097599 | RZC(Raise) | Hyundai/KIA | AZERA | 2022.High.Import |  |
| canbus.db | 2163135 | RZC(Raise) | Hyundai/KIA | CRETA | 2022.High.Import |  |
| canbus.db | 2228671 | RZC(Raise) | Hyundai/KIA | STARGAZER | 2022.High.Import |  |
| canbus.db | 2425279 | RZC(Raise) | Hyundai/KIA | SANTAFE | 2017 |  |
| canbus.db | 2294207 | RZC(Raise) | Hyundai/KIA | OPTIMA | 2017 |  |
| canbus.db | 2490815 | RZC(Raise) | Hyundai/KIA | SANTAFE | 2017.360View |  |
| canbus.db | 2359743 | RZC(Raise) | Hyundai/KIA | OPTIMA | 2017.360View |  |
| canbus_hsp.db | 137 | RZC | Hyundai/KIA | IX35/IX45 | RZC(L) |  |
| canbus_hsp.db | 131209 | RZC | Hyundai/KIA | IX35/IX45 | RZC(H) |  |
| canbus_hsp.db | 393578 | RZC | Hyundai/KIA | IX35/IX45 | KOREA CAR.LOW(2010-2015) |  |
| canbus_hsp.db | 102 | RZC | Hyundai/KIA | IX45 | RZC |  |
| canbus_hsp.db | 153 | RZC | Hyundai/KIA | MISTRA | RZC(L) |  |
| canbus_hsp.db | 131225 | RZC | Hyundai/KIA | MISTRA | RZC(H) |  |
| canbus_hsp.db | 249 | RZC | Hyundai/KIA | Sonata9 | RZC(L) |  |
| canbus_hsp.db | 131321 | RZC | Hyundai/KIA | Sonata9 | RZC(H) |  |
| canbus_hsp.db | 328042 | RZC | Hyundai/KIA | Sonata9 | 2018(H) |  |
| canbus_hsp.db | 262506 | RZC | Hyundai/KIA | Sonata9 | 2018(M) |  |
| canbus_hsp.db | 196970 | RZC | Hyundai/KIA | Sonata9 | 2018(L) |  |
| canbus_hsp.db | 459114 | RZC | Hyundai/KIA | FESTA | KOREA CAR(2019) |  |
| canbus_hsp.db | 131434 | RZC | Hyundai/KIA | Other | KOREA CAR.ALL |  |
| canbus_hsp.db | 362 | RZC | Hyundai/KIA | K5 | RZC2016 |  |
| canbus_hsp.db | 393 | RZC | Hyundai/KIA | KX5 | RZC(L)2016 |  |
| canbus_hsp.db | 65929 | RZC | Hyundai/KIA | KX5 | RZC(M)2016 |  |
| canbus_hsp.db | 131465 | RZC | Hyundai/KIA | KX5 | RZC(H)2016 |  |
| canbus_hsp.db | 262537 | RZC | Hyundai/KIA | KaiShen | RZC |  |
| canbus_hsp.db | 65898 | RZC | Hyundai/KIA | Sportage | 2018 |  |
| canbus_hsp.db | 262591 | RZC | Hyundai/KIA | IX45 | 2019 |  |
| canbus_hsp.db | 328127 | RZC | Hyundai/KIA | IX45 | 2019.HIGH.ALL |  |
| canbus_hsp.db | 393663 | RZC | Hyundai/KIA | IX45 | 2019.HIGH.ALL.AMP |  |
| canbus_hsp.db | 524650 | RZC | Hyundai/KIA | Other | ALL.HIGH.AMP |  |
| canbus_sp.db | 137 | RZC(Raise) | Hyundai/KIA | IX35 | RZC.XP(L) |  |
| canbus_sp.db | 131209 | RZC(Raise) | Hyundai/KIA | IX35 | RZC.XP(H) |  |
| canbus_sp.db | 393578 | RZC(Raise) | Hyundai/KIA | IX35 | KOREA CAR.LOW(2010-2015) |  |
| canbus_sp.db | 328042 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2018(H) |  |
| canbus_sp.db | 262506 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2018(M) |  |
| canbus_sp.db | 196970 | RZC(Raise) | Hyundai/KIA | Sonata9 | 2018(L) |  |
| canbus_sp.db | 459114 | RZC(Raise) | Hyundai/KIA | FESTA | KOREA CAR(2019) |  |
| canbus_sp.db | 131434 | RZC(Raise) | Hyundai/KIA | Other | KOREA CAR.ALL |  |
| canbus_sp.db | 1835370 | RZC(Raise) | Hyundai/KIA | I30 | 2015 |  |
| canbus_sp.db | 1900906 | RZC(Raise) | Hyundai/KIA | CEED | 2011 |  |

## XP(Simple) Hyundai-Kia

Найдено строк: 53

| db | profile_id | company | carset | cartype | canbox | limit |
|---|---:|---|---|---|---|---|
| canbus.db | 15 | XP(Simple) | Hyundai/KIA | IX35 | XP(OLD) |  |
| canbus.db | 31 | XP(Simple) | Hyundai/KIA | IX45 | XP(L) |  |
| canbus.db | 65567 | XP(Simple) | Hyundai/KIA | IX45 | XP(M) |  |
| canbus.db | 131103 | XP(Simple) | Hyundai/KIA | IX45 | XP(H) |  |
| canbus.db | 168 | XP(Simple) | Hyundai/KIA | Sonata | XP(L) |  |
| canbus.db | 65704 | XP(Simple) | Hyundai/KIA | Sonata | XP(M) |  |
| canbus.db | 131240 | XP(Simple) | Hyundai/KIA | Sonata | XP(H) |  |
| canbus.db | 266 | XP(Simple) | Hyundai/KIA | Sonata9 | 2016.LOW |  |
| canbus.db | 131338 | XP(Simple) | Hyundai/KIA | Sonata9 | 2016.HIGH.AMP |  |
| canbus.db | 655626 | XP(Simple) | Hyundai/KIA | Sonata9 | 2018.SCREENBUTTON(6) |  |
| canbus.db | 721162 | XP(Simple) | Hyundai/KIA | Sonata9 | 2018.SCREENBUTTON(8) |  |
| canbus.db | 367 | XP(Simple) | Hyundai/KIA | tucson | 2016(L) |  |
| canbus.db | 65903 | XP(Simple) | Hyundai/KIA | tucson | 2016(H) |  |
| canbus.db | 917859 | XP(Simple) | Hyundai/KIA | ALL | LOW |  |
| canbus.db | 786787 | XP(Simple) | Hyundai/KIA | Santa | 2019.360 |  |
| canbus.db | 852323 | XP(Simple) | Hyundai/KIA | Santa | 2019.360.AMP |  |
| canbus.db | 251 | XP(Simple) | Hyundai/KIA | sorento | XP(L) |  |
| canbus.db | 65787 | XP(Simple) | Hyundai/KIA | sorento | XP(M) |  |
| canbus.db | 131323 | XP(Simple) | Hyundai/KIA | sorento | XP(H) |  |
| canbus.db | 590075 | XP(Simple) | Hyundai/KIA | sorento | XP(AMP-35) |  |
| canbus.db | 355 | XP(Simple) | Hyundai/KIA | K5 | 2016 |  |
| canbus.db | 65891 | XP(Simple) | Hyundai/KIA | K5 | 2016.MID |  |
| canbus.db | 131427 | XP(Simple) | Hyundai/KIA | K5 | 2016.HIGH |  |
| canbus.db | 391 | XP(Simple) | Hyundai/KIA | KX5 | 2016 |  |
| canbus.db | 721251 | XP(Simple) | Hyundai/KIA | Santa | 2019 |  |
| canbus.db | 983395 | XP(Simple) | Hyundai/KIA | ALL | 360View |  |
| canbus.db | 1048931 | XP(Simple) | Hyundai/KIA | ALL | 360View.AMP |  |
| canbus.db | 1114467 | XP(Simple) | Hyundai/KIA | IX45 | AMP |  |
| canbus.db | 1180003 | XP(Simple) | Hyundai/KIA | Borrego | XP |  |
| canbus.db | 1245539 | XP(Simple) | Hyundai/KIA | ALL | 2019.LOW |  |
| canbus.db | 1311075 | XP(Simple) | Hyundai/KIA | ALL | 2019.HIGH.360View |  |
| canbus.db | 1376611 | XP(Simple) | Hyundai/KIA | ALL | 2019.HIGH.360View.AMP |  |
| canbus.db | 786698 | XP(Simple) | Hyundai/KIA | Sonata9 | 2018.SCREENBUTTON(10) |  |
| canbus.db | 852234 | XP(Simple) | Hyundai/KIA | Sonata9 | 2019 |  |
| canbus.db | 1507683 | XP(Simple) | Hyundai/KIA | K5 | 2018 |  |
| canbus.db | 1573219 | XP(Simple) | Hyundai/KIA | K5 | 2018.MID |  |
| canbus.db | 1638755 | XP(Simple) | Hyundai/KIA | K5 | 2018.HIGH |  |
| canbus.db | 1704291 | XP(Simple) | Hyundai/KIA | KX5 | 2016 |  |
| canbus.db | 1769827 | XP(Simple) | Hyundai/KIA | KX5 | 2016.MID |  |
| canbus.db | 1835363 | XP(Simple) | Hyundai/KIA | KX5 | 2016.HIGH |  |
| canbus.db | 1900899 | XP(Simple) | Hyundai/KIA | Sportage R | 2019.MID |  |
| canbus.db | 1966435 | XP(Simple) | Hyundai/KIA | Sportage R | 2019.HIGH |  |
| canbus.db | 2097507 | XP(Simple) | Hyundai/KIA | Sportage R | 2019.LOW |  |
| canbus.db | 2163043 | XP(Simple) | Hyundai/KIA | CARNIVAL | 2018 |  |
| canbus.db | 2228579 | XP(Simple) | Hyundai/KIA | KX5 | 2016.HIGH.NO AMP |  |
| canbus_sp.db | 31 | XP(Simple) | Hyundai/KIA | IX45 | XP(L) |  |
| canbus_sp.db | 65567 | XP(Simple) | Hyundai/KIA | IX45 | XP(M) |  |
| canbus_sp.db | 131103 | XP(Simple) | Hyundai/KIA | IX45 | XP(H) |  |
| canbus_sp.db | 367 | XP(Simple) | Hyundai/KIA | tucson | XP(L)2016 |  |
| canbus_sp.db | 65903 | XP(Simple) | Hyundai/KIA | tucson | XP(H)2016 |  |
| canbus_sp.db | 2097507 | XP(Simple) | KIA | Sportage | 2019.LOW |  |
| canbus_sp.db | 1900899 | XP(Simple) | KIA | Sportage | 2019.MID |  |
| canbus_sp.db | 1966435 | XP(Simple) | KIA | Sportage | 2019.HIGH |  |

## Sportage candidates

Найдено строк: 13

| db | profile_id | company | carset | cartype | canbox | limit |
|---|---:|---|---|---|---|---|
| canbus.db | 786826 | HIWORLD | Hyundai/KIA | Sportage R | 2018 |  |
| canbus.db | 65898 | RZC(Raise) | Hyundai/KIA | Sportage R | 2018 |  |
| canbus.db | 3408315 | HIWORLD | Hyundai/KIA | Sportage R | 2018 |  |
| canbus.db | 1900899 | XP(Simple) | Hyundai/KIA | Sportage R | 2019.MID |  |
| canbus.db | 1966435 | XP(Simple) | Hyundai/KIA | Sportage R | 2019.HIGH |  |
| canbus.db | 2097507 | XP(Simple) | Hyundai/KIA | Sportage R | 2019.LOW |  |
| canbus.db | 1442154 | RZC(Raise) | Hyundai/KIA | Sportage | 2016 |  |
| canbus_hsp.db | 786826 | HIWORLD | Hyundai/KIA | Sportage | 2018 |  |
| canbus_hsp.db | 65898 | RZC | Hyundai/KIA | Sportage | 2018 |  |
| canbus_hsp.db | 3408315 | HIWORLD | Hyundai/KIA | Sportage | 2018 |  |
| canbus_sp.db | 2097507 | XP(Simple) | KIA | Sportage | 2019.LOW |  |
| canbus_sp.db | 1900899 | XP(Simple) | KIA | Sportage | 2019.MID |  |
| canbus_sp.db | 1966435 | XP(Simple) | KIA | Sportage | 2019.HIGH |  |

## Sonata 8 candidates

Найдено строк: 21

| db | profile_id | company | carset | cartype | canbox | limit |
|---|---:|---|---|---|---|---|
| canbus.db | 2228667 | HIWORLD | Hyundai/KIA | Sonata8 | ALL |  |
| canbus.db | 2294203 | HIWORLD | Hyundai/KIA | Sonata8 | ALL.HIGH.AMP |  |
| canbus.db | 393571 | BNR | Hyundai/KIA | Sonata8 | BNR |  |
| canbus.db | 459107 | BNR | Hyundai/KIA | Sonata8 | BNR(AMP) |  |
| canbus.db | 983402 | RZC(Raise) | Hyundai/KIA | Sonata8 | 2010-2015 |  |
| canbus_hsp.db | 2228667 | HIWORLD | Hyundai/KIA | Sonata8 | ALL |  |
| canbus_hsp.db | 2294203 | HIWORLD | Hyundai/KIA | Sonata8 | ALL.HIGH.AMP |  |
| canbus_hsp.db | 393571 | BNR | Hyundai/KIA | Sonata8 | BNR |  |
| canbus_hsp.db | 459107 | BNR | Hyundai/KIA | Sonata8 | BNR(AMP) |  |
| canbus_hsp.db | 196776 | CYT | Hyundai/KIA | Sonata8 | CYT.L |  |
| canbus_hsp.db | 262312 | CYT | Hyundai/KIA | Sonata8 | CYT.H |  |
| canbus_hsp.db | 327848 | CYT | Hyundai/KIA | Sonata8 | CYT.B |  |
| canbus_hsp.db | 524643 | BNR | Hyundai/KIA | Sonata8 | BNR(AMP.PANEL) |  |
| canbus_sp.db | 196776 | CYT | Hyundai/KIA | Sonata8 | CYT.L |  |
| canbus_sp.db | 262312 | CYT | Hyundai/KIA | Sonata8 | CYT.H |  |
| canbus_sp.db | 327848 | CYT | Hyundai/KIA | Sonata8 | CYT.B |  |
| canbus_sp.db | 393571 | BNR | Hyundai/KIA | Sonata8 | BNR |  |
| canbus_sp.db | 459107 | BNR | Hyundai/KIA | Sonata8 | BNR(AMP) |  |
| canbus_sp.db | 524643 | BNR | Hyundai/KIA | Sonata8 | BNR(AMP.PANEL) |  |
| canbus_sp.db | 2228667 | HIWORLD | Hyundai/KIA | Sonata8 | ALL |  |
| canbus_sp.db | 2294203 | HIWORLD | Hyundai/KIA | Sonata8 | ALL.HIGH.AMP |  |
