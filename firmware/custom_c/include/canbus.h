#pragma once

#include <stdbool.h>
#include <stdint.h>

typedef enum {
	CANBUS_C = 0,
	CANBUS_M = 1,
	CANBUS_COUNT = 2,
} can_bus_t;

typedef struct {
	uint32_t id;
	uint8_t len;
	uint8_t data[8];
	bool ext;
	bool rtr;
} can_frame_t;

typedef enum {
	CAN_SPEED_100K = 3,
	CAN_SPEED_125K = 4,
	CAN_SPEED_250K = 5,
	CAN_SPEED_500K = 6,
	CAN_SPEED_1000K = 8,
} can_speed_code_t;

typedef void (*can_rx_callback_t)(can_bus_t bus, const can_frame_t *frame);

void canbus_set_rx_callback(can_rx_callback_t cb);
void canbus_init(void);
bool canbus_ready(can_bus_t bus);
void canbus_set_speed(can_bus_t bus, can_speed_code_t speed);
can_speed_code_t canbus_get_speed(can_bus_t bus);
bool canbus_send(can_bus_t bus, const can_frame_t *frame);
void canbus_poll(void);
