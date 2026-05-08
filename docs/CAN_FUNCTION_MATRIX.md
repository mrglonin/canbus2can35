# 2CAN35 Sportage CAN Function Matrix

Дата: 2026-05-07

Цель этого файла - держать одну рабочую таблицу функций машины, CAN-кандидатов
из DBC и полей, которые надо заполнить по реальным логам. Это не финальная
расшифровка конкретной машины. Финальная расшифровка появляется только после
логов `baseline -> action -> back` на нашем авто.

## Как Пользоваться

1. Для каждого теста пишем marker вида `START door_driver_open_close`.
2. Логируем обе шины:
   - `ch0 = 100000`, предположительно M-CAN.
   - `ch1 = 500000`, предположительно C-CAN.
3. Одно действие - один лог. Не смешивать двери, климат, музыку и парктроники.
4. После анализа переносим факт в `data/can_function_matrix.csv`:
   - `observed_channel`
   - `observed_id`
   - `observed_signal_or_byte`
   - `off_value`
   - `on_value`
   - `confirmed`
   - `implementation_note`
5. Если DBC ID совпал, но канал другой, не считаем это ошибкой автоматически:
   один и тот же hex ID может значить разные сообщения на M-CAN и C-CAN.

## Источники

- Hyundai/Kia M-CAN DBC:
  https://github.com/BogGyver/opendbc/blob/tesla_unity_dev/hyundai_2015_mcan.dbc
- Hyundai/Kia C-CAN DBC:
  https://github.com/BogGyver/opendbc/blob/tesla_unity_dev/hyundai_2015_ccan.dbc
- commaai/opendbc:
  https://github.com/commaai/opendbc

## Статусы

| Статус | Значение |
|---|---|
| `dbc_candidate` | Есть в DBC, но на нашем авто еще не подтверждено. |
| `seen_in_log` | ID уже видели в нашем логе, но событие еще не привязано. |
| `confirmed_rx` | Машина отправляет это, мы подтвердили байт/бит логом. |
| `confirmed_tx` | Мы отправили это, машина/приборка отреагировала. |
| `implemented` | Добавлено в прошивку/USB bridge и проверено. |
| `disabled` | Сознательно не отправляем, чтобы не вызвать ошибки. |

## Базовые Функции Кузова

| Функция | Направление | Приоритетные кандидаты | Что проверять в логе | Статус |
|---|---|---|---|---|
| Зажигание ACC/IGN/Start | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_IGNSw`, `CF_Gway_Ign1`, `CF_Gway_Ign2`; M-CAN `0x522 GW_IPM_PE_1`: `C_IGNSW` | `off`, `acc`, `ign_on`, `engine_start`, `engine_idle` | `dbc_candidate` |
| Водительская дверь | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_DrvDrSw`; M-CAN `0x1EB GW_DDM_PE`: `C_DRVDoorStatus`; M-CAN `0x521 GW_DDM_PE`: `C_DRVDoorStatus` | 3 цикла open/close, найти устойчивые значения | `dbc_candidate` |
| Передняя пассажирская дверь | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_AstDrSw`; M-CAN `0x1EB`/`0x521`: `C_ASTDoorStatus` | 3 цикла open/close | `dbc_candidate` |
| Задняя левая дверь | car -> adapter | C-CAN `0x553 CGW2`: `CF_Gway_RLDrSw`; M-CAN `0x1EB`/`0x521`: `C_RLDoorStatus` | 3 цикла open/close | `dbc_candidate` |
| Задняя правая дверь | car -> adapter | C-CAN `0x553 CGW2`: `CF_Gway_RRDrSw`; M-CAN `0x1EB`/`0x521`: `C_RRDoorStatus` | 3 цикла open/close | `dbc_candidate` |
| Багажник | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_TrunkTgSw`; M-CAN `0x1EB`/`0x521`: `C_TrunkStatus` | open/close, отдельно от дверей | `dbc_candidate` |
| Капот | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_HoodSw`; C-CAN `0x4B2 CGW3`: `CF_Hoodsw_memory` | open/close, если доступно безопасно | `dbc_candidate` |
| Lock/unlock | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_RKECmd`, `CF_Gway_DrvKeyLockSw`, `CF_Gway_DrvKeyUnlockSw`, `CF_Gway_PassiveAccessLock`, `CF_Gway_PassiveAccessUnlock` | key fob lock/unlock, кнопка салона отдельно | `dbc_candidate` |
| Ремень водителя/пассажира | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_DrvSeatBeltSw`, `CF_Gway_AstSeatBeltSw`; C-CAN `0x560 ACU11`: `CF_PasBkl_Stat` | пристегнуть/отстегнуть отдельно | `dbc_candidate` |
| Люк открыт | car -> adapter | C-CAN `0x541 CGW1`: `C_SunRoofOpenState`, `CF_Gway_RrSunRoofOpenState` | открыть/закрыть люк, если безопасно; проверить сообщение на приборке | `dbc_candidate` |
| Стекла дверей | car -> adapter | C-CAN `0x559 CGW4`: `CF_Gway_DrvWdwStat`, `CF_Gway_AstWdwStat`, `CF_Gway_RLWdwState`, `CF_Gway_RRWdwState` | каждое стекло отдельно: чуть открыть/закрыть | `dbc_candidate` |
| Подогрев руля, статус | car -> adapter | C-CAN `0x559 CGW4`: `CF_Gway_StrgWhlHeatedState` | кнопка подогрева руля on/off, 3 цикла | `dbc_candidate` |
| Наружная температура | car -> adapter / adapter -> HU | C-CAN `0x383 FATC11`: `CR_Fatc_OutTemp`; C-CAN `0x044 DATC11`: `CR_Datc_OutTempC`; C-CAN `0x4B2 CGW3`: `C_MirOutTempSns` | сравнить с приборкой/климатом | `dbc_candidate` |
| Температура двигателя | car -> adapter | C-CAN `0x329 EMS12`: `TEMP_ENG`; C-CAN `0x492 EMS19`: `CR_Ems_EngOilTemp` | холодный старт -> прогрев | `dbc_candidate` |
| Напряжение АКБ | car -> adapter | C-CAN `0x545 EMS14`: `VB` | ACC/engine_idle | `dbc_candidate` |

## Движение, Руль, Передачи

| Функция | Направление | Приоритетные кандидаты | Что проверять в логе | Статус |
|---|---|---|---|---|
| Скорость авто | car -> adapter | C-CAN `0x316 EMS11`: `VS`; C-CAN `0x386 WHL_SPD11`: `WHL_SPD_FL/FR/RL/RR`; C-CAN `0x52A CLU15`: `CF_Clu_VehicleSpeed`; C-CAN `0x112 TCU12`: `VS_TCU` | 0/5/20/40 km/h безопасно | `dbc_candidate` |
| Обороты двигателя | car -> adapter | C-CAN `0x316 EMS11`: `N`; C-CAN `0x080 EMS_DCT11`: `N`; C-CAN `0x280 EMS13`: `N_32`; C-CAN `0x162 TCU_DCT13`: `Cluster_Engine_RPM` | idle/1000/1500/2000 rpm | `dbc_candidate` |
| Выбор передачи P/R/N/D | car -> adapter | C-CAN `0x111 TCU11`: `G_SEL_DISP`, `GEAR_TYPE`, `SWI_GS`; C-CAN `0x354 LVR11`: `CF_Lvr_GearInf`; C-CAN `0x112 TCU12`: `CUR_GR` | P->R->N->D на тормозе | `dbc_candidate` |
| Задний ход по CAN | car -> adapter | C-CAN `0x111 TCU11`: `G_SEL_DISP`; C-CAN `0x354 LVR11`: `CF_Lvr_GearInf`; M-CAN `0x522 GW_IPM_PE_1`: `C_AV_Tail` может влиять на AV/reverse display | включить R, сравнить с физическим reverse input | `dbc_candidate` |
| Угол руля | car -> adapter | C-CAN `0x2B0 SAS11`: `SAS_Angle`; C-CAN `0x381 MDPS11`: `CR_Mdps_StrAng`; C-CAN `0x390 SPAS11`: `CR_Spas_StrAngCmd` | center/left/right в R и без R | `dbc_candidate` |
| Колесные импульсы | car -> adapter | C-CAN `0x387 WHL_PUL11`: `WHL_PUL_*`, `WHL_DIR_*` | очень медленное движение вперед/назад | `dbc_candidate` |
| Тормоз | car -> adapter | C-CAN `0x394 TCS13`: `BrakeLight`, `PBRAKE_ACT`; C-CAN `0x329 EMS12`: `BRAKE_ACT`; C-CAN `0x541 CGW1`: `CF_Gway_ParkBrakeSw` | press/release brake, parking brake separately | `dbc_candidate` |
| Ручник/EPB | car -> adapter | C-CAN `0x490 EPB11`: `EPB_SWITCH`, `EPB_I_LAMP`, `EPB_F_LAMP`; C-CAN `0x541 CGW1`: `CF_Gway_ParkBrakeSw`; M-CAN `0x522`: `C_ParkingBrakeSW` | on/off | `dbc_candidate` |

## Свет, Поворотники, Дворники

| Функция | Направление | Приоритетные кандидаты | Что проверять в логе | Статус |
|---|---|---|---|---|
| Левый поворотник | car -> adapter / adapter -> HU | C-CAN `0x541 CGW1`: `CF_Gway_TurnSigLh`, `CF_Gway_TSigLHSw`; M-CAN `0x522 GW_IPM_PE_1` mirror possible | 3 цикла, 10 секунд | `dbc_candidate` |
| Правый поворотник | car -> adapter / adapter -> HU | C-CAN `0x541 CGW1`: `CF_Gway_TurnSigRh`, `CF_Gway_TSigRHSw`; M-CAN mirror possible | 3 цикла | `dbc_candidate` |
| Аварийка | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_HazardSw` | on/off 3 раза | `dbc_candidate` |
| Ближний свет | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_HeadLampLow`, `CF_Gway_LightSwState`; C-CAN `0x07F CGW5` open-status fields only for lamp faults | off/auto/low | `dbc_candidate` |
| Дальний свет / passing | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_HeadLampHigh`, `CF_Gway_HLpHighSw`, `CF_Gway_PassingSW` | flash/high on/off | `dbc_candidate` |
| Передние ПТФ | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_Frt_Fog_Act` | on/off | `dbc_candidate` |
| Задний обогрев стекла | car -> adapter / climate | C-CAN `0x541 CGW1`: `CF_Gway_DefoggerRly`; M-CAN `0x034 HU_DATC_E_02`: `HU_DATC_RearDefog`; M-CAN `0x134 DATC_PE_05`: `DATC_RrDefLed` | rear defog button | `dbc_candidate` |
| Дворники | car -> adapter | C-CAN `0x541 CGW1`: `CF_Gway_WiperIntSw`, `WiperLowSw`, `WiperHighSw`, `WiperAutoSw`, `WiperMistSw`, `WiperIntT`; C-CAN `0x553 CGW2`: rear wiper low/high | each position separately | `dbc_candidate` |

## Руль, Кнопки, UART/Магнитола

| Функция | Направление | Приоритетные кандидаты | Что проверять в логе | Статус |
|---|---|---|---|---|
| Mode/source | car -> adapter -> HU/UART/USB | C-CAN `0x523 GW_SWRC_PE`: `C_ModeSW`; UART SimpleSoft path separately | нажать коротко/долго если есть | `dbc_candidate` |
| Mute | car -> adapter -> HU | C-CAN `0x523`: `C_MuteSW` | press/release | `dbc_candidate` |
| Seek prev/next | car -> adapter -> HU | C-CAN `0x523`: `C_SeekDnSW`, `C_SeekUpSW` | press/release | `dbc_candidate` |
| Volume down/up | car -> adapter -> HU | C-CAN `0x523`: `C_VolDnSW`, `C_VolUpSW` | single press and hold | `dbc_candidate` |
| Phone answer/hangup | car -> adapter -> HU | C-CAN `0x523`: `C_BTPhoneCallSW`, `C_BTPhoneHangUpSW` | press/release | `dbc_candidate` |
| Voice/SDS | car -> adapter -> HU | C-CAN `0x523`: `C_SdsSW` | press/release | `dbc_candidate` |
| MTS/DISC | car -> adapter -> HU | C-CAN `0x523`: `C_MTSSW`, `C_DISCDownSW`, `C_DISCUpSW` | if buttons exist | `dbc_candidate` |

## Мультимедиа, Приборка, Навигация

| Функция | Направление | Приоритетные кандидаты | Что проверять в логе | Статус |
|---|---|---|---|---|
| Общий HU state / source | adapter/HU -> cluster | M-CAN `0x114 HU_CLU_PE_01`: `HU_OpState`, `HU_Navi_On_Off`, `HU_Track_Number`, `HU_Frequency`; M-CAN `0x197 HU_CLU_PE_05`: `HU_MuteStatus`, `HU_VolumeStatus`, `HU_NaviStatus`, `HU_Navigation_On_Off` | source none/FM/AM/USB/BT/CarPlay/Android Auto/nav/default compass | `seen_in_log` |
| FM название станции | adapter/HU -> cluster | M-CAN `0x4E8 TP_HU_FM_CLU`; reverse `0x4E9 TP_CLU_FM_HU`; status `0x114` frequency/preset | отправлять 16 символов, потом >16 | `dbc_candidate` |
| AM | adapter/HU -> cluster | M-CAN source/status likely `0x114`; отдельного AM TP в DBC нет, надо ловить в логах | включить AM и сравнить с FM | `dbc_candidate` |
| USB media text | adapter/HU -> cluster | M-CAN `0x490 TP_HU_USB_CLU`; reverse `0x497 TP_CLU_USB_HU`; status `0x114` | USB source/title/artist/track state; проверить нормальную смену источников | `seen_in_log` |
| Multimedia list / generic media | adapter/HU -> cluster | M-CAN `0x4E6 TP_HU_MLT_CLU`; reverse `0x4E7 TP_CLU_MLT_HU` | музыка, список, неизвестный source | `seen_in_log` |
| Media player | adapter/HU -> cluster | M-CAN `0x4EA TP_HU_MP_CLU`; reverse `0x4EB TP_CLU_MP_HU` | трек из APK/Android, повтор 5 секунд | `dbc_candidate` |
| Bluetooth/iBox music | adapter/HU -> cluster | M-CAN `0x4EE TP_HU_IBOX_CLU`; reverse `0x4EF`; M-CAN `0x485 TP_HU_CLU_HF` для phone/HF | BT музыка и звонок отдельно | `dbc_candidate` |
| Android Auto | adapter/HU -> cluster | M-CAN `0x4F4 TP_HU_ANDAUTO_CLU`; reverse `0x4F5`; важно не путать с C-CAN `0x4F4 SPAS12` | Android source, если доступно | `dbc_candidate` |
| CarPlay | adapter/HU -> cluster | M-CAN `0x4F2 TP_HU_CARPLAY_CLU`; reverse `0x4F3` | если есть source | `dbc_candidate` |
| Навигация: TBT icon/distance | adapter/HU -> cluster | M-CAN `0x115 HU_CLU_PE_02`; M-CAN `0x4BB TP_HU_TBT_CLU`; reverse `0x4AB`; APK cmd `0x45` | маневры 0/1/2/3, расстояния 80m/150m/1.2km | `dbc_candidate` |
| Навигация: улицы/text | adapter/HU -> cluster | M-CAN `0x49B TP_HU_NAVI_CLU`; reverse `0x48C`; APK cmd `0x45/0x48` | street name 16 chars, кириллица | `dbc_candidate` |
| Distance/ETA до финиша | adapter/HU -> cluster | M-CAN `0x1E6 HU_CLU_PE_12`, `0x1E7 HU_CLU_PE_13`; APK cmd `0x47` | 1h05m, разные distance units | `dbc_candidate` |
| Ограничение скорости / камеры | adapter/HU -> cluster | M-CAN `0x1E5 HU_CLU_PE_11`; M-CAN `0x03E HU_Navi_E_00`; APK cmd `0x44` | осторожно: может давать ошибки на комплектациях без функции | `disabled` |
| Штатный усилитель: настройки | adapter/HU <-> AMP | M-CAN `0x008..0x011 HU_AMP_E_*`, `0x180..0x185 AMP_HU_PE_*`, APK cmd `0x30` | volume/balance/fader/bass/mid/treble/mode | `dbc_candidate` |
| Beep через AMP | adapter/HU <-> AMP | M-CAN `0x011 HU_AMP_E_10`, `0x08A/0x08B AMP_HU_E_11/12` | не трогать без отдельного теста | `disabled` |

## Климат

Климат надо делить на два направления:

- `DATC -> HU/cluster`: машина показывает текущее состояние климата.
- `HU -> DATC`: магнитола/адаптер просит изменить состояние.

### Управление Климатом, HU -> DATC

Главный DBC-кандидат: M-CAN `0x034 HU_DATC_E_02`.

| Кнопка/функция | DBC signal | Что логировать | Статус |
|---|---|---|---|
| Температура водитель up/down | `HU_DATC_DrTempUpDn` | min -> 22 -> max, отдельно up/down | `dbc_candidate` |
| Температура пассажир up/down | `HU_DATC_PsTempUpDn` | min -> 22 -> max | `dbc_candidate` |
| Левая/правая задняя температура | `HU_DATC_RlTempUpDn`, `HU_DATC_RrTempUpDn` | если есть rear climate | `dbc_candidate` |
| Основной вентилятор | `HU_DATC_MainBlower` | 0 -> max -> 0, каждый шаг желательно | `dbc_candidate` |
| Дополнительный/задний вентилятор | `HU_DATC_SubBlower`, `HU_DATC_RearBlower` | если есть | `dbc_candidate` |
| Режим обдува перед | `HU_DATC_FrontModeSet` | face/feet/defrost/mix | `dbc_candidate` |
| Режим обдува зад | `HU_DATC_RearModeSet`, `HU_DATCRearPsModeSet` | если есть | `dbc_candidate` |
| Auto | `HU_DATC_AutoSet` | on/off | `dbc_candidate` |
| Off | `HU_DATC_OffReq` | on/off | `dbc_candidate` |
| Забор воздуха / recirculation | `HU_DATC_IntakeSet` | fresh/recirc | `dbc_candidate` |
| Rear climate on/off | `HU_DATC_RearOnOffSet` | on/off | `dbc_candidate` |
| A/C | `HU_DATC_AcSet` | on/off | `dbc_candidate` |
| AQS | `HU_DATC_AqsSet` | on/off/levels | `dbc_candidate` |
| Front defog | `HU_DATC_FrontDefog` | on/off | `dbc_candidate` |
| Rear defog | `HU_DATC_RearDefog` | on/off | `dbc_candidate` |
| Подогрев руля | C-CAN `0x559 CGW4`: `CF_Gway_StrgWhlHeatedState`; command frame пока неизвестен | on/off кнопкой, потом отдельно искать команду StarLine/BCM | `dbc_candidate` |
| Zone / sync | `HU_DATC_ZoneControl` | sync/dual/zone | `dbc_candidate` |
| CO2 | `HU_DATC_CO2Set` | if present | `dbc_candidate` |
| Smart vent | `DATC_SmartVentOnOffSet` | if present | `dbc_candidate` |
| ADS | `DATC_ADSOnOffSet` | if present | `dbc_candidate` |

### Отображение Климатa, DATC -> HU/cluster

| Функция | Приоритетные кандидаты | Что проверять | Статус |
|---|---|---|---|
| Версия/тип блока климата | C-CAN `0x044 DATC11`, M-CAN `0x130 DATC_PE_01` | baseline | `dbc_candidate` |
| Температура водитель/пассажир | C-CAN `0x042 DATC12`, M-CAN `0x131 DATC_PE_02` | min/22/max | `dbc_candidate` |
| Ambient/outside temp | C-CAN `0x044 DATC11`, `0x383 FATC11`, M-CAN `0x531 DATC_P_02` | сравнить с экраном | `dbc_candidate` |
| Mode display | C-CAN `0x043 DATC13`, M-CAN `0x132 DATC_PE_03` | face/feet/defrost/mix | `dbc_candidate` |
| Fan display | C-CAN `0x043 DATC13`: `CF_Datc_FrontBlwDisp`; M-CAN `0x132 DATC_PE_03`: `DATC_MainBlowerDisp` | все скорости | `dbc_candidate` |
| AC / Auto / Intake / Dual / Off | C-CAN `0x043 DATC13`; M-CAN `0x132 DATC_PE_03` | каждую кнопку отдельно | `dbc_candidate` |
| Front/rear defog LED/display | C-CAN `0x043 DATC13`, M-CAN `0x132/0x134 DATC_PE_03/05` | on/off | `dbc_candidate` |
| Seat heat/vent display | M-CAN `0x134 DATC_PE_05`: `DATC_DrSeatWarmerDisp`, `DATC_PsSeatWarmerDisp`, `DATC_DrVentSeatDisp`, `DATC_PSVentSeatDisp` | если есть кнопки | `dbc_candidate` |
| Climate load/request to engine | C-CAN `0x383 FATC11`: `CF_Fatc_AcnRqSwi`, `CF_Fatc_BlwrOn`, `CF_Fatc_DefSw` | A/C on/off, blower | `dbc_candidate` |

## Парковка, Камера, Безопасность

| Функция | Направление | Приоритетные кандидаты | Что проверять в логе | Статус |
|---|---|---|---|---|
| Парктроники PAS | car -> adapter/HU | C-CAN `0x436 PAS11`: `CF_Gway_PASDisplayFLH/FRH/FCTR/RCTR/RLH/RRH`, `PASRsound`, `PASFsound`, `PASSystemOn` | препятствие перед/сзади/лево/право отдельно | `seen_constant_zero_in_log`: в parking/reverse сегментах `00 00 00 00`, реальное препятствие не поймано |
| SPAS статус/команда руля | car -> adapter/HU | C-CAN `0x390 SPAS11`: `CF_Spas_Stat`, `CR_Spas_StrAngCmd`, `CF_Spas_BeepAlarm`, `CF_Spas_PasVol` | R + руль + препятствие | `seen_in_log`: частые изменения, смешано с рулём/статусом SPAS |
| SPAS display zones | car -> adapter/HU | C-CAN `0x4F4 SPAS12`: `CF_Spas_*_Ind`, `CF_Spas_*_Alarm`, `CF_Spas_BEEP_Alarm`; не путать с M-CAN `0x4F4 Android Auto` | все зоны парктроника | `best_current_candidate`: кнопка парктроника toggles `...00/01`, reverse даёт `00 01 C0 ... 30 01`, parking assist даёт `24/25/26/27 02/03 C0 ...` |
| Dynamic reverse lines | car -> adapter/HU | C-CAN `0x2B0 SAS11`, `0x381 MDPS11`, `0x390 SPAS11`, reverse gear candidates | R + руль left/center/right | `dbc_candidate` |
| RCTA/RCCW left/right | car -> adapter/HU | C-CAN `0x58B LCA11`: `CF_Rcta_Stat`, `CF_RCTA_IndLeft`, `CF_RCTA_IndRight`, `CF_RCTA_IndBriLeft/Right` | reverse + safe left/right cross traffic | `seen_in_log` |
| Blind spot / LCA | car -> adapter/HU | C-CAN `0x58B LCA11`: `CF_Lca_Stat`, `CF_Lca_IndLeft`, `CF_Lca_IndRight`, brightness fields | left/right side warning | `seen_in_log` |
| Rear collision warning | car -> adapter/HU | C-CAN `0x58B LCA11`: `CF_Rcw_Stat`, `CF_RCW_Warning` | если функция есть, только безопасно | `dbc_candidate` |
| LKAS / lane warning | car -> adapter/HU | C-CAN `0x340 LKAS11`: `CF_Lkas_SysWarning`, `CF_Lkas_HbaLamp`; C-CAN `0x53E LKAS12`: TSR speed fields | не активировать без безопасной дороги | `dbc_candidate` |
| TPMS | car -> adapter/HU | C-CAN `0x593 TPMS11`: warning lamps/positions; APK cmd `0x51` | baseline, request/report | `dbc_candidate` |
| ABS/TCS/ESP lamps | car -> adapter/HU | C-CAN `0x38A ABS11`, `0x153 TCS11`, `0x507 TCS15` | только пассивно, без провокации | `dbc_candidate` |
| SCC/AEB settings | car -> adapter/HU | C-CAN `0x50A SCC13`: `AebDrvSetStatus`, `SCCDrvModeRValue`; C-CAN `0x522 CLU14` settings | только читать | `dbc_candidate` |

## Настройки Приборки, USM, Диагностика, Экспериментальные Команды

Этот раздел отдельно от обычных функций. Тут есть два разных типа:

- `settings/read`: меню приборки, значения настроек, подтверждения gateway.
- `control/active`: команды, которые могут реально управлять машиной. Их нельзя
  слать вслепую. Сначала только логируем, потом повторяем на столе/в безопасном
  состоянии короткими пакетами.

| Функция | Направление | Приоритетные кандидаты | Что проверять в логе | Статус |
|---|---|---|---|---|
| Настройки приборки/USM, новые значения | cluster/HU -> gateway | C-CAN `0x515 CLU14`: `CF_Clu_*NValueSet`, включая `DoorLS`, `TempUnit`, `Lca`, `Rcta`, `Rcw`, `LkasMode`, `Fcw`, `PasSpkrLv`, `HfreeTrunk` | менять настройки в меню приборки по одной | `dbc_candidate` |
| Ответ gateway по настройкам | gateway -> cluster/HU | C-CAN `0x410 CGW_USM1`: `CF_Gway_*RValue` | после изменения настройки смотреть подтверждение | `dbc_candidate` |
| HU запрос/установка настроек gateway | HU -> gateway | C-CAN `0x526 HU_GW_E_00`, `0x527 HU_GW_E_01` | если магнитола меняет настройки авто | `dbc_candidate` |
| Gateway confirm для HU | gateway -> HU | C-CAN `0x524 GW_HU_E_00`, `0x525 GW_HU_E_01` | пара к `HU_GW_E_*` | `dbc_candidate` |
| Телематика/StarLine-подобные команды | telematics/HU -> gateway | M-CAN `0x043 TMU_GW_E_01`: `C_ReqDrLock`, `C_ReqDrUnlock`, `C_ReqHazard`, `C_ReqHorn`, `C_ReqEngineOperate`; C-CAN `0x53A TMU_GW_E_01`: `CF_Gway_TeleReq*` | только логировать при работе сигнализации/приложения; активную отправку не делать | `blocked` |
| Подогрев руля, команда включения | unknown -> gateway/BCM | Статус виден в C-CAN `0x559 CGW4`; команду надо поймать при StarLine/штатной кнопке | лог `heated_steering_button` и `heated_steering_remote`, если есть | `blocked` |
| Задний обогрев, активное включение | HU/adapter -> DATC/BCM | M-CAN `0x034 HU_DATC_E_02`: `HU_DATC_RearDefog`; C-CAN `0x541 CGW1`: `CF_Gway_DefoggerRly`; M-CAN `0x134 DATC_PE_05`: `DATC_RrDefLed` | кнопка заднего обогрева, потом пробная команда только после подтверждения | `dbc_candidate` |
| DATC self-diagnostic | DATC -> cluster/HU | C-CAN `0x040 DATC14`: `CF_Datc_DiagMode`, `CR_Datc_SelfDiagCode`; M-CAN `0x133 DATC_PE_04`: `DATC_DiagMode`, `DATC_SelfDiagDisp` | не включать диагностику специально; читать пассивно | `dbc_candidate` |
| Airbag/ACU DTC passive | ACU -> cluster/HU | C-CAN `0x5A0 ACU11`: `CF_Acu_Dtc` | только пассивно, ничего не отправлять | `blocked` |
| Gateway/module diag states | gateway -> cluster/HU | C-CAN `0x553 CGW2`: `CF_Gway_GwayDiagState`, `DDMDiagState`, `SCMDiagState`, `PSMDiagState`, `SJBDiagState`, `IPMDiagState` | baseline и после включения зажигания | `dbc_candidate` |
| OBD/UDS over CAN | tester <-> ECU | Скорее ISO-TP `0x7DF/0x7E0..0x7EF` или model-specific TP frames; отдельно от canbox-функций | только read-only запросы после отдельного решения | `blocked` |

## Локальный Интерфейс / Dashboard

Сделать реально. Нормальная архитектура такая:

```text
2CAN35 logger/update USB
        |
Python service on Mac
        |
CAN decoder + function matrix CSV
        |
Web dashboard at localhost
```

Что должен уметь dashboard:

- показывать live state: двери, багажник, капот, люк, IGN, R, скорость, RPM,
  температура, поворотники, парктроники, RCTA/LCA;
- показывать только отфильтрованные события, а не сырой CAN поток;
- иметь вкладку `Log Session`: выбрать marker, нажать start/stop, сохранить лог;
- иметь вкладку `Transmit Tests`: FM/USB/BT source, трек, навигация, TBT,
  distance/ETA, но только для whitelisted пакетов;
- иметь вкладку `Experimental`: задний обогрев, подогрев руля, настройки приборки,
  диагностика. По умолчанию все выключено и требует явного unlock;
- писать все клики и отправленные CAN/USB команды в audit log.

Первую версию проще сделать как локальную web-страницу на Mac:

- backend: Python `FastAPI` или обычный `aiohttp`;
- USB: наши текущие `tools/*logger.py` и `tools/usb_mode_2can35.py`;
- frontend: одна страница с таблицей состояний и кнопками тестов;
- входные данные: `data/can_function_matrix.csv`.

Это не заменяет прошивку. Это лабораторный пульт: видим события, кликаем
безопасные тесты, собираем доказательства, потом переносим подтвержденную
логику в прошивку адаптера/APK.

## Что Снимать Первым

Приоритет на ближайший день:

1. `baseline_engine_idle_120s`.
2. `source_usb_selected_60s`.
3. `source_fm_60s`.
4. `source_bt_music_60s`.
5. `source_carplay_or_android_auto_60s`.
6. `source_compass_default_60s`.
7. `nav_test_repeated_60s`.
8. `climate_main_blower_steps`.
9. `climate_ac_auto_intake_defog`.
10. `door_driver/passenger/rear/trunk`.
11. `sunroof_open_close`.
12. `heated_steering_button`.
13. `rear_defog_button`.
14. `cluster_settings_one_by_one`.
15. `reverse_gear_no_obstacle`.
16. `parking_rear_obstacle`.
17. `rcta_left/right`, только если безопасно.

## Поле Для Реализации

После подтверждения каждой функции надо решить тип реализации:

| Тип | Когда использовать |
|---|---|
| `read_only` | Только читать из CAN и отдавать Android/UART. |
| `mirror_to_hu` | Принять из C-CAN и отдать в M-CAN/HU. |
| `usb_to_can` | Android/Mac отправляет команду, адаптер генерирует CAN. |
| `uart_bridge` | SimpleSoft/HU UART проходит через наш адаптер с фильтром. |
| `filter_only` | Ничего нового не генерируем, только гасим мусорный кадр. |
| `blocked` | Не реализуем без дополнительных логов или есть риск ошибок. |
