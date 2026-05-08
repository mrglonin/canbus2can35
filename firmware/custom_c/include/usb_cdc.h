#pragma once

#include <stdbool.h>
#include <stdint.h>

typedef void (*usb_rx_fn_t)(const uint8_t *data, uint16_t len);

void usb_cdc_init(usb_rx_fn_t rx_fn);
void usb_cdc_poll(void);
bool usb_cdc_ready(void);
void usb_cdc_write(const uint8_t *data, uint16_t len);
uint32_t usb_cdc_dropped(void);
