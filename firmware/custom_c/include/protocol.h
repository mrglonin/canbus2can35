#pragma once

#include <stdbool.h>
#include <stdint.h>

typedef void (*protocol_write_fn_t)(const uint8_t *data, uint16_t len);

void protocol_init(protocol_write_fn_t write_fn);
void protocol_rx_bytes(const uint8_t *data, uint16_t len);
void protocol_poll(void);
void protocol_emit_text(const char *s);
void protocol_emit_can(uint8_t bus, uint32_t id, bool ext, bool rtr, uint8_t len, const uint8_t *data);
void protocol_emit_uart_rx(const uint8_t *data, uint8_t len);
