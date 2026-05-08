#include <stdint.h>

#include "board.h"
#include "canbus.h"
#include "kia_profile.h"
#include "protocol.h"
#include "teyes_raise_uart.h"
#include "usb_cdc.h"

static void usb_rx(const uint8_t *data, uint16_t len)
{
	protocol_rx_bytes(data, len);
}

static void usb_write(const uint8_t *data, uint16_t len)
{
	usb_cdc_write(data, len);
}

static void can_rx(can_bus_t bus, const can_frame_t *frame)
{
	kia_profile_observe(bus, frame);
	protocol_emit_can((uint8_t)bus, frame->id, frame->ext, frame->rtr, frame->len, frame->data);
}

static void apply_vehicle_outputs(void)
{
	const kia_vehicle_state_t *state = kia_profile_vehicle_state();
	bool canbox_mode = board_get_mode() == BOARD_MODE_CANBOX;

	board_reverse_output_set(canbox_mode && state->reverse);
	teyes_raise_uart_poll(canbox_mode ? state : 0);
}

int main(void)
{
	board_runtime_sanitize();
	board_clock_setup();
	board_systick_setup();
	board_set_mode(BOARD_MODE_CANBOX);
	board_reverse_output_init();

	kia_profile_init();
	protocol_init(usb_write);
	canbus_set_rx_callback(can_rx);
	canbus_init();
	teyes_raise_uart_init();
	usb_cdc_init(usb_rx);

	protocol_emit_text("\r\n2CAN35 custom-c boot\r\n");

	while (1) {
		usb_cdc_poll();
		canbus_poll();
		apply_vehicle_outputs();
		protocol_poll();
	}
}
