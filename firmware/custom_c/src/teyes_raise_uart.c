#include "teyes_raise_uart.h"

#include <stddef.h>

#include <libopencm3/cm3/common.h>
#include <libopencm3/stm32/f1/gpio.h>
#include <libopencm3/stm32/rcc.h>

#include "board.h"

#define TEYES_USART_BASE 0x40004400U
#define TEYES_USART_SR   MMIO32(TEYES_USART_BASE + 0x00U)
#define TEYES_USART_DR   MMIO32(TEYES_USART_BASE + 0x04U)
#define TEYES_USART_BRR  MMIO32(TEYES_USART_BASE + 0x08U)
#define TEYES_USART_CR1  MMIO32(TEYES_USART_BASE + 0x0cU)
#define TEYES_USART_CR2  MMIO32(TEYES_USART_BASE + 0x10U)
#define TEYES_USART_CR3  MMIO32(TEYES_USART_BASE + 0x14U)

#define USART_SR_RXNE (1U << 5)
#define USART_SR_TXE  (1U << 7)
#define USART_CR1_RE  (1U << 2)
#define USART_CR1_TE  (1U << 3)
#define USART_CR1_UE  (1U << 13)

#define TEYES_FRAME_MAX 32U

static bool initialized;
static uint8_t last_body_flags = 0xffU;
static uint8_t last_reverse = 0xffU;
static uint32_t last_heartbeat_ms;
static uint8_t rx_frame[TEYES_FRAME_MAX];
static uint8_t rx_pos;

static uint16_t frame_checksum(const uint8_t *frame, uint8_t len_field)
{
	uint16_t sum = 0;

	for (uint8_t i = 1; i < (uint8_t)(len_field - 1U); i++) {
		sum = (uint16_t)(sum + frame[i]);
	}
	return sum;
}

static void write_byte(uint8_t byte)
{
	uint32_t timeout = 100000U;

	while ((TEYES_USART_SR & USART_SR_TXE) == 0U && timeout != 0U) {
		timeout--;
	}
	if (timeout != 0U) {
		TEYES_USART_DR = byte;
	}
}

static void send_frame(uint8_t cmd, const uint8_t *payload, uint8_t payload_len)
{
	uint8_t frame[TEYES_FRAME_MAX];
	uint8_t len_field = (uint8_t)(payload_len + 4U);
	uint8_t pos = 0;
	uint16_t sum;

	if (!initialized || len_field >= TEYES_FRAME_MAX) {
		return;
	}

	frame[pos++] = 0xfdU;
	frame[pos++] = len_field;
	frame[pos++] = cmd;
	for (uint8_t i = 0; i < payload_len; i++) {
		frame[pos++] = payload[i];
	}
	sum = frame_checksum(frame, len_field);
	frame[pos++] = (uint8_t)(sum >> 8);
	frame[pos++] = (uint8_t)sum;

	for (uint8_t i = 0; i < pos; i++) {
		write_byte(frame[i]);
	}
}

static uint8_t body_flags_from_state(const kia_vehicle_state_t *state)
{
	uint8_t flags = 0;

	if (state == 0) {
		return 0;
	}
	if (state->left_front_door) {
		flags |= 0x01U;
	}
	if (state->right_front_door) {
		flags |= 0x02U;
	}
	if (state->left_rear_door) {
		flags |= 0x04U;
	}
	if (state->right_rear_door) {
		flags |= 0x08U;
	}
	if (state->trunk) {
		flags |= 0x10U;
	}
	if (state->hood) {
		flags |= 0x20U;
	}
	return flags;
}

static void send_body(uint8_t flags)
{
	send_frame(0x05U, &flags, 1U);
}

static void send_reverse(bool on)
{
	uint8_t payload[2] = {0x06U, on ? 0x02U : 0x00U};

	send_frame(0x7dU, payload, sizeof(payload));
}

static void drain_rx(void)
{
	while ((TEYES_USART_SR & USART_SR_RXNE) != 0U) {
		uint8_t b = (uint8_t)TEYES_USART_DR;

		if (rx_pos == 0U && b != 0xfdU) {
			continue;
		}
		if (rx_pos < sizeof(rx_frame)) {
			rx_frame[rx_pos++] = b;
		} else {
			rx_pos = 0;
			continue;
		}
		if (rx_pos >= 2U && rx_frame[1] < (TEYES_FRAME_MAX - 1U) &&
		    rx_pos > rx_frame[1]) {
			rx_pos = 0;
		}
	}
}

void teyes_raise_uart_init(void)
{
#if ENABLE_TEYES_UART
	rcc_periph_clock_enable(RCC_AFIO);
	rcc_periph_clock_enable(RCC_GPIOA);
	rcc_periph_clock_enable(RCC_USART2);

	gpio_set_mode(GPIOA, GPIO_MODE_OUTPUT_50_MHZ, GPIO_CNF_OUTPUT_ALTFN_PUSHPULL, GPIO2);
	gpio_set_mode(GPIOA, GPIO_MODE_INPUT, GPIO_CNF_INPUT_FLOAT, GPIO3);

	TEYES_USART_CR1 = 0;
	TEYES_USART_CR2 = 0;
	TEYES_USART_CR3 = 0;
	TEYES_USART_BRR = (36000000U + (TEYES_UART_BAUD / 2U)) / TEYES_UART_BAUD;
	TEYES_USART_CR1 = USART_CR1_RE | USART_CR1_TE | USART_CR1_UE;
	initialized = true;
	last_body_flags = 0xffU;
	last_reverse = 0xffU;
#else
	initialized = false;
#endif
}

void teyes_raise_uart_poll(const kia_vehicle_state_t *state)
{
#if ENABLE_TEYES_UART
	uint8_t flags;
	uint8_t reverse;
	uint32_t now;

	if (!initialized) {
		return;
	}
	drain_rx();

	flags = body_flags_from_state(state);
	reverse = state != 0 && state->reverse ? 1U : 0U;
	now = board_millis();

	if (flags != last_body_flags || (now - last_heartbeat_ms) >= 5000U) {
		send_body(flags);
		last_body_flags = flags;
		last_heartbeat_ms = now;
	}
	if (reverse != last_reverse) {
		send_reverse(reverse != 0U);
		last_reverse = reverse;
	}
#else
	(void)state;
#endif
}

bool teyes_raise_uart_ready(void)
{
	return initialized;
}

void teyes_raise_uart_send_raw(const uint8_t *data, uint8_t len)
{
#if ENABLE_TEYES_UART
	if (data == 0 || !initialized) {
		return;
	}
	for (uint8_t i = 0; i < len; i++) {
		write_byte(data[i]);
	}
#else
	(void)data;
	(void)len;
#endif
}
