#include "canbus.h"

#include <libopencm3/stm32/can.h>
#include <libopencm3/stm32/f1/gpio.h>
#include <libopencm3/stm32/rcc.h>

static bool initialized;
static bool ready[CANBUS_COUNT];
static can_speed_code_t speed_code[CANBUS_COUNT] = {
	CAN_SPEED_500K,
	CAN_SPEED_100K,
};
static can_rx_callback_t rx_callback;

#if ENABLE_CAN_HW
static uint32_t can_port(can_bus_t bus)
{
	return bus == CANBUS_M ? CAN2 : CAN1;
}

static uint32_t can_btr_for_speed(can_speed_code_t code)
{
	switch (code) {
	case CAN_SPEED_100K:
		return CAN_BTR_SJW_2TQ | CAN_BTR_TS1_13TQ | CAN_BTR_TS2_4TQ | (20U - 1U);
	case CAN_SPEED_125K:
		return CAN_BTR_SJW_2TQ | CAN_BTR_TS1_13TQ | CAN_BTR_TS2_4TQ | (16U - 1U);
	case CAN_SPEED_250K:
		return CAN_BTR_SJW_2TQ | CAN_BTR_TS1_13TQ | CAN_BTR_TS2_4TQ | (8U - 1U);
	case CAN_SPEED_1000K:
		return CAN_BTR_SJW_2TQ | CAN_BTR_TS1_13TQ | CAN_BTR_TS2_4TQ | (2U - 1U);
	case CAN_SPEED_500K:
	default:
		return CAN_BTR_SJW_2TQ | CAN_BTR_TS1_13TQ | CAN_BTR_TS2_4TQ | (4U - 1U);
	}
}

static bool can_setup_one(can_bus_t bus)
{
	uint32_t port = can_port(bus);
	uint32_t btr = can_btr_for_speed(speed_code[bus]);

	can_reset(port);
	return can_init(port,
			false, true, false, true, false, false,
			btr & CAN_BTR_SJW_MASK,
			btr & CAN_BTR_TS1_MASK,
			btr & CAN_BTR_TS2_MASK,
			(btr & CAN_BTR_BRP_MASK) + 1U,
			false, true) == 0;
}

static void can_setup_filters(void)
{
	CAN_FMR(CAN1) |= CAN_FMR_FINIT;
	CAN_FMR(CAN1) = (CAN_FMR(CAN1) & ~(0x3fU << 8)) | (14U << 8) | CAN_FMR_FINIT;
	CAN_FMR(CAN1) &= ~CAN_FMR_FINIT;

	can_filter_id_mask_32bit_init(CAN1, 0, 0, 0, 0, true);
	can_filter_id_mask_32bit_init(CAN1, 14, 0, 0, 0, true);
}
#endif

void canbus_set_rx_callback(can_rx_callback_t cb)
{
	rx_callback = cb;
}

void canbus_init(void)
{
	if (initialized) {
		return;
	}

#if ENABLE_CAN_HW
	rcc_periph_clock_enable(RCC_AFIO);
	rcc_periph_clock_enable(RCC_GPIOB);
	rcc_periph_clock_enable(RCC_CAN1);
	rcc_periph_clock_enable(RCC_CAN2);

	gpio_primary_remap(AFIO_MAPR_SWJ_CFG_FULL_SWJ, AFIO_MAPR_CAN1_REMAP_PORTB);

	/* CAN1: C-CAN PB8/PB9. CAN2: M-CAN PB12/PB13. */
	gpio_set_mode(GPIOB, GPIO_MODE_INPUT, GPIO_CNF_INPUT_PULL_UPDOWN, GPIO8 | GPIO12);
	gpio_set(GPIOB, GPIO8 | GPIO12);
	gpio_set_mode(GPIOB, GPIO_MODE_OUTPUT_50_MHZ, GPIO_CNF_OUTPUT_ALTFN_PUSHPULL, GPIO9 | GPIO13);

	/* Inferred transceiver silent pins. High keeps the transceivers awake on this board family. */
	gpio_set(GPIOB, GPIO15 | GPIO11);
	gpio_set_mode(GPIOB, GPIO_MODE_OUTPUT_50_MHZ, GPIO_CNF_OUTPUT_OPENDRAIN, GPIO15 | GPIO11);

	ready[CANBUS_C] = can_setup_one(CANBUS_C);
	ready[CANBUS_M] = can_setup_one(CANBUS_M);
	can_setup_filters();
#else
	ready[CANBUS_C] = false;
	ready[CANBUS_M] = false;
#endif
	initialized = true;
}

bool canbus_ready(can_bus_t bus)
{
	return bus < CANBUS_COUNT && ready[bus];
}

void canbus_set_speed(can_bus_t bus, can_speed_code_t speed)
{
	if (bus >= CANBUS_COUNT) {
		return;
	}
	speed_code[bus] = speed;
#if ENABLE_CAN_HW
	ready[bus] = can_setup_one(bus);
	can_setup_filters();
#endif
}

can_speed_code_t canbus_get_speed(can_bus_t bus)
{
	if (bus >= CANBUS_COUNT) {
		return CAN_SPEED_500K;
	}
	return speed_code[bus];
}

bool canbus_send(can_bus_t bus, const can_frame_t *frame)
{
#if ENABLE_CAN_HW
	uint8_t data[8];

	if (bus >= CANBUS_COUNT || frame == 0 || !ready[bus]) {
		return false;
	}

	for (uint8_t i = 0; i < 8U; i++) {
		data[i] = frame->data[i];
	}

	return can_transmit(can_port(bus), frame->id, frame->ext, frame->rtr, frame->len, data) >= 0;
#else
	(void)bus;
	(void)frame;
	return false;
#endif
}

static void poll_one(can_bus_t bus)
{
#if ENABLE_CAN_HW
	uint32_t port = can_port(bus);

	if (!ready[bus]) {
		return;
	}

	while ((CAN_RF0R(port) & CAN_RF0R_FMP0_MASK) != 0) {
		uint32_t id = 0;
		uint32_t fmi = 0;
		can_frame_t frame;

		can_receive(port, 0, true, &id, &frame.ext, &frame.rtr, &fmi, &frame.len, frame.data);
		frame.id = id;
		if (rx_callback != 0) {
			rx_callback(bus, &frame);
		}
	}
#else
	(void)bus;
#endif
}

void canbus_poll(void)
{
	poll_one(CANBUS_C);
	poll_one(CANBUS_M);
}
