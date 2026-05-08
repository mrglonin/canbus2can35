# Clean Common CAN Log 2026-05-07

Source raw log: `full_gsusb_20260507_132119.txt` (5,879,560 lines, 319 MB).
Cleaning rule: exact periodic repeats and irrelevant high-frequency raw spam are removed. Each action keeps marker ranges and candidate frames with first/last/top payloads.

## Marker Timeline
- line 1: `# MARK 2026-05-07 13:22:48 +0500 capture_start idle_car`
- line 80590: `# MARK 2026-05-07 13:23:20 +0500 baseline_idle_start_do_nothing_10s`
- line 100660: `# MARK 2026-05-07 13:23:30 +0500 baseline_idle_end`
- line 120999: `# MARK 2026-05-07 13:24:19 +0500 driver_door_open_close_done`
- line 128544: `# MARK 2026-05-07 13:24:53 +0500 passenger_front_door_open_close_done`
- line 136691: `# MARK 2026-05-07 13:25:31 +0500 rear_left_door_open_close_done`
- line 143941: `# MARK 2026-05-07 13:26:05 +0500 rear_right_door_open_close_done`
- line 148516: `# MARK 2026-05-07 13:26:44 +0500 trunk_open_close_done`
- line 157412: `# MARK 2026-05-07 13:27:50 +0500 hood_open_close_done_contaminated_with_driver_door`
- line 250337: `# MARK 2026-05-07 13:28:47 +0500 ignition_on_engine_off_and_sunroof_3x_open_close_done`
- line 372195: `# MARK 2026-05-07 13:30:00 +0500 parking_lights_on_off_done_ignition_on_engine_off`
- line 482343: `# MARK 2026-05-07 13:30:57 +0500 light_switch_auto_to_lowbeam_to_auto_done`
- line 541783: `# MARK 2026-05-07 13:31:31 +0500 highbeam_flash_3x_done`
- line 653114: `# MARK 2026-05-07 13:32:25 +0500 left_turn_signal_5_blinks_done`
- line 716381: `# MARK 2026-05-07 13:33:03 +0500 right_turn_signal_5_blinks_done`
- line 784596: `# MARK 2026-05-07 13:33:42 +0500 hazard_5_blinks_done`
- line 892648: `# MARK 2026-05-07 13:34:35 +0500 reverse_R_2x_done`
- line 1002104: `# MARK 2026-05-07 13:35:37 +0500 brake_pedal_3x_done`
- line 1221772: `# MARK 2026-05-07 13:37:25 +0500 engine_started_steering_left_center_right_cycles_done_ignition_off`
- line 1223123: `# MARK 2026-05-07 13:37:33 +0500 post_ignition_off_baseline_8s_done`
- line 1239124: `# MARK 2026-05-07 13:38:48 +0500 central_lock_lock_unlock_2x_done`
- line 1335223: `# MARK 2026-05-07 13:40:06 +0500 ignition_on_driver_window_down_up_done`
- line 1433357: `# MARK 2026-05-07 13:41:01 +0500 front_passenger_window_down_up_done`
- line 1554473: `# MARK 2026-05-07 13:42:07 +0500 rear_left_window_down_up_done`
- line 1673044: `# MARK 2026-05-07 13:43:05 +0500 rear_right_window_down_up_done`
- line 1810190: `# MARK 2026-05-07 13:44:10 +0500 climate_auto_on_off_2x_done`
- line 1955129: `# MARK 2026-05-07 13:45:17 +0500 climate_driver_temp_knob_left_right_done`
- line 2028592: `# MARK 2026-05-07 13:45:57 +0500 climate_passenger_temp_left_right_done`
- line 2112132: `# MARK 2026-05-07 13:46:42 +0500 climate_fan_speed_up3_down3_done`
- line 2376393: `# MARK 2026-05-07 13:48:35 +0500 climate_air_direction_face3_defrost4_feet5_toggle_done`
- line 2517639: `# MARK 2026-05-07 13:49:42 +0500 correction_air_direction_face4_defrost3_feet5_previous_block`
- line 2517648: `# MARK 2026-05-07 13:49:42 +0500 climate_ac_on_off_5x_done`
- line 2666971: `# MARK 2026-05-07 13:51:01 +0500 climate_recirc_on_off_5x_done`
- line 2852224: `# MARK 2026-05-07 13:52:27 +0500 engine_started_rear_defrost_on_off_4x_done`
- line 2965790: `# MARK 2026-05-07 13:53:24 +0500 heated_steering_wheel_on_off_4x_done`
- line 3086423: `# MARK 2026-05-07 13:54:29 +0500 driver_seat_heater_levels_1_2_3_off_3cycles_done`
- line 3218533: `# MARK 2026-05-07 13:55:40 +0500 passenger_seat_heater_levels_1_2_3_off_3cycles_done`
- line 3437993: `# MARK 2026-05-07 13:57:20 +0500 ignition_on_engine_off_P_to_R_4x_brake_pressed_done`
- line 3576970: `# MARK 2026-05-07 13:58:27 +0500 gear_sequence_P_R_N_D_N_R_P_brake_pressed_done`
- line 3727802: `# MARK 2026-05-07 13:59:39 +0500 steering_wheel_volume_plus5_minus5_done_engine_on_or_ignition_on`
- line 3847329: `# MARK 2026-05-07 14:00:40 +0500 steering_wheel_next5_prev5_done`
- line 3943761: `# MARK 2026-05-07 14:01:30 +0500 steering_wheel_mode_source_5x_done`
- line 4013334: `# MARK 2026-05-07 14:02:09 +0500 steering_wheel_mute_5x_done`
- line 4162062: `# MARK 2026-05-07 14:03:20 +0500 steering_wheel_call2_end2_done`
- line 4230539: `# MARK 2026-05-07 14:03:59 +0500 steering_wheel_microphone_5x_done`
- line 4413991: `# MARK 2026-05-07 14:05:31 +0500 climate_panel_common_button_5x_2repeats_done`
- line 4509520: `# MARK 2026-05-07 14:06:26 +0500 climate_off_button_5x_done`
- line 4662692: `# MARK 2026-05-07 14:07:48 +0500 driver_seat_ventilation_levels_1_2_3_off_3cycles_done`
- line 4829130: `# MARK 2026-05-07 14:09:14 +0500 passenger_seat_ventilation_levels_1_2_3_off_3cycles_done`
- line 4988424: `# MARK 2026-05-07 14:10:37 +0500 front_parking_sensors_button_on_off_5x_done`
- line 5239726: `# MARK 2026-05-07 14:12:36 +0500 parking_assist_button_3x_engine_off_rejected_cluster_message`
- line 5239777: `# MARK 2026-05-07 14:12:36 +0500 engine_started_parking_exit_assist_5x_done`
- line 5373890: `# MARK 2026-05-07 14:13:47 +0500 auto_hold_on_off_5x_done`
- line 5556351: `# MARK 2026-05-07 14:15:12 +0500 drive_mode_button_5x_done`
- line 5705440: `# MARK 2026-05-07 14:16:26 +0500 drive_lock_button_5x_done`
- line 5837288: `# MARK 2026-05-07 14:17:34 +0500 hill_descent_button_5x_done`
- line 5837308: `# MARK 2026-05-07 14:17:34 +0500 final_baseline_start_15s`
- line 5862442: `# MARK 2026-05-07 14:17:49 +0500 final_baseline_end_stop_capture_next`

## Cleaned Segments
### baseline_idle_start_do_nothing_10s
range lines 1..80590; duration 97.0s; previous marker `capture_start idle_car`
- ch1 `0x547` count=313 unique=2 first=`00 00 80 3D AC 47 CD 18` last=`00 00 81 3D AC 47 CD 18` top=00 00 81 3D AC 47 CD 18 x280; 00 00 80 3D AC 47 CD 18 x33
- ch1 `0x4E5` count=313 unique=4 first=`80 42 00 01 49 C4 00 00` last=`80 3E 00 01 49 C4 00 00` top=80 42 00 01 49 C4 00 00 x211; 80 3E 00 01 49 C4 00 00 x97; 80 42 00 01 49 C3 00 00 x4
- ch1 `0x18F` count=3134 unique=2 first=`FA 63 00 00 00 44 00 20` last=`FA 64 00 00 00 44 00 20` top=FA 64 00 00 00 44 00 20 x2335; FA 63 00 00 00 44 00 20 x799
- ch1 `0x4E6` count=313 unique=2 first=`49 C4 00 00 83 83 8C 00` last=`49 C4 00 00 83 83 8C 00` top=49 C4 00 00 83 83 8C 00 x257; 49 C4 00 00 83 83 8D 00 x56
- ch1 `0x5A0` count=31 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x16; 00 00 00 C0 20 05 A5 02 x15
- ch1 `0x113` count=3140 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 60` top=00 20 00 80 00 00 00 60 x786; 00 20 00 80 00 00 00 E8 x785; 00 20 00 80 00 00 00 AC x785
- ch1 `0x492` count=627 unique=8 first=`00 00 00 00 AD 80 75 9C` last=`00 00 00 00 AE 80 75 40` top=00 00 00 00 AD 80 75 9C x105; 00 00 00 00 AD 80 75 50 x105; 00 00 00 00 AD 80 75 D8 x105
- ch1 `0x428` count=1569 unique=1 first=`05 00 00 80 60 08 02 00` last=`05 00 00 80 60 08 02 00` top=05 00 00 80 60 08 02 00 x1569
- ch1 `0x387` count=1567 unique=1 first=`04 05 00 00 00 09` last=`04 05 00 00 00 09` top=04 05 00 00 00 09 x1567
- ch1 `0x429` count=1567 unique=1 first=`5D 02 00 00 00 00 2F 03` last=`5D 02 00 00 00 00 2F 03` top=5D 02 00 00 00 00 2F 03 x1567
- ch1 `0x436` count=627 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x627
- ch1 `0x4F4` count=627 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x627
- ch1 `0x490` count=626 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x626
- ch1 `0x58B` count=625 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x625
- ch1 `0x502` count=314 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x314
- ch1 `0x507` count=314 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x314
- ch1 `0x520` count=314 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x314
- ch1 `0x541` count=314 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x314
- ch1 `0x500` count=313 unique=1 first=`3C` last=`3C` top=3C x313
- ch1 `0x549` count=313 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x313
- ch1 `0x53E` count=310 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x310
- ch0 `0x44D` count=167 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x167
- ch0 `0x44B` count=166 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x166
- ch0 `0x157` count=158 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x158
- ch1 `0x040` count=157 unique=1 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x157
- ch0 `0x133` count=157 unique=1 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 82` top=00 00 00 00 00 00 00 82 x157
- ch0 `0x158` count=157 unique=1 first=`FF C4 00 00 00 00 00 00` last=`FF C4 00 00 00 00 00 00` top=FF C4 00 00 00 00 00 00 x157
- ch0 `0x169` count=157 unique=1 first=`20 01 03 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x157
- ch0 `0x16A` count=157 unique=1 first=`01 00 00 00 00 00 00 C3` last=`01 00 00 00 00 00 00 C3` top=01 00 00 00 00 00 00 C3 x157
- ch0 `0x1DF` count=157 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x157

### baseline_idle_end
range lines 80590..100660; duration 10.0s; previous marker `baseline_idle_start_do_nothing_10s`
- ch1 `0x18F` count=1002 unique=2 first=`FA 64 00 00 00 44 00 20` last=`FA 64 00 00 00 44 00 00` top=FA 64 00 00 00 44 00 20 x534; FA 64 00 00 00 44 00 00 x468
- ch1 `0x541` count=102 unique=2 first=`03 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x54; 00 00 41 00 C0 00 00 01 x48
- ch1 `0x547` count=101 unique=2 first=`00 00 81 3D AC 47 CD 18` last=`02 00 81 3D AC 47 CD 18` top=00 00 81 3D AC 47 CD 18 x59; 02 00 81 3D AC 47 CD 18 x42
- ch0 `0x44B` count=56 unique=2 first=`4D 02 00 00 00 00 FF FF` last=`4D 12 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x29; 4D 12 00 00 00 00 FF FF x27
- ch0 `0x158` count=53 unique=2 first=`FF C4 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C4 00 00 00 00 00 00 x27; FF C0 00 00 00 00 00 00 x26
- ch0 `0x16A` count=53 unique=2 first=`01 00 00 00 00 00 00 C3` last=`01 00 00 00 00 00 FF FF` top=01 00 00 00 00 00 00 C3 x29; 01 00 00 00 00 00 FF FF x24
- ch0 `0x1DF` count=53 unique=2 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 00 8F 00` top=38 FF 00 00 03 20 8F 00 x27; 38 FF 00 00 03 00 8F 00 x26
- ch1 `0x52A` count=53 unique=2 first=`00 00 00 00 3E 00 00 00` last=`FF 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x27; FF 00 00 00 3E 00 00 00 x26
- ch1 `0x559` count=53 unique=2 first=`00 40 00 00 20 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 20 00 00 40 x27; 00 40 00 00 00 00 00 40 x26
- ch0 `0x157` count=51 unique=2 first=`02 00 00 00 00 00 00 00` last=`CE 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x29; CE 00 00 00 00 00 00 00 x22
- ch0 `0x17B` count=51 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 03 00` top=00 00 00 00 00 00 00 00 x29; 00 00 00 00 00 00 03 00 x22
- ch1 `0x593` count=50 unique=2 first=`00 10 FF FF FF FF` last=`01 00 00 00 00 00` top=00 10 FF FF FF FF x37; 01 00 00 00 00 00 x13
- ch0 `0x132` count=13 unique=2 first=`90 00 00 28 00 00 00 00` last=`D0 C0 00 20 00 00 00 00` top=D0 C0 00 20 00 00 00 00 x7; 90 00 00 28 00 00 00 00 x6
- ch0 `0x134` count=13 unique=2 first=`03 00 00 02 00 00 10 00` last=`03 00 00 00 00 00 30 00` top=03 00 00 00 00 00 30 00 x7; 03 00 00 02 00 00 10 00 x6
- ch0 `0x167` count=13 unique=2 first=`00 00 00 00 07 C0 00 00` last=`00 00 00 F0 0F C0 00 00` top=00 00 00 F0 0F C0 00 00 x7; 00 00 00 00 07 C0 00 00 x6
- ch0 `0x5D7` count=10 unique=2 first=`04 30 00 00 00 12 80 B4` last=`00 00 00 00 00 00 00 00` top=04 30 00 00 00 12 80 B4 x5; 00 00 00 00 00 00 00 00 x5
- ch1 `0x5A0` count=6 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 05 A5 02 x3; 00 00 00 C0 20 27 95 02 x3
- ch1 `0x428` count=282 unique=3 first=`05 00 00 80 60 08 02 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 02 00 x269; 05 00 00 80 60 08 00 00 x12; 05 00 00 80 60 08 01 00 x1
- ch1 `0x429` count=282 unique=3 first=`5D 02 00 00 00 00 2F 03` last=`5D 02 00 00 00 00 20 03` top=5D 02 00 00 00 00 2F 03 x269; 5D 02 00 00 00 00 20 03 x12; 5D 02 00 00 00 00 2C 03 x1
- ch1 `0x329` count=1001 unique=6 first=`E2 BB 7F 10 11 20 00 18` last=`E2 BB 7F 12 11 2A 00 18` top=E2 BB 7F 12 11 2A 00 18 x220; E2 BB 7F 10 11 20 00 18 x193; E2 BB 7F 12 11 20 00 18 x180
- ch1 `0x4E6` count=101 unique=6 first=`49 C4 00 00 83 83 8C 00` last=`80 80 00 00 80 80 00 00` top=49 C4 00 00 83 83 8C 00 x45; 80 80 00 00 80 80 00 00 x40; 49 C4 00 00 83 83 8D 00 x9
- ch0 `0x169` count=60 unique=6 first=`20 01 03 FF 14 00 40 00` last=`2F 00 00 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x26; 2F 00 00 FF 14 00 40 00 x18; 29 01 00 FF 14 00 40 00 x7
- ch1 `0x200` count=1002 unique=8 first=`4A 00 6F 00 00 00` last=`00 00 BF 00 00 00` top=00 00 6F 00 00 00 x478; 00 00 BF 00 00 00 x213; 00 00 AF 00 00 00 x187
- ch1 `0x113` count=637 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 24` top=00 20 00 80 00 00 00 24 x160; 00 20 00 80 00 00 00 E8 x159; 00 20 00 80 00 00 00 AC x159
- ch1 `0x492` count=201 unique=4 first=`00 00 00 00 AE 80 75 04` last=`00 00 00 00 AE 80 75 04` top=00 00 00 00 AE 80 75 04 x51; 00 00 00 00 AE 80 75 C8 x50; 00 00 00 00 AE 80 75 8C x50
- ch1 `0x1BF` count=1002 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x1002
- ch1 `0x387` count=501 unique=1 first=`04 05 00 00 00 09` last=`04 05 00 00 00 09` top=04 05 00 00 00 09 x501
- ch1 `0x436` count=201 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x201
- ch1 `0x490` count=200 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x200
- ch1 `0x58B` count=107 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x107

### driver_door_open_close_done
range lines 100660..120999; duration 49.4s; previous marker `baseline_idle_end`
- ch1 `0x329` count=1167 unique=2 first=`E2 BB 7F 12 11 2A 00 18` last=`E2 BA 7F 12 11 2A 00 18` top=E2 BA 7F 12 11 2A 00 18 x892; E2 BB 7F 12 11 2A 00 18 x275
- ch1 `0x18F` count=1166 unique=2 first=`FA 64 00 00 00 44 00 00` last=`FA 65 00 00 00 44 00 00` top=FA 65 00 00 00 44 00 00 x642; FA 64 00 00 00 44 00 00 x524
- ch0 `0x133` count=116 unique=2 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x115; 00 00 00 00 00 00 00 82 x1
- ch1 `0x4E7` count=116 unique=2 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 80 00 00` top=00 79 00 08 00 80 00 00 x112; 00 79 00 08 00 81 00 00 x4
- ch1 `0x545` count=116 unique=2 first=`D9 59 00 67 00 00 00 C4` last=`D9 59 00 00 00 00 00 C4` top=D9 59 00 00 00 00 00 C4 x115; D9 59 00 67 00 00 00 C4 x1
- ch1 `0x556` count=116 unique=2 first=`00 84 00 00 00 26 00 00` last=`00 83 00 00 00 26 00 00` top=00 83 00 00 00 26 00 00 x89; 00 84 00 00 00 26 00 00 x27
- ch1 `0x5CF` count=116 unique=2 first=`3F 22 00 00 00 7F 80 00` last=`3F 21 00 00 00 7F 80 00` top=3F 21 00 00 00 7F 80 00 x113; 3F 22 00 00 00 7F 80 00 x3
- ch0 `0x135` count=115 unique=2 first=`00 00 00 00 08 00 03 00` last=`30 00 00 00 FF FF FF 00` top=30 00 00 00 FF FF FF 00 x114; 00 00 00 00 08 00 03 00 x1
- ch0 `0x169` count=113 unique=2 first=`2F 00 00 FF 14 00 40 00` last=`1F 00 00 FF 14 00 00 00` top=1F 00 00 FF 14 00 00 00 x111; 2F 00 00 FF 14 00 40 00 x2
- ch1 `0x57F` count=23 unique=2 first=`41 00 00 06 00 00 00 07` last=`FF 00 00 FF 00 00 00 07` top=FF 00 00 FF 00 00 00 07 x13; 41 00 00 06 00 00 00 07 x10
- ch0 `0x44B` count=119 unique=3 first=`4D 12 00 00 00 00 FF FF` last=`4D 32 00 00 00 00 FF FF` top=4D 12 00 00 00 00 FF FF x114; 4D 32 00 00 00 00 FF FF x3; 4B 01 00 00 00 00 FF FF x2
- ch0 `0x44D` count=118 unique=3 first=`4B 02 00 00 00 00 FF FF` last=`4B 12 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x113; 4B 12 00 00 00 00 FF FF x3; 4D 01 00 00 00 00 FF FF x2
- ch1 `0x260` count=1166 unique=4 first=`17 00 A9 00 00 88 56 37` last=`17 00 A9 00 00 88 56 28` top=17 00 A9 00 00 88 56 19 x292; 17 00 A9 00 00 88 56 28 x292; 17 00 A9 00 00 88 56 37 x291
- ch1 `0x5CE` count=116 unique=4 first=`1F 2C 80 00 00 00 80 26` last=`20 04 80 00 00 00 80 26` top=20 04 80 00 00 00 80 26 x111; 1F 2C 80 00 00 00 80 26 x3; 20 2C 80 00 00 00 80 26 x1
- ch1 `0x541` count=497 unique=2 first=`00 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=00 00 41 00 C0 00 00 01 x428; 00 01 41 00 C0 00 00 01 x69
- ch1 `0x544` count=121 unique=2 first=`FF 37 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x109; FF 77 FF FF 00 00 00 00 x12
- ch0 `0x1DF` count=119 unique=2 first=`FF FF 00 00 03 00 8F 00` last=`FF FF 00 00 03 00 8F 00` top=FF FF 00 00 03 00 8F 00 x90; FF FF 00 00 03 01 8F 00 x29
- ch0 `0x15D` count=114 unique=2 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x85; 04 00 00 00 00 70 C0 F3 x29
- ch1 `0x492` count=232 unique=4 first=`00 00 00 00 AE 80 75 C8` last=`00 00 00 00 AE 80 75 C8` top=00 00 00 00 AE 80 75 C8 x58; 00 00 00 00 AE 80 75 8C x58; 00 00 00 00 AE 80 75 40 x58
- ch1 `0x316` count=1167 unique=1 first=`04 00 00 00 00 12 00 70` last=`04 00 00 00 00 12 00 70` top=04 00 00 00 00 12 00 70 x1167
- ch1 `0x1BF` count=1166 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x1166
- ch1 `0x200` count=1166 unique=1 first=`00 00 BF 00 00 00` last=`00 00 BF 00 00 00` top=00 00 BF 00 00 00 x1166
- ch1 `0x436` count=985 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x985
- ch1 `0x520` count=493 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x493
- ch1 `0x410` count=247 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x247
- ch1 `0x50E` count=247 unique=1 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x247
- ch1 `0x553` count=247 unique=1 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x247
- ch1 `0x559` count=247 unique=1 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 00 00 00 40 x247
- ch1 `0x593` count=247 unique=1 first=`01 00 00 00 00 00` last=`01 00 00 00 00 00` top=01 00 00 00 00 00 x247
- ch1 `0x490` count=207 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x207

### passenger_front_door_open_close_done
range lines 120999..128544; duration 33.9s; previous marker `driver_door_open_close_done`
- ch1 `0x544` count=66 unique=2 first=`FF 77 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x60; FF 77 FF FF 00 00 00 00 x6
- ch1 `0x57F` count=12 unique=2 first=`41 00 00 06 00 00 00 07` last=`FF 00 00 FF 00 00 00 07` top=FF 00 00 FF 00 00 00 07 x7; 41 00 00 06 00 00 00 07 x5
- ch0 `0x44D` count=63 unique=3 first=`4D 01 00 00 00 00 FF FF` last=`4B 12 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x61; 4D 01 00 00 00 00 FF FF x1; 4B 12 00 00 00 00 FF FF x1
- ch0 `0x16A` count=69 unique=4 first=`01 80 00 00 00 00 FF FF` last=`01 00 00 00 00 00 FF FF` top=01 00 00 00 00 00 FF FF x45; 01 80 00 00 00 00 FF FF x18; 03 80 00 00 E0 E0 FF FF x5
- ch0 `0x44B` count=63 unique=4 first=`4B 01 00 00 00 00 FF FF` last=`4D 32 00 00 00 00 FF FF` top=4D 12 00 00 00 00 FF FF x60; 4B 01 00 00 00 00 FF FF x1; 4B 02 00 00 00 00 FF FF x1
- ch1 `0x541` count=336 unique=2 first=`00 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=00 00 41 00 C0 00 00 01 x302; 00 00 41 00 C8 00 00 01 x34
- ch0 `0x169` count=67 unique=2 first=`1F 00 00 FF 14 00 00 00` last=`1F 00 00 FF 14 00 00 00` top=1F 00 00 FF 14 00 00 00 x62; 3F 00 00 FF 14 00 00 00 x5
- ch0 `0x1DF` count=67 unique=2 first=`FF FF 00 00 03 00 8F 00` last=`FF FF 00 00 03 00 8F 00` top=FF FF 00 00 03 00 8F 00 x62; FF FF 00 00 03 63 8F 00 x5
- ch0 `0x15D` count=66 unique=2 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x48; 01 00 00 00 00 70 C0 F3 x18
- ch0 `0x1EB` count=65 unique=2 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x61; 00 00 00 00 00 00 00 00 x4
- ch1 `0x553` count=173 unique=3 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x153; 00 00 01 08 01 00 C0 00 x19; 00 00 10 08 01 00 C0 00 x1
- ch1 `0x387` count=1694 unique=1 first=`04 05 00 00 00 09` last=`04 05 00 00 00 09` top=04 05 00 00 00 09 x1694
- ch1 `0x436` count=662 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x662
- ch1 `0x520` count=332 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x332
- ch1 `0x410` count=166 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x166
- ch1 `0x50E` count=166 unique=1 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x166
- ch1 `0x559` count=166 unique=1 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 00 00 00 40 x166
- ch1 `0x593` count=166 unique=1 first=`01 00 00 00 00 00` last=`01 00 00 00 00 00` top=01 00 00 00 00 00 x166
- ch0 `0x133` count=61 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x61
- ch0 `0x157` count=61 unique=1 first=`CE 00 00 00 00 00 00 00` last=`CE 00 00 00 00 00 00 00` top=CE 00 00 00 00 00 00 00 x61
- ch0 `0x158` count=61 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x61
- ch0 `0x17B` count=61 unique=1 first=`00 00 00 00 00 00 03 00` last=`00 00 00 00 00 00 03 00` top=00 00 00 00 00 00 03 00 x61
- ch1 `0x52A` count=61 unique=1 first=`FF 00 00 00 3E 00 00 00` last=`FF 00 00 00 3E 00 00 00` top=FF 00 00 00 3E 00 00 00 x61
- ch0 `0x531` count=61 unique=1 first=`FE 00 FE 00 00 00 00 00` last=`FE 00 FE 00 00 00 00 00` top=FE 00 FE 00 00 00 00 00 x61
- ch0 `0x56E` count=61 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x61
- ch0 `0x135` count=60 unique=1 first=`30 00 00 00 FF FF FF 00` last=`30 00 00 00 FF FF FF 00` top=30 00 00 00 FF FF FF 00 x60
- ch1 `0x07F` count=34 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x34
- ch1 `0x5C0` count=34 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x34
- ch0 `0x121` count=19 unique=1 first=`1F FF 00 F0 7F FF FF FF` last=`1F FF 00 F0 7F FF FF FF` top=1F FF 00 F0 7F FF FF FF x19
- ch0 `0x136` count=19 unique=1 first=`00 00 00 00 00 00 F0 00` last=`00 00 00 00 00 00 F0 00` top=00 00 00 00 00 00 F0 00 x19

### rear_left_door_open_close_done
range lines 128544..136691; duration 37.7s; previous marker `passenger_front_door_open_close_done`
- ch1 `0x544` count=62 unique=2 first=`FF 77 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x56; FF 77 FF FF 00 00 00 00 x6
- ch0 `0x040` count=24 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 08 00 00 00 00` top=00 00 00 08 00 00 00 00 x18; 00 00 00 00 00 00 00 00 x6
- ch1 `0x57F` count=12 unique=2 first=`41 00 00 06 00 00 00 07` last=`FF 00 00 FF 00 00 00 07` top=FF 00 00 FF 00 00 00 07 x7; 41 00 00 06 00 00 00 07 x5
- ch0 `0x44D` count=64 unique=3 first=`4D 01 00 00 00 00 FF FF` last=`4B 12 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x62; 4D 01 00 00 00 00 FF FF x1; 4B 12 00 00 00 00 FF FF x1
- ch0 `0x16A` count=61 unique=3 first=`01 80 00 00 00 00 FF FF` last=`01 00 00 00 00 00 FF FF` top=01 00 00 00 00 00 FF FF x52; 01 80 00 00 00 00 FF FF x8; 02 00 00 00 00 00 FF FF x1
- ch0 `0x44B` count=64 unique=4 first=`4B 01 00 00 00 00 FF FF` last=`4D 32 00 00 00 00 FF FF` top=4D 12 00 00 00 00 FF FF x36; 4D 02 00 00 00 00 FF FF x26; 4B 01 00 00 00 00 FF FF x1
- ch0 `0x15D` count=63 unique=2 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x36; 00 10 00 00 00 70 C0 F3 x27
- ch1 `0x553` count=199 unique=5 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x159; 00 00 00 09 01 00 C0 00 x27; 00 00 01 08 01 00 C0 00 x11
- ch1 `0x387` count=1888 unique=1 first=`04 05 00 00 00 09` last=`04 05 00 00 00 09` top=04 05 00 00 00 09 x1888
- ch1 `0x436` count=755 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x755
- ch1 `0x520` count=377 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x377
- ch1 `0x541` count=377 unique=1 first=`00 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=00 00 41 00 C0 00 00 01 x377
- ch1 `0x410` count=189 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x189
- ch1 `0x50E` count=189 unique=1 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x189
- ch1 `0x559` count=189 unique=1 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 00 00 00 40 x189
- ch1 `0x593` count=189 unique=1 first=`01 00 00 00 00 00` last=`01 00 00 00 00 00` top=01 00 00 00 00 00 x189
- ch0 `0x133` count=58 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x58
- ch0 `0x157` count=58 unique=1 first=`CE 00 00 00 00 00 00 00` last=`CE 00 00 00 00 00 00 00` top=CE 00 00 00 00 00 00 00 x58
- ch0 `0x158` count=58 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x58
- ch0 `0x169` count=58 unique=1 first=`1F 00 00 FF 14 00 00 00` last=`1F 00 00 FF 14 00 00 00` top=1F 00 00 FF 14 00 00 00 x58
- ch0 `0x17B` count=58 unique=1 first=`00 00 00 00 00 00 03 00` last=`00 00 00 00 00 00 03 00` top=00 00 00 00 00 00 03 00 x58
- ch0 `0x1DF` count=58 unique=1 first=`FF FF 00 00 03 00 8F 00` last=`FF FF 00 00 03 00 8F 00` top=FF FF 00 00 03 00 8F 00 x58
- ch0 `0x1EB` count=58 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x58
- ch0 `0x531` count=58 unique=1 first=`FE 00 FE 00 00 00 00 00` last=`FE 00 FE 00 00 00 00 00` top=FE 00 FE 00 00 00 00 00 x58
- ch0 `0x56E` count=58 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x58
- ch0 `0x135` count=57 unique=1 first=`30 00 00 00 FF FF FF 00` last=`30 00 00 00 FF FF FF 00` top=30 00 00 00 FF FF FF 00 x57
- ch1 `0x52A` count=57 unique=1 first=`FF 00 00 00 3E 00 00 00` last=`FF 00 00 00 3E 00 00 00` top=FF 00 00 00 3E 00 00 00 x57
- ch1 `0x07F` count=38 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x38
- ch1 `0x5C0` count=38 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x38
- ch0 `0x121` count=19 unique=1 first=`1F FF 00 F0 7F FF FF FF` last=`1F FF 00 F0 7F FF FF FF` top=1F FF 00 F0 7F FF FF FF x19

### rear_right_door_open_close_done
range lines 136691..143941; duration 34.4s; previous marker `rear_left_door_open_close_done`
- ch0 `0x15D` count=50 unique=2 first=`00 40 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x29; 00 40 00 00 00 70 C0 F3 x21
- ch0 `0x040` count=20 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 08 00 00 00 00` top=00 00 00 08 00 00 00 00 x15; 00 00 00 00 00 00 00 00 x5
- ch1 `0x57F` count=11 unique=2 first=`41 00 00 06 00 00 00 07` last=`FF 00 00 FF 00 00 00 07` top=FF 00 00 FF 00 00 00 07 x6; 41 00 00 06 00 00 00 07 x5
- ch0 `0x44D` count=49 unique=3 first=`4D 01 00 00 00 00 FF FF` last=`4B 12 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x47; 4D 01 00 00 00 00 FF FF x1; 4B 12 00 00 00 00 FF FF x1
- ch0 `0x44B` count=49 unique=4 first=`4B 01 00 00 00 00 FF FF` last=`4D 32 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x25; 4D 12 00 00 00 00 FF FF x22; 4B 01 00 00 00 00 FF FF x1
- ch1 `0x553` count=176 unique=2 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x152; 00 00 80 08 01 00 C0 00 x24
- ch1 `0x544` count=52 unique=2 first=`FF 37 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x46; FF 77 FF FF 00 00 00 00 x6
- ch1 `0x387` count=1723 unique=1 first=`04 05 00 00 00 09` last=`04 05 00 00 00 09` top=04 05 00 00 00 09 x1723
- ch1 `0x436` count=689 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x689
- ch1 `0x520` count=345 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x345
- ch1 `0x541` count=345 unique=1 first=`00 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=00 00 41 00 C0 00 00 01 x345
- ch1 `0x410` count=172 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x172
- ch1 `0x50E` count=172 unique=1 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x172
- ch1 `0x559` count=172 unique=1 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 00 00 00 40 x172
- ch1 `0x593` count=172 unique=1 first=`01 00 00 00 00 00` last=`01 00 00 00 00 00` top=01 00 00 00 00 00 x172
- ch0 `0x157` count=48 unique=1 first=`CE 00 00 00 00 00 00 00` last=`CE 00 00 00 00 00 00 00` top=CE 00 00 00 00 00 00 00 x48
- ch0 `0x158` count=48 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x48
- ch0 `0x169` count=48 unique=1 first=`1F 00 00 FF 14 00 00 00` last=`1F 00 00 FF 14 00 00 00` top=1F 00 00 FF 14 00 00 00 x48
- ch0 `0x16A` count=48 unique=1 first=`01 00 00 00 00 00 FF FF` last=`01 00 00 00 00 00 FF FF` top=01 00 00 00 00 00 FF FF x48
- ch0 `0x1DF` count=48 unique=1 first=`FF FF 00 00 03 00 8F 00` last=`FF FF 00 00 03 00 8F 00` top=FF FF 00 00 03 00 8F 00 x48
- ch0 `0x531` count=48 unique=1 first=`FE 00 FE 00 00 00 00 00` last=`FE 00 FE 00 00 00 00 00` top=FE 00 FE 00 00 00 00 00 x48
- ch0 `0x56E` count=48 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x48
- ch0 `0x133` count=47 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x47
- ch0 `0x135` count=47 unique=1 first=`30 00 00 00 FF FF FF 00` last=`30 00 00 00 FF FF FF 00` top=30 00 00 00 FF FF FF 00 x47
- ch0 `0x17B` count=47 unique=1 first=`00 00 00 00 00 00 03 00` last=`00 00 00 00 00 00 03 00` top=00 00 00 00 00 00 03 00 x47
- ch0 `0x1EB` count=47 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x47
- ch1 `0x52A` count=47 unique=1 first=`FF 00 00 00 3E 00 00 00` last=`FF 00 00 00 3E 00 00 00` top=FF 00 00 00 3E 00 00 00 x47
- ch1 `0x07F` count=34 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x34
- ch1 `0x5C0` count=34 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x34
- ch0 `0x121` count=16 unique=1 first=`1F FF 00 F0 7F FF FF FF` last=`1F FF 00 F0 7F FF FF FF` top=1F FF 00 F0 7F FF FF FF x16

### trunk_open_close_done
range lines 143941..148516; duration 38.2s; previous marker `rear_right_door_open_close_done`
- ch1 `0x541` count=411 unique=4 first=`00 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=00 00 41 00 C0 00 00 01 x283; 00 10 41 00 C0 00 00 01 x84; 00 10 49 00 C0 00 00 41 x35
- ch1 `0x387` count=992 unique=1 first=`04 05 00 00 00 09` last=`04 05 00 00 00 09` top=04 05 00 00 00 09 x992
- ch1 `0x436` count=765 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x765
- ch1 `0x520` count=382 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x382
- ch1 `0x410` count=191 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x191
- ch1 `0x50E` count=191 unique=1 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x191
- ch1 `0x553` count=191 unique=1 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x191
- ch1 `0x559` count=191 unique=1 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 00 00 00 40 x191
- ch1 `0x593` count=191 unique=1 first=`01 00 00 00 00 00` last=`01 00 00 00 00 00` top=01 00 00 00 00 00 x191
- ch1 `0x07F` count=39 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x39
- ch1 `0x5C0` count=39 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x39
- ch1 `0x386` count=992 unique=16 first=`00 40 00 40 00 40 00 80` last=`00 00 00 40 00 40 00 80` top=00 40 00 40 00 40 00 80 x62; 00 80 00 40 00 40 00 80 x62; 00 C0 00 40 00 40 00 80 x62

### hood_open_close_done_contaminated_with_driver_door
range lines 148516..157412; duration 66.1s; previous marker `trunk_open_close_done`
- ch1 `0x544` count=154 unique=2 first=`FF 77 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x136; FF 77 FF FF 00 00 00 00 x18
- ch0 `0x15D` count=149 unique=2 first=`04 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x91; 04 00 00 00 00 70 C0 F3 x58
- ch0 `0x1DF` count=148 unique=2 first=`FF FF 00 00 03 01 8F 00` last=`FF FF 00 00 03 00 8F 00` top=FF FF 00 00 03 00 8F 00 x90; FF FF 00 00 03 01 8F 00 x58
- ch0 `0x040` count=62 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 08 00 00 00 00` top=00 00 00 08 00 00 00 00 x46; 00 00 00 00 00 00 00 00 x16
- ch1 `0x57F` count=32 unique=2 first=`41 00 00 06 00 00 00 07` last=`FF 00 00 FF 00 00 00 07` top=FF 00 00 FF 00 00 00 07 x19; 41 00 00 06 00 00 00 07 x13
- ch0 `0x44D` count=148 unique=3 first=`4D 01 00 00 00 00 FF FF` last=`4B 12 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x142; 4D 01 00 00 00 00 FF FF x3; 4B 12 00 00 00 00 FF FF x3
- ch0 `0x44B` count=148 unique=4 first=`4B 01 00 00 00 00 FF FF` last=`4D 32 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x75; 4D 12 00 00 00 00 FF FF x67; 4B 01 00 00 00 00 FF FF x3
- ch1 `0x541` count=682 unique=5 first=`00 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=00 00 41 00 C0 00 00 01 x478; 00 01 41 00 C0 00 00 01 x80; 00 00 43 00 C0 00 00 01 x75
- ch1 `0x436` count=1323 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1323
- ch1 `0x520` count=661 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x661
- ch1 `0x410` count=331 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x331
- ch1 `0x50E` count=331 unique=1 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x331
- ch1 `0x553` count=331 unique=1 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x331
- ch1 `0x559` count=331 unique=1 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 00 00 00 40 x331
- ch1 `0x593` count=331 unique=1 first=`01 00 00 00 00 00` last=`01 00 00 00 00 00` top=01 00 00 00 00 00 x331
- ch0 `0x157` count=142 unique=1 first=`CE 00 00 00 00 00 00 00` last=`CE 00 00 00 00 00 00 00` top=CE 00 00 00 00 00 00 00 x142
- ch0 `0x158` count=142 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x142
- ch0 `0x169` count=142 unique=1 first=`1F 00 00 FF 14 00 00 00` last=`1F 00 00 FF 14 00 00 00` top=1F 00 00 FF 14 00 00 00 x142
- ch0 `0x531` count=142 unique=1 first=`FE 00 FE 00 00 00 00 00` last=`FE 00 FE 00 00 00 00 00` top=FE 00 FE 00 00 00 00 00 x142
- ch0 `0x56E` count=142 unique=1 first=`FF 00 00 00 FF E0 00 00` last=`FF 00 00 00 FF E0 00 00` top=FF 00 00 00 FF E0 00 00 x142
- ch0 `0x16A` count=141 unique=1 first=`01 00 00 00 00 00 FF FF` last=`01 00 00 00 00 00 FF FF` top=01 00 00 00 00 00 FF FF x141
- ch0 `0x133` count=140 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x140
- ch0 `0x135` count=140 unique=1 first=`30 00 00 00 FF FF FF 00` last=`30 00 00 00 FF FF FF 00` top=30 00 00 00 FF FF FF 00 x140
- ch0 `0x17B` count=140 unique=1 first=`00 00 00 00 00 00 03 00` last=`00 00 00 00 00 00 03 00` top=00 00 00 00 00 00 03 00 x140
- ch0 `0x1EB` count=140 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x140
- ch1 `0x52A` count=139 unique=1 first=`FF 00 00 00 3E 00 00 00` last=`FF 00 00 00 3E 00 00 00` top=FF 00 00 00 3E 00 00 00 x139
- ch1 `0x07F` count=66 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x66
- ch1 `0x5C0` count=66 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x66
- ch0 `0x121` count=47 unique=1 first=`1F FF 00 F0 7F FF FF FF` last=`1F FF 00 F0 7F FF FF FF` top=1F FF 00 F0 7F FF FF FF x47
- ch0 `0x136` count=47 unique=1 first=`00 00 00 00 00 00 F0 00` last=`00 00 00 00 00 00 F0 00` top=00 00 00 00 00 00 F0 00 x47

### ignition_on_engine_off_and_sunroof_3x_open_close_done
range lines 157412..250337; duration 57.0s; previous marker `hood_open_close_done_contaminated_with_driver_door`
- ch1 `0x428` count=1786 unique=2 first=`00 00 00 00 00 00 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1760; 00 00 00 00 00 00 00 00 x26
- ch1 `0x429` count=1778 unique=2 first=`58 02 00 00 00 00 20 03` last=`5D 02 00 00 00 00 20 03` top=5D 02 00 00 00 00 20 03 x1752; 58 02 00 00 00 00 20 03 x26
- ch1 `0x4F4` count=718 unique=2 first=`00 00 C0 00 00 00 00 00` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x717; 00 00 C0 00 00 00 00 00 x1
- ch1 `0x4E5` count=351 unique=2 first=`80 00 00 17 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x350; 80 00 00 17 49 D0 00 00 x1
- ch0 `0x17B` count=235 unique=2 first=`00 00 00 00 00 00 03 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x186; 00 00 00 00 00 00 03 00 x49
- ch0 `0x56E` count=228 unique=2 first=`FF 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x180; FF 00 00 00 FF E0 00 00 x48
- ch1 `0x559` count=219 unique=2 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 20 00 00 40` top=00 40 00 00 20 00 00 40 x181; 00 40 00 00 00 00 00 40 x38
- ch1 `0x593` count=215 unique=2 first=`01 00 00 00 00 00` last=`00 10 FF FF FF FF` top=00 10 FF FF FF FF x178; 01 00 00 00 00 00 x37
- ch1 `0x040` count=178 unique=2 first=`00 00 02 0C 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x174; 00 00 02 0C 02 00 00 00 x4
- ch1 `0x4A2` count=72 unique=2 first=`00 00` last=`01 00` top=01 00 x71; 00 00 x1
- ch0 `0x040` count=60 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 08 00 00 00 00` top=00 00 00 08 00 00 00 00 x45; 00 00 00 00 00 00 00 00 x15
- ch1 `0x5BE` count=46 unique=2 first=`04 00 00 FF 00 00 00 00` last=`0C 00 00 00 00 00 00 00` top=0C 00 00 00 00 00 00 00 x38; 04 00 00 FF 00 00 00 00 x8
- ch0 `0x130` count=43 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 FF FF 00 00 00 00 00` top=00 FF FF 00 00 00 00 00 x34; 00 00 00 00 00 00 00 00 x9
- ch1 `0x043` count=40 unique=2 first=`00 00 00 0E 00 00 00 00` last=`00 00 00 04 03 00 00 00` top=00 00 00 04 03 00 00 00 x37; 00 00 00 0E 00 00 00 00 x3
- ch1 `0x7E0` count=2 unique=2 first=`03 22 F1 90 AA AA AA AA` last=`30 08 08 AA AA AA AA AA` top=03 22 F1 90 AA AA AA AA x1; 30 08 08 AA AA AA AA AA x1
- ch1 `0x58B` count=709 unique=3 first=`00 00 0C 00 00 00 00 60` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x647; 11 03 07 00 00 00 00 2F x61; 00 00 0C 00 00 00 00 60 x1
- ch1 `0x490` count=700 unique=3 first=`01 00 00 40 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x691; 01 00 10 40 00 00 00 x8; 01 00 00 40 00 00 00 x1
- ch1 `0x4E6` count=356 unique=3 first=`80 80 00 00 80 80 7E 00` last=`80 80 00 00 80 80 7D 00` top=80 80 00 00 80 80 7D 00 x277; 80 80 00 00 80 80 7E 00 x54; 80 80 00 00 80 80 7C 00 x25
- ch1 `0x5CE` count=347 unique=3 first=`2F 2C A4 00 00 00 80 26` last=`30 2C 80 00 00 00 80 26` top=2F 2C 80 00 00 00 80 26 x260; 30 2C 80 00 00 00 80 26 x86; 2F 2C A4 00 00 00 80 26 x1
- ch0 `0x44D` count=254 unique=3 first=`4D 01 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x251; 4D 01 00 00 00 00 FF FF x2; 4B 12 00 00 00 00 FF FF x1
- ch1 `0x544` count=236 unique=3 first=`FF 77 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x226; FF 77 FF FF 00 00 00 00 x6; FF F7 FF FF 00 00 00 00 x4
- ch0 `0x135` count=235 unique=3 first=`30 00 00 00 FF FF FF 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x185; 30 00 00 00 FF FF FF 00 x43; 00 00 00 00 FF FF FF 00 x7
- ch1 `0x52A` count=235 unique=3 first=`FF 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x182; FF 00 00 00 3E 00 00 00 x46; FF 00 00 00 00 00 00 00 x7
- ch0 `0x531` count=223 unique=3 first=`FE 00 FE 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x171; FE 00 FE 00 00 00 00 00 x44; FF 00 FF 00 00 00 00 00 x8
- ch0 `0x134` count=52 unique=3 first=`00 00 00 00 00 00 30 00` last=`03 00 00 02 00 00 10 00` top=03 00 00 02 00 00 10 00 x38; 00 00 00 00 00 00 30 00 x11; 00 00 00 02 00 00 30 00 x3
- ch0 `0x131` count=48 unique=3 first=`00 00 00 00 00 00 00 00` last=`00 00 FF 00 00 00 FF 00` top=00 00 FF 00 00 00 FF 00 x37; 00 00 00 00 00 00 00 00 x9; FF 00 FF 00 FF 00 FF 00 x2
- ch0 `0x5D7` count=48 unique=3 first=`00 00 00 00 00 00 00 00` last=`04 30 00 00 00 12 80 B4` top=04 30 00 00 00 12 80 B4 x35; 00 00 00 00 00 00 00 00 x11; 00 00 00 00 00 12 80 B4 x2
- ch1 `0x57F` count=28 unique=3 first=`41 00 00 06 00 00 00 07` last=`FF 00 00 FF 00 00 00 00` top=FF 00 00 FF 00 00 00 00 x18; FF 00 00 FF 00 00 00 07 x6; 41 00 00 06 00 00 00 07 x4
- ch1 `0x7E8` count=3 unique=3 first=`10 14 62 F1 90 55 35 59` last=`22 4C 30 38 33 37 36 34` top=10 14 62 F1 90 55 35 59 x1; 21 50 48 38 31 43 44 4D x1; 22 4C 30 38 33 37 36 34 x1
- ch1 `0x541` count=442 unique=4 first=`00 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x284; 03 00 41 00 C0 00 00 03 x81; 00 00 41 00 C0 00 00 01 x67

### parking_lights_on_off_done_ignition_on_engine_off
range lines 250337..372195; duration 72.5s; previous marker `ignition_on_engine_off_and_sunroof_3x_open_close_done`
- ch1 `0x7E0` count=2 unique=2 first=`03 22 F1 90 AA AA AA AA` last=`30 08 08 AA AA AA AA AA` top=03 22 F1 90 AA AA AA AA x1; 30 08 08 AA AA AA AA AA x1
- ch1 `0x4E6` count=533 unique=3 first=`80 80 00 00 80 80 7D 00` last=`80 80 00 00 80 80 7C 00` top=80 80 00 00 80 80 7C 00 x346; 80 80 00 00 80 80 00 00 x94; 80 80 00 00 80 80 7D 00 x93
- ch1 `0x547` count=522 unique=3 first=`00 00 7F 00 AC 47 CD 18` last=`00 00 80 00 AC 47 CD 18` top=00 00 80 00 AC 47 CD 18 x295; 00 00 7F 00 AC 47 CD 18 x131; 02 00 7F 00 AC 47 CD 18 x96
- ch1 `0x7E8` count=3 unique=3 first=`10 14 62 F1 90 55 35 59` last=`22 4C 30 38 33 37 36 34` top=10 14 62 F1 90 55 35 59 x1; 21 50 48 38 31 43 44 4D x1; 22 4C 30 38 33 37 36 34 x1
- ch1 `0x556` count=523 unique=4 first=`00 81 00 00 00 27 00 00` last=`00 80 00 00 00 27 00 00` top=00 81 00 00 00 27 00 00 x307; 00 80 00 00 00 27 00 00 x214; 00 81 00 00 00 31 00 00 x1
- ch1 `0x5CF` count=523 unique=4 first=`3F 07 00 00 00 7F 80 00` last=`3F 06 00 00 00 7F 80 00` top=3F 06 00 00 00 7F 80 00 x450; 3F 04 00 00 00 7F 80 00 x37; 3F 05 00 00 00 7F 80 00 x33
- ch1 `0x5A0` count=44 unique=4 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 05 A5 02 x21; 00 00 00 C0 20 27 95 02 x17; 00 00 00 00 20 00 00 00 x5
- ch1 `0x18F` count=5353 unique=2 first=`FA 64 00 00 00 44 00 00` last=`FA 63 00 00 00 44 00 00` top=FA 64 00 00 00 44 00 00 x5212; FA 63 00 00 00 44 00 00 x141
- ch1 `0x5CE` count=524 unique=5 first=`30 2C 80 00 00 00 80 26` last=`32 2C 80 00 00 00 80 26` top=31 2C 80 00 00 00 80 26 x339; 30 2C 80 00 00 00 80 26 x104; 30 04 80 00 00 00 80 26 x57
- ch1 `0x4F4` count=884 unique=2 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x883; 00 00 C0 00 00 00 00 00 x1
- ch1 `0x4E5` count=544 unique=2 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x543; 80 00 00 1C 49 D0 00 00 x1
- ch1 `0x4E7` count=535 unique=2 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x478; 00 80 00 08 00 80 00 00 x57
- ch1 `0x500` count=436 unique=2 first=`3C` last=`3C` top=3C x426; 3D x10
- ch1 `0x559` count=270 unique=2 first=`00 40 00 00 20 00 00 40` last=`00 40 00 00 20 00 00 40` top=00 40 00 00 20 00 00 40 x223; 00 40 00 00 00 00 00 40 x47
- ch1 `0x593` count=260 unique=2 first=`00 10 FF FF FF FF` last=`00 10 FF FF FF FF` top=00 10 FF FF FF FF x220; 01 00 00 00 00 00 x40
- ch0 `0x44D` count=249 unique=2 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x248; 4D 01 00 00 00 00 FF FF x1
- ch0 `0x531` count=248 unique=2 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x241; FE 00 FE 00 00 00 00 00 x7
- ch0 `0x133` count=246 unique=2 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 82` top=00 00 00 00 00 00 00 82 x236; 00 00 00 00 00 00 00 00 x10
- ch0 `0x157` count=245 unique=2 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x220; CE 00 00 00 00 00 00 00 x25
- ch1 `0x52A` count=243 unique=2 first=`00 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x222; FF 00 00 00 3E 00 00 00 x21
- ch0 `0x135` count=239 unique=2 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x230; 30 00 00 00 FF FF FF 00 x9
- ch0 `0x17B` count=239 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x215; 00 00 00 00 00 00 03 00 x24
- ch1 `0x544` count=239 unique=2 first=`FF 37 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x233; FF F7 FF FF 00 00 00 00 x6
- ch1 `0x040` count=221 unique=2 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x217; 00 00 02 0C 02 00 00 00 x4
- ch1 `0x4A2` count=92 unique=2 first=`01 00` last=`01 00` top=01 00 x91; 00 00 x1
- ch0 `0x130` count=54 unique=2 first=`00 FF FF 00 00 00 00 00` last=`00 FF FF 00 00 00 00 00` top=00 FF FF 00 00 00 00 00 x49; 00 00 00 00 00 00 00 00 x5
- ch0 `0x131` count=54 unique=2 first=`00 00 FF 00 00 00 FF 00` last=`00 00 FF 00 00 00 FF 00` top=00 00 FF 00 00 00 FF 00 x49; 00 00 00 00 00 00 00 00 x5
- ch0 `0x5D7` count=54 unique=2 first=`04 30 00 00 00 12 80 B4` last=`04 30 00 00 00 12 80 B4` top=04 30 00 00 00 12 80 B4 x50; 00 00 00 00 00 00 00 00 x4
- ch1 `0x043` count=47 unique=2 first=`00 00 00 04 03 00 00 00` last=`00 00 00 04 03 00 00 00` top=00 00 00 04 03 00 00 00 x44; 00 00 00 0E 00 00 00 00 x3
- ch1 `0x111` count=4461 unique=4 first=`00 40 61 FF 64 00 00 94` last=`00 40 61 FF 64 00 00 58` top=00 40 61 FF 64 00 00 94 x1123; 00 40 61 FF 64 00 00 58 x1116; 00 40 61 FF 64 00 00 D0 x1114

### light_switch_auto_to_lowbeam_to_auto_done
range lines 372195..482343; duration 57.5s; previous marker `parking_lights_on_off_done_ignition_on_engine_off`
- ch1 `0x556` count=396 unique=2 first=`00 80 00 00 00 27 00 00` last=`00 7F 00 00 00 27 00 00` top=00 7F 00 00 00 27 00 00 x258; 00 80 00 00 00 27 00 00 x138
- ch1 `0x5A0` count=45 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x23; 00 00 00 C0 20 27 95 02 x22
- ch1 `0x4E6` count=414 unique=2 first=`80 80 00 00 80 80 7C 00` last=`80 80 00 00 80 80 7C 00` top=80 80 00 00 80 80 7C 00 x298; 80 80 00 00 80 80 7B 00 x116
- ch1 `0x5CE` count=390 unique=2 first=`32 2C 80 00 00 00 80 26` last=`32 2C 80 00 00 00 80 26` top=32 2C 80 00 00 00 80 26 x389; 33 2C 80 00 00 00 80 26 x1
- ch1 `0x553` count=230 unique=2 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x171; 00 00 40 0A 01 00 C0 00 x59
- ch1 `0x111` count=4227 unique=4 first=`00 40 61 FF 64 00 00 58` last=`00 40 61 FF 64 00 00 1C` top=00 40 61 FF 64 00 00 94 x1063; 00 40 61 FF 64 00 00 1C x1056; 00 40 61 FF 64 00 00 D0 x1056
- ch1 `0x112` count=4181 unique=4 first=`FF A0 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF F0 00 00 FF 00 00 00 x1050; FF 00 00 00 FF 00 00 00 x1046; FF 50 00 00 FF 00 00 00 x1044
- ch1 `0x113` count=4154 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 AC x1047; 00 20 00 80 00 00 00 24 x1044; 00 20 00 80 00 00 00 60 x1037
- ch1 `0x545` count=405 unique=3 first=`DA 17 00 7D 00 00 00 B7` last=`DA 17 00 7D 00 00 00 B7` top=DA 17 00 7D 00 00 00 B7 x271; DA 17 00 7C 00 00 00 B7 x107; DA 17 00 7B 00 00 00 B7 x27
- ch1 `0x492` count=844 unique=8 first=`00 00 00 00 AA 80 73 EC` last=`00 00 00 00 A9 80 73 74` top=00 00 00 00 A9 80 73 74 x142; 00 00 00 00 A9 80 73 B0 x140; 00 00 00 00 A9 80 73 38 x134
- ch1 `0x18F` count=4432 unique=2 first=`FA 63 00 00 00 44 00 00` last=`FA 63 00 00 00 44 00 00` top=FA 63 00 00 00 44 00 00 x4422; FA 64 00 00 00 44 00 00 x10
- ch1 `0x541` count=439 unique=5 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x300; 03 00 41 80 80 00 00 01 x102; 03 00 41 00 00 00 00 01 x21
- ch1 `0x436` count=890 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x890
- ch1 `0x490` count=853 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x853
- ch1 `0x4F4` count=835 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x835
- ch1 `0x58B` count=803 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x803
- ch1 `0x520` count=434 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x434
- ch1 `0x500` count=426 unique=1 first=`3C` last=`3C` top=3C x426
- ch1 `0x507` count=420 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x420
- ch1 `0x4E5` count=418 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x418
- ch1 `0x53E` count=417 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x417
- ch1 `0x4E7` count=414 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x414
- ch1 `0x502` count=413 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x413
- ch1 `0x547` count=403 unique=1 first=`00 00 80 00 AC 47 CD 18` last=`00 00 80 00 AC 47 CD 18` top=00 00 80 00 AC 47 CD 18 x403
- ch1 `0x549` count=398 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x398
- ch1 `0x5CF` count=393 unique=1 first=`3F 06 00 00 00 7F 80 00` last=`3F 06 00 00 00 7F 80 00` top=3F 06 00 00 00 7F 80 00 x393
- ch0 `0x44B` count=251 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x251
- ch1 `0x410` count=239 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x239
- ch0 `0x1EB` count=233 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x233
- ch0 `0x44D` count=229 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x229

### highbeam_flash_3x_done
range lines 482343..541783; duration 33.7s; previous marker `light_switch_auto_to_lowbeam_to_auto_done`
- ch1 `0x4E6` count=222 unique=2 first=`80 80 00 00 80 80 7C 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7C 00 x131; 80 80 00 00 80 80 7B 00 x91
- ch1 `0x5CE` count=219 unique=2 first=`32 2C 80 00 00 00 80 26` last=`33 2C 80 00 00 00 80 26` top=33 2C 80 00 00 00 80 26 x210; 32 2C 80 00 00 00 80 26 x9
- ch1 `0x545` count=209 unique=2 first=`DA 17 00 7D 00 00 00 B7` last=`DA 17 00 7C 00 00 00 B7` top=DA 17 00 7C 00 00 00 B7 x154; DA 17 00 7D 00 00 00 B7 x55
- ch1 `0x5A0` count=25 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x13; 00 00 00 C0 20 27 95 02 x12
- ch1 `0x492` count=460 unique=4 first=`00 00 00 00 A9 80 73 B0` last=`00 00 00 00 A9 80 73 74` top=00 00 00 00 A9 80 73 FC x124; 00 00 00 00 A9 80 73 38 x119; 00 00 00 00 A9 80 73 B0 x116
- ch1 `0x557` count=208 unique=6 first=`80 85 00 00 64 A4 02 00` last=`80 85 00 00 64 A9 02 00` top=80 85 00 00 64 A6 02 00 x104; 80 85 00 00 64 A9 02 00 x39; 80 85 00 00 64 A7 02 00 x26
- ch1 `0x111` count=2318 unique=4 first=`00 40 61 FF 64 00 00 D0` last=`00 40 61 FF 64 00 00 94` top=00 40 61 FF 64 00 00 1C x598; 00 40 61 FF 64 00 00 58 x582; 00 40 61 FF 64 00 00 94 x576
- ch1 `0x112` count=2303 unique=4 first=`FF 00 00 00 FF 00 00 00` last=`FF 50 00 00 FF 00 00 00` top=FF F0 00 00 FF 00 00 00 x592; FF A0 00 00 FF 00 00 00 x581; FF 50 00 00 FF 00 00 00 x572
- ch1 `0x541` count=244 unique=4 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x221; 03 00 41 80 C1 00 08 01 x15; 03 00 41 00 C0 00 08 01 x4
- ch1 `0x428` count=1176 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1176
- ch1 `0x429` count=1144 unique=1 first=`5D 02 00 00 00 00 20 03` last=`5D 02 00 00 00 00 20 03` top=5D 02 00 00 00 00 20 03 x1144
- ch1 `0x387` count=1136 unique=1 first=`00 00 00 00 00 00` last=`00 00 00 00 00 00` top=00 00 00 00 00 00 x1136
- ch1 `0x436` count=454 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x454
- ch1 `0x490` count=447 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x447
- ch1 `0x4F4` count=438 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x438
- ch1 `0x58B` count=428 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x428
- ch1 `0x500` count=239 unique=1 first=`3C` last=`3C` top=3C x239
- ch1 `0x502` count=235 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x235
- ch1 `0x53E` count=228 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x228
- ch1 `0x4E5` count=221 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x221
- ch1 `0x520` count=217 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x217
- ch1 `0x4E7` count=215 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x215
- ch1 `0x507` count=215 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x215
- ch1 `0x5CF` count=214 unique=1 first=`3F 06 00 00 00 7F 80 00` last=`3F 06 00 00 00 7F 80 00` top=3F 06 00 00 00 7F 80 00 x214
- ch1 `0x547` count=209 unique=1 first=`00 00 80 00 AC 47 CD 18` last=`00 00 80 00 AC 47 CD 18` top=00 00 80 00 AC 47 CD 18 x209
- ch1 `0x556` count=208 unique=1 first=`00 7F 00 00 00 27 00 00` last=`00 7F 00 00 00 27 00 00` top=00 7F 00 00 00 27 00 00 x208
- ch1 `0x549` count=207 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x207
- ch0 `0x44B` count=138 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x138
- ch0 `0x158` count=132 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x132
- ch0 `0x169` count=127 unique=1 first=`20 01 00 FF 14 00 40 00` last=`20 01 00 FF 14 00 40 00` top=20 01 00 FF 14 00 40 00 x127

### left_turn_signal_5_blinks_done
range lines 541783..653114; duration 54.7s; previous marker `highbeam_flash_3x_done`
- ch1 `0x556` count=417 unique=2 first=`00 7F 00 00 00 27 00 00` last=`00 7E 00 00 00 27 00 00` top=00 7F 00 00 00 27 00 00 x383; 00 7E 00 00 00 27 00 00 x34
- ch1 `0x5CF` count=413 unique=2 first=`3F 06 00 00 00 7F 80 00` last=`3F 07 00 00 00 7F 80 00` top=3F 07 00 00 00 7F 80 00 x359; 3F 06 00 00 00 7F 80 00 x54
- ch1 `0x557` count=419 unique=5 first=`80 85 00 00 64 A9 02 00` last=`80 85 00 00 64 AD 02 00` top=80 85 00 00 64 AD 02 00 x134; 80 85 00 00 64 AC 02 00 x104; 80 85 00 00 64 A9 02 00 x66
- ch1 `0x18F` count=4669 unique=3 first=`FA 63 00 00 00 44 00 00` last=`FA 61 00 00 00 44 00 00` top=FA 62 00 00 00 44 00 00 x4550; FA 63 00 00 00 44 00 00 x109; FA 61 00 00 00 44 00 00 x10
- ch1 `0x5A0` count=42 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x22; 00 00 00 C0 20 05 A5 02 x20
- ch1 `0x113` count=4342 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 E8` top=00 20 00 80 00 00 00 AC x1102; 00 20 00 80 00 00 00 24 x1085; 00 20 00 80 00 00 00 E8 x1083
- ch1 `0x541` count=457 unique=3 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x399; 03 00 49 00 C0 04 00 01 x30; 03 00 41 00 C0 04 00 01 x28
- ch1 `0x492` count=860 unique=8 first=`00 00 00 00 A9 80 73 38` last=`00 00 00 00 A8 80 73 84` top=00 00 00 00 A9 80 73 FC x199; 00 00 00 00 A9 80 73 38 x197; 00 00 00 00 A9 80 73 B0 x193
- ch1 `0x436` count=871 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x871
- ch1 `0x490` count=857 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x857
- ch1 `0x4F4` count=845 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x845
- ch1 `0x58B` count=841 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x841
- ch1 `0x520` count=430 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x430
- ch1 `0x4E5` count=429 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x429
- ch1 `0x502` count=429 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x429
- ch1 `0x507` count=429 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x429
- ch1 `0x53E` count=427 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x427
- ch1 `0x4E6` count=423 unique=1 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7B 00 x423
- ch1 `0x500` count=421 unique=1 first=`3C` last=`3C` top=3C x421
- ch1 `0x4E7` count=420 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x420
- ch1 `0x549` count=414 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x414
- ch1 `0x547` count=413 unique=1 first=`00 00 80 00 AC 47 CD 18` last=`00 00 80 00 AC 47 CD 18` top=00 00 80 00 AC 47 CD 18 x413
- ch1 `0x5CE` count=413 unique=1 first=`33 2C 80 00 00 00 80 26` last=`33 2C 80 00 00 00 80 26` top=33 2C 80 00 00 00 80 26 x413
- ch1 `0x545` count=410 unique=1 first=`DA 17 00 7C 00 00 00 B7` last=`DA 17 00 7C 00 00 00 B7` top=DA 17 00 7C 00 00 00 B7 x410
- ch0 `0x44B` count=238 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x238
- ch0 `0x169` count=233 unique=1 first=`20 01 00 FF 14 00 40 00` last=`20 01 00 FF 14 00 40 00` top=20 01 00 FF 14 00 40 00 x233
- ch0 `0x1DF` count=227 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x227
- ch0 `0x44D` count=227 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x227
- ch1 `0x040` count=226 unique=1 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x226
- ch0 `0x158` count=226 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x226

### right_turn_signal_5_blinks_done
range lines 653114..716381; duration 37.5s; previous marker `left_turn_signal_5_blinks_done`
- ch1 `0x557` count=230 unique=3 first=`80 85 00 00 64 AD 02 00` last=`80 85 00 00 64 AE 02 00` top=80 85 00 00 64 AD 02 00 x117; 80 85 00 00 64 AE 02 00 x97; 80 85 00 00 64 AF 02 00 x16
- ch1 `0x492` count=457 unique=4 first=`00 00 00 00 A8 80 73 C0` last=`00 00 00 00 A8 80 73 84` top=00 00 00 00 A8 80 73 C0 x122; 00 00 00 00 A8 80 73 0C x116; 00 00 00 00 A8 80 73 48 x113
- ch1 `0x545` count=220 unique=2 first=`DA 17 00 7C 00 00 00 B7` last=`DA 17 00 7C 00 00 00 B7` top=DA 17 00 7C 00 00 00 B7 x202; DA 17 00 7B 00 00 00 B7 x18
- ch1 `0x4E6` count=217 unique=2 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7B 00 x213; 80 80 00 00 80 80 7A 00 x4
- ch1 `0x5A0` count=23 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x12; 00 00 00 C0 20 27 95 02 x11
- ch1 `0x260` count=2456 unique=4 first=`02 02 B8 00 09 A6 5F 0C` last=`02 02 B8 00 09 A6 5F 2A` top=02 02 B8 00 09 A6 5F 2A x626; 02 02 B8 00 09 A6 5F 0C x618; 02 02 B8 00 09 A6 5F 39 x613
- ch1 `0x113` count=2448 unique=4 first=`00 20 00 80 00 00 00 AC` last=`00 20 00 80 00 00 00 24` top=00 20 00 80 00 00 00 24 x628; 00 20 00 80 00 00 00 AC x622; 00 20 00 80 00 00 00 E8 x612
- ch1 `0x329` count=2446 unique=4 first=`40 B3 7F 12 11 2B 00 18` last=`80 B3 7F 12 11 2B 00 18` top=40 B3 7F 12 11 2B 00 18 x656; E2 B3 7F 12 11 2B 00 18 x610; 80 B3 7F 12 11 2B 00 18 x598
- ch1 `0x111` count=2445 unique=4 first=`00 40 61 FF 64 00 00 1C` last=`00 40 61 FF 64 00 00 94` top=00 40 61 FF 64 00 00 1C x623; 00 40 61 FF 64 00 00 94 x621; 00 40 61 FF 64 00 00 58 x611
- ch1 `0x112` count=2433 unique=4 first=`FF F0 00 00 FF 00 00 00` last=`FF 50 00 00 FF 00 00 00` top=FF F0 00 00 FF 00 00 00 x621; FF 50 00 00 FF 00 00 00 x619; FF A0 00 00 FF 00 00 00 x608
- ch1 `0x541` count=236 unique=3 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x188; 03 00 41 00 C0 02 00 41 x26; 03 00 41 00 C0 02 00 01 x22
- ch1 `0x18F` count=2481 unique=2 first=`FA 61 00 00 00 44 00 00` last=`FA 61 00 00 00 44 00 00` top=FA 61 00 00 00 44 00 00 x2419; FA 62 00 00 00 44 00 00 x62
- ch1 `0x387` count=1253 unique=1 first=`00 00 00 00 00 00` last=`00 00 00 00 00 00` top=00 00 00 00 00 00 x1253
- ch1 `0x428` count=1238 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1238
- ch1 `0x429` count=1222 unique=1 first=`5D 02 00 00 00 00 20 03` last=`5D 02 00 00 00 00 20 03` top=5D 02 00 00 00 00 20 03 x1222
- ch1 `0x490` count=474 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x474
- ch1 `0x58B` count=468 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x468
- ch1 `0x4F4` count=449 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x449
- ch1 `0x436` count=447 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x447
- ch1 `0x53E` count=246 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x246
- ch1 `0x5CE` count=245 unique=1 first=`33 2C 80 00 00 00 80 26` last=`33 2C 80 00 00 00 80 26` top=33 2C 80 00 00 00 80 26 x245
- ch1 `0x500` count=244 unique=1 first=`3C` last=`3C` top=3C x244
- ch1 `0x502` count=242 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x242
- ch1 `0x5CF` count=239 unique=1 first=`3F 07 00 00 00 7F 80 00` last=`3F 07 00 00 00 7F 80 00` top=3F 07 00 00 00 7F 80 00 x239
- ch1 `0x549` count=228 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x228
- ch1 `0x547` count=227 unique=1 first=`00 00 80 00 AC 47 CD 18` last=`00 00 80 00 AC 47 CD 18` top=00 00 80 00 AC 47 CD 18 x227
- ch1 `0x556` count=226 unique=1 first=`00 7E 00 00 00 27 00 00` last=`00 7E 00 00 00 27 00 00` top=00 7E 00 00 00 27 00 00 x226
- ch1 `0x4E5` count=220 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x220
- ch1 `0x520` count=220 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x220
- ch1 `0x507` count=217 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x217

### hazard_5_blinks_done
range lines 716381..784596; duration 39.4s; previous marker `right_turn_signal_5_blinks_done`
- ch1 `0x556` count=259 unique=2 first=`00 7E 00 00 00 27 00 00` last=`00 7D 00 00 00 27 00 00` top=00 7D 00 00 00 27 00 00 x150; 00 7E 00 00 00 27 00 00 x109
- ch1 `0x5CF` count=256 unique=2 first=`3F 07 00 00 00 7F 80 00` last=`3F 08 00 00 00 7F 80 00` top=3F 07 00 00 00 7F 80 00 x156; 3F 08 00 00 00 7F 80 00 x100
- ch1 `0x18F` count=2715 unique=2 first=`FA 61 00 00 00 44 00 00` last=`FA 5F 00 00 00 44 00 00` top=FA 61 00 00 00 44 00 00 x1978; FA 5F 00 00 00 44 00 00 x737
- ch1 `0x557` count=264 unique=5 first=`80 85 00 00 64 AE 02 00` last=`80 85 00 00 64 AD 02 00` top=80 85 00 00 64 AD 02 00 x163; 80 85 00 00 64 AE 02 00 x80; 80 85 00 00 64 AF 02 00 x19
- ch1 `0x545` count=259 unique=2 first=`DA 17 00 7C 00 00 00 B7` last=`DA 17 00 7C 00 00 00 B7` top=DA 17 00 7C 00 00 00 B7 x239; DA 17 00 7B 00 00 00 B7 x20
- ch1 `0x4E6` count=250 unique=2 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7B 00 x230; 80 80 00 00 80 80 7A 00 x20
- ch1 `0x5A0` count=26 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x13; 00 00 00 C0 20 27 95 02 x13
- ch1 `0x111` count=2832 unique=4 first=`00 40 61 FF 64 00 00 58` last=`00 40 61 FF 64 00 00 94` top=00 40 61 FF 64 00 00 1C x722; 00 40 61 FF 64 00 00 D0 x712; 00 40 61 FF 64 00 00 58 x706
- ch1 `0x112` count=2732 unique=4 first=`FF A0 00 00 FF 00 00 00` last=`FF 50 00 00 FF 00 00 00` top=FF F0 00 00 FF 00 00 00 x698; FF A0 00 00 FF 00 00 00 x683; FF 00 00 00 FF 00 00 00 x680
- ch1 `0x113` count=2698 unique=4 first=`00 20 00 80 00 00 00 E8` last=`00 20 00 80 00 00 00 24` top=00 20 00 80 00 00 00 AC x684; 00 20 00 80 00 00 00 E8 x676; 00 20 00 80 00 00 00 60 x670
- ch1 `0x492` count=489 unique=8 first=`00 00 00 00 A8 80 73 48` last=`00 00 00 00 A6 80 73 2C` top=00 00 00 00 A6 80 73 2C x82; 00 00 00 00 A6 80 73 A4 x76; 00 00 00 00 A6 80 73 68 x76
- ch1 `0x541` count=278 unique=4 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x224; 03 00 41 00 C2 00 00 01 x25; 03 00 49 00 C2 00 00 41 x24
- ch1 `0x387` count=1297 unique=1 first=`00 00 00 00 00 00` last=`00 00 00 00 00 00` top=00 00 00 00 00 00 x1297
- ch1 `0x428` count=1260 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1260
- ch1 `0x429` count=1238 unique=1 first=`5D 02 00 00 00 00 20 03` last=`5D 02 00 00 00 00 20 03` top=5D 02 00 00 00 00 20 03 x1238
- ch1 `0x58B` count=547 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x547
- ch1 `0x436` count=536 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x536
- ch1 `0x4F4` count=493 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x493
- ch1 `0x490` count=490 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x490
- ch1 `0x500` count=265 unique=1 first=`3C` last=`3C` top=3C x265
- ch1 `0x520` count=263 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x263
- ch1 `0x547` count=261 unique=1 first=`00 00 80 00 AC 47 CD 18` last=`00 00 80 00 AC 47 CD 18` top=00 00 80 00 AC 47 CD 18 x261
- ch1 `0x549` count=260 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x260
- ch1 `0x5CE` count=260 unique=1 first=`33 2C 80 00 00 00 80 26` last=`33 2C 80 00 00 00 80 26` top=33 2C 80 00 00 00 80 26 x260
- ch1 `0x502` count=255 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x255
- ch1 `0x53E` count=254 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x254
- ch1 `0x4E5` count=253 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x253
- ch1 `0x507` count=248 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x248
- ch1 `0x4E7` count=247 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x247
- ch0 `0x1EB` count=149 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x149

### reverse_R_2x_done
range lines 784596..892648; duration 53.1s; previous marker `hazard_5_blinks_done`
- ch1 `0x53E` count=415 unique=2 first=`00 08 00 00 80 05` last=`00 08 00 00 80 0E` top=00 08 00 00 80 05 x214; 00 08 00 00 80 0E x201
- ch1 `0x556` count=414 unique=2 first=`00 7D 00 00 00 27 00 00` last=`00 7C 00 00 00 27 00 00` top=00 7D 00 00 00 27 00 00 x296; 00 7C 00 00 00 27 00 00 x118
- ch1 `0x18F` count=4291 unique=2 first=`FA 5F 00 00 00 44 00 00` last=`FA 5E 00 00 00 44 00 00` top=FA 5F 00 00 00 44 00 00 x2503; FA 5E 00 00 00 44 00 00 x1788
- ch1 `0x557` count=421 unique=5 first=`80 85 00 00 64 AD 02 00` last=`80 85 00 00 64 A9 02 00` top=80 85 00 00 64 AD 02 00 x180; 80 85 00 00 64 AC 02 00 x109; 80 85 00 00 64 AB 02 00 x59
- ch1 `0x545` count=424 unique=2 first=`DA 17 00 7C 00 00 00 B7` last=`DA 17 00 7C 00 00 00 B7` top=DA 17 00 7B 00 00 00 B7 x250; DA 17 00 7C 00 00 00 B7 x174
- ch1 `0x50E` count=211 unique=2 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x155; 00 00 00 20 00 00 00 00 x56
- ch1 `0x5A0` count=41 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x21; 00 00 00 C0 20 27 95 02 x20
- ch1 `0x112` count=4256 unique=4 first=`FF A0 00 00 FF 00 00 00` last=`FF 00 00 00 FF 00 00 00` top=FF F0 00 00 FF 00 00 00 x1073; FF 00 00 00 FF 00 00 00 x1066; FF A0 00 00 FF 00 00 00 x1060
- ch1 `0x113` count=4226 unique=4 first=`00 20 00 80 00 00 00 E8` last=`00 20 00 80 00 00 00 60` top=00 20 00 80 00 00 00 60 x1060; 00 20 00 80 00 00 00 AC x1057; 00 20 00 80 00 00 00 E8 x1055
- ch0 `0x169` count=227 unique=3 first=`20 01 00 FF 14 00 40 00` last=`20 01 00 FF 14 00 40 00` top=20 01 00 FF 14 00 40 00 x167; 27 01 00 FF 14 00 40 00 x53; 2E 01 00 FF 14 00 40 00 x7
- ch1 `0x492` count=828 unique=8 first=`00 00 00 00 A6 80 73 68` last=`00 00 00 00 A5 80 73 3C` top=00 00 00 00 A6 80 73 68 x153; 00 00 00 00 A6 80 73 A4 x149; 00 00 00 00 A6 80 73 2C x148
- ch1 `0x4F4` count=848 unique=4 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x748; 00 01 C0 00 00 00 00 01 x74; 00 00 C0 00 00 00 00 00 x19
- ch1 `0x4E6` count=412 unique=4 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7B 00 x228; 80 80 00 00 80 80 79 00 x98; 80 80 00 00 80 80 7A 00 x85
- ch1 `0x58B` count=872 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x872
- ch1 `0x436` count=850 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x850
- ch1 `0x490` count=831 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x831
- ch1 `0x4E7` count=419 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x419
- ch1 `0x547` count=419 unique=1 first=`00 00 80 00 AC 47 CD 18` last=`00 00 80 00 AC 47 CD 18` top=00 00 80 00 AC 47 CD 18 x419
- ch1 `0x549` count=418 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x418
- ch1 `0x5CF` count=415 unique=1 first=`3F 08 00 00 00 7F 80 00` last=`3F 08 00 00 00 7F 80 00` top=3F 08 00 00 00 7F 80 00 x415
- ch1 `0x502` count=414 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x414
- ch1 `0x541` count=414 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x414
- ch1 `0x5CE` count=414 unique=1 first=`33 2C 80 00 00 00 80 26` last=`33 2C 80 00 00 00 80 26` top=33 2C 80 00 00 00 80 26 x414
- ch1 `0x500` count=412 unique=1 first=`3C` last=`3C` top=3C x412
- ch1 `0x507` count=412 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x412
- ch1 `0x4E5` count=411 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x411
- ch1 `0x520` count=409 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x409
- ch0 `0x44B` count=227 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x227
- ch0 `0x44D` count=225 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x225
- ch0 `0x15D` count=216 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x216

### brake_pedal_3x_done
range lines 892648..1002104; duration 62.2s; previous marker `reverse_R_2x_done`
- ch1 `0x5CE` count=422 unique=2 first=`33 2C 80 00 00 00 80 26` last=`32 2C 80 00 00 00 80 26` top=33 2C 80 00 00 00 80 26 x334; 32 2C 80 00 00 00 80 26 x88
- ch1 `0x5A0` count=41 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x21; 00 00 00 C0 20 05 A5 02 x20
- ch1 `0x5CF` count=422 unique=2 first=`3F 08 00 00 00 7F 80 00` last=`3F 08 00 00 00 7F 80 00` top=3F 08 00 00 00 7F 80 00 x421; 3F 09 00 00 00 7F 80 00 x1
- ch1 `0x545` count=410 unique=2 first=`DA 17 00 7C 00 00 00 B7` last=`DA 17 00 7C 00 00 00 B7` top=DA 17 00 7C 00 00 00 B7 x295; DA 17 00 7B 00 00 00 B7 x115
- ch1 `0x4E6` count=397 unique=2 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7B 00 x293; 80 80 00 00 80 80 7A 00 x104
- ch1 `0x112` count=4288 unique=4 first=`FF 50 00 00 FF 00 00 00` last=`FF A0 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x1079; FF A0 00 00 FF 00 00 00 x1079; FF F0 00 00 FF 00 00 00 x1072
- ch1 `0x113` count=4256 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 E8 x1085; 00 20 00 80 00 00 00 60 x1060; 00 20 00 80 00 00 00 24 x1058
- ch1 `0x492` count=800 unique=4 first=`00 00 00 00 A5 80 73 B4` last=`00 00 00 00 A5 80 73 B4` top=00 00 00 00 A5 80 73 78 x208; 00 00 00 00 A5 80 73 B4 x200; 00 00 00 00 A5 80 73 F0 x198
- ch1 `0x58B` count=859 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x859
- ch1 `0x490` count=854 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x854
- ch1 `0x436` count=843 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x843
- ch1 `0x4F4` count=842 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x842
- ch1 `0x556` count=418 unique=1 first=`00 7C 00 00 00 27 00 00` last=`00 7C 00 00 00 27 00 00` top=00 7C 00 00 00 27 00 00 x418
- ch1 `0x547` count=416 unique=1 first=`00 00 80 00 AC 47 CD 18` last=`00 00 80 00 AC 47 CD 18` top=00 00 80 00 AC 47 CD 18 x416
- ch1 `0x549` count=416 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x416
- ch1 `0x500` count=415 unique=1 first=`3C` last=`3C` top=3C x415
- ch1 `0x53E` count=413 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x413
- ch1 `0x507` count=412 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x412
- ch1 `0x502` count=409 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x409
- ch1 `0x4E7` count=401 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x401
- ch1 `0x520` count=401 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x401
- ch1 `0x541` count=398 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x398
- ch1 `0x4E5` count=396 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x396
- ch0 `0x44B` count=260 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x260
- ch0 `0x44D` count=242 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x242
- ch0 `0x15D` count=230 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x230
- ch0 `0x157` count=226 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x226
- ch0 `0x135` count=221 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x221
- ch0 `0x56E` count=221 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x221
- ch0 `0x1EB` count=220 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x220

### engine_started_steering_left_center_right_cycles_done_ignition_off
range lines 1002104..1221772; duration 107.6s; previous marker `brake_pedal_3x_done`
- ch1 `0x559` count=504 unique=2 first=`00 40 00 00 20 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 20 00 00 40 x402; 00 40 00 00 00 00 00 40 x102
- ch1 `0x593` count=502 unique=2 first=`00 10 FF FF FF FF` last=`01 00 00 00 00 00` top=00 10 FF FF FF FF x412; 01 00 00 00 00 00 x90
- ch0 `0x135` count=459 unique=2 first=`00 00 00 00 08 00 03 00` last=`30 00 00 00 FF FF FF 00` top=00 00 00 00 08 00 03 00 x429; 30 00 00 00 FF FF FF 00 x30
- ch0 `0x157` count=457 unique=2 first=`02 00 00 00 00 00 00 00` last=`CE 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x411; CE 00 00 00 00 00 00 00 x46
- ch0 `0x16A` count=456 unique=2 first=`01 00 00 00 00 00 00 C3` last=`01 00 00 00 00 00 FF FF` top=01 00 00 00 00 00 00 C3 x408; 01 00 00 00 00 00 FF FF x48
- ch1 `0x52A` count=453 unique=2 first=`00 00 00 00 3E 00 00 00` last=`FF 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x406; FF 00 00 00 3E 00 00 00 x47
- ch0 `0x133` count=452 unique=2 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 82 x422; 00 00 00 00 00 00 00 00 x30
- ch0 `0x531` count=446 unique=2 first=`73 00 FF 00 00 00 00 00` last=`FE 00 FE 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x417; FE 00 FE 00 00 00 00 00 x29
- ch0 `0x17B` count=443 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 03 00` top=00 00 00 00 00 00 00 00 x399; 00 00 00 00 00 00 03 00 x44
- ch0 `0x167` count=95 unique=2 first=`00 00 00 00 07 C0 00 00` last=`00 00 00 F0 0F C0 00 00` top=00 00 00 00 07 C0 00 00 x82; 00 00 00 F0 0F C0 00 00 x13
- ch0 `0x130` count=93 unique=2 first=`00 FF FF 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 FF FF 00 00 00 00 00 x85; 00 00 00 00 00 00 00 00 x8
- ch0 `0x131` count=93 unique=2 first=`00 00 FF 00 00 00 FF 00` last=`00 00 00 00 00 00 00 00` top=00 00 FF 00 00 00 FF 00 x85; 00 00 00 00 00 00 00 00 x8
- ch1 `0x5A0` count=81 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x41; 00 00 00 C0 20 27 95 02 x40
- ch0 `0x44D` count=492 unique=3 first=`4B 02 00 00 00 00 FF FF` last=`4B 12 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x489; 4B 12 00 00 00 00 FF FF x2; 4D 01 00 00 00 00 FF FF x1
- ch0 `0x1DF` count=450 unique=3 first=`38 FF 00 00 03 20 8F 00` last=`FF FF 00 00 03 00 8F 00` top=38 FF 00 00 03 20 8F 00 x400; FF FF 00 00 03 00 8F 00 x30; 38 FF 00 00 03 00 8F 00 x20
- ch0 `0x5D7` count=92 unique=3 first=`04 30 00 00 00 12 80 B4` last=`00 00 00 00 00 00 00 00` top=04 30 00 00 00 12 80 B4 x59; 04 20 00 00 00 12 80 B4 x22; 00 00 00 00 00 00 00 00 x11
- ch0 `0x44B` count=491 unique=4 first=`4D 02 00 00 00 00 FF FF` last=`4D 32 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x435; 4D 12 00 00 00 00 FF FF x53; 4D 32 00 00 00 00 FF FF x2
- ch0 `0x132` count=104 unique=4 first=`90 00 00 28 00 00 00 00` last=`F0 C0 00 00 00 00 00 00` top=90 00 00 28 00 00 00 00 x84; D0 C0 00 20 00 00 00 00 x8; F0 C0 00 00 00 00 00 00 x8
- ch0 `0x134` count=104 unique=4 first=`03 00 00 02 00 00 10 00` last=`00 00 00 00 00 00 30 00` top=03 00 00 02 00 00 10 00 x84; 03 00 00 00 00 00 30 00 x8; 00 00 00 00 00 00 30 00 x8
- ch1 `0x541` count=1001 unique=5 first=`03 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x791; 00 00 41 00 C0 00 00 01 x200; 04 00 41 00 C0 10 00 01 x7
- ch1 `0x547` count=897 unique=6 first=`00 00 80 00 AC 47 CD 18` last=`02 00 81 3C AD 47 CD 18` top=00 00 80 00 AC 47 CD 18 x436; 00 00 80 38 AD 47 CD 18 x172; 00 00 81 38 AD 47 CD 18 x103
- ch1 `0x553` count=506 unique=2 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x501; 00 00 00 08 01 01 C0 00 x5
- ch0 `0x158` count=459 unique=2 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x277; FF C4 00 00 00 00 00 00 x182
- ch1 `0x544` count=454 unique=2 first=`FF 37 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x448; FF F7 FF FF 00 00 00 00 x6
- ch1 `0x040` count=414 unique=2 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x405; 00 00 02 0C 02 00 00 00 x9
- ch1 `0x043` count=89 unique=2 first=`00 00 00 04 03 00 00 00` last=`00 00 00 04 03 00 00 00` top=00 00 00 04 03 00 00 00 x84; 00 00 00 0E 00 00 00 00 x5
- ch1 `0x113` count=8066 unique=4 first=`00 20 00 80 00 00 00 AC` last=`00 20 00 80 00 00 00 24` top=00 20 00 80 00 00 00 60 x2026; 00 20 00 80 00 00 00 E8 x2019; 00 20 00 80 00 00 00 24 x2011
- ch0 `0x169` count=463 unique=7 first=`20 01 00 FF 14 00 40 00` last=`1F 00 00 FF 14 00 00 00` top=20 01 00 FF 14 00 40 00 x222; 20 01 03 FF 14 00 40 00 x182; 1F 00 00 FF 14 00 00 00 x26
- ch1 `0x492` count=1815 unique=8 first=`00 00 00 00 A5 80 73 F0` last=`00 00 00 00 A4 80 73 88` top=00 00 00 00 A5 80 73 78 x286; 00 00 00 00 A5 80 73 F0 x285; 00 00 00 00 A5 80 73 B4 x280
- ch1 `0x490` count=1876 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1876

### post_ignition_off_baseline_8s_done
range lines 1221772..1223123; duration 8.1s; previous marker `engine_started_steering_left_center_right_cycles_done_ignition_off`
- ch1 `0x387` count=404 unique=1 first=`09 0E 00 00 00 17` last=`09 0E 00 00 00 17` top=09 0E 00 00 00 17 x404
- ch1 `0x436` count=162 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x162
- ch1 `0x520` count=81 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x81
- ch1 `0x541` count=81 unique=1 first=`00 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=00 00 41 00 C0 00 00 01 x81
- ch1 `0x410` count=41 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x41
- ch1 `0x50E` count=41 unique=1 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x41
- ch1 `0x553` count=41 unique=1 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x41
- ch1 `0x559` count=41 unique=1 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 00 00 00 40 x41
- ch1 `0x593` count=41 unique=1 first=`01 00 00 00 00 00` last=`01 00 00 00 00 00` top=01 00 00 00 00 00 x41
- ch1 `0x07F` count=9 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x9
- ch1 `0x5C0` count=8 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x8
- ch1 `0x386` count=404 unique=16 first=`00 C0 00 C0 00 40 00 80` last=`00 80 00 00 00 40 00 80` top=00 C0 00 C0 00 40 00 80 x26; 00 00 00 00 00 40 00 80 x26; 00 40 00 00 00 40 00 80 x26

### central_lock_lock_unlock_2x_done
range lines 1223123..1239124; duration 74.7s; previous marker `post_ignition_off_baseline_8s_done`
- ch1 `0x544` count=200 unique=2 first=`FF F7 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x188; FF F7 FF FF 00 00 00 00 x12
- ch0 `0x169` count=195 unique=2 first=`1F 00 00 FF 14 00 00 00` last=`3F 00 00 FF 14 00 00 00` top=1F 00 00 FF 14 00 00 00 x179; 3F 00 00 FF 14 00 00 00 x16
- ch0 `0x1DF` count=195 unique=2 first=`FF FF 00 00 03 00 8F 00` last=`FF FF 00 00 03 63 8F 00` top=FF FF 00 00 03 00 8F 00 x179; FF FF 00 00 03 63 8F 00 x16
- ch0 `0x1EB` count=194 unique=2 first=`00 08 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x178; 00 00 00 00 00 00 00 00 x16
- ch1 `0x553` count=217 unique=3 first=`00 00 00 08 01 00 C0 00` last=`00 00 01 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x125; 00 00 01 08 01 00 C0 00 x88; 00 00 10 08 01 00 C0 00 x4
- ch0 `0x44D` count=195 unique=3 first=`4D 01 00 00 00 00 FF FF` last=`4B 12 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x190; 4B 12 00 00 00 00 FF FF x3; 4D 01 00 00 00 00 FF FF x2
- ch0 `0x16A` count=213 unique=4 first=`01 80 00 00 00 00 FF FF` last=`03 80 00 00 E0 E0 FF FF` top=01 00 00 00 00 00 FF FF x106; 01 80 00 00 00 00 FF FF x88; 03 80 00 00 E0 E0 FF FF x17
- ch0 `0x44B` count=195 unique=4 first=`4B 01 00 00 00 00 FF FF` last=`4D 32 00 00 00 00 FF FF` top=4D 12 00 00 00 00 FF FF x187; 4B 01 00 00 00 00 FF FF x3; 4D 32 00 00 00 00 FF FF x3
- ch1 `0x559` count=209 unique=2 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 00 00 00 40` top=00 40 00 00 00 00 00 40 x130; 00 00 00 00 00 00 00 40 x79
- ch1 `0x436` count=763 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x763
- ch1 `0x541` count=383 unique=1 first=`00 00 41 00 C0 00 00 01` last=`00 00 41 00 C0 00 00 01` top=00 00 41 00 C0 00 00 01 x383
- ch1 `0x520` count=382 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x382
- ch0 `0x157` count=193 unique=1 first=`CE 00 00 00 00 00 00 00` last=`CE 00 00 00 00 00 00 00` top=CE 00 00 00 00 00 00 00 x193
- ch0 `0x158` count=193 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x193
- ch0 `0x531` count=192 unique=1 first=`FE 00 FE 00 00 00 00 00` last=`FE 00 FE 00 00 00 00 00` top=FE 00 FE 00 00 00 00 00 x192
- ch0 `0x56E` count=192 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x192
- ch1 `0x593` count=192 unique=1 first=`01 00 00 00 00 00` last=`01 00 00 00 00 00` top=01 00 00 00 00 00 x192
- ch0 `0x133` count=191 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x191
- ch0 `0x135` count=191 unique=1 first=`30 00 00 00 FF FF FF 00` last=`30 00 00 00 FF FF FF 00` top=30 00 00 00 FF FF FF 00 x191
- ch0 `0x15D` count=191 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x191
- ch0 `0x17B` count=191 unique=1 first=`00 00 00 00 00 00 03 00` last=`00 00 00 00 00 00 03 00` top=00 00 00 00 00 00 03 00 x191
- ch1 `0x410` count=191 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x191
- ch1 `0x50E` count=191 unique=1 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x191
- ch1 `0x52A` count=190 unique=1 first=`FF 00 00 00 3E 00 00 00` last=`FF 00 00 00 3E 00 00 00` top=FF 00 00 00 3E 00 00 00 x190
- ch0 `0x121` count=63 unique=1 first=`1F FF 00 F0 7F FF FF FF` last=`1F FF 00 F0 7F FF FF FF` top=1F FF 00 F0 7F FF FF FF x63
- ch0 `0x136` count=63 unique=1 first=`00 00 00 00 00 00 F0 00` last=`00 00 00 00 00 00 F0 00` top=00 00 00 00 00 00 F0 00 x63
- ch0 `0x167` count=40 unique=1 first=`00 00 00 F0 0F C0 00 00` last=`00 00 00 F0 0F C0 00 00` top=00 00 00 F0 0F C0 00 00 x40
- ch0 `0x5D7` count=40 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x40
- ch0 `0x130` count=39 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x39
- ch0 `0x131` count=39 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x39

### ignition_on_driver_window_down_up_done
range lines 1239124..1335223; duration 77.8s; previous marker `central_lock_lock_unlock_2x_done`
- ch1 `0x428` count=1926 unique=2 first=`00 00 00 00 00 00 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1900; 00 00 00 00 00 00 00 00 x26
- ch1 `0x429` count=1841 unique=2 first=`58 02 00 00 00 00 20 03` last=`B5 03 00 00 00 00 20 03` top=B5 03 00 00 00 00 20 03 x1815; 58 02 00 00 00 00 20 03 x26
- ch1 `0x4F4` count=740 unique=2 first=`00 00 C0 00 00 00 00 00` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x739; 00 00 C0 00 00 00 00 00 x1
- ch1 `0x4E5` count=353 unique=2 first=`80 00 00 17 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x352; 80 00 00 17 49 D0 00 00 x1
- ch0 `0x17B` count=202 unique=2 first=`00 00 00 00 00 00 03 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x198; 00 00 00 00 00 00 03 00 x4
- ch0 `0x135` count=201 unique=2 first=`30 00 00 00 FF FF FF 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x197; 30 00 00 00 FF FF FF 00 x4
- ch0 `0x44B` count=197 unique=2 first=`4B 01 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x196; 4B 01 00 00 00 00 FF FF x1
- ch0 `0x133` count=195 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 82` top=00 00 00 00 00 00 00 82 x191; 00 00 00 00 00 00 00 00 x4
- ch1 `0x040` count=194 unique=2 first=`00 00 02 0C 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x190; 00 00 02 0C 02 00 00 00 x4
- ch1 `0x544` count=190 unique=2 first=`FF F7 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x184; FF F7 FF FF 00 00 00 00 x6
- ch0 `0x157` count=187 unique=2 first=`CE 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x183; CE 00 00 00 00 00 00 00 x4
- ch0 `0x44D` count=187 unique=2 first=`4D 01 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x186; 4D 01 00 00 00 00 FF FF x1
- ch1 `0x52A` count=186 unique=2 first=`FF 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x182; FF 00 00 00 3E 00 00 00 x4
- ch0 `0x531` count=174 unique=2 first=`FE 00 FE 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x169; FE 00 FE 00 00 00 00 00 x5
- ch1 `0x559` count=171 unique=2 first=`00 40 00 00 00 00 00 40` last=`00 40 00 00 20 00 00 40` top=00 40 00 00 20 00 00 40 x166; 00 40 00 00 00 00 00 40 x5
- ch1 `0x593` count=165 unique=2 first=`01 00 00 00 00 00` last=`00 10 FF FF FF FF` top=00 10 FF FF FF FF x161; 01 00 00 00 00 00 x4
- ch1 `0x4A2` count=69 unique=2 first=`00 00` last=`01 00` top=01 00 x68; 00 00 x1
- ch0 `0x130` count=44 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 FF FF 00 00 00 00 00` top=00 FF FF 00 00 00 00 00 x43; 00 00 00 00 00 00 00 00 x1
- ch1 `0x043` count=41 unique=2 first=`00 00 00 0E 00 00 00 00` last=`00 00 00 04 03 00 00 00` top=00 00 00 04 03 00 00 00 x38; 00 00 00 0E 00 00 00 00 x3
- ch0 `0x131` count=41 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 FF 00 00 00 FF 00` top=00 00 FF 00 00 00 FF 00 x40; 00 00 00 00 00 00 00 00 x1
- ch0 `0x040` count=40 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 08 00 00 00 00` top=00 00 00 08 00 00 00 00 x30; 00 00 00 00 00 00 00 00 x10
- ch0 `0x5D7` count=36 unique=2 first=`00 00 00 00 00 00 00 00` last=`04 20 00 00 00 12 80 B4` top=04 20 00 00 00 12 80 B4 x35; 00 00 00 00 00 00 00 00 x1
- ch1 `0x7E0` count=2 unique=2 first=`03 22 F1 90 AA AA AA AA` last=`30 08 08 AA AA AA AA AA` top=03 22 F1 90 AA AA AA AA x1; 30 08 08 AA AA AA AA AA x1
- ch1 `0x58B` count=762 unique=3 first=`00 00 0C 00 00 00 00 60` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x700; 11 03 07 00 00 00 00 2F x61; 00 00 0C 00 00 00 00 60 x1
- ch1 `0x541` count=366 unique=3 first=`00 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x356; 00 00 41 00 C0 00 00 01 x6; 02 00 41 00 C0 00 00 01 x4
- ch1 `0x556` count=341 unique=3 first=`00 78 00 00 00 2D 00 00` last=`00 78 00 00 00 27 00 00` top=00 78 00 00 00 27 00 00 x339; 00 78 00 00 00 2D 00 00 x1; 00 78 00 00 00 39 00 00 x1
- ch1 `0x5CE` count=336 unique=3 first=`1B 2C A9 00 00 00 80 26` last=`1A 2C 80 00 00 00 80 26` top=1B 2C 80 00 00 00 80 26 x286; 1A 2C 80 00 00 00 80 26 x49; 1B 2C A9 00 00 00 80 26 x1
- ch0 `0x169` count=183 unique=3 first=`1F 00 00 FF 14 00 00 00` last=`20 01 00 FF 14 00 40 00` top=20 01 00 FF 14 00 40 00 x175; 1F 00 00 FF 14 00 00 00 x4; 2F 00 00 FF 14 00 40 00 x4
- ch0 `0x16A` count=183 unique=3 first=`01 00 00 00 00 00 FF FF` last=`01 00 00 00 00 00 00 C3` top=01 00 00 00 00 00 00 C3 x161; 01 00 00 00 00 00 C3 C3 x18; 01 00 00 00 00 00 FF FF x4
- ch0 `0x1DF` count=179 unique=3 first=`FF FF 00 00 03 00 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x171; FF FF 00 00 03 00 8F 00 x4; FF FF 00 00 03 20 8F 00 x4

### front_passenger_window_down_up_done
range lines 1335223..1433357; duration 55.7s; previous marker `ignition_on_driver_window_down_up_done`
- ch1 `0x5CF` count=373 unique=2 first=`3F 08 00 00 00 7F 80 00` last=`3F 09 00 00 00 7F 80 00` top=3F 08 00 00 00 7F 80 00 x312; 3F 09 00 00 00 7F 80 00 x61
- ch1 `0x5CE` count=368 unique=2 first=`1A 2C 80 00 00 00 80 26` last=`19 2C 80 00 00 00 80 26` top=1A 2C 80 00 00 00 80 26 x221; 19 2C 80 00 00 00 80 26 x147
- ch1 `0x492` count=752 unique=4 first=`00 00 00 00 A2 80 73 6C` last=`00 00 00 00 A2 80 73 E4` top=00 00 00 00 A2 80 73 E4 x199; 00 00 00 00 A2 80 73 20 x191; 00 00 00 00 A2 80 73 A8 x182
- ch1 `0x18F` count=3890 unique=2 first=`FA 5D 00 00 00 47 00 00` last=`FA 5C 00 00 00 47 00 00` top=FA 5D 00 00 00 47 00 00 x2507; FA 5C 00 00 00 47 00 00 x1383
- ch1 `0x5A0` count=39 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x21; 00 00 00 C0 20 27 95 02 x18
- ch1 `0x111` count=3952 unique=4 first=`00 40 61 FF 63 00 00 2C` last=`00 40 61 FF 63 00 00 A4` top=00 40 61 FF 63 00 00 A4 x995; 00 40 61 FF 63 00 00 2C x993; 00 40 61 FF 63 00 00 E0 x990
- ch1 `0x112` count=3850 unique=4 first=`FF F0 00 00 FF 00 00 00` last=`FF 50 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x979; FF 00 00 00 FF 00 00 00 x962; FF F0 00 00 FF 00 00 00 x955
- ch1 `0x113` count=3831 unique=4 first=`00 20 00 80 00 00 00 AC` last=`00 20 00 80 00 00 00 24` top=00 20 00 80 00 00 00 24 x968; 00 20 00 80 00 00 00 AC x957; 00 20 00 80 00 00 00 60 x954
- ch1 `0x329` count=3689 unique=4 first=`0F AB 7F 12 11 2B 00 18` last=`40 AB 7F 12 11 2B 00 18` top=0F AB 7F 12 11 2B 00 18 x945; 80 AB 7F 12 11 2B 00 18 x935; 40 AB 7F 12 11 2B 00 18 x906
- ch1 `0x4E6` count=369 unique=4 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7B 00 x263; 80 80 00 00 80 80 7A 00 x99; 80 80 00 00 80 80 79 00 x6
- ch1 `0x545` count=369 unique=4 first=`DB 17 00 7C 00 00 00 AB` last=`DB 17 00 7C 00 00 00 AB` top=DB 17 00 7C 00 00 00 AB x230; DB 17 00 7B 00 00 00 AB x132; DB 17 00 7A 00 00 00 AB x4
- ch1 `0x428` count=1961 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1961
- ch1 `0x387` count=1896 unique=1 first=`09 0E 00 00 00 17` last=`09 0E 00 00 00 17` top=09 0E 00 00 00 17 x1896
- ch1 `0x429` count=1889 unique=1 first=`B5 03 00 00 00 00 20 03` last=`B5 03 00 00 00 00 20 03` top=B5 03 00 00 00 00 20 03 x1889
- ch1 `0x58B` count=790 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x790
- ch1 `0x436` count=766 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x766
- ch1 `0x4F4` count=765 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x765
- ch1 `0x490` count=736 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x736
- ch1 `0x502` count=387 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x387
- ch1 `0x549` count=381 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x381
- ch1 `0x556` count=375 unique=1 first=`00 78 00 00 00 27 00 00` last=`00 78 00 00 00 27 00 00` top=00 78 00 00 00 27 00 00 x375
- ch1 `0x4E7` count=374 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x374
- ch1 `0x541` count=374 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x374
- ch1 `0x500` count=373 unique=1 first=`3C` last=`3C` top=3C x373
- ch1 `0x53E` count=372 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x372
- ch1 `0x547` count=372 unique=1 first=`00 00 80 00 AD 47 CD 18` last=`00 00 80 00 AD 47 CD 18` top=00 00 80 00 AD 47 CD 18 x372
- ch1 `0x4E5` count=370 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x370
- ch1 `0x520` count=369 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x369
- ch1 `0x507` count=368 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x368
- ch0 `0x1DF` count=211 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x211

### rear_left_window_down_up_done
range lines 1433357..1554473; duration 65.6s; previous marker `front_passenger_window_down_up_done`
- ch1 `0x556` count=463 unique=2 first=`00 78 00 00 00 27 00 00` last=`00 77 00 00 00 27 00 00` top=00 77 00 00 00 27 00 00 x374; 00 78 00 00 00 27 00 00 x89
- ch1 `0x5A0` count=48 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 05 A5 02 x27; 00 00 00 C0 20 27 95 02 x21
- ch1 `0x5CE` count=451 unique=3 first=`19 2C 80 00 00 00 80 26` last=`17 2C 80 00 00 00 80 26` top=18 2C 80 00 00 00 80 26 x295; 19 2C 80 00 00 00 80 26 x90; 17 2C 80 00 00 00 80 26 x66
- ch1 `0x5CF` count=451 unique=2 first=`3F 09 00 00 00 7F 80 00` last=`3F 09 00 00 00 7F 80 00` top=3F 08 00 00 00 7F 80 00 x253; 3F 09 00 00 00 7F 80 00 x198
- ch1 `0x112` count=4770 unique=4 first=`FF A0 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x1204; FF F0 00 00 FF 00 00 00 x1199; FF A0 00 00 FF 00 00 00 x1191
- ch1 `0x113` count=4743 unique=4 first=`00 20 00 80 00 00 00 E8` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 AC x1193; 00 20 00 80 00 00 00 24 x1192; 00 20 00 80 00 00 00 60 x1181
- ch1 `0x545` count=471 unique=3 first=`DB 17 00 7C 00 00 00 AB` last=`DB 17 00 7C 00 00 00 AB` top=DB 17 00 7C 00 00 00 AB x321; DB 17 00 7B 00 00 00 AB x142; DB 17 00 7A 00 00 00 AB x8
- ch1 `0x4E6` count=469 unique=3 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7B 00 x342; 80 80 00 00 80 80 7A 00 x119; 80 80 00 00 80 80 79 00 x8
- ch1 `0x58B` count=987 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x987
- ch1 `0x490` count=928 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x928
- ch1 `0x4F4` count=928 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x928
- ch1 `0x436` count=924 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x924
- ch1 `0x4E7` count=470 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x470
- ch1 `0x4E5` count=469 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x469
- ch1 `0x547` count=467 unique=1 first=`00 00 80 00 AD 47 CD 18` last=`00 00 80 00 AD 47 CD 18` top=00 00 80 00 AD 47 CD 18 x467
- ch1 `0x502` count=463 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x463
- ch1 `0x520` count=462 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x462
- ch1 `0x53E` count=462 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x462
- ch1 `0x549` count=461 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x461
- ch1 `0x500` count=459 unique=1 first=`3C` last=`3C` top=3C x459
- ch1 `0x541` count=452 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x452
- ch1 `0x507` count=451 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x451
- ch0 `0x44B` count=283 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x283
- ch0 `0x44D` count=261 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x261
- ch0 `0x1DF` count=252 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x252
- ch0 `0x157` count=248 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x248
- ch0 `0x531` count=248 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x248
- ch0 `0x169` count=246 unique=1 first=`20 01 00 FF 14 00 40 00` last=`20 01 00 FF 14 00 40 00` top=20 01 00 FF 14 00 40 00 x246
- ch0 `0x135` count=241 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x241
- ch0 `0x16A` count=241 unique=1 first=`01 00 00 00 00 00 00 C3` last=`01 00 00 00 00 00 00 C3` top=01 00 00 00 00 00 00 C3 x241

### rear_right_window_down_up_done
range lines 1554473..1673044; duration 57.8s; previous marker `rear_left_window_down_up_done`
- ch1 `0x556` count=452 unique=2 first=`00 77 00 00 00 27 00 00` last=`00 76 00 00 00 27 00 00` top=00 77 00 00 00 27 00 00 x332; 00 76 00 00 00 27 00 00 x120
- ch1 `0x5CE` count=435 unique=2 first=`17 2C 80 00 00 00 80 26` last=`16 2C 80 00 00 00 80 26` top=16 2C 80 00 00 00 80 26 x252; 17 2C 80 00 00 00 80 26 x183
- ch1 `0x5A0` count=44 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 05 A5 02 x22; 00 00 00 C0 20 27 95 02 x22
- ch1 `0x316` count=4606 unique=2 first=`05 02 00 00 02 15 00 80` last=`05 02 00 00 02 16 00 80` top=05 02 00 00 02 15 00 80 x3228; 05 02 00 00 02 16 00 80 x1378
- ch1 `0x18F` count=4705 unique=3 first=`FA 5C 00 00 00 47 00 00` last=`FA 5B 00 00 00 48 00 00` top=FA 5B 00 00 00 47 00 00 x2616; FA 5B 00 00 00 48 00 00 x1324; FA 5C 00 00 00 47 00 00 x765
- ch1 `0x547` count=448 unique=2 first=`00 00 80 00 AD 47 CD 18` last=`00 00 80 00 AD 47 CD 18` top=00 00 80 00 AD 47 CD 18 x447; 00 00 7F 00 AD 47 CD 18 x1
- ch1 `0x4E6` count=463 unique=3 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7B 00 x315; 80 80 00 00 80 80 7A 00 x136; 80 80 00 00 80 80 79 00 x12
- ch1 `0x545` count=451 unique=3 first=`DB 17 00 7C 00 00 00 AB` last=`DB 17 00 7C 00 00 00 AB` top=DB 17 00 7C 00 00 00 AB x257; DB 17 00 7B 00 00 00 AB x182; DB 17 00 7A 00 00 00 AB x12
- ch1 `0x492` count=926 unique=8 first=`00 00 00 00 A0 80 73 8C` last=`00 00 00 00 9E 80 73 70` top=00 00 00 00 A0 80 73 8C x176; 00 00 00 00 A0 80 73 40 x176; 00 00 00 00 A0 80 73 04 x171
- ch1 `0x58B` count=937 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x937
- ch1 `0x436` count=935 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x935
- ch1 `0x490` count=924 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x924
- ch1 `0x4F4` count=900 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x900
- ch1 `0x500` count=469 unique=1 first=`3C` last=`3C` top=3C x469
- ch1 `0x541` count=469 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x469
- ch1 `0x4E5` count=466 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x466
- ch1 `0x520` count=461 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x461
- ch1 `0x502` count=459 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x459
- ch1 `0x4E7` count=456 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x456
- ch1 `0x507` count=455 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x455
- ch1 `0x53E` count=452 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x452
- ch1 `0x549` count=448 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x448
- ch1 `0x5CF` count=438 unique=1 first=`3F 09 00 00 00 7F 80 00` last=`3F 09 00 00 00 7F 80 00` top=3F 09 00 00 00 7F 80 00 x438
- ch0 `0x44B` count=259 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x259
- ch0 `0x44D` count=257 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x257
- ch1 `0x52A` count=242 unique=1 first=`00 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x242
- ch1 `0x544` count=240 unique=1 first=`FF 37 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x240
- ch0 `0x157` count=237 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x237
- ch1 `0x040` count=235 unique=1 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x235
- ch0 `0x135` count=235 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x235

### climate_auto_on_off_2x_done
range lines 1673044..1810190; duration 65.5s; previous marker `rear_right_window_down_up_done`
- ch1 `0x547` count=507 unique=2 first=`00 00 80 00 AD 47 CD 18` last=`00 00 7F 00 AD 47 CD 18` top=00 00 7F 00 AD 47 CD 18 x465; 00 00 80 00 AD 47 CD 18 x42
- ch1 `0x5CE` count=509 unique=3 first=`16 2C 80 00 00 00 80 26` last=`14 2C 80 00 00 00 80 26` top=15 2C 80 00 00 00 80 26 x303; 14 2C 80 00 00 00 80 26 x188; 16 2C 80 00 00 00 80 26 x18
- ch1 `0x18F` count=5489 unique=3 first=`FA 5B 00 00 00 48 00 00` last=`FA 59 00 00 00 48 00 00` top=FA 5A 00 00 00 48 00 00 x2870; FA 5B 00 00 00 48 00 00 x1698; FA 59 00 00 00 48 00 00 x921
- ch1 `0x040` count=275 unique=2 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x223; 00 00 12 04 02 00 00 00 x52
- ch1 `0x042` count=70 unique=2 first=`00 FF 00 FF 00 00 00 00` last=`00 FF 00 FF 00 00 00 00` top=00 FF 00 FF 00 00 00 00 x55; 10 FF 10 FF 00 00 00 00 x15
- ch0 `0x131` count=63 unique=2 first=`00 00 FF 00 00 00 FF 00` last=`00 00 FF 00 00 00 FF 00` top=00 00 FF 00 00 00 FF 00 x48; 10 00 FF 00 10 00 FF 00 x15
- ch1 `0x434` count=55 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x46; 01 00 00 00 00 00 00 00 x9
- ch1 `0x5A0` count=52 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x28; 00 00 00 C0 20 27 95 02 x24
- ch1 `0x111` count=5468 unique=4 first=`00 40 61 FF 62 00 00 B4` last=`00 40 61 FF 62 00 00 3C` top=00 40 61 FF 62 00 00 B4 x1380; 00 40 61 FF 62 00 00 3C x1373; 00 40 61 FF 62 00 00 78 x1363
- ch1 `0x112` count=5374 unique=4 first=`FF 50 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x1359; FF F0 00 00 FF 00 00 00 x1356; FF 00 00 00 FF 00 00 00 x1334
- ch1 `0x113` count=5364 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 24 x1354; 00 20 00 80 00 00 00 AC x1351; 00 20 00 80 00 00 00 E8 x1331
- ch1 `0x329` count=5281 unique=4 first=`0F A9 7F 12 11 2B 00 18` last=`80 A9 7F 12 11 2B 00 18` top=0F A9 7F 12 11 2B 00 18 x1329; 80 A9 7F 12 11 2B 00 18 x1329; E2 A9 7F 12 11 2B 00 18 x1317
- ch1 `0x4E6` count=517 unique=3 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7B 00` top=80 80 00 00 80 80 7B 00 x423; 80 80 00 00 80 80 7A 00 x93; 80 80 00 00 80 80 79 00 x1
- ch1 `0x545` count=508 unique=3 first=`DB 17 00 7C 00 00 00 AB` last=`DB 17 00 7C 00 00 00 AB` top=DB 17 00 7C 00 00 00 AB x399; DB 17 00 7B 00 00 00 AB x108; DB 17 00 7A 00 00 00 AB x1
- ch1 `0x492` count=1059 unique=4 first=`00 00 00 00 9E 80 73 F8` last=`00 00 00 00 9E 80 73 F8` top=00 00 00 00 9E 80 73 70 x272; 00 00 00 00 9E 80 73 F8 x264; 00 00 00 00 9E 80 73 34 x263
- ch1 `0x043` count=87 unique=4 first=`00 00 00 04 03 00 00 00` last=`00 00 00 04 03 00 00 00` top=00 00 00 04 03 00 00 00 x54; 00 10 40 02 03 00 00 10 x18; 00 11 40 02 03 00 00 10 x9
- ch0 `0x132` count=82 unique=4 first=`90 00 00 28 00 00 00 00` last=`90 00 00 28 00 00 00 00` top=90 00 00 28 00 00 00 00 x49; 90 01 01 14 00 00 00 01 x18; 90 11 01 14 00 00 00 01 x9
- ch1 `0x58B` count=1095 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1095
- ch1 `0x436` count=1057 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1057
- ch1 `0x490` count=1051 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1051
- ch1 `0x4F4` count=1043 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1043
- ch1 `0x53E` count=534 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x534
- ch1 `0x500` count=531 unique=1 first=`3C` last=`3C` top=3C x531
- ch1 `0x502` count=529 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x529
- ch1 `0x507` count=526 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x526
- ch1 `0x4E5` count=522 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x522
- ch1 `0x520` count=522 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x522
- ch1 `0x4E7` count=516 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x516
- ch1 `0x541` count=516 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x516
- ch1 `0x556` count=508 unique=1 first=`00 76 00 00 00 27 00 00` last=`00 76 00 00 00 27 00 00` top=00 76 00 00 00 27 00 00 x508

### climate_driver_temp_knob_left_right_done
range lines 1810190..1955129; duration 66.4s; previous marker `climate_auto_on_off_2x_done`
- ch1 `0x53E` count=549 unique=2 first=`00 08 00 00 80 05` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x492; 00 08 00 00 80 05 x57
- ch1 `0x040` count=289 unique=2 first=`00 00 22 04 02 00 00 00` last=`00 00 12 04 02 00 00 00` top=00 00 12 04 02 00 00 00 x164; 00 00 22 04 02 00 00 00 x125
- ch1 `0x434` count=59 unique=2 first=`00 00 00 00 00 00 00 00` last=`01 00 00 00 00 00 00 00` top=01 00 00 00 00 00 00 00 x32; 00 00 00 00 00 00 00 00 x27
- ch1 `0x4E6` count=550 unique=3 first=`80 80 00 00 80 80 7B 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x306; 80 80 00 00 80 80 7B 00 x243; 80 80 00 00 80 80 79 00 x1
- ch1 `0x545` count=545 unique=3 first=`DB 17 00 7C 00 00 00 AB` last=`DB 17 00 7B 00 00 00 AB` top=DB 17 00 7B 00 00 00 AB x306; DB 17 00 7C 00 00 00 AB x238; DB 17 00 7A 00 00 00 AB x1
- ch1 `0x5CE` count=542 unique=3 first=`14 2C 80 00 00 00 80 26` last=`12 2C 80 00 00 00 80 26` top=13 2C 80 00 00 00 80 26 x338; 12 2C 80 00 00 00 80 26 x149; 14 2C 80 00 00 00 80 26 x55
- ch1 `0x043` count=158 unique=3 first=`00 00 00 04 03 00 00 00` last=`00 10 40 02 03 00 00 10` top=00 10 40 02 03 00 00 10 x80; 00 11 40 02 03 00 00 10 x51; 00 00 00 04 03 00 00 00 x27
- ch0 `0x132` count=158 unique=3 first=`90 00 00 28 00 00 00 00` last=`90 01 01 14 00 00 00 01` top=90 01 01 14 00 00 00 01 x80; 90 11 01 14 00 00 00 01 x51; 90 00 00 28 00 00 00 00 x27
- ch1 `0x492` count=1103 unique=4 first=`00 00 00 00 9E 80 73 34` last=`00 00 00 00 9E 80 73 70` top=00 00 00 00 9E 80 73 F8 x283; 00 00 00 00 9E 80 73 70 x275; 00 00 00 00 9E 80 73 34 x273
- ch1 `0x042` count=109 unique=4 first=`00 FF 00 FF 00 00 00 00` last=`10 FF 10 FF 00 00 00 00` top=10 FF 10 FF 00 00 00 00 x35; 0F FF 10 FF 00 00 00 00 x29; 00 FF 00 FF 00 00 00 00 x26
- ch0 `0x131` count=107 unique=4 first=`00 00 FF 00 00 00 FF 00` last=`10 00 FF 00 10 00 FF 00` top=10 00 FF 00 10 00 FF 00 x36; 0F 00 FF 00 10 00 FF 00 x29; 00 00 FF 00 00 00 FF 00 x23
- ch1 `0x18F` count=5683 unique=3 first=`FA 59 00 00 00 48 00 00` last=`FA 57 00 00 00 48 00 00` top=FA 59 00 00 00 48 00 00 x2959; FA 57 00 00 00 48 00 00 x1484; FA 58 00 00 00 48 00 00 x1240
- ch1 `0x5A0` count=59 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 05 A5 02 x30; 00 00 00 C0 20 27 95 02 x29
- ch1 `0x111` count=5778 unique=4 first=`00 40 61 FF 62 00 00 78` last=`00 40 61 FF 62 00 00 3C` top=00 40 61 FF 62 00 00 3C x1462; 00 40 61 FF 62 00 00 B4 x1453; 00 40 61 FF 62 00 00 F0 x1434
- ch1 `0x112` count=5677 unique=4 first=`FF A0 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF F0 00 00 FF 00 00 00 x1432; FF 50 00 00 FF 00 00 00 x1419; FF 00 00 00 FF 00 00 00 x1417
- ch1 `0x113` count=5650 unique=4 first=`00 20 00 80 00 00 00 E8` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 AC x1429; 00 20 00 80 00 00 00 60 x1413; 00 20 00 80 00 00 00 24 x1408
- ch1 `0x58B` count=1136 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1136
- ch1 `0x490` count=1120 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1120
- ch1 `0x436` count=1119 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1119
- ch1 `0x4F4` count=1118 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1118
- ch1 `0x500` count=570 unique=1 first=`3C` last=`3C` top=3C x570
- ch1 `0x507` count=563 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x563
- ch1 `0x502` count=557 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x557
- ch1 `0x541` count=553 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x553
- ch1 `0x520` count=551 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x551
- ch1 `0x4E5` count=548 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x548
- ch1 `0x4E7` count=546 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x546
- ch1 `0x549` count=545 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x545
- ch1 `0x5CF` count=544 unique=1 first=`3F 09 00 00 00 7F 80 00` last=`3F 09 00 00 00 7F 80 00` top=3F 09 00 00 00 7F 80 00 x544
- ch1 `0x547` count=543 unique=1 first=`00 00 7F 00 AD 47 CD 18` last=`00 00 7F 00 AD 47 CD 18` top=00 00 7F 00 AD 47 CD 18 x543

### climate_passenger_temp_left_right_done
range lines 1955129..2028592; duration 40.3s; previous marker `climate_driver_temp_knob_left_right_done`
- ch1 `0x5CE` count=278 unique=2 first=`12 2C 80 00 00 00 80 26` last=`11 2C 80 00 00 00 80 26` top=11 2C 80 00 00 00 80 26 x204; 12 2C 80 00 00 00 80 26 x74
- ch1 `0x492` count=541 unique=4 first=`00 00 00 00 9E 80 73 34` last=`00 00 00 00 9E 80 73 BC` top=00 00 00 00 9E 80 73 F8 x146; 00 00 00 00 9E 80 73 BC x135; 00 00 00 00 9E 80 73 34 x132
- ch1 `0x18F` count=2971 unique=2 first=`FA 57 00 00 00 48 00 00` last=`FA 56 00 00 00 48 00 00` top=FA 57 00 00 00 48 00 00 x2766; FA 56 00 00 00 48 00 00 x205
- ch0 `0x132` count=102 unique=2 first=`90 01 01 14 00 00 00 01` last=`90 01 01 14 00 00 00 01` top=90 01 01 14 00 00 00 01 x68; 90 11 01 14 00 00 00 01 x34
- ch1 `0x043` count=101 unique=2 first=`00 10 40 02 03 00 00 10` last=`00 10 40 02 03 00 00 10` top=00 10 40 02 03 00 00 10 x65; 00 11 40 02 03 00 00 10 x36
- ch1 `0x5A0` count=27 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 27 95 02 x14; 00 00 00 C0 20 05 A5 02 x13
- ch1 `0x111` count=2913 unique=4 first=`00 40 61 FF 62 00 00 F0` last=`00 40 61 FF 62 00 00 B4` top=00 40 61 FF 62 00 00 3C x757; 00 40 61 FF 62 00 00 B4 x749; 00 40 61 FF 62 00 00 78 x705
- ch0 `0x131` count=68 unique=3 first=`10 00 FF 00 10 00 FF 00` last=`10 00 FF 00 10 00 FF 00` top=10 00 FF 00 10 00 FF 00 x34; 10 00 FF 00 0F 00 FF 00 x21; 10 00 FF 00 0E 00 FF 00 x13
- ch1 `0x042` count=65 unique=3 first=`10 FF 10 FF 00 00 00 00` last=`10 FF 10 FF 00 00 00 00` top=10 FF 10 FF 00 00 00 00 x31; 10 FF 0F FF 00 00 00 00 x21; 10 FF 0E FF 00 00 00 00 x13
- ch1 `0x428` count=1425 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1425
- ch1 `0x387` count=1418 unique=1 first=`09 0E 00 00 00 17` last=`09 0E 00 00 00 17` top=09 0E 00 00 00 17 x1418
- ch1 `0x429` count=1400 unique=1 first=`B5 03 00 00 00 00 20 03` last=`B5 03 00 00 00 00 20 03` top=B5 03 00 00 00 00 20 03 x1400
- ch1 `0x4F4` count=570 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x570
- ch1 `0x58B` count=567 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x567
- ch1 `0x436` count=560 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x560
- ch1 `0x490` count=560 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x560
- ch1 `0x520` count=285 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x285
- ch1 `0x541` count=282 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x282
- ch1 `0x547` count=280 unique=1 first=`00 00 7F 00 AD 47 CD 18` last=`00 00 7F 00 AD 47 CD 18` top=00 00 7F 00 AD 47 CD 18 x280
- ch1 `0x549` count=279 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x279
- ch1 `0x556` count=279 unique=1 first=`00 76 00 00 00 27 00 00` last=`00 76 00 00 00 27 00 00` top=00 76 00 00 00 27 00 00 x279
- ch1 `0x500` count=278 unique=1 first=`3C` last=`3C` top=3C x278
- ch1 `0x545` count=278 unique=1 first=`DB 17 00 7B 00 00 00 AB` last=`DB 17 00 7B 00 00 00 AB` top=DB 17 00 7B 00 00 00 AB x278
- ch1 `0x5CF` count=278 unique=1 first=`3F 09 00 00 00 7F 80 00` last=`3F 09 00 00 00 7F 80 00` top=3F 09 00 00 00 7F 80 00 x278
- ch1 `0x4E6` count=277 unique=1 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x277
- ch1 `0x4E7` count=275 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x275
- ch1 `0x4E5` count=272 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x272
- ch1 `0x53E` count=272 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x272
- ch1 `0x502` count=269 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x269
- ch1 `0x507` count=269 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x269

### climate_fan_speed_up3_down3_done
range lines 2028592..2112132; duration 45.2s; previous marker `climate_passenger_temp_left_right_done`
- ch1 `0x556` count=310 unique=2 first=`00 76 00 00 00 27 00 00` last=`00 75 00 00 00 27 00 00` top=00 76 00 00 00 27 00 00 x265; 00 75 00 00 00 27 00 00 x45
- ch1 `0x5CE` count=307 unique=3 first=`11 2C 80 00 00 00 80 26` last=`0F 2C 80 00 00 00 80 26` top=10 2C 80 00 00 00 80 26 x247; 0F 2C 80 00 00 00 80 26 x41; 11 2C 80 00 00 00 80 26 x19
- ch1 `0x18F` count=3454 unique=2 first=`FA 56 00 00 00 48 00 00` last=`FA 56 00 00 00 49 00 00` top=FA 56 00 00 00 48 00 00 x2867; FA 56 00 00 00 49 00 00 x587
- ch1 `0x434` count=27 unique=5 first=`01 00 00 00 00 00 00 00` last=`03 00 00 00 00 00 00 00` top=03 00 00 00 00 00 00 00 x11; 01 00 00 00 00 00 00 00 x7; 05 00 00 00 00 00 00 00 x4
- ch1 `0x545` count=313 unique=2 first=`DB 17 00 7B 00 00 00 AB` last=`DB 17 00 7B 00 00 00 AB` top=DB 17 00 7B 00 00 00 AB x255; DB 17 00 7A 00 00 00 AB x58
- ch1 `0x4E6` count=312 unique=2 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x255; 80 80 00 00 80 80 79 00 x57
- ch1 `0x5A0` count=31 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x17; 00 00 00 C0 20 05 A5 02 x14
- ch1 `0x111` count=3242 unique=4 first=`00 40 61 FF 62 00 00 78` last=`00 40 61 FF 62 00 00 F0` top=00 40 61 FF 62 00 00 3C x829; 00 40 61 FF 62 00 00 B4 x829; 00 40 61 FF 62 00 00 78 x793
- ch1 `0x113` count=3187 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 60` top=00 20 00 80 00 00 00 24 x812; 00 20 00 80 00 00 00 AC x812; 00 20 00 80 00 00 00 E8 x783
- ch1 `0x112` count=3171 unique=4 first=`FF 50 00 00 FF 00 00 00` last=`FF 00 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x808; FF F0 00 00 FF 00 00 00 x806; FF A0 00 00 FF 00 00 00 x780
- ch1 `0x492` count=637 unique=8 first=`00 00 00 00 9E 80 73 34` last=`00 00 00 00 9D 80 73 80` top=00 00 00 00 9E 80 73 70 x143; 00 00 00 00 9E 80 73 34 x140; 00 00 00 00 9E 80 73 BC x137
- ch1 `0x387` count=1638 unique=1 first=`09 0E 00 00 00 17` last=`09 0E 00 00 00 17` top=09 0E 00 00 00 17 x1638
- ch1 `0x428` count=1626 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1626
- ch1 `0x429` count=1602 unique=1 first=`B5 03 00 00 00 00 20 03` last=`B5 03 00 00 00 00 20 03` top=B5 03 00 00 00 00 20 03 x1602
- ch1 `0x436` count=659 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x659
- ch1 `0x58B` count=659 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x659
- ch1 `0x4F4` count=649 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x649
- ch1 `0x490` count=645 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x645
- ch1 `0x502` count=329 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x329
- ch1 `0x520` count=325 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x325
- ch1 `0x500` count=320 unique=1 first=`3C` last=`3C` top=3C x320
- ch1 `0x4E7` count=315 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x315
- ch1 `0x53E` count=315 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x315
- ch1 `0x4E5` count=313 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x313
- ch1 `0x547` count=313 unique=1 first=`00 00 7F 00 AD 47 CD 18` last=`00 00 7F 00 AD 47 CD 18` top=00 00 7F 00 AD 47 CD 18 x313
- ch1 `0x549` count=313 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x313
- ch1 `0x507` count=312 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x312
- ch1 `0x541` count=311 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x311
- ch1 `0x5CF` count=307 unique=1 first=`3F 09 00 00 00 7F 80 00` last=`3F 09 00 00 00 7F 80 00` top=3F 09 00 00 00 7F 80 00 x307
- ch0 `0x44B` count=190 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x190

### climate_air_direction_face3_defrost4_feet5_toggle_done
range lines 2112132..2376393; duration 112.4s; previous marker `climate_fan_speed_up3_down3_done`
- ch1 `0x5CF` count=1021 unique=2 first=`3F 09 00 00 00 7F 80 00` last=`3F 0A 00 00 00 7F 80 00` top=3F 0A 00 00 00 7F 80 00 x571; 3F 09 00 00 00 7F 80 00 x450
- ch1 `0x556` count=1011 unique=2 first=`00 75 00 00 00 27 00 00` last=`00 74 00 00 00 27 00 00` top=00 75 00 00 00 27 00 00 x869; 00 74 00 00 00 27 00 00 x142
- ch1 `0x5CE` count=1008 unique=4 first=`0F 2C 80 00 00 00 80 26` last=`0C 2C 80 00 00 00 80 26` top=0E 2C 80 00 00 00 80 26 x337; 0D 2C 80 00 00 00 80 26 x316; 0F 2C 80 00 00 00 80 26 x190
- ch1 `0x18F` count=10515 unique=3 first=`FA 56 00 00 00 49 00 00` last=`FA 53 00 00 00 49 00 00` top=FA 54 00 00 00 49 00 00 x4627; FA 53 00 00 00 49 00 00 x4045; FA 56 00 00 00 49 00 00 x1843
- ch1 `0x5A0` count=101 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 05 A5 02 x51; 00 00 00 C0 20 27 95 02 x50
- ch1 `0x112` count=10266 unique=4 first=`FF 50 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x2589; FF F0 00 00 FF 00 00 00 x2573; FF 00 00 00 FF 00 00 00 x2561
- ch1 `0x113` count=10258 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 24 x2586; 00 20 00 80 00 00 00 AC x2567; 00 20 00 80 00 00 00 60 x2558
- ch1 `0x4E7` count=1030 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x1030
- ch1 `0x500` count=1028 unique=1 first=`3C` last=`3C` top=3C x1028
- ch1 `0x545` count=1028 unique=1 first=`DB 17 00 7B 00 00 00 AB` last=`DB 17 00 7B 00 00 00 AB` top=DB 17 00 7B 00 00 00 AB x1028
- ch1 `0x4E5` count=1027 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x1027
- ch1 `0x507` count=1027 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x1027
- ch1 `0x4E6` count=1026 unique=1 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x1026
- ch1 `0x547` count=1026 unique=1 first=`00 00 7F 00 AD 47 CD 18` last=`00 00 7F 00 AD 47 CD 18` top=00 00 7F 00 AD 47 CD 18 x1026
- ch1 `0x520` count=1023 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x1023
- ch1 `0x53E` count=1022 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x1022
- ch1 `0x549` count=1019 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x1019
- ch1 `0x541` count=1018 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x1018
- ch1 `0x502` count=1009 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1009
- ch0 `0x44B` count=581 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x581
- ch0 `0x44D` count=565 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x565
- ch1 `0x040` count=524 unique=1 first=`00 00 12 04 02 00 00 00` last=`00 00 12 04 02 00 00 00` top=00 00 12 04 02 00 00 00 x524
- ch0 `0x16A` count=520 unique=1 first=`01 00 00 00 00 00 00 C3` last=`01 00 00 00 00 00 00 C3` top=01 00 00 00 00 00 00 C3 x520
- ch1 `0x52A` count=520 unique=1 first=`00 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x520
- ch0 `0x15D` count=519 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x519
- ch0 `0x135` count=518 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x518
- ch0 `0x157` count=518 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x518
- ch0 `0x158` count=518 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x518
- ch0 `0x1EB` count=517 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x517
- ch1 `0x544` count=517 unique=1 first=`FF 37 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x517

### correction_air_direction_face4_defrost3_feet5_previous_block
range lines 2376393..2517639; duration 67.7s; previous marker `climate_air_direction_face3_defrost4_feet5_toggle_done`
- ch1 `0x547` count=538 unique=2 first=`00 00 7F 00 AD 47 CD 18` last=`00 00 7E 00 AD 47 CD 18` top=00 00 7E 00 AD 47 CD 18 x380; 00 00 7F 00 AD 47 CD 18 x158
- ch1 `0x492` count=1097 unique=4 first=`00 00 00 00 9A 80 73 FC` last=`00 00 00 00 9A 80 73 74` top=00 00 00 00 9A 80 73 FC x279; 00 00 00 00 9A 80 73 B0 x278; 00 00 00 00 9A 80 73 38 x271
- ch1 `0x5CE` count=533 unique=4 first=`0C 2C 80 00 00 00 80 26` last=`09 2C 80 00 00 00 80 26` top=0B 2C 80 00 00 00 80 26 x287; 0A 2C 80 00 00 00 80 26 x206; 0C 2C 80 00 00 00 80 26 x34
- ch1 `0x043` count=122 unique=4 first=`00 00 40 02 03 00 00 30` last=`00 00 00 02 03 00 00 30` top=00 00 40 02 03 00 00 30 x49; 00 00 00 02 03 00 00 30 x40; 00 01 00 02 03 00 00 30 x18
- ch0 `0x132` count=115 unique=4 first=`90 00 01 14 00 00 00 03` last=`90 00 00 14 00 00 00 03` top=90 00 01 14 00 00 00 03 x44; 90 00 00 14 00 00 00 03 x38; 90 10 00 14 00 00 00 03 x18
- ch1 `0x18F` count=5760 unique=2 first=`FA 53 00 00 00 49 00 00` last=`FA 52 00 00 00 49 00 00` top=FA 52 00 00 00 49 00 00 x3017; FA 53 00 00 00 49 00 00 x2743
- ch1 `0x5A0` count=50 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x25; 00 00 00 C0 20 27 95 02 x25
- ch1 `0x329` count=5472 unique=4 first=`80 A6 7F 12 11 2B 00 18` last=`E2 A6 7F 12 11 2B 00 18` top=80 A6 7F 12 11 2B 00 18 x1411; 0F A6 7F 12 11 2B 00 18 x1399; E2 A6 7F 12 11 2B 00 18 x1343
- ch1 `0x111` count=5469 unique=4 first=`00 40 61 FF 61 00 00 00` last=`00 40 61 FF 61 00 00 C4` top=00 40 61 FF 61 00 00 C4 x1376; 00 40 61 FF 61 00 00 88 x1366; 00 40 61 FF 61 00 00 00 x1364
- ch1 `0x112` count=5465 unique=4 first=`FF 00 00 00 FF 00 00 00` last=`FF A0 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x1371; FF A0 00 00 FF 00 00 00 x1367; FF F0 00 00 FF 00 00 00 x1364
- ch1 `0x58B` count=1121 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1121
- ch1 `0x4F4` count=1095 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1095
- ch1 `0x436` count=1092 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1092
- ch1 `0x490` count=1080 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1080
- ch1 `0x4E6` count=554 unique=1 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x554
- ch1 `0x520` count=551 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x551
- ch1 `0x500` count=548 unique=1 first=`3C` last=`3C` top=3C x548
- ch1 `0x4E5` count=547 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x547
- ch1 `0x4E7` count=543 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x543
- ch1 `0x545` count=542 unique=1 first=`DB 17 00 7B 00 00 00 AB` last=`DB 17 00 7B 00 00 00 AB` top=DB 17 00 7B 00 00 00 AB x542
- ch1 `0x541` count=541 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x541
- ch1 `0x507` count=536 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x536
- ch1 `0x53E` count=536 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x536
- ch1 `0x549` count=536 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x536
- ch1 `0x556` count=530 unique=1 first=`00 74 00 00 00 27 00 00` last=`00 74 00 00 00 27 00 00` top=00 74 00 00 00 27 00 00 x530
- ch1 `0x502` count=528 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x528
- ch1 `0x5CF` count=526 unique=1 first=`3F 0A 00 00 00 7F 80 00` last=`3F 0A 00 00 00 7F 80 00` top=3F 0A 00 00 00 7F 80 00 x526
- ch0 `0x44B` count=309 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x309
- ch0 `0x44D` count=309 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x309
- ch0 `0x56E` count=280 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x280

### climate_ac_on_off_5x_done
range lines 2517639..2517648; duration 0.0s; previous marker `correction_air_direction_face4_defrost3_feet5_previous_block`
- no candidate frame changes after filtering

### climate_recirc_on_off_5x_done
range lines 2517648..2666971; duration 78.2s; previous marker `climate_ac_on_off_5x_done`
- ch1 `0x556` count=559 unique=2 first=`00 74 00 00 00 27 00 00` last=`00 73 00 00 00 27 00 00` top=00 73 00 00 00 27 00 00 x533; 00 74 00 00 00 27 00 00 x26
- ch1 `0x5CE` count=555 unique=3 first=`09 2C 80 00 00 00 80 26` last=`07 2C 80 00 00 00 80 26` top=08 2C 80 00 00 00 80 26 x264; 09 2C 80 00 00 00 80 26 x163; 07 2C 80 00 00 00 80 26 x128
- ch1 `0x18F` count=5950 unique=3 first=`FA 52 00 00 00 49 00 00` last=`FA 51 00 00 00 4A 00 00` top=FA 51 00 00 00 4A 00 00 x4386; FA 52 00 00 00 4A 00 00 x1217; FA 52 00 00 00 49 00 00 x347
- ch1 `0x043` count=89 unique=2 first=`00 00 00 02 03 00 00 30` last=`00 00 00 02 03 00 00 30` top=00 00 00 02 03 00 00 30 x63; 00 04 00 02 03 00 00 30 x26
- ch0 `0x132` count=81 unique=2 first=`90 00 00 14 00 00 00 03` last=`90 00 00 14 00 00 00 03` top=90 00 00 14 00 00 00 03 x55; 90 04 00 14 00 00 00 03 x26
- ch1 `0x5A0` count=61 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x31; 00 00 00 C0 20 05 A5 02 x30
- ch1 `0x111` count=5776 unique=4 first=`00 40 61 FF 61 00 00 00` last=`00 40 61 FF 61 00 00 C4` top=00 40 61 FF 61 00 00 C4 x1463; 00 40 61 FF 61 00 00 4C x1444; 00 40 61 FF 61 00 00 00 x1438
- ch1 `0x113` count=5760 unique=4 first=`00 20 00 80 00 00 00 60` last=`00 20 00 80 00 00 00 24` top=00 20 00 80 00 00 00 60 x1452; 00 20 00 80 00 00 00 AC x1448; 00 20 00 80 00 00 00 24 x1433
- ch1 `0x112` count=5759 unique=4 first=`FF 00 00 00 FF 00 00 00` last=`FF 50 00 00 FF 00 00 00` top=FF 00 00 00 FF 00 00 00 x1453; FF F0 00 00 FF 00 00 00 x1439; FF 50 00 00 FF 00 00 00 x1434
- ch1 `0x492` count=1158 unique=8 first=`00 00 00 00 9A 80 73 B0` last=`00 00 00 00 99 80 73 C0` top=00 00 00 00 99 80 73 48 x277; 00 00 00 00 99 80 73 C0 x276; 00 00 00 00 99 80 73 0C x272
- ch1 `0x58B` count=1227 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1227
- ch1 `0x4F4` count=1154 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1154
- ch1 `0x490` count=1146 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1146
- ch1 `0x436` count=1135 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1135
- ch1 `0x53E` count=586 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x586
- ch1 `0x4E7` count=580 unique=1 first=`00 80 00 08 00 81 00 00` last=`00 80 00 08 00 81 00 00` top=00 80 00 08 00 81 00 00 x580
- ch1 `0x545` count=578 unique=1 first=`DB 17 00 7B 00 00 00 AB` last=`DB 17 00 7B 00 00 00 AB` top=DB 17 00 7B 00 00 00 AB x578
- ch1 `0x4E5` count=573 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x573
- ch1 `0x520` count=573 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x573
- ch1 `0x4E6` count=572 unique=1 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x572
- ch1 `0x547` count=569 unique=1 first=`00 00 7E 00 AD 47 CD 18` last=`00 00 7E 00 AD 47 CD 18` top=00 00 7E 00 AD 47 CD 18 x569
- ch1 `0x507` count=568 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x568
- ch1 `0x500` count=567 unique=1 first=`3C` last=`3C` top=3C x567
- ch1 `0x541` count=567 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x567
- ch1 `0x549` count=562 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x562
- ch1 `0x502` count=556 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x556
- ch1 `0x5CF` count=554 unique=1 first=`3F 0A 00 00 00 7F 80 00` last=`3F 0A 00 00 00 7F 80 00` top=3F 0A 00 00 00 7F 80 00 x554
- ch0 `0x44B` count=322 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x322
- ch0 `0x44D` count=314 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x314
- ch0 `0x531` count=309 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x309

### engine_started_rear_defrost_on_off_4x_done
range lines 2666971..2852224; duration 86.7s; previous marker `climate_recirc_on_off_5x_done`
- ch0 `0x158` count=387 unique=2 first=`FF C0 00 00 00 00 00 00` last=`FF C4 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x205; FF C4 00 00 00 00 00 00 x182
- ch1 `0x5A0` count=73 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 27 95 02 x39; 00 00 00 C0 20 05 A5 02 x34
- ch0 `0x169` count=383 unique=3 first=`20 01 00 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 00 FF 14 00 40 00 x207; 20 01 03 FF 14 00 40 00 x171; 20 01 01 FF 14 00 40 00 x5
- ch1 `0x428` count=3660 unique=2 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 02 00` top=05 00 00 80 60 08 00 00 x2072; 05 00 00 80 60 08 02 00 x1588
- ch1 `0x429` count=3590 unique=2 first=`B5 03 00 00 00 00 20 03` last=`B5 03 00 00 00 00 2F 03` top=B5 03 00 00 00 00 20 03 x2046; B5 03 00 00 00 00 2F 03 x1544
- ch1 `0x547` count=711 unique=5 first=`00 00 7E 00 AD 47 CD 18` last=`00 00 7F 3C AE 47 CD 18` top=00 00 7E 00 AD 47 CD 18 x403; 00 00 7F 38 AE 47 CD 18 x113; 00 00 7E 38 AE 47 CD 18 x111
- ch0 `0x44B` count=375 unique=2 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x372; 4D 12 00 00 00 00 FF FF x3
- ch1 `0x040` count=373 unique=2 first=`00 00 12 04 02 00 00 00` last=`00 00 12 04 02 00 00 00` top=00 00 12 04 02 00 00 00 x365; 00 00 02 0C 02 00 00 00 x8
- ch1 `0x553` count=360 unique=2 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x354; 00 00 00 08 01 01 C0 00 x6
- ch1 `0x042` count=74 unique=2 first=`10 FF 10 FF 00 00 00 00` last=`10 FF 10 FF 00 00 00 00` top=10 FF 10 FF 00 00 00 00 x70; 00 FF 00 FF 00 00 00 00 x4
- ch0 `0x134` count=71 unique=2 first=`03 00 00 02 00 00 10 00` last=`03 00 00 02 00 00 10 00` top=03 00 00 02 00 00 10 00 x67; 00 00 00 02 00 00 30 00 x4
- ch0 `0x131` count=68 unique=2 first=`10 00 FF 00 10 00 FF 00` last=`10 00 FF 00 10 00 FF 00` top=10 00 FF 00 10 00 FF 00 x64; 00 00 FF 00 00 00 FF 00 x4
- ch1 `0x113` count=7234 unique=4 first=`00 20 00 80 00 00 00 60` last=`00 20 00 80 00 00 00 24` top=00 20 00 80 00 00 00 AC x1826; 00 20 00 80 00 00 00 24 x1817; 00 20 00 80 00 00 00 60 x1800
- ch1 `0x043` count=77 unique=3 first=`00 00 00 02 03 00 00 30` last=`00 00 00 02 03 00 00 30` top=00 00 00 02 03 00 00 30 x71; 00 00 00 0E 00 00 00 00 x4; 00 00 00 02 03 00 00 20 x2
- ch0 `0x132` count=72 unique=3 first=`90 00 00 14 00 00 00 03` last=`90 00 00 14 00 00 00 03` top=90 00 00 14 00 00 00 03 x67; 90 00 00 70 00 00 00 00 x4; 90 00 00 14 00 00 00 02 x1
- ch1 `0x434` count=72 unique=3 first=`03 00 00 00 00 00 00 00` last=`03 00 00 00 00 00 00 00` top=03 00 00 00 00 00 00 00 x69; 02 00 00 00 00 00 00 00 x2; 00 00 00 00 00 00 00 00 x1
- ch1 `0x541` count=750 unique=5 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x656; 03 00 41 00 D0 00 00 01 x83; 04 00 41 00 C0 10 00 01 x7
- ch1 `0x58B` count=1466 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1466
- ch1 `0x490` count=1442 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1442
- ch1 `0x436` count=1438 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1438
- ch1 `0x4F4` count=1392 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1392
- ch1 `0x507` count=719 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x719
- ch1 `0x520` count=719 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x719
- ch1 `0x549` count=711 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x711
- ch1 `0x500` count=709 unique=1 first=`3C` last=`3C` top=3C x709
- ch1 `0x53E` count=706 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x706
- ch1 `0x502` count=703 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x703
- ch0 `0x531` count=388 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x388
- ch0 `0x56E` count=384 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x384
- ch0 `0x1DF` count=381 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x381

### heated_steering_wheel_on_off_4x_done
range lines 2852224..2965790; duration 56.8s; previous marker `engine_started_rear_defrost_on_off_4x_done`
- ch1 `0x547` count=432 unique=3 first=`00 00 7F 3C AE 47 CD 18` last=`00 00 80 3C AE 47 CD 18` top=00 00 80 3C AE 47 CD 18 x398; 00 00 7F 3C AE 47 CD 18 x32; 00 00 81 3C AE 47 CD 18 x2
- ch1 `0x492` count=874 unique=4 first=`00 00 00 00 9D 80 73 08` last=`00 00 00 00 9D 80 73 44` top=00 00 00 00 9D 80 73 08 x221; 00 00 00 00 9D 80 73 CC x218; 00 00 00 00 9D 80 73 80 x218
- ch1 `0x4E6` count=429 unique=6 first=`49 C2 00 00 84 84 8B 00` last=`49 C4 00 00 84 84 8B 00` top=49 C3 00 00 84 84 8B 00 x243; 49 C2 00 00 84 84 8B 00 x120; 49 C3 00 00 84 84 8C 00 x30
- ch1 `0x559` count=236 unique=2 first=`00 40 00 00 20 00 00 40` last=`00 40 00 00 20 00 00 40` top=00 40 00 00 20 00 00 40 x182; 10 40 00 00 20 00 00 40 x54
- ch1 `0x5A0` count=46 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x25; 00 00 00 C0 20 27 95 02 x21
- ch1 `0x4E5` count=429 unique=7 first=`80 52 00 02 49 C2 00 00` last=`80 4A 00 02 49 C4 00 00` top=80 4E 00 02 49 C3 00 00 x215; 80 4E 00 02 49 C2 00 00 x71; 80 52 00 02 49 C2 00 00 x56
- ch1 `0x18F` count=4546 unique=5 first=`FA 4F 00 00 00 4B 00 20` last=`FA 4E 00 00 00 48 00 20` top=FA 4E 00 00 00 49 00 20 x1985; FA 4E 00 00 00 4A 00 20 x1179; FA 4E 00 00 00 48 00 20 x1096
- ch1 `0x436` count=900 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x900
- ch1 `0x490` count=885 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x885
- ch1 `0x58B` count=874 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x874
- ch1 `0x4F4` count=851 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x851
- ch1 `0x520` count=447 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x447
- ch1 `0x541` count=442 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x442
- ch1 `0x507` count=438 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x438
- ch1 `0x500` count=433 unique=1 first=`3C` last=`3C` top=3C x433
- ch1 `0x502` count=431 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x431
- ch1 `0x53E` count=428 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x428
- ch1 `0x549` count=427 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x427
- ch0 `0x1EB` count=243 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x243
- ch0 `0x44B` count=242 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x242
- ch0 `0x44D` count=242 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x242
- ch0 `0x169` count=235 unique=1 first=`20 01 03 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x235
- ch0 `0x158` count=230 unique=1 first=`FF C4 00 00 00 00 00 00` last=`FF C4 00 00 00 00 00 00` top=FF C4 00 00 00 00 00 00 x230
- ch1 `0x410` count=229 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x229
- ch0 `0x531` count=229 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x229
- ch0 `0x157` count=228 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x228
- ch0 `0x15D` count=228 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x228
- ch0 `0x1DF` count=227 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x227
- ch0 `0x56E` count=227 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x227
- ch1 `0x553` count=219 unique=1 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x219

### driver_seat_heater_levels_1_2_3_off_3cycles_done
range lines 2965790..3086423; duration 65.2s; previous marker `heated_steering_wheel_on_off_4x_done`
- ch1 `0x547` count=468 unique=2 first=`00 00 80 3C AE 47 CD 18` last=`00 00 81 3C AE 47 CD 18` top=00 00 81 3C AE 47 CD 18 x464; 00 00 80 3C AE 47 CD 18 x4
- ch1 `0x5A0` count=47 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 27 95 02 x24; 00 00 00 C0 20 05 A5 02 x23
- ch1 `0x4E6` count=472 unique=5 first=`49 C4 00 00 84 84 8B 00` last=`49 C4 00 00 84 84 8C 00` top=49 C4 00 00 84 84 8B 00 x334; 49 C4 00 00 84 84 8C 00 x128; 49 C4 00 00 83 83 8B 00 x7
- ch1 `0x113` count=4781 unique=4 first=`00 20 00 80 00 00 00 60` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 E8 x1205; 00 20 00 80 00 00 00 AC x1194; 00 20 00 80 00 00 00 60 x1191
- ch1 `0x18F` count=4737 unique=4 first=`FA 4E 00 00 00 48 00 20` last=`FA 4E 00 00 00 46 00 20` top=FA 4E 00 00 00 47 00 20 x2164; FA 4E 00 00 00 46 00 20 x2049; FA 4F 00 00 00 46 00 20 x296
- ch1 `0x436` count=924 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x924
- ch1 `0x4F4` count=920 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x920
- ch1 `0x490` count=905 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x905
- ch1 `0x58B` count=905 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x905
- ch1 `0x500` count=469 unique=1 first=`3C` last=`3C` top=3C x469
- ch1 `0x549` count=468 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x468
- ch1 `0x541` count=465 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x465
- ch1 `0x520` count=463 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x463
- ch1 `0x53E` count=457 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x457
- ch1 `0x502` count=456 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x456
- ch1 `0x507` count=427 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x427
- ch0 `0x44B` count=283 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x283
- ch0 `0x44D` count=260 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x260
- ch0 `0x1DF` count=248 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x248
- ch1 `0x040` count=243 unique=1 first=`00 00 12 04 02 00 00 00` last=`00 00 12 04 02 00 00 00` top=00 00 12 04 02 00 00 00 x243
- ch0 `0x169` count=243 unique=1 first=`20 01 03 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x243
- ch0 `0x158` count=241 unique=1 first=`FF C4 00 00 00 00 00 00` last=`FF C4 00 00 00 00 00 00` top=FF C4 00 00 00 00 00 00 x241
- ch0 `0x157` count=240 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x240
- ch0 `0x531` count=238 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x238
- ch0 `0x1EB` count=233 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x233
- ch0 `0x56E` count=233 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x233
- ch0 `0x16A` count=231 unique=1 first=`01 00 00 00 00 00 00 C3` last=`01 00 00 00 00 00 00 C3` top=01 00 00 00 00 00 00 C3 x231
- ch1 `0x410` count=230 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x230
- ch1 `0x553` count=230 unique=1 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x230
- ch1 `0x50A` count=229 unique=1 first=`0B 00 00 00 00 00 00 00` last=`0B 00 00 00 00 00 00 00` top=0B 00 00 00 00 00 00 00 x229

### passenger_seat_heater_levels_1_2_3_off_3cycles_done
range lines 3086423..3218533; duration 70.4s; previous marker `driver_seat_heater_levels_1_2_3_off_3cycles_done`
- ch1 `0x547` count=505 unique=2 first=`00 00 81 3C AE 47 CD 18` last=`00 00 82 3C AE 47 CD 18` top=00 00 82 3C AE 47 CD 18 x295; 00 00 81 3C AE 47 CD 18 x210
- ch1 `0x492` count=1011 unique=4 first=`00 00 00 00 A0 80 73 04` last=`00 00 00 00 A0 80 73 8C` top=00 00 00 00 A0 80 73 8C x266; 00 00 00 00 A0 80 73 40 x252; 00 00 00 00 A0 80 73 C8 x249
- ch1 `0x18F` count=5185 unique=3 first=`FA 4E 00 00 00 46 00 20` last=`FA 50 00 00 00 46 00 20` top=FA 4F 00 00 00 46 00 20 x3196; FA 50 00 00 00 46 00 20 x1636; FA 4E 00 00 00 46 00 20 x353
- ch1 `0x5A0` count=51 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x28; 00 00 00 C0 20 27 95 02 x23
- ch1 `0x4E5` count=502 unique=7 first=`80 46 00 01 49 C4 00 00` last=`80 4A 00 02 49 C4 00 00` top=80 4A 00 02 49 C4 00 00 x289; 80 4E 00 02 49 C4 00 00 x143; 80 4A 00 01 49 C4 00 00 x47
- ch1 `0x436` count=1025 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1025
- ch1 `0x4F4` count=1024 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1024
- ch1 `0x58B` count=1014 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1014
- ch1 `0x490` count=1008 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1008
- ch1 `0x500` count=519 unique=1 first=`3C` last=`3C` top=3C x519
- ch1 `0x549` count=514 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x514
- ch1 `0x53E` count=509 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x509
- ch1 `0x507` count=505 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x505
- ch1 `0x541` count=501 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x501
- ch1 `0x520` count=498 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x498
- ch1 `0x502` count=484 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x484
- ch0 `0x44D` count=297 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x297
- ch0 `0x44B` count=275 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x275
- ch0 `0x15D` count=274 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x274
- ch0 `0x135` count=273 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x273
- ch0 `0x157` count=267 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x267
- ch0 `0x169` count=266 unique=1 first=`20 01 03 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x266
- ch0 `0x1DF` count=265 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x265
- ch1 `0x52A` count=264 unique=1 first=`00 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x264
- ch0 `0x1EB` count=262 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x262
- ch0 `0x158` count=258 unique=1 first=`FF C4 00 00 00 00 00 00` last=`FF C4 00 00 00 00 00 00` top=FF C4 00 00 00 00 00 00 x258
- ch0 `0x16A` count=258 unique=1 first=`01 00 00 00 00 00 00 C3` last=`01 00 00 00 00 00 00 C3` top=01 00 00 00 00 00 00 C3 x258
- ch1 `0x410` count=258 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x258
- ch1 `0x544` count=258 unique=1 first=`FF 37 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x258
- ch0 `0x133` count=257 unique=1 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 82` top=00 00 00 00 00 00 00 82 x257

### ignition_on_engine_off_P_to_R_4x_brake_pressed_done
range lines 3218533..3437993; duration 100.3s; previous marker `passenger_seat_heater_levels_1_2_3_off_3cycles_done`
- ch1 `0x53E` count=830 unique=2 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x782; 00 08 00 00 80 0E x48
- ch0 `0x158` count=429 unique=2 first=`FF C4 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x403; FF C4 00 00 00 00 00 00 x26
- ch1 `0x7E0` count=2 unique=2 first=`03 22 F1 90 AA AA AA AA` last=`30 08 08 AA AA AA AA AA` top=03 22 F1 90 AA AA AA AA x1; 30 08 08 AA AA AA AA AA x1
- ch1 `0x7E8` count=3 unique=3 first=`10 14 62 F1 90 55 35 59` last=`22 4C 30 38 33 37 36 34` top=10 14 62 F1 90 55 35 59 x1; 21 50 48 38 31 43 44 4D x1; 22 4C 30 38 33 37 36 34 x1
- ch1 `0x547` count=858 unique=4 first=`00 00 82 3C AE 47 CD 18` last=`00 00 80 3C AE 47 CD 18` top=00 00 81 3C AE 47 CD 18 x546; 00 00 82 3C AE 47 CD 18 x225; 00 00 80 3C AE 47 CD 18 x78
- ch1 `0x429` count=4232 unique=3 first=`B5 03 00 00 00 00 2F 03` last=`B5 03 00 00 00 00 20 03` top=B5 03 00 00 00 00 20 03 x3939; B5 03 00 00 00 00 2F 03 x275; 58 02 00 00 00 00 20 03 x18
- ch1 `0x500` count=850 unique=2 first=`3C` last=`3C` top=3C x842; 3D x8
- ch0 `0x44B` count=486 unique=2 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x481; 4D 12 00 00 00 00 FF FF x5
- ch0 `0x157` count=436 unique=2 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x430; CE 00 00 00 00 00 00 00 x6
- ch1 `0x52A` count=435 unique=2 first=`00 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x427; FF 00 00 00 3E 00 00 00 x8
- ch1 `0x040` count=431 unique=2 first=`00 00 12 04 02 00 00 00` last=`00 00 12 04 02 00 00 00` top=00 00 12 04 02 00 00 00 x429; 00 00 02 0C 02 00 00 00 x2
- ch0 `0x1DF` count=431 unique=2 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x422; 38 FF 00 00 03 00 8F 00 x9
- ch1 `0x50E` count=431 unique=2 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x283; 00 00 00 20 00 00 00 00 x148
- ch0 `0x17B` count=425 unique=2 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x418; 00 00 00 00 00 00 03 00 x7
- ch1 `0x559` count=424 unique=2 first=`00 40 00 00 20 00 00 40` last=`00 40 00 00 20 00 00 40` top=00 40 00 00 20 00 00 40 x417; 00 40 00 00 00 00 00 40 x7
- ch1 `0x553` count=423 unique=2 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x292; 00 00 00 08 01 01 C0 00 x131
- ch1 `0x4A2` count=170 unique=2 first=`01 00` last=`01 00` top=01 00 x169; 00 00 x1
- ch0 `0x131` count=91 unique=2 first=`10 00 FF 00 10 00 FF 00` last=`10 00 FF 00 10 00 FF 00` top=10 00 FF 00 10 00 FF 00 x88; 00 00 FF 00 00 00 FF 00 x3
- ch1 `0x042` count=87 unique=2 first=`10 FF 10 FF 00 00 00 00` last=`10 FF 10 FF 00 00 00 00` top=10 FF 10 FF 00 00 00 00 x86; 00 FF 00 FF 00 00 00 00 x1
- ch0 `0x5D7` count=82 unique=2 first=`04 20 00 00 00 12 80 B4` last=`04 20 00 00 00 12 80 B4` top=04 20 00 00 00 12 80 B4 x81; 00 00 00 00 00 00 00 00 x1
- ch1 `0x434` count=81 unique=2 first=`03 00 00 00 00 00 00 00` last=`03 00 00 00 00 00 00 00` top=03 00 00 00 00 00 00 00 x80; 00 00 00 00 00 00 00 00 x1
- ch1 `0x428` count=4239 unique=4 first=`05 00 00 80 60 08 02 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x3931; 05 00 00 80 60 08 02 00 x289; 00 00 00 00 00 00 00 00 x18
- ch0 `0x169` count=481 unique=7 first=`20 01 03 FF 14 00 40 00` last=`20 01 00 FF 14 00 40 00` top=20 01 00 FF 14 00 40 00 x303; 27 01 00 FF 14 00 40 00 x125; 20 01 03 FF 14 00 40 00 x27
- ch1 `0x58B` count=1662 unique=3 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1639; 11 03 07 00 00 00 00 2F x22; 00 00 0C 00 00 00 00 60 x1
- ch1 `0x541` count=857 unique=3 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x842; 00 00 41 00 C0 00 00 01 x12; 02 00 41 00 C0 00 00 01 x3
- ch0 `0x16A` count=420 unique=3 first=`01 00 00 00 00 00 00 C3` last=`01 00 00 00 00 00 00 C3` top=01 00 00 00 00 00 00 C3 x409; 01 00 00 00 00 00 FF FF x8; 01 00 00 00 00 00 C3 C3 x3
- ch0 `0x134` count=93 unique=3 first=`03 00 00 02 00 00 10 00` last=`03 00 00 02 00 00 10 00` top=03 00 00 02 00 00 10 00 x87; 03 00 00 00 00 00 30 00 x4; 00 00 00 02 00 00 30 00 x2
- ch1 `0x043` count=88 unique=3 first=`00 00 00 02 03 00 00 30` last=`00 00 00 02 03 00 00 30` top=00 00 00 02 03 00 00 30 x84; 00 00 00 02 03 00 00 20 x3; 00 00 00 0E 00 00 00 00 x1
- ch1 `0x492` count=1712 unique=8 first=`00 00 00 00 A0 80 73 8C` last=`00 00 00 00 A1 80 73 F4` top=00 00 00 00 A0 80 73 04 x304; 00 00 00 00 A0 80 73 40 x300; 00 00 00 00 A0 80 73 C8 x299
- ch1 `0x4E5` count=860 unique=8 first=`80 4A 00 02 49 C4 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x797; 80 4A 00 02 49 C4 00 00 x52; 80 00 00 03 49 D0 00 00 x6

### gear_sequence_P_R_N_D_N_R_P_brake_pressed_done
range lines 3437993..3576970; duration 66.8s; previous marker `ignition_on_engine_off_P_to_R_4x_brake_pressed_done`
- ch1 `0x556` count=512 unique=2 first=`00 79 00 00 00 27 00 00` last=`00 78 00 00 00 27 00 00` top=00 79 00 00 00 27 00 00 x466; 00 78 00 00 00 27 00 00 x46
- ch1 `0x5A0` count=54 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 05 A5 02 x27; 00 00 00 C0 20 27 95 02 x27
- ch1 `0x545` count=519 unique=4 first=`DA 17 00 7E 00 00 00 B8` last=`DA 17 00 7D 00 00 00 B7` top=DA 17 00 7D 00 00 00 B8 x403; DA 17 00 7E 00 00 00 B8 x49; DA 17 00 7D 00 00 00 B7 x48
- ch1 `0x4E6` count=515 unique=5 first=`80 80 00 00 80 80 7D 00` last=`80 80 00 00 80 80 7C 00` top=80 80 00 00 80 80 7C 00 x316; 80 80 00 00 80 80 7D 00 x128; 80 80 00 00 80 80 7A 00 x35
- ch1 `0x50E` count=270 unique=2 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x163; 00 00 00 20 00 00 00 00 x107
- ch1 `0x492` count=1061 unique=8 first=`00 00 00 00 A1 80 73 B8` last=`00 00 00 00 A0 80 73 04` top=00 00 00 00 A1 80 73 30 x248; 00 00 00 00 A1 80 73 B8 x247; 00 00 00 00 A1 80 73 F4 x237
- ch1 `0x4F4` count=1088 unique=4 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x987; 00 01 C0 00 00 00 00 01 x75; 00 00 C0 00 00 00 00 00 x20
- ch1 `0x557` count=512 unique=4 first=`80 84 00 00 64 90 01 00` last=`80 84 00 00 64 90 01 00` top=80 84 00 00 64 90 01 00 x334; 80 84 00 00 64 8F 01 00 x167; 80 84 00 00 64 8E 01 00 x10
- ch0 `0x169` count=305 unique=5 first=`20 01 00 FF 14 00 40 00` last=`20 01 00 FF 14 00 40 00` top=20 01 00 FF 14 00 40 00 x188; 26 01 00 FF 14 00 40 00 x42; 27 01 00 FF 14 00 40 00 x40
- ch1 `0x58B` count=1090 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1090
- ch1 `0x490` count=1084 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1084
- ch1 `0x436` count=1071 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1071
- ch1 `0x520` count=538 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x538
- ch1 `0x53E` count=536 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x536
- ch1 `0x541` count=536 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x536
- ch1 `0x500` count=534 unique=1 first=`3C` last=`3C` top=3C x534
- ch1 `0x502` count=534 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x534
- ch1 `0x507` count=529 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x529
- ch1 `0x547` count=520 unique=1 first=`00 00 80 3C AE 47 CD 18` last=`00 00 80 3C AE 47 CD 18` top=00 00 80 3C AE 47 CD 18 x520
- ch1 `0x4E5` count=517 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x517
- ch1 `0x4E7` count=517 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x517
- ch1 `0x549` count=515 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x515
- ch1 `0x5CE` count=506 unique=1 first=`1E 2C 80 00 00 00 80 26` last=`1E 2C 80 00 00 00 80 26` top=1E 2C 80 00 00 00 80 26 x506
- ch0 `0x44B` count=306 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x306
- ch0 `0x44D` count=298 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x298
- ch0 `0x158` count=286 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x286
- ch0 `0x157` count=283 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x283
- ch1 `0x040` count=278 unique=1 first=`00 00 12 04 02 00 00 00` last=`00 00 12 04 02 00 00 00` top=00 00 12 04 02 00 00 00 x278
- ch0 `0x1DF` count=276 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x276
- ch1 `0x544` count=276 unique=1 first=`FF 37 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x276

### steering_wheel_volume_plus5_minus5_done_engine_on_or_ignition_on
range lines 3576970..3727802; duration 72.6s; previous marker `gear_sequence_P_R_N_D_N_R_P_brake_pressed_done`
- ch1 `0x547` count=571 unique=2 first=`00 00 80 3C AE 47 CD 18` last=`00 00 7F 3C AE 47 CD 18` top=00 00 80 3C AE 47 CD 18 x295; 00 00 7F 3C AE 47 CD 18 x276
- ch1 `0x5CF` count=576 unique=3 first=`3F 08 00 00 00 7F 80 00` last=`3F 06 00 00 00 7F 80 00` top=3F 06 00 00 00 7F 80 00 x416; 3F 07 00 00 00 7F 80 00 x144; 3F 08 00 00 00 7F 80 00 x16
- ch1 `0x545` count=575 unique=3 first=`DA 17 00 7D 00 00 00 B7` last=`DA 17 00 7B 00 00 00 B7` top=DA 17 00 7C 00 00 00 B7 x452; DA 17 00 7B 00 00 00 B7 x85; DA 17 00 7D 00 00 00 B7 x38
- ch1 `0x4E6` count=562 unique=3 first=`80 80 00 00 80 80 7C 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7B 00 x456; 80 80 00 00 80 80 7C 00 x62; 80 80 00 00 80 80 7A 00 x44
- ch1 `0x557` count=572 unique=4 first=`80 84 00 00 64 90 01 00` last=`80 84 00 00 64 8D 01 00` top=80 84 00 00 64 90 01 00 x297; 80 84 00 00 64 8F 01 00 x173; 80 84 00 00 64 8E 01 00 x56
- ch1 `0x18F` count=5946 unique=2 first=`FA 53 00 00 00 47 00 00` last=`FA 54 00 00 00 47 00 00` top=FA 53 00 00 00 47 00 00 x3278; FA 54 00 00 00 47 00 00 x2668
- ch1 `0x5A0` count=58 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x30; 00 00 00 C0 20 27 95 02 x28
- ch1 `0x111` count=6159 unique=4 first=`00 40 61 FF 62 00 00 B4` last=`00 40 61 FF 62 00 00 F0` top=00 40 61 FF 62 00 00 3C x1546; 00 40 61 FF 62 00 00 78 x1544; 00 40 61 FF 62 00 00 F0 x1542
- ch1 `0x112` count=5978 unique=4 first=`FF F0 00 00 FF 00 00 00` last=`FF 00 00 00 FF 00 00 00` top=FF 00 00 00 FF 00 00 00 x1502; FF F0 00 00 FF 00 00 00 x1499; FF 50 00 00 FF 00 00 00 x1493
- ch1 `0x492` count=1142 unique=4 first=`00 00 00 00 A0 80 73 04` last=`00 00 00 00 A0 80 73 04` top=00 00 00 00 A0 80 73 C8 x291; 00 00 00 00 A0 80 73 04 x284; 00 00 00 00 A0 80 73 8C x284
- ch1 `0x58B` count=1241 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1241
- ch1 `0x436` count=1171 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1171
- ch1 `0x490` count=1165 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1165
- ch1 `0x4F4` count=1144 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1144
- ch1 `0x500` count=618 unique=1 first=`3C` last=`3C` top=3C x618
- ch1 `0x520` count=583 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x583
- ch1 `0x502` count=578 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x578
- ch1 `0x541` count=578 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x578
- ch1 `0x507` count=577 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x577
- ch1 `0x53E` count=576 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x576
- ch1 `0x5CE` count=575 unique=1 first=`1E 2C 80 00 00 00 80 26` last=`1E 2C 80 00 00 00 80 26` top=1E 2C 80 00 00 00 80 26 x575
- ch1 `0x549` count=570 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x570
- ch1 `0x4E7` count=569 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x569
- ch1 `0x4E5` count=567 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x567
- ch1 `0x556` count=565 unique=1 first=`00 78 00 00 00 27 00 00` last=`00 78 00 00 00 27 00 00` top=00 78 00 00 00 27 00 00 x565
- ch0 `0x15D` count=315 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x315
- ch0 `0x157` count=313 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x313
- ch0 `0x158` count=307 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x307
- ch0 `0x1EB` count=306 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x306
- ch0 `0x169` count=303 unique=1 first=`20 01 00 FF 14 00 40 00` last=`20 01 00 FF 14 00 40 00` top=20 01 00 FF 14 00 40 00 x303

### steering_wheel_next5_prev5_done
range lines 3727802..3847329; duration 61.0s; previous marker `steering_wheel_volume_plus5_minus5_done_engine_on_or_ignition_on`
- ch1 `0x556` count=464 unique=2 first=`00 78 00 00 00 27 00 00` last=`00 77 00 00 00 27 00 00` top=00 77 00 00 00 27 00 00 x368; 00 78 00 00 00 27 00 00 x96
- ch1 `0x5A0` count=50 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x27; 00 00 00 C0 20 27 95 02 x23
- ch1 `0x545` count=452 unique=3 first=`DA 17 00 7B 00 00 00 B7` last=`DA 17 00 7B 00 00 00 B5` top=DA 17 00 7B 00 00 00 B5 x347; DA 17 00 7B 00 00 00 B7 x78; DA 17 00 7B 00 00 00 B6 x27
- ch1 `0x557` count=455 unique=6 first=`80 84 00 00 64 8E 01 00` last=`80 84 00 00 64 89 01 00` top=80 84 00 00 64 8D 01 00 x201; 80 84 00 00 64 8C 01 00 x97; 80 84 00 00 64 89 01 00 x57
- ch1 `0x111` count=4882 unique=4 first=`00 40 61 FF 62 00 00 B4` last=`00 40 61 FF 62 00 00 F0` top=00 40 61 FF 62 00 00 B4 x1242; 00 40 61 FF 62 00 00 78 x1228; 00 40 61 FF 62 00 00 3C x1207
- ch1 `0x112` count=4680 unique=4 first=`FF 50 00 00 FF 00 00 00` last=`FF 00 00 00 FF 00 00 00` top=FF A0 00 00 FF 00 00 00 x1189; FF 50 00 00 FF 00 00 00 x1178; FF 00 00 00 FF 00 00 00 x1158
- ch1 `0x113` count=4645 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 60` top=00 20 00 80 00 00 00 E8 x1177; 00 20 00 80 00 00 00 24 x1168; 00 20 00 80 00 00 00 60 x1154
- ch1 `0x492` count=899 unique=8 first=`00 00 00 00 A0 80 73 40` last=`00 00 00 00 9E 80 73 BC` top=00 00 00 00 9E 80 73 70 x189; 00 00 00 00 9E 80 73 34 x187; 00 00 00 00 9E 80 73 F8 x186
- ch1 `0x18F` count=4776 unique=2 first=`FA 54 00 00 00 47 00 00` last=`FA 54 00 00 00 47 00 00` top=FA 54 00 00 00 47 00 00 x4714; FA 56 00 00 00 47 00 00 x62
- ch1 `0x58B` count=1004 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1004
- ch1 `0x436` count=926 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x926
- ch1 `0x490` count=915 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x915
- ch1 `0x4F4` count=909 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x909
- ch1 `0x500` count=468 unique=1 first=`3C` last=`3C` top=3C x468
- ch1 `0x5CE` count=466 unique=1 first=`1E 2C 80 00 00 00 80 26` last=`1E 2C 80 00 00 00 80 26` top=1E 2C 80 00 00 00 80 26 x466
- ch1 `0x502` count=465 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x465
- ch1 `0x547` count=464 unique=1 first=`00 00 7F 3C AE 47 CD 18` last=`00 00 7F 3C AE 47 CD 18` top=00 00 7F 3C AE 47 CD 18 x464
- ch1 `0x549` count=463 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x463
- ch1 `0x5CF` count=461 unique=1 first=`3F 06 00 00 00 7F 80 00` last=`3F 06 00 00 00 7F 80 00` top=3F 06 00 00 00 7F 80 00 x461
- ch1 `0x53E` count=457 unique=1 first=`00 08 00 00 80 05` last=`00 08 00 00 80 05` top=00 08 00 00 80 05 x457
- ch1 `0x520` count=455 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x455
- ch1 `0x507` count=453 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x453
- ch1 `0x541` count=450 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x450
- ch1 `0x4E7` count=447 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x447
- ch1 `0x4E6` count=442 unique=1 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x442
- ch1 `0x4E5` count=440 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x440
- ch0 `0x133` count=246 unique=1 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 82` top=00 00 00 00 00 00 00 82 x246
- ch0 `0x15D` count=246 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x246
- ch0 `0x1EB` count=244 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x244
- ch0 `0x44B` count=244 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x244

### steering_wheel_mode_source_5x_done
range lines 3847329..3943761; duration 49.9s; previous marker `steering_wheel_next5_prev5_done`
- ch1 `0x545` count=375 unique=2 first=`DA 17 00 7B 00 00 00 B5` last=`DA 17 00 7B 00 00 00 B4` top=DA 17 00 7B 00 00 00 B4 x226; DA 17 00 7B 00 00 00 B5 x149
- ch1 `0x556` count=374 unique=2 first=`00 77 00 00 00 27 00 00` last=`00 76 00 00 00 27 00 00` top=00 76 00 00 00 27 00 00 x222; 00 77 00 00 00 27 00 00 x152
- ch1 `0x5CF` count=373 unique=2 first=`3F 06 00 00 00 7F 80 00` last=`3F 07 00 00 00 7F 80 00` top=3F 07 00 00 00 7F 80 00 x365; 3F 06 00 00 00 7F 80 00 x8
- ch1 `0x5CE` count=372 unique=2 first=`1E 2C 80 00 00 00 80 26` last=`1D 2C 80 00 00 00 80 26` top=1D 2C 80 00 00 00 80 26 x260; 1E 2C 80 00 00 00 80 26 x112
- ch1 `0x53E` count=371 unique=2 first=`00 08 00 00 80 05` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x294; 00 08 00 00 80 05 x77
- ch1 `0x316` count=3676 unique=2 first=`05 00 00 00 00 15 00 6F` last=`05 00 00 00 00 16 00 6F` top=05 00 00 00 00 16 00 6F x2219; 05 00 00 00 00 15 00 6F x1457
- ch1 `0x18F` count=3815 unique=3 first=`FA 54 00 00 00 47 00 00` last=`FA 54 00 00 00 48 00 00` top=FA 54 00 00 00 48 00 00 x2292; FA 54 00 00 00 47 00 00 x1222; FA 56 00 00 00 47 00 00 x301
- ch1 `0x4E6` count=370 unique=2 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x369; 80 80 00 00 80 80 79 00 x1
- ch1 `0x5A0` count=40 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x20; 00 00 00 C0 20 05 A5 02 x20
- ch1 `0x111` count=3791 unique=4 first=`00 40 61 FF 62 00 00 B4` last=`00 40 61 FF 62 00 00 3C` top=00 40 61 FF 62 00 00 78 x966; 00 40 61 FF 62 00 00 F0 x961; 00 40 61 FF 62 00 00 B4 x937
- ch1 `0x113` count=3745 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 60 x949; 00 20 00 80 00 00 00 E8 x942; 00 20 00 80 00 00 00 24 x933
- ch1 `0x112` count=3730 unique=4 first=`FF 50 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF 00 00 00 FF 00 00 00 x952; FF A0 00 00 FF 00 00 00 x948; FF 50 00 00 FF 00 00 00 x923
- ch1 `0x492` count=739 unique=8 first=`00 00 00 00 9E 80 73 70` last=`00 00 00 00 9D 80 73 CC` top=00 00 00 00 9D 80 73 44 x107; 00 00 00 00 9D 80 73 08 x105; 00 00 00 00 9D 80 73 CC x105
- ch1 `0x557` count=380 unique=8 first=`80 84 00 00 64 89 01 00` last=`80 84 00 00 64 83 01 00` top=80 84 00 00 64 86 01 00 x151; 80 84 00 00 64 89 01 00 x88; 80 84 00 00 64 87 01 00 x36
- ch1 `0x387` count=1890 unique=1 first=`09 0E 00 00 00 17` last=`09 0E 00 00 00 17` top=09 0E 00 00 00 17 x1890
- ch1 `0x428` count=1881 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1881
- ch1 `0x429` count=1858 unique=1 first=`B5 03 00 00 00 00 20 03` last=`B5 03 00 00 00 00 20 03` top=B5 03 00 00 00 00 20 03 x1858
- ch1 `0x58B` count=812 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x812
- ch1 `0x436` count=758 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x758
- ch1 `0x4F4` count=737 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x737
- ch1 `0x490` count=729 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x729
- ch1 `0x520` count=379 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x379
- ch1 `0x547` count=379 unique=1 first=`00 00 7F 3C AE 47 CD 18` last=`00 00 7F 3C AE 47 CD 18` top=00 00 7F 3C AE 47 CD 18 x379
- ch1 `0x549` count=378 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x378
- ch1 `0x541` count=376 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x376
- ch1 `0x4E7` count=374 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x374
- ch1 `0x4E5` count=373 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x373
- ch1 `0x500` count=369 unique=1 first=`3C` last=`3C` top=3C x369
- ch1 `0x502` count=364 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x364
- ch1 `0x507` count=354 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x354

### steering_wheel_mute_5x_done
range lines 3943761..4013334; duration 39.2s; previous marker `steering_wheel_mode_source_5x_done`
- ch1 `0x5CF` count=257 unique=2 first=`3F 07 00 00 00 7F 80 00` last=`3F 08 00 00 00 7F 80 00` top=3F 08 00 00 00 7F 80 00 x190; 3F 07 00 00 00 7F 80 00 x67
- ch1 `0x5A0` count=35 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 27 95 02 x19; 00 00 00 C0 20 05 A5 02 x16
- ch1 `0x492` count=541 unique=4 first=`00 00 00 00 9D 80 73 80` last=`00 00 00 00 9D 80 73 44` top=00 00 00 00 9D 80 73 80 x139; 00 00 00 00 9D 80 73 44 x139; 00 00 00 00 9D 80 73 CC x139
- ch1 `0x4E6` count=278 unique=2 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x263; 80 80 00 00 80 80 79 00 x15
- ch1 `0x545` count=272 unique=2 first=`DA 17 00 7B 00 00 00 B4` last=`DA 17 00 7B 00 00 00 B4` top=DA 17 00 7B 00 00 00 B4 x269; DA 17 00 7A 00 00 00 B4 x3
- ch1 `0x111` count=2767 unique=4 first=`00 40 61 FF 62 00 00 F0` last=`00 40 61 FF 62 00 00 3C` top=00 40 61 FF 62 00 00 F0 x714; 00 40 61 FF 62 00 00 B4 x696; 00 40 61 FF 62 00 00 78 x692
- ch1 `0x113` count=2722 unique=4 first=`00 20 00 80 00 00 00 60` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 60 x701; 00 20 00 80 00 00 00 E8 x682; 00 20 00 80 00 00 00 24 x674
- ch1 `0x112` count=2720 unique=4 first=`FF 00 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF 00 00 00 FF 00 00 00 x700; FF A0 00 00 FF 00 00 00 x693; FF 50 00 00 FF 00 00 00 x670
- ch1 `0x329` count=2587 unique=4 first=`80 A9 7F 12 11 2A 00 18` last=`E2 A9 7F 12 11 2A 00 18` top=E2 A9 7F 12 11 2A 00 18 x668; 40 A9 7F 12 11 2A 00 18 x662; 0F A9 7F 12 11 2A 00 18 x639
- ch1 `0x557` count=258 unique=7 first=`80 84 00 00 64 82 01 00` last=`80 84 00 00 64 7D 01 00` top=80 84 00 00 64 7F 01 00 x99; 80 84 00 00 64 82 01 00 x80; 80 84 00 00 64 80 01 00 x34
- ch1 `0x428` count=1365 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1365
- ch1 `0x387` count=1357 unique=1 first=`09 0E 00 00 00 17` last=`09 0E 00 00 00 17` top=09 0E 00 00 00 17 x1357
- ch1 `0x429` count=1350 unique=1 first=`B5 03 00 00 00 00 20 03` last=`B5 03 00 00 00 00 20 03` top=B5 03 00 00 00 00 20 03 x1350
- ch1 `0x58B` count=605 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x605
- ch1 `0x436` count=555 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x555
- ch1 `0x4F4` count=542 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x542
- ch1 `0x490` count=521 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x521
- ch1 `0x4E5` count=286 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x286
- ch1 `0x4E7` count=279 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x279
- ch1 `0x520` count=279 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x279
- ch1 `0x541` count=277 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x277
- ch1 `0x547` count=274 unique=1 first=`00 00 7F 3C AE 47 CD 18` last=`00 00 7F 3C AE 47 CD 18` top=00 00 7F 3C AE 47 CD 18 x274
- ch1 `0x500` count=271 unique=1 first=`3C` last=`3C` top=3C x271
- ch1 `0x549` count=271 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x271
- ch1 `0x556` count=265 unique=1 first=`00 76 00 00 00 27 00 00` last=`00 76 00 00 00 27 00 00` top=00 76 00 00 00 27 00 00 x265
- ch1 `0x53E` count=264 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x264
- ch1 `0x502` count=261 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x261
- ch1 `0x5CE` count=254 unique=1 first=`1D 2C 80 00 00 00 80 26` last=`1D 2C 80 00 00 00 80 26` top=1D 2C 80 00 00 00 80 26 x254
- ch1 `0x507` count=250 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x250
- ch0 `0x44D` count=157 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x157

### steering_wheel_call2_end2_done
range lines 4013334..4162062; duration 70.0s; previous marker `steering_wheel_mute_5x_done`
- ch1 `0x4E6` count=579 unique=2 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 79 00` top=80 80 00 00 80 80 79 00 x505; 80 80 00 00 80 80 7A 00 x74
- ch1 `0x547` count=565 unique=2 first=`00 00 7F 3C AE 47 CD 18` last=`00 00 7E 3C AE 47 CD 18` top=00 00 7F 3C AE 47 CD 18 x499; 00 00 7E 3C AE 47 CD 18 x66
- ch1 `0x5CE` count=556 unique=2 first=`1D 2C 80 00 00 00 80 26` last=`1C 2C 80 00 00 00 80 26` top=1C 2C 80 00 00 00 80 26 x448; 1D 2C 80 00 00 00 80 26 x108
- ch1 `0x5CF` count=556 unique=2 first=`3F 08 00 00 00 7F 80 00` last=`3F 09 00 00 00 7F 80 00` top=3F 08 00 00 00 7F 80 00 x321; 3F 09 00 00 00 7F 80 00 x235
- ch1 `0x545` count=574 unique=4 first=`DA 17 00 7B 00 00 00 B4` last=`DA 17 00 7A 00 00 00 B3` top=DA 17 00 7A 00 00 00 B3 x461; DA 17 00 7A 00 00 00 B4 x84; DA 17 00 7B 00 00 00 B4 x25
- ch1 `0x18F` count=5902 unique=2 first=`FA 54 00 00 00 48 00 00` last=`FA 53 00 00 00 48 00 00` top=FA 53 00 00 00 48 00 00 x4949; FA 54 00 00 00 48 00 00 x953
- ch1 `0x5A0` count=55 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x28; 00 00 00 C0 20 27 95 02 x27
- ch1 `0x111` count=5825 unique=4 first=`00 40 61 FF 62 00 00 3C` last=`00 40 61 FF 62 00 00 B4` top=00 40 61 FF 62 00 00 78 x1469; 00 40 61 FF 62 00 00 F0 x1461; 00 40 61 FF 62 00 00 3C x1448
- ch1 `0x112` count=5787 unique=4 first=`FF F0 00 00 FF 00 00 00` last=`FF 50 00 00 FF 00 00 00` top=FF A0 00 00 FF 00 00 00 x1461; FF 50 00 00 FF 00 00 00 x1451; FF 00 00 00 FF 00 00 00 x1438
- ch1 `0x492` count=1177 unique=4 first=`00 00 00 00 9D 80 73 80` last=`00 00 00 00 9D 80 73 80` top=00 00 00 00 9D 80 73 08 x298; 00 00 00 00 9D 80 73 CC x298; 00 00 00 00 9D 80 73 80 x293
- ch1 `0x58B` count=1206 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1206
- ch1 `0x436` count=1166 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1166
- ch1 `0x4F4` count=1165 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1165
- ch1 `0x490` count=1130 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1130
- ch1 `0x4E5` count=583 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x583
- ch1 `0x4E7` count=580 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x580
- ch1 `0x53E` count=578 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x578
- ch1 `0x502` count=574 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x574
- ch1 `0x500` count=572 unique=1 first=`3C` last=`3C` top=3C x572
- ch1 `0x520` count=569 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x569
- ch1 `0x549` count=565 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x565
- ch1 `0x541` count=564 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x564
- ch1 `0x556` count=562 unique=1 first=`00 76 00 00 00 27 00 00` last=`00 76 00 00 00 27 00 00` top=00 76 00 00 00 27 00 00 x562
- ch1 `0x507` count=551 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x551
- ch0 `0x44B` count=340 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x340
- ch0 `0x44D` count=314 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x314
- ch0 `0x1EB` count=298 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x298
- ch1 `0x52A` count=297 unique=1 first=`00 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x297
- ch1 `0x040` count=295 unique=1 first=`00 00 12 04 02 00 00 00` last=`00 00 12 04 02 00 00 00` top=00 00 12 04 02 00 00 00 x295
- ch0 `0x135` count=295 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x295

### steering_wheel_microphone_5x_done
range lines 4162062..4230539; duration 39.2s; previous marker `steering_wheel_call2_end2_done`
- ch1 `0x5CE` count=250 unique=2 first=`1C 2C 80 00 00 00 80 26` last=`1B 2C 80 00 00 00 80 26` top=1B 2C 80 00 00 00 80 26 x156; 1C 2C 80 00 00 00 80 26 x94
- ch1 `0x492` count=547 unique=4 first=`00 00 00 00 9D 80 73 CC` last=`00 00 00 00 9D 80 73 44` top=00 00 00 00 9D 80 73 44 x145; 00 00 00 00 9D 80 73 08 x143; 00 00 00 00 9D 80 73 80 x132
- ch1 `0x545` count=263 unique=2 first=`DA 17 00 7A 00 00 00 B3` last=`DA 17 00 7A 00 00 00 B3` top=DA 17 00 7A 00 00 00 B3 x262; DA 17 00 7A 00 00 00 B2 x1
- ch1 `0x547` count=261 unique=2 first=`00 00 7E 3C AE 47 CD 18` last=`00 00 7E 3C AE 47 CD 18` top=00 00 7E 3C AE 47 CD 18 x260; 00 00 7F 3C AE 47 CD 18 x1
- ch1 `0x5A0` count=29 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x16; 00 00 00 C0 20 05 A5 02 x13
- ch1 `0x111` count=2782 unique=4 first=`00 40 61 FF 62 00 00 F0` last=`00 40 61 FF 62 00 00 3C` top=00 40 61 FF 62 00 00 B4 x707; 00 40 61 FF 62 00 00 3C x699; 00 40 61 FF 62 00 00 F0 x688
- ch1 `0x112` count=2695 unique=4 first=`FF 00 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF F0 00 00 FF 00 00 00 x677; FF 50 00 00 FF 00 00 00 x674; FF 00 00 00 FF 00 00 00 x672
- ch1 `0x113` count=2694 unique=4 first=`00 20 00 80 00 00 00 60` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 AC x683; 00 20 00 80 00 00 00 24 x677; 00 20 00 80 00 00 00 60 x671
- ch1 `0x18F` count=2817 unique=2 first=`FA 53 00 00 00 48 00 00` last=`FA 53 00 00 00 48 00 00` top=FA 53 00 00 00 48 00 00 x2808; FA 53 00 00 00 49 00 00 x9
- ch1 `0x387` count=1331 unique=1 first=`09 0E 00 00 00 17` last=`09 0E 00 00 00 17` top=09 0E 00 00 00 17 x1331
- ch1 `0x428` count=1329 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1329
- ch1 `0x429` count=1296 unique=1 first=`B5 03 00 00 00 00 20 03` last=`B5 03 00 00 00 00 20 03` top=B5 03 00 00 00 00 20 03 x1296
- ch1 `0x58B` count=588 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x588
- ch1 `0x4F4` count=536 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x536
- ch1 `0x490` count=525 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x525
- ch1 `0x436` count=516 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x516
- ch1 `0x4E5` count=269 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x269
- ch1 `0x500` count=269 unique=1 first=`3C` last=`3C` top=3C x269
- ch1 `0x4E6` count=268 unique=1 first=`80 80 00 00 80 80 79 00` last=`80 80 00 00 80 80 79 00` top=80 80 00 00 80 80 79 00 x268
- ch1 `0x4E7` count=267 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x267
- ch1 `0x507` count=256 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x256
- ch1 `0x502` count=254 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x254
- ch1 `0x549` count=252 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x252
- ch1 `0x5CF` count=251 unique=1 first=`3F 09 00 00 00 7F 80 00` last=`3F 09 00 00 00 7F 80 00` top=3F 09 00 00 00 7F 80 00 x251
- ch1 `0x53E` count=250 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x250
- ch1 `0x556` count=249 unique=1 first=`00 76 00 00 00 27 00 00` last=`00 76 00 00 00 27 00 00` top=00 76 00 00 00 27 00 00 x249
- ch1 `0x520` count=244 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x244
- ch1 `0x541` count=241 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x241
- ch0 `0x44B` count=156 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x156
- ch0 `0x44D` count=156 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x156

### climate_panel_common_button_5x_2repeats_done
range lines 4230539..4413991; duration 91.8s; previous marker `steering_wheel_microphone_5x_done`
- ch1 `0x545` count=710 unique=2 first=`DA 17 00 7A 00 00 00 B3` last=`DA 17 00 7A 00 00 00 B2` top=DA 17 00 7A 00 00 00 B2 x683; DA 17 00 7A 00 00 00 B3 x27
- ch1 `0x5CF` count=710 unique=2 first=`3F 09 00 00 00 7F 80 00` last=`3F 0A 00 00 00 7F 80 00` top=3F 0A 00 00 00 7F 80 00 x528; 3F 09 00 00 00 7F 80 00 x182
- ch1 `0x556` count=695 unique=2 first=`00 76 00 00 00 27 00 00` last=`00 75 00 00 00 27 00 00` top=00 75 00 00 00 27 00 00 x665; 00 76 00 00 00 27 00 00 x30
- ch1 `0x5CE` count=696 unique=3 first=`1B 2C 80 00 00 00 80 26` last=`1A 2C 80 00 00 00 80 26` top=1A 2C 80 00 00 00 80 26 x500; 1B 2C 80 00 00 00 80 26 x193; 19 2C 80 00 00 00 80 26 x3
- ch1 `0x043` count=131 unique=2 first=`00 00 00 02 03 00 00 30` last=`00 00 00 02 03 00 00 30` top=00 00 00 02 03 00 00 30 x101; 00 00 04 02 03 00 00 30 x30
- ch0 `0x132` count=130 unique=2 first=`90 00 00 14 00 00 00 03` last=`90 00 00 14 00 00 00 03` top=90 00 00 14 00 00 00 03 x100; 90 00 10 14 00 00 00 03 x30
- ch1 `0x5A0` count=76 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x38; 00 00 00 C0 20 05 A5 02 x38
- ch1 `0x111` count=7426 unique=4 first=`00 40 61 FF 62 00 00 F0` last=`00 40 61 FF 62 00 00 B4` top=00 40 61 FF 62 00 00 3C x1893; 00 40 61 FF 62 00 00 B4 x1880; 00 40 61 FF 62 00 00 F0 x1834
- ch1 `0x112` count=7208 unique=4 first=`FF 00 00 00 FF 00 00 00` last=`FF 50 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x1814; FF F0 00 00 FF 00 00 00 x1814; FF 00 00 00 FF 00 00 00 x1794
- ch1 `0x113` count=7167 unique=4 first=`00 20 00 80 00 00 00 60` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 AC x1803; 00 20 00 80 00 00 00 24 x1800; 00 20 00 80 00 00 00 E8 x1784
- ch1 `0x18F` count=7498 unique=5 first=`FA 53 00 00 00 48 00 00` last=`FA 51 00 00 00 49 00 00` top=FA 52 00 00 00 49 00 00 x3561; FA 51 00 00 00 49 00 00 x3263; FA 53 00 00 00 48 00 00 x355
- ch1 `0x492` count=1417 unique=8 first=`00 00 00 00 9D 80 73 80` last=`00 00 00 00 9C 80 73 DC` top=00 00 00 00 9C 80 73 DC x342; 00 00 00 00 9C 80 73 90 x341; 00 00 00 00 9C 80 73 54 x331
- ch1 `0x436` count=1433 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1433
- ch1 `0x58B` count=1428 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1428
- ch1 `0x4F4` count=1413 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1413
- ch1 `0x490` count=1402 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1402
- ch1 `0x502` count=724 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x724
- ch1 `0x4E5` count=719 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x719
- ch1 `0x4E6` count=719 unique=1 first=`80 80 00 00 80 80 79 00` last=`80 80 00 00 80 80 79 00` top=80 80 00 00 80 80 79 00 x719
- ch1 `0x4E7` count=711 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x711
- ch1 `0x541` count=710 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x710
- ch1 `0x507` count=709 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x709
- ch1 `0x520` count=709 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x709
- ch1 `0x547` count=707 unique=1 first=`00 00 7E 3C AE 47 CD 18` last=`00 00 7E 3C AE 47 CD 18` top=00 00 7E 3C AE 47 CD 18 x707
- ch1 `0x500` count=705 unique=1 first=`3C` last=`3C` top=3C x705
- ch1 `0x549` count=700 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x700
- ch1 `0x53E` count=686 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x686
- ch0 `0x44B` count=416 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x416
- ch0 `0x44D` count=391 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x391
- ch0 `0x157` count=374 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x374

### climate_off_button_5x_done
range lines 4413991..4509520; duration 55.7s; previous marker `climate_panel_common_button_5x_2repeats_done`
- ch1 `0x4E6` count=370 unique=2 first=`80 80 00 00 80 80 79 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x196; 80 80 00 00 80 80 79 00 x174
- ch1 `0x556` count=363 unique=2 first=`00 75 00 00 00 27 00 00` last=`00 74 00 00 00 27 00 00` top=00 74 00 00 00 27 00 00 x255; 00 75 00 00 00 27 00 00 x108
- ch1 `0x040` count=196 unique=2 first=`00 00 12 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 12 04 02 00 00 00 x98; 00 00 22 04 02 00 00 00 x98
- ch0 `0x131` count=43 unique=2 first=`10 00 FF 00 10 00 FF 00` last=`00 00 FF 00 00 00 FF 00` top=00 00 FF 00 00 00 FF 00 x22; 10 00 FF 00 10 00 FF 00 x21
- ch1 `0x042` count=41 unique=2 first=`10 FF 10 FF 00 00 00 00` last=`00 FF 00 FF 00 00 00 00` top=10 FF 10 FF 00 00 00 00 x21; 00 FF 00 FF 00 00 00 00 x20
- ch1 `0x434` count=39 unique=2 first=`03 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=03 00 00 00 00 00 00 00 x20; 00 00 00 00 00 00 00 00 x19
- ch1 `0x5CE` count=369 unique=3 first=`19 2C 80 00 00 00 80 26` last=`18 2C 80 00 00 00 80 26` top=19 2C 80 00 00 00 80 26 x311; 18 2C 80 00 00 00 80 26 x56; 1A 2C 80 00 00 00 80 26 x2
- ch1 `0x545` count=366 unique=3 first=`DA 17 00 7A 00 00 00 B2` last=`DA 17 00 7B 00 00 00 B1` top=DA 17 00 7B 00 00 00 B1 x192; DA 17 00 7A 00 00 00 B2 x117; DA 17 00 7A 00 00 00 B1 x57
- ch1 `0x043` count=68 unique=3 first=`00 00 00 02 03 00 00 30` last=`00 00 00 04 03 00 00 00` top=00 00 00 04 03 00 00 00 x31; 00 00 00 02 03 00 00 30 x22; 00 01 00 04 03 00 00 00 x15
- ch0 `0x132` count=61 unique=3 first=`90 00 00 14 00 00 00 03` last=`90 00 00 28 00 00 00 00` top=90 00 00 28 00 00 00 00 x30; 90 00 00 14 00 00 00 03 x16; 90 10 00 28 00 00 00 00 x15
- ch1 `0x5A0` count=39 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x21; 00 00 00 C0 20 27 95 02 x18
- ch1 `0x112` count=3728 unique=4 first=`FF A0 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF A0 00 00 FF 00 00 00 x947; FF F0 00 00 FF 00 00 00 x938; FF 00 00 00 FF 00 00 00 x926
- ch1 `0x492` count=755 unique=8 first=`00 00 00 00 9C 80 73 18` last=`00 00 00 00 9A 80 73 FC` top=00 00 00 00 9A 80 73 38 x143; 00 00 00 00 9A 80 73 74 x142; 00 00 00 00 9A 80 73 B0 x141
- ch1 `0x387` count=1807 unique=1 first=`09 0E 00 00 00 17` last=`09 0E 00 00 00 17` top=09 0E 00 00 00 17 x1807
- ch1 `0x428` count=1798 unique=1 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 00 00` top=05 00 00 80 60 08 00 00 x1798
- ch1 `0x429` count=1767 unique=1 first=`B5 03 00 00 00 00 20 03` last=`B5 03 00 00 00 00 20 03` top=B5 03 00 00 00 00 20 03 x1767
- ch1 `0x58B` count=770 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x770
- ch1 `0x4F4` count=747 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x747
- ch1 `0x436` count=709 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x709
- ch1 `0x490` count=695 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x695
- ch1 `0x4E7` count=376 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x376
- ch1 `0x4E5` count=375 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x375
- ch1 `0x5CF` count=368 unique=1 first=`3F 0A 00 00 00 7F 80 00` last=`3F 0A 00 00 00 7F 80 00` top=3F 0A 00 00 00 7F 80 00 x368
- ch1 `0x547` count=366 unique=1 first=`00 00 7E 3C AE 47 CD 18` last=`00 00 7E 3C AE 47 CD 18` top=00 00 7E 3C AE 47 CD 18 x366
- ch1 `0x500` count=364 unique=1 first=`3C` last=`3C` top=3C x364
- ch1 `0x53E` count=361 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x361
- ch1 `0x549` count=358 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x358
- ch1 `0x541` count=355 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x355
- ch1 `0x520` count=351 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x351
- ch1 `0x502` count=339 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x339

### driver_seat_ventilation_levels_1_2_3_off_3cycles_done
range lines 4509520..4662692; duration 81.5s; previous marker `climate_off_button_5x_done`
- ch1 `0x545` count=587 unique=2 first=`DA 17 00 7B 00 00 00 B1` last=`DA 17 00 7B 00 00 00 B0` top=DA 17 00 7B 00 00 00 B0 x336; DA 17 00 7B 00 00 00 B1 x251
- ch1 `0x556` count=576 unique=2 first=`00 74 00 00 00 27 00 00` last=`00 73 00 00 00 27 00 00` top=00 73 00 00 00 27 00 00 x337; 00 74 00 00 00 27 00 00 x239
- ch1 `0x5CE` count=569 unique=2 first=`18 2C 80 00 00 00 80 26` last=`17 2C 80 00 00 00 80 26` top=17 2C 80 00 00 00 80 26 x376; 18 2C 80 00 00 00 80 26 x193
- ch1 `0x18F` count=6270 unique=3 first=`FA 51 00 00 00 49 00 00` last=`FA 50 00 00 00 4A 00 00` top=FA 51 00 00 00 49 00 00 x2878; FA 50 00 00 00 4A 00 00 x1847; FA 51 00 00 00 4A 00 00 x1545
- ch1 `0x547` count=581 unique=2 first=`00 00 7E 3C AE 47 CD 18` last=`00 00 7E 3C AE 47 CD 18` top=00 00 7E 3C AE 47 CD 18 x574; 00 00 7D 3C AE 47 CD 18 x7
- ch1 `0x5A0` count=63 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x33; 00 00 00 C0 20 05 A5 02 x30
- ch1 `0x112` count=6030 unique=4 first=`FF 50 00 00 FF 00 00 00` last=`FF F0 00 00 FF 00 00 00` top=FF A0 00 00 FF 00 00 00 x1527; FF 00 00 00 FF 00 00 00 x1506; FF 50 00 00 FF 00 00 00 x1504
- ch1 `0x113` count=5962 unique=4 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 E8 x1507; 00 20 00 80 00 00 00 24 x1492; 00 20 00 80 00 00 00 60 x1483
- ch1 `0x492` count=1210 unique=8 first=`00 00 00 00 9A 80 73 74` last=`00 00 00 00 99 80 73 0C` top=00 00 00 00 99 80 73 48 x168; 00 00 00 00 99 80 73 0C x166; 00 00 00 00 99 80 73 84 x165
- ch1 `0x58B` count=1275 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1275
- ch1 `0x436` count=1223 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1223
- ch1 `0x4F4` count=1197 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1197
- ch1 `0x490` count=1174 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1174
- ch1 `0x4E7` count=600 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x600
- ch1 `0x4E5` count=596 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x596
- ch1 `0x4E6` count=596 unique=1 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x596
- ch1 `0x500` count=594 unique=1 first=`3C` last=`3C` top=3C x594
- ch1 `0x520` count=590 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x590
- ch1 `0x541` count=582 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x582
- ch1 `0x549` count=580 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x580
- ch1 `0x53E` count=576 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x576
- ch1 `0x502` count=574 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x574
- ch1 `0x507` count=573 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x573
- ch1 `0x5CF` count=559 unique=1 first=`3F 0A 00 00 00 7F 80 00` last=`3F 0A 00 00 00 7F 80 00` top=3F 0A 00 00 00 7F 80 00 x559
- ch0 `0x44B` count=358 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x358
- ch0 `0x44D` count=331 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x331
- ch0 `0x15D` count=321 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x321
- ch0 `0x1EB` count=318 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x318
- ch0 `0x157` count=317 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x317
- ch0 `0x133` count=311 unique=1 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 82` top=00 00 00 00 00 00 00 82 x311

### passenger_seat_ventilation_levels_1_2_3_off_3cycles_done
range lines 4662692..4829130; duration 85.7s; previous marker `driver_seat_ventilation_levels_1_2_3_off_3cycles_done`
- ch1 `0x547` count=618 unique=2 first=`00 00 7E 3C AE 47 CD 18` last=`00 00 7D 3C AE 47 CD 18` top=00 00 7D 3C AE 47 CD 18 x588; 00 00 7E 3C AE 47 CD 18 x30
- ch1 `0x5CF` count=607 unique=2 first=`3F 0A 00 00 00 7F 80 00` last=`3F 0B 00 00 00 7F 80 00` top=3F 0B 00 00 00 7F 80 00 x504; 3F 0A 00 00 00 7F 80 00 x103
- ch1 `0x5A0` count=69 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 27 95 02 x35; 00 00 00 C0 20 05 A5 02 x34
- ch1 `0x5CE` count=603 unique=3 first=`17 2C 80 00 00 00 80 26` last=`15 2C 80 00 00 00 80 26` top=15 2C 80 00 00 00 80 26 x308; 16 2C 80 00 00 00 80 26 x288; 17 2C 80 00 00 00 80 26 x7
- ch1 `0x492` count=1253 unique=4 first=`00 00 00 00 99 80 73 C0` last=`00 00 00 00 99 80 73 0C` top=00 00 00 00 99 80 73 84 x318; 00 00 00 00 99 80 73 48 x318; 00 00 00 00 99 80 73 0C x314
- ch1 `0x18F` count=6751 unique=2 first=`FA 50 00 00 00 4A 00 00` last=`FA 4F 00 00 00 4A 00 00` top=FA 50 00 00 00 4A 00 00 x5985; FA 4F 00 00 00 4A 00 00 x766
- ch1 `0x112` count=6340 unique=4 first=`FF 00 00 00 FF 00 00 00` last=`FF A0 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x1591; FF A0 00 00 FF 00 00 00 x1589; FF F0 00 00 FF 00 00 00 x1584
- ch1 `0x58B` count=1366 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1366
- ch1 `0x490` count=1295 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1295
- ch1 `0x436` count=1282 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1282
- ch1 `0x4F4` count=1245 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1245
- ch1 `0x507` count=644 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x644
- ch1 `0x500` count=640 unique=1 first=`3C` last=`3C` top=3C x640
- ch1 `0x53E` count=637 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x637
- ch1 `0x4E6` count=629 unique=1 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x629
- ch1 `0x520` count=627 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x627
- ch1 `0x4E5` count=625 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x625
- ch1 `0x541` count=625 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x625
- ch1 `0x4E7` count=622 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x622
- ch1 `0x502` count=619 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x619
- ch1 `0x545` count=618 unique=1 first=`DA 17 00 7B 00 00 00 B0` last=`DA 17 00 7B 00 00 00 B0` top=DA 17 00 7B 00 00 00 B0 x618
- ch1 `0x549` count=612 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x612
- ch1 `0x556` count=608 unique=1 first=`00 73 00 00 00 27 00 00` last=`00 73 00 00 00 27 00 00` top=00 73 00 00 00 27 00 00 x608
- ch0 `0x44D` count=382 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x382
- ch0 `0x44B` count=356 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x356
- ch0 `0x1EB` count=354 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x354
- ch0 `0x15D` count=348 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x348
- ch0 `0x135` count=344 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x344
- ch0 `0x133` count=340 unique=1 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 82` top=00 00 00 00 00 00 00 82 x340
- ch0 `0x158` count=338 unique=1 first=`FF C0 00 00 00 00 00 00` last=`FF C0 00 00 00 00 00 00` top=FF C0 00 00 00 00 00 00 x338

### front_parking_sensors_button_on_off_5x_done
range lines 4829130..4988424; duration 83.2s; previous marker `passenger_seat_ventilation_levels_1_2_3_off_3cycles_done`
- ch1 `0x4F4` count=1201 unique=2 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 00` top=00 00 C0 00 00 00 00 00 x859; 00 00 C0 00 00 00 00 01 x342
- ch1 `0x545` count=597 unique=2 first=`DA 17 00 7B 00 00 00 B0` last=`DA 17 00 7B 00 00 00 AF` top=DA 17 00 7B 00 00 00 AF x341; DA 17 00 7B 00 00 00 B0 x256
- ch1 `0x556` count=594 unique=2 first=`00 73 00 00 00 27 00 00` last=`00 72 00 00 00 27 00 00` top=00 72 00 00 00 27 00 00 x348; 00 73 00 00 00 27 00 00 x246
- ch1 `0x5CE` count=601 unique=4 first=`15 2C 80 00 00 00 80 26` last=`12 2C 80 00 00 00 80 26` top=13 2C 80 00 00 00 80 26 x311; 14 2C 80 00 00 00 80 26 x252; 12 2C 80 00 00 00 80 26 x21
- ch1 `0x18F` count=6399 unique=2 first=`FA 4F 00 00 00 4A 00 00` last=`FA 4F 00 00 00 4B 00 00` top=FA 4F 00 00 00 4B 00 00 x3552; FA 4F 00 00 00 4A 00 00 x2847
- ch1 `0x5A0` count=66 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x33; 00 00 00 C0 20 05 A5 02 x33
- ch1 `0x111` count=6049 unique=4 first=`00 40 61 FF 61 00 00 C4` last=`00 40 61 FF 61 00 00 88` top=00 40 61 FF 61 00 00 C4 x1519; 00 40 61 FF 61 00 00 4C x1517; 00 40 61 FF 61 00 00 00 x1507
- ch1 `0x112` count=5998 unique=4 first=`FF 50 00 00 FF 00 00 00` last=`FF A0 00 00 FF 00 00 00` top=FF 50 00 00 FF 00 00 00 x1512; FF F0 00 00 FF 00 00 00 x1504; FF A0 00 00 FF 00 00 00 x1493
- ch1 `0x58B` count=1302 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1302
- ch1 `0x436` count=1267 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1267
- ch1 `0x490` count=1230 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1230
- ch1 `0x500` count=623 unique=1 first=`3C` last=`3C` top=3C x623
- ch1 `0x541` count=621 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x621
- ch1 `0x520` count=620 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x620
- ch1 `0x5CF` count=615 unique=1 first=`3F 0B 00 00 00 7F 80 00` last=`3F 0B 00 00 00 7F 80 00` top=3F 0B 00 00 00 7F 80 00 x615
- ch1 `0x4E5` count=605 unique=1 first=`80 00 00 0B 49 D0 00 00` last=`80 00 00 0B 49 D0 00 00` top=80 00 00 0B 49 D0 00 00 x605
- ch1 `0x4E6` count=604 unique=1 first=`80 80 00 00 80 80 7A 00` last=`80 80 00 00 80 80 7A 00` top=80 80 00 00 80 80 7A 00 x604
- ch1 `0x547` count=603 unique=1 first=`00 00 7D 3C AE 47 CD 18` last=`00 00 7D 3C AE 47 CD 18` top=00 00 7D 3C AE 47 CD 18 x603
- ch1 `0x502` count=599 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x599
- ch1 `0x4E7` count=598 unique=1 first=`00 79 00 08 00 81 00 00` last=`00 79 00 08 00 81 00 00` top=00 79 00 08 00 81 00 00 x598
- ch1 `0x507` count=597 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x597
- ch1 `0x53E` count=594 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x594
- ch1 `0x549` count=594 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x594
- ch0 `0x44B` count=358 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x358
- ch0 `0x44D` count=345 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x345
- ch0 `0x15D` count=340 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x340
- ch0 `0x56E` count=336 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x336
- ch0 `0x531` count=330 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x330
- ch0 `0x135` count=326 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x326
- ch0 `0x157` count=326 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x326

### parking_assist_button_3x_engine_off_rejected_cluster_message
range lines 4988424..5239726; duration 119.1s; previous marker `front_parking_sensors_button_on_off_5x_done`
- ch0 `0x158` count=481 unique=2 first=`FF C0 00 00 00 00 00 00` last=`FF C4 00 00 00 00 00 00` top=FF C4 00 00 00 00 00 00 x282; FF C0 00 00 00 00 00 00 x199
- ch1 `0x5A0` count=103 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 27 95 02 x53; 00 00 00 C0 20 05 A5 02 x50
- ch0 `0x5D7` count=87 unique=2 first=`04 20 00 00 00 12 80 B4` last=`04 10 00 00 00 12 80 B4` top=04 20 00 00 00 12 80 B4 x72; 04 10 00 00 00 12 80 B4 x15
- ch0 `0x169` count=478 unique=3 first=`20 01 00 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x277; 20 01 00 FF 14 00 40 00 x196; 20 01 01 FF 14 00 40 00 x5
- ch1 `0x547` count=954 unique=4 first=`00 00 7D 3C AE 47 CD 18` last=`00 00 7F 3C AF 47 CD 18` top=00 00 7D 3C AE 47 CD 18 x432; 00 00 7E 3C AF 47 CD 18 x278; 00 00 7D 3C AF 47 CD 18 x125
- ch1 `0x428` count=4855 unique=2 first=`05 00 00 80 60 08 00 00` last=`05 00 00 80 60 08 02 00` top=05 00 00 80 60 08 02 00 x2786; 05 00 00 80 60 08 00 00 x2069
- ch1 `0x429` count=4851 unique=2 first=`B5 03 00 00 00 00 20 03` last=`B5 03 00 00 00 00 2F 03` top=B5 03 00 00 00 00 2F 03 x2783; B5 03 00 00 00 00 20 03 x2068
- ch0 `0x44B` count=527 unique=2 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x523; 4D 12 00 00 00 00 FF FF x4
- ch1 `0x040` count=503 unique=2 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x495; 00 00 02 0C 02 00 00 00 x8
- ch1 `0x553` count=481 unique=2 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x476; 00 00 00 08 01 01 C0 00 x5
- ch0 `0x132` count=110 unique=2 first=`90 00 00 28 00 00 00 00` last=`90 00 00 28 00 00 00 00` top=90 00 00 28 00 00 00 00 x106; 90 00 00 70 00 00 00 00 x4
- ch0 `0x134` count=106 unique=2 first=`03 00 00 02 00 00 10 00` last=`03 00 00 02 00 00 10 00` top=03 00 00 02 00 00 10 00 x102; 00 00 00 02 00 00 30 00 x4
- ch1 `0x043` count=104 unique=2 first=`00 00 00 04 03 00 00 00` last=`00 00 00 04 03 00 00 00` top=00 00 00 04 03 00 00 00 x100; 00 00 00 0E 00 00 00 00 x4
- ch1 `0x113` count=9811 unique=4 first=`00 20 00 80 00 00 00 E8` last=`00 20 00 80 00 00 00 60` top=00 20 00 80 00 00 00 AC x2477; 00 20 00 80 00 00 00 24 x2462; 00 20 00 80 00 00 00 E8 x2438
- ch1 `0x4F4` count=1906 unique=8 first=`00 00 C0 00 00 00 00 00` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 00 x981; 24 02 C0 00 00 00 00 00 x255; 25 03 C0 00 00 00 00 01 x186
- ch1 `0x541` count=971 unique=4 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x961; 04 00 41 00 C0 10 00 01 x7; 03 00 41 00 C0 10 00 01 x2
- ch1 `0x18F` count=9857 unique=7 first=`FA 4F 00 00 00 4B 00 00` last=`FA 4E 00 00 00 4A 00 20` top=FA 4E 00 00 00 4B 00 00 x3848; FA 4E 00 00 00 4B 00 20 x1560; FA 4E 00 00 00 4C 00 20 x1399
- ch1 `0x58B` count=1992 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1992
- ch1 `0x436` count=1969 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1969
- ch1 `0x490` count=1935 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1935
- ch1 `0x520` count=973 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x973
- ch1 `0x53E` count=969 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x969
- ch1 `0x549` count=969 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x969
- ch1 `0x500` count=968 unique=1 first=`3C` last=`3C` top=3C x968
- ch1 `0x502` count=967 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x967
- ch1 `0x507` count=950 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x950
- ch0 `0x44D` count=572 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x572
- ch0 `0x135` count=512 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x512
- ch1 `0x52A` count=510 unique=1 first=`00 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x510
- ch1 `0x544` count=502 unique=1 first=`FF 37 FF FF 00 00 00 00` last=`FF 37 FF FF 00 00 00 00` top=FF 37 FF FF 00 00 00 00 x502

### engine_started_parking_exit_assist_5x_done
range lines 5239726..5239777; duration 0.0s; previous marker `parking_assist_button_3x_engine_off_rejected_cluster_message`
- ch1 `0x200` count=3 unique=2 first=`5A 00 6F 00 00 00` last=`00 00 6F 00 00 00` top=00 00 6F 00 00 00 x2; 5A 00 6F 00 00 00 x1
- ch1 `0x111` count=2 unique=2 first=`00 40 61 FF 60 DB 0A B4` last=`00 40 61 FF 60 DB 0A 78` top=00 40 61 FF 60 DB 0A B4 x1; 00 40 61 FF 60 DB 0A 78 x1
- ch1 `0x112` count=2 unique=2 first=`FF 50 00 00 FF DB 0A 00` last=`FF A0 00 00 FF DB 0A 00` top=FF 50 00 00 FF DB 0A 00 x1; FF A0 00 00 FF DB 0A 00 x1
- ch1 `0x113` count=2 unique=2 first=`00 20 00 80 00 00 00 24` last=`00 20 00 80 00 00 00 E8` top=00 20 00 80 00 00 00 24 x1; 00 20 00 80 00 00 00 E8 x1
- ch1 `0x153` count=2 unique=2 first=`20 00 10 FF 00 FF 00 2E` last=`20 00 10 FF 00 FF 10 3E` top=20 00 10 FF 00 FF 00 2E x1; 20 00 10 FF 00 FF 10 3E x1
- ch1 `0x164` count=2 unique=2 first=`00 08 02 0A` last=`00 08 04 0C` top=00 08 02 0A x1; 00 08 04 0C x1
- ch1 `0x220` count=2 unique=2 first=`07 C4 7F 00 00 57 10 18` last=`05 64 7F 00 00 4A 10 2B` top=07 C4 7F 00 00 57 10 18 x1; 05 64 7F 00 00 4A 10 2B x1
- ch1 `0x251` count=2 unique=2 first=`12 04 46 32 00 0E 48 80` last=`10 04 47 2F 00 0C 48 80` top=12 04 46 32 00 0E 48 80 x1; 10 04 47 2F 00 0C 48 80 x1
- ch1 `0x2B0` count=2 unique=2 first=`AB 0D 02 07 B2` last=`AB 0D 00 07 83` top=AB 0D 02 07 B2 x1; AB 0D 00 07 83 x1
- ch1 `0x340` count=2 unique=2 first=`04 00 00 24 74 00 AD 11` last=`04 00 00 24 84 00 BD 11` top=04 00 00 24 74 00 AD 11 x1; 04 00 00 24 84 00 BD 11 x1
- ch1 `0x260` count=3 unique=3 first=`0E 1A 1A 30 00 B9 70 2C` last=`0E 1A 1A 30 00 B9 71 0D` top=0E 1A 1A 30 00 B9 70 2C x1; 0E 1A 1A 30 00 B9 71 3A x1; 0E 1A 1A 30 00 B9 71 0D x1
- ch1 `0x316` count=3 unique=1 first=`05 1A CC 0B 1A 18 00 77` last=`05 1A CC 0B 1A 18 00 77` top=05 1A CC 0B 1A 18 00 77 x3
- ch1 `0x329` count=3 unique=1 first=`86 A5 7F 10 11 20 00 18` last=`86 A5 7F 10 11 20 00 18` top=86 A5 7F 10 11 20 00 18 x3
- ch1 `0x18F` count=2 unique=1 first=`FA 4E 00 00 00 4A 00 20` last=`FA 4E 00 00 00 4A 00 20` top=FA 4E 00 00 00 4A 00 20 x2
- ch1 `0x1BF` count=2 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x2

### auto_hold_on_off_5x_done
range lines 5239777..5373890; duration 71.5s; previous marker `engine_started_parking_exit_assist_5x_done`
- ch1 `0x547` count=512 unique=2 first=`00 00 7F 3C AF 47 CD 18` last=`00 00 80 3C AF 47 CD 18` top=00 00 80 3C AF 47 CD 18 x303; 00 00 7F 3C AF 47 CD 18 x209
- ch1 `0x4E6` count=494 unique=3 first=`49 C2 00 00 84 84 8B 00` last=`49 C4 00 00 84 84 8B 00` top=49 C4 00 00 84 84 8B 00 x269; 49 C3 00 00 84 84 8B 00 x207; 49 C2 00 00 84 84 8B 00 x18
- ch1 `0x4E5` count=510 unique=4 first=`80 4E 00 02 49 C2 00 00` last=`80 4A 00 02 49 C4 00 00` top=80 4A 00 02 49 C4 00 00 x196; 80 4E 00 02 49 C4 00 00 x173; 80 4E 00 02 49 C3 00 00 x114
- ch1 `0x507` count=483 unique=2 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x369; 00 00 00 60 x114
- ch0 `0x169` count=297 unique=2 first=`20 01 03 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x228; 20 01 03 FF 14 03 40 00 x69
- ch1 `0x5A0` count=54 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x27; 00 00 00 C0 20 27 95 02 x27
- ch1 `0x18F` count=5260 unique=4 first=`FA 4E 00 00 00 4A 00 20` last=`FA 4E 00 00 00 47 00 20` top=FA 4E 00 00 00 48 00 20 x1996; FA 4E 00 00 00 47 00 20 x1886; FA 4E 00 00 00 49 00 20 x1121
- ch1 `0x492` count=1013 unique=4 first=`00 00 00 00 9C 80 73 90` last=`00 00 00 00 9C 80 73 90` top=00 00 00 00 9C 80 73 90 x265; 00 00 00 00 9C 80 73 DC x258; 00 00 00 00 9C 80 73 54 x251
- ch1 `0x436` count=1012 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1012
- ch1 `0x4F4` count=1001 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1001
- ch1 `0x58B` count=998 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x998
- ch1 `0x490` count=997 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x997
- ch1 `0x500` count=526 unique=1 first=`3C` last=`3C` top=3C x526
- ch1 `0x549` count=512 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x512
- ch1 `0x53E` count=504 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x504
- ch1 `0x502` count=502 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x502
- ch1 `0x520` count=501 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x501
- ch1 `0x541` count=500 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x500
- ch0 `0x44B` count=297 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x297
- ch0 `0x44D` count=276 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x276
- ch0 `0x135` count=273 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x273
- ch0 `0x158` count=273 unique=1 first=`FF C4 00 00 00 00 00 00` last=`FF C4 00 00 00 00 00 00` top=FF C4 00 00 00 00 00 00 x273
- ch0 `0x157` count=272 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x272
- ch0 `0x15D` count=270 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x270
- ch0 `0x1DF` count=266 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x266
- ch1 `0x52A` count=265 unique=1 first=`00 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x265
- ch0 `0x1EB` count=263 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x263
- ch0 `0x56E` count=262 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x262
- ch0 `0x531` count=261 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x261
- ch0 `0x133` count=256 unique=1 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 82` top=00 00 00 00 00 00 00 82 x256

### drive_mode_button_5x_done
range lines 5373890..5556351; duration 84.9s; previous marker `auto_hold_on_off_5x_done`
- ch1 `0x547` count=712 unique=2 first=`00 00 80 3C AF 47 CD 18` last=`00 00 81 3C AF 47 CD 18` top=00 00 81 3C AF 47 CD 18 x378; 00 00 80 3C AF 47 CD 18 x334
- ch1 `0x5A0` count=75 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 05 A5 02 x38; 00 00 00 C0 20 27 95 02 x37
- ch1 `0x4E5` count=713 unique=3 first=`80 4A 00 02 49 C4 00 00` last=`80 46 00 01 49 C4 00 00` top=80 4A 00 02 49 C4 00 00 x411; 80 4A 00 01 49 C4 00 00 x264; 80 46 00 01 49 C4 00 00 x38
- ch1 `0x18F` count=7207 unique=4 first=`FA 4E 00 00 00 47 00 20` last=`FA 4F 00 00 00 46 00 20` top=FA 4E 00 00 00 47 00 20 x3718; FA 4E 00 00 00 46 00 20 x2825; FA 4F 00 00 00 46 00 20 x634
- ch1 `0x4E6` count=713 unique=3 first=`49 C4 00 00 84 84 8B 00` last=`49 C4 00 00 84 84 8B 00` top=49 C4 00 00 84 84 8B 00 x630; 49 C4 00 00 84 84 8C 00 x81; 49 C4 00 00 83 83 8B 00 x2
- ch1 `0x4F4` count=1424 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1424
- ch1 `0x58B` count=1421 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1421
- ch1 `0x490` count=1415 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1415
- ch1 `0x436` count=1402 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1402
- ch1 `0x549` count=717 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x717
- ch1 `0x502` count=712 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x712
- ch1 `0x541` count=707 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x707
- ch1 `0x500` count=705 unique=1 first=`3C` last=`3C` top=3C x705
- ch1 `0x507` count=699 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x699
- ch1 `0x520` count=698 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x698
- ch1 `0x53E` count=690 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x690
- ch0 `0x44D` count=364 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x364
- ch0 `0x44B` count=363 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x363
- ch0 `0x17B` count=362 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x362
- ch0 `0x158` count=360 unique=1 first=`FF C4 00 00 00 00 00 00` last=`FF C4 00 00 00 00 00 00` top=FF C4 00 00 00 00 00 00 x360
- ch1 `0x040` count=359 unique=1 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x359
- ch0 `0x157` count=359 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x359
- ch0 `0x1DF` count=358 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x358
- ch0 `0x531` count=358 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x358
- ch0 `0x169` count=357 unique=1 first=`20 01 03 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x357
- ch0 `0x133` count=356 unique=1 first=`00 00 00 00 00 00 00 82` last=`00 00 00 00 00 00 00 82` top=00 00 00 00 00 00 00 82 x356
- ch1 `0x50A` count=355 unique=1 first=`0B 00 00 00 00 00 00 00` last=`0B 00 00 00 00 00 00 00` top=0B 00 00 00 00 00 00 00 x355
- ch0 `0x15D` count=353 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x353
- ch1 `0x52A` count=353 unique=1 first=`00 00 00 00 3E 00 00 00` last=`00 00 00 00 3E 00 00 00` top=00 00 00 00 3E 00 00 00 x353
- ch1 `0x410` count=352 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x352

### drive_lock_button_5x_done
range lines 5556351..5705440; duration 73.3s; previous marker `drive_mode_button_5x_done`
- ch1 `0x5A0` count=57 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 05 A5 02 x30; 00 00 00 C0 20 27 95 02 x27
- ch1 `0x4E5` count=570 unique=4 first=`80 4A 00 01 49 C4 00 00` last=`80 46 00 01 49 C4 00 00` top=80 46 00 01 49 C4 00 00 x474; 80 4A 00 02 49 C4 00 00 x63; 80 4A 00 01 49 C4 00 00 x21
- ch1 `0x18F` count=5787 unique=3 first=`FA 4F 00 00 00 46 00 20` last=`FA 51 00 00 00 46 00 20` top=FA 50 00 00 00 46 00 20 x4235; FA 4F 00 00 00 46 00 20 x1479; FA 51 00 00 00 46 00 20 x73
- ch1 `0x113` count=5840 unique=4 first=`00 20 00 80 00 00 00 AC` last=`00 20 00 80 00 00 00 24` top=00 20 00 80 00 00 00 24 x1474; 00 20 00 80 00 00 00 60 x1462; 00 20 00 80 00 00 00 AC x1458
- ch1 `0x492` count=1145 unique=8 first=`00 00 00 00 9E 80 73 34` last=`00 00 00 00 A0 80 73 C8` top=00 00 00 00 9E 80 73 34 x163; 00 00 00 00 9E 80 73 70 x156; 00 00 00 00 9E 80 73 F8 x153
- ch1 `0x428` count=2951 unique=2 first=`05 00 00 80 60 08 02 00` last=`05 00 00 80 60 08 02 00` top=05 00 00 80 60 08 02 00 x2182; 05 00 00 80 60 10 02 00 x769
- ch1 `0x436` count=1213 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1213
- ch1 `0x58B` count=1172 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1172
- ch1 `0x4F4` count=1147 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1147
- ch1 `0x490` count=1141 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1141
- ch1 `0x520` count=590 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x590
- ch1 `0x541` count=586 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x586
- ch1 `0x500` count=583 unique=1 first=`3C` last=`3C` top=3C x583
- ch1 `0x53E` count=577 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x577
- ch1 `0x507` count=571 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x571
- ch1 `0x502` count=570 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x570
- ch1 `0x547` count=557 unique=1 first=`00 00 81 3C AF 47 CD 18` last=`00 00 81 3C AF 47 CD 18` top=00 00 81 3C AF 47 CD 18 x557
- ch1 `0x549` count=556 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x556
- ch1 `0x410` count=315 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x315
- ch0 `0x531` count=313 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x313
- ch0 `0x169` count=312 unique=1 first=`20 01 03 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x312
- ch0 `0x1DF` count=308 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x308
- ch1 `0x040` count=305 unique=1 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x305
- ch1 `0x50E` count=304 unique=1 first=`20 00 00 20 00 00 00 00` last=`20 00 00 20 00 00 00 00` top=20 00 00 20 00 00 00 00 x304
- ch0 `0x158` count=301 unique=1 first=`FF C4 00 00 00 00 00 00` last=`FF C4 00 00 00 00 00 00` top=FF C4 00 00 00 00 00 00 x301
- ch0 `0x56E` count=301 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x301
- ch0 `0x44D` count=298 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x298
- ch1 `0x553` count=298 unique=1 first=`00 00 00 08 01 00 C0 00` last=`00 00 00 08 01 00 C0 00` top=00 00 00 08 01 00 C0 00 x298
- ch0 `0x157` count=296 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x296
- ch1 `0x559` count=296 unique=1 first=`00 40 00 00 20 00 00 40` last=`00 40 00 00 20 00 00 40` top=00 40 00 00 20 00 00 40 x296

### hill_descent_button_5x_done
range lines 5705440..5837288; duration 68.0s; previous marker `drive_lock_button_5x_done`
- ch1 `0x547` count=487 unique=2 first=`00 00 81 3C AF 47 CD 18` last=`00 00 82 3C AF 47 CD 18` top=00 00 82 3C AF 47 CD 18 x428; 00 00 81 3C AF 47 CD 18 x59
- ch1 `0x4E5` count=499 unique=4 first=`80 46 00 01 49 C4 00 00` last=`80 4A 00 02 49 C4 00 00` top=80 46 00 01 49 C4 00 00 x464; 80 4A 00 02 49 C4 00 00 x17; 80 4A 00 01 49 C4 00 00 x14
- ch1 `0x507` count=528 unique=2 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x412; 40 00 00 01 x116
- ch1 `0x5A0` count=48 unique=2 first=`00 00 00 C0 20 05 A5 02` last=`00 00 00 C0 20 05 A5 02` top=00 00 00 C0 20 05 A5 02 x24; 00 00 00 C0 20 27 95 02 x24
- ch1 `0x18F` count=5243 unique=4 first=`FA 51 00 00 00 46 00 20` last=`FA 53 00 00 00 45 00 20` top=FA 51 00 00 00 46 00 20 x1635; FA 53 00 00 00 45 00 20 x1430; FA 52 00 00 00 46 00 20 x1112
- ch1 `0x113` count=5135 unique=4 first=`00 20 00 80 00 00 00 E8` last=`00 20 00 80 00 00 00 24` top=00 20 00 80 00 00 00 60 x1288; 00 20 00 80 00 00 00 AC x1286; 00 20 00 80 00 00 00 24 x1286
- ch1 `0x492` count=1003 unique=8 first=`00 00 00 00 A0 80 73 8C` last=`00 00 00 00 A1 80 73 30` top=00 00 00 00 A0 80 73 C8 x173; 00 00 00 00 A0 80 73 04 x166; 00 00 00 00 A0 80 73 8C x159
- ch1 `0x200` count=5158 unique=6 first=`52 00 6F 00 00 00` last=`00 00 6F 00 00 00` top=00 00 6F 00 00 00 x3956; 51 00 6F 00 00 00 x627; 52 00 6F 00 00 00 x254
- ch1 `0x58B` count=1041 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x1041
- ch1 `0x436` count=1020 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x1020
- ch1 `0x4F4` count=1003 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x1003
- ch1 `0x490` count=1001 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x1001
- ch1 `0x500` count=514 unique=1 first=`3C` last=`3C` top=3C x514
- ch1 `0x502` count=503 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x503
- ch1 `0x53E` count=497 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x497
- ch1 `0x520` count=492 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x492
- ch1 `0x549` count=492 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x492
- ch1 `0x541` count=489 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x489
- ch0 `0x44D` count=296 unique=1 first=`4B 02 00 00 00 00 FF FF` last=`4B 02 00 00 00 00 FF FF` top=4B 02 00 00 00 00 FF FF x296
- ch0 `0x1DF` count=280 unique=1 first=`38 FF 00 00 03 20 8F 00` last=`38 FF 00 00 03 20 8F 00` top=38 FF 00 00 03 20 8F 00 x280
- ch0 `0x16A` count=267 unique=1 first=`01 00 00 00 00 00 00 C3` last=`01 00 00 00 00 00 00 C3` top=01 00 00 00 00 00 00 C3 x267
- ch1 `0x040` count=265 unique=1 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x265
- ch0 `0x15D` count=265 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x265
- ch0 `0x169` count=265 unique=1 first=`20 01 03 FF 14 00 40 00` last=`20 01 03 FF 14 00 40 00` top=20 01 03 FF 14 00 40 00 x265
- ch0 `0x531` count=264 unique=1 first=`73 00 FF 00 00 00 00 00` last=`73 00 FF 00 00 00 00 00` top=73 00 FF 00 00 00 00 00 x264
- ch0 `0x135` count=262 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x262
- ch0 `0x157` count=258 unique=1 first=`02 00 00 00 00 00 00 00` last=`02 00 00 00 00 00 00 00` top=02 00 00 00 00 00 00 00 x258
- ch0 `0x44B` count=258 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x258
- ch0 `0x56E` count=258 unique=1 first=`00 00 00 00 FF E0 00 00` last=`00 00 00 00 FF E0 00 00` top=00 00 00 00 FF E0 00 00 x258
- ch1 `0x410` count=257 unique=1 first=`E4 1A 42 18 82 07 00 20` last=`E4 1A 42 18 82 07 00 20` top=E4 1A 42 18 82 07 00 20 x257

### final_baseline_start_15s
range lines 5837288..5837308; duration 0.0s; previous marker `hill_descent_button_5x_done`
- ch1 `0x251` count=2 unique=2 first=`10 04 95 7D 00 0C 48 80` last=`10 04 96 7E 00 0C 48 80` top=10 04 95 7D 00 0C 48 80 x1; 10 04 96 7E 00 0C 48 80 x1
- ch1 `0x260` count=2 unique=2 first=`0C 18 18 30 00 B6 7A 2B` last=`0C 18 18 30 00 B6 79 3B` top=0C 18 18 30 00 B6 7A 2B x1; 0C 18 18 30 00 B6 79 3B x1
- ch1 `0x2B0` count=2 unique=2 first=`AA 0D 02 07 91` last=`AB 0D 02 07 B2` top=AA 0D 02 07 91 x1; AB 0D 02 07 B2 x1
- ch1 `0x316` count=2 unique=1 first=`05 18 FC 0A 18 16 00 76` last=`05 18 FC 0A 18 16 00 76` top=05 18 FC 0A 18 16 00 76 x2
- ch1 `0x329` count=2 unique=1 first=`0F AE 7F 10 11 20 00 18` last=`0F AE 7F 10 11 20 00 18` top=0F AE 7F 10 11 20 00 18 x2

### final_baseline_end_stop_capture_next
range lines 5837308..5862442; duration 15.1s; previous marker `final_baseline_start_15s`
- ch1 `0x4E5` count=93 unique=3 first=`80 46 00 02 49 C4 00 00` last=`80 46 00 01 49 C4 00 00` top=80 46 00 01 49 C4 00 00 x44; 80 46 00 02 49 C4 00 00 x32; 80 4A 00 02 49 C4 00 00 x17
- ch1 `0x113` count=970 unique=4 first=`00 20 00 80 00 00 00 E8` last=`00 20 00 80 00 00 00 AC` top=00 20 00 80 00 00 00 AC x249; 00 20 00 80 00 00 00 E8 x244; 00 20 00 80 00 00 00 60 x242
- ch1 `0x492` count=190 unique=4 first=`00 00 00 00 A1 80 73 7C` last=`00 00 00 00 A1 80 73 B8` top=00 00 00 00 A1 80 73 7C x56; 00 00 00 00 A1 80 73 B8 x53; 00 00 00 00 A1 80 73 30 x41
- ch1 `0x4E6` count=92 unique=5 first=`49 C4 00 00 83 83 8C 00` last=`49 C4 00 00 83 83 8B 00` top=49 C4 00 00 83 83 8C 00 x40; 49 C4 00 00 83 83 8B 00 x40; 49 C4 00 00 84 84 8B 00 x6
- ch1 `0x557` count=91 unique=6 first=`80 85 00 00 1D 97 01 00` last=`80 85 00 00 1D 98 01 00` top=80 85 00 00 1D 97 01 00 x43; 80 85 00 00 1D 98 01 00 x31; 80 85 00 00 1D 99 01 00 x8
- ch1 `0x18F` count=1135 unique=2 first=`FA 53 00 00 00 45 00 20` last=`FA 53 00 00 00 45 00 20` top=FA 53 00 00 00 45 00 20 x1126; FA 54 00 00 00 45 00 20 x9
- ch1 `0x5A0` count=13 unique=2 first=`00 00 00 C0 20 27 95 02` last=`00 00 00 C0 20 27 95 02` top=00 00 00 C0 20 27 95 02 x7; 00 00 00 C0 20 05 A5 02 x6
- ch1 `0x329` count=921 unique=4 first=`0F AE 7F 10 11 20 00 18` last=`0F AE 7F 10 11 20 00 18` top=0F AE 7F 10 11 20 00 18 x248; 40 AE 7F 10 11 20 00 18 x226; E2 AE 7F 10 11 20 00 18 x224
- ch1 `0x200` count=995 unique=5 first=`00 00 6F 00 00 00` last=`00 00 6F 00 00 00` top=00 00 6F 00 00 00 x752; 51 00 6F 00 00 00 x109; 50 00 6F 00 00 00 x95
- ch1 `0x1BF` count=1019 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x1019
- ch1 `0x428` count=516 unique=1 first=`05 00 00 80 60 08 02 00` last=`05 00 00 80 60 08 02 00` top=05 00 00 80 60 08 02 00 x516
- ch1 `0x429` count=480 unique=1 first=`B5 03 00 00 00 00 2F 03` last=`B5 03 00 00 00 00 2F 03` top=B5 03 00 00 00 00 2F 03 x480
- ch1 `0x387` count=479 unique=1 first=`09 0E 00 00 00 17` last=`09 0E 00 00 00 17` top=09 0E 00 00 00 17 x479
- ch1 `0x4F4` count=192 unique=1 first=`00 00 C0 00 00 00 00 01` last=`00 00 C0 00 00 00 00 01` top=00 00 C0 00 00 00 00 01 x192
- ch1 `0x58B` count=188 unique=1 first=`11 00 04 00 00 00 00 20` last=`11 00 04 00 00 00 00 20` top=11 00 04 00 00 00 00 20 x188
- ch1 `0x436` count=183 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x183
- ch1 `0x490` count=181 unique=1 first=`01 00 10 20 00 00 00` last=`01 00 10 20 00 00 00` top=01 00 10 20 00 00 00 x181
- ch1 `0x500` count=99 unique=1 first=`3C` last=`3C` top=3C x99
- ch1 `0x507` count=97 unique=1 first=`00 00 00 01` last=`00 00 00 01` top=00 00 00 01 x97
- ch1 `0x53E` count=94 unique=1 first=`00 08 00 00 80 0E` last=`00 08 00 00 80 0E` top=00 08 00 00 80 0E x94
- ch1 `0x520` count=93 unique=1 first=`00 00 00 00 00 00 00 00` last=`00 00 00 00 00 00 00 00` top=00 00 00 00 00 00 00 00 x93
- ch1 `0x547` count=93 unique=1 first=`00 00 82 3C AF 47 CD 18` last=`00 00 82 3C AF 47 CD 18` top=00 00 82 3C AF 47 CD 18 x93
- ch1 `0x541` count=92 unique=1 first=`03 00 41 00 C0 00 00 01` last=`03 00 41 00 C0 00 00 01` top=03 00 41 00 C0 00 00 01 x92
- ch1 `0x549` count=92 unique=1 first=`FF FF FF FF FF FF FF 7F` last=`FF FF FF FF FF FF FF 7F` top=FF FF FF FF FF FF FF 7F x92
- ch1 `0x502` count=90 unique=1 first=`00 00 00 00` last=`00 00 00 00` top=00 00 00 00 x90
- ch0 `0x44B` count=68 unique=1 first=`4D 02 00 00 00 00 FF FF` last=`4D 02 00 00 00 00 FF FF` top=4D 02 00 00 00 00 FF FF x68
- ch0 `0x1EB` count=64 unique=1 first=`00 08 00 00 00 00 00 00` last=`00 08 00 00 00 00 00 00` top=00 08 00 00 00 00 00 00 x64
- ch0 `0x135` count=63 unique=1 first=`00 00 00 00 08 00 03 00` last=`00 00 00 00 08 00 03 00` top=00 00 00 00 08 00 03 00 x63
- ch0 `0x15D` count=62 unique=1 first=`00 00 00 00 00 70 C0 F3` last=`00 00 00 00 00 70 C0 F3` top=00 00 00 00 00 70 C0 F3 x62
- ch1 `0x040` count=60 unique=1 first=`00 00 22 04 02 00 00 00` last=`00 00 22 04 02 00 00 00` top=00 00 22 04 02 00 00 00 x60

## Parking/SPAS Extract
# Parking / SPAS Log Analysis

Source: `logs/session_20260507/full_gsusb_20260507_132119.txt`

Note: dashboard-only walk after this capture was not raw-saved; full analysis below uses the 319 MB raw log from the main session.

## Conclusions

- `0x436 PAS11` is present at about 20 Hz, but in every captured parking/reverse
  segment it stayed `00 00 00 00`. We did not capture a real obstacle-distance
  event on this frame.
- `0x4F4 SPAS12` is the strongest visible parking-display candidate. It toggles
  with front parking sensor button presses and with reverse:
  - idle/on-like: `00 00 C0 00 00 00 00 01`
  - off-like: `00 00 C0 00 00 00 00 00`
  - reverse transition: `00 01 C0 00 00 00 30 01` then
    `00 01 C0 00 00 00 00 01`
  - parking assist/rejected/active path: `24 02 C0 ...`,
    `25/26/27 03 C0 ...`, and transient `... 90 01`.
- `0x390 SPAS11` changes continuously and is mixed with steering/SPAS status.
  It is useful for dynamic reverse lines and SPAS status, but the current log
  does not isolate obstacle zones.
- `0x58B LCA11/RCTA` stayed fixed at `11 00 04 00 00 00 00 20` in all parking
  segments, so no RCTA/side warning event was captured.
- The latest dashboard-only test confirms the front/rear walk without R had no
  visible response. Because that live run was not raw-saved by the dashboard,
  it is useful only as a negative observation, not as full byte-level evidence.

## Implementation Notes

- For the adapter firmware, do not depend only on `0x436`; treat `0x4F4` as the
  current display/on-off/SPAS indicator source.
- For camera/reverse activation, combine gear candidates `0x169`, `0x50E`,
  `0x53E`, and `0x111` until exact gear bits are isolated.
- To implement parking overlay later, we still need one clean obstacle log with
  R active and an actual obstacle at rear-left/rear-center/rear-right and
  front-left/front-center/front-right. Without that, we can mirror enable/status
  but cannot map distance bars reliably.

