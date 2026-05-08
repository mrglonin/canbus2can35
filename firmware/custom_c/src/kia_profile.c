#include "kia_profile.h"

#include <stddef.h>

static kia_vehicle_state_t vehicle_state;

static void observe_c_can(uint32_t id, const uint8_t *d, uint8_t len)
{
	if (id == 0x541U && len >= 8U) {
		vehicle_state.ignition = (d[0] & 0x03U) != 0U;
		vehicle_state.left_front_door = (d[1] & 0x01U) != 0U;
		vehicle_state.right_front_door = (d[4] & 0x08U) != 0U;
		vehicle_state.trunk = (d[1] & 0x10U) != 0U;
		vehicle_state.hood = (d[2] & 0x02U) != 0U;
		vehicle_state.sunroof_open = ((d[0] & 0x02U) != 0U) || ((d[7] & 0x02U) != 0U);
	}
	if (id == 0x553U && len >= 8U) {
		vehicle_state.left_rear_door = (d[3] & 0x01U) != 0U;
		vehicle_state.right_rear_door = (d[2] & 0x80U) != 0U;
	}
	if (id == 0x111U && len >= 8U) {
		vehicle_state.reverse = d[4] == 0x64U;
	}
	if (id == 0x169U && len >= 8U) {
		vehicle_state.reverse = (d[0] & 0x0fU) == 0x07U;
	}
	if (id == 0x316U && len >= 8U) {
		vehicle_state.vehicle_speed_x10 = (uint16_t)(((uint16_t)d[2] << 8) | d[3]);
	}
	if (id == 0x383U && len >= 8U) {
		vehicle_state.outside_temp_c = (int8_t)d[4] - 40;
	}
	if (id == 0x559U && len >= 8U) {
		vehicle_state.steering_wheel_heater = (d[0] & 0x10U) != 0U ? 1U : 0U;
	}
}

void kia_profile_init(void)
{
}

void kia_profile_observe(can_bus_t bus, const can_frame_t *frame)
{
	if (frame == 0) {
		return;
	}
	if (bus == CANBUS_C) {
		observe_c_can(frame->id, frame->data, frame->len);
	}
}

const kia_vehicle_state_t *kia_profile_vehicle_state(void)
{
	return &vehicle_state;
}

bool kia_profile_send_raw(can_bus_t bus, uint32_t id, const uint8_t *data, uint8_t len)
{
	can_frame_t frame = {
		.id = id,
		.len = len > 8U ? 8U : len,
		.ext = false,
		.rtr = false,
		.data = {0},
	};

	for (uint8_t i = 0; i < frame.len; i++) {
		frame.data[i] = data[i];
	}
	return canbus_send(bus, &frame);
}
