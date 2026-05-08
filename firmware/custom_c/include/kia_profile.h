#pragma once

#include <stdbool.h>
#include <stdint.h>

#include "canbus.h"

typedef struct {
	bool left_front_door;
	bool right_front_door;
	bool left_rear_door;
	bool right_rear_door;
	bool trunk;
	bool hood;
	bool sunroof_open;
	bool reverse;
	bool ignition;
	uint16_t vehicle_speed_x10;
	int8_t outside_temp_c;
	uint8_t steering_wheel_heater;
	uint8_t driver_seat_heat;
	uint8_t passenger_seat_heat;
} kia_vehicle_state_t;

void kia_profile_init(void);
void kia_profile_observe(can_bus_t bus, const can_frame_t *frame);
const kia_vehicle_state_t *kia_profile_vehicle_state(void);

bool kia_profile_send_raw(can_bus_t bus, uint32_t id, const uint8_t *data, uint8_t len);
