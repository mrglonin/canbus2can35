#pragma once

#include <stdbool.h>
#include <stdint.h>

#include "kia_profile.h"

typedef void (*teyes_raise_uart_rx_fn_t)(const uint8_t *data, uint8_t len);

void teyes_raise_uart_init(void);
void teyes_raise_uart_poll(const kia_vehicle_state_t *state);
bool teyes_raise_uart_ready(void);
void teyes_raise_uart_send_raw(const uint8_t *data, uint8_t len);
void teyes_raise_uart_set_rx_callback(teyes_raise_uart_rx_fn_t cb);
