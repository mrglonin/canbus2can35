#include "protocol.h"

#include <stdbool.h>
#include <stddef.h>

#include "board.h"
#include "canbus.h"
#include "kia_profile.h"
#include "teyes_raise_uart.h"
#include "usb_cdc.h"

#define FRAME_MAX 80U
#define LINE_MAX 96U

static protocol_write_fn_t write_bytes;
static uint8_t frame_buf[FRAME_MAX];
static uint8_t frame_pos;
static uint8_t line_buf[LINE_MAX];
static uint8_t line_pos;
static bool logging_enabled = true;

static void emit(const char *s)
{
	if (write_bytes == 0 || s == 0) {
		return;
	}
	while (*s != '\0') {
		write_bytes((const uint8_t *)s, 1);
		s++;
	}
}

void protocol_emit_text(const char *s)
{
	emit(s);
}

static char hex_nibble(uint8_t v)
{
	v &= 0x0fU;
	return v < 10U ? (char)('0' + v) : (char)('A' + v - 10U);
}

static int hex_value(uint8_t c)
{
	if (c >= '0' && c <= '9') {
		return (int)(c - '0');
	}
	if (c >= 'a' && c <= 'f') {
		return (int)(c - 'a' + 10);
	}
	if (c >= 'A' && c <= 'F') {
		return (int)(c - 'A' + 10);
	}
	return -1;
}

static void emit_hex8(uint8_t v)
{
	char out[2];

	out[0] = hex_nibble(v >> 4);
	out[1] = hex_nibble(v);
	write_bytes((const uint8_t *)out, sizeof(out));
}

static void emit_hex_id(uint32_t id, uint8_t nibbles)
{
	for (int shift = (int)(nibbles - 1U) * 4; shift >= 0; shift -= 4) {
		char c = hex_nibble((uint8_t)(id >> shift));
		write_bytes((const uint8_t *)&c, 1);
	}
}

static void emit_u32(uint32_t v)
{
	char tmp[10];
	uint8_t n = 0;

	if (v == 0U) {
		emit("0");
		return;
	}
	while (v != 0U && n < sizeof(tmp)) {
		tmp[n++] = (char)('0' + (v % 10U));
		v /= 10U;
	}
	while (n != 0U) {
		char c = tmp[--n];
		write_bytes((const uint8_t *)&c, 1);
	}
}

void protocol_emit_can(uint8_t bus, uint32_t id, bool ext, bool rtr, uint8_t len, const uint8_t *data)
{
	if (!logging_enabled) {
		return;
	}
	char prefix[3];

	prefix[0] = (char)('0' + bus);
	prefix[1] = ' ';
	prefix[2] = ext ? 'T' : 't';
	write_bytes((const uint8_t *)prefix, sizeof(prefix));
	emit_hex_id(id, ext ? 8U : 3U);
	char dlc = hex_nibble(len & 0x0fU);
	write_bytes((const uint8_t *)&dlc, 1);
	if (rtr) {
		emit("R");
	} else {
		for (uint8_t i = 0; i < len && i < 8U; i++) {
			emit_hex8(data[i]);
		}
	}
	emit("\r\n");
}

static uint8_t checksum(const uint8_t *data, uint8_t len)
{
	uint8_t sum = 0;

	for (uint8_t i = 0; i < len; i++) {
		sum = (uint8_t)(sum + data[i]);
	}
	return sum;
}

static void send_frame(uint8_t cmd, const uint8_t *payload, uint8_t payload_len)
{
	uint8_t out[96];
	uint8_t pos = 0;

	out[pos++] = 0xbbU;
	out[pos++] = 0xa1U;
	out[pos++] = 0x41U;
	out[pos++] = (uint8_t)(payload_len + 1U);
	out[pos++] = cmd;
	for (uint8_t i = 0; i < payload_len && pos < (sizeof(out) - 1U); i++) {
		out[pos++] = payload[i];
	}
	out[pos] = checksum(out, pos);
	pos++;
	write_bytes(out, pos);
}

static void send_ack(uint8_t cmd, uint8_t status)
{
	uint8_t payload[1] = {status};

	send_frame(cmd, payload, sizeof(payload));
}

static void handle_binary_payload(const uint8_t *payload, uint8_t len)
{
	if (len == 0U) {
		return;
	}

	switch (payload[0]) {
	case 0x56: {
		uint8_t out[18];
		for (uint8_t i = 0; i < CANBOX_UID_LEN; i++) {
			out[i] = canbox_uid[i];
		}
		for (uint8_t i = 0; i < CANBOX_VERSION_LEN; i++) {
			out[CANBOX_UID_LEN + i] = canbox_version[i];
		}
		out[16] = (uint8_t)board_get_mode();
		out[17] = ENABLE_CAN_HW ? 1U : 0U;
		send_frame(0x56, out, sizeof(out));
		break;
	}
	case 0x55:
		if (len >= 2U && payload[1] == 1U) {
			board_set_mode(BOARD_MODE_CANBOX);
			send_ack(0x55, 0x01);
		} else if (len >= 2U && payload[1] == 3U) {
			board_set_mode(BOARD_MODE_LAB);
			send_ack(0x55, 0x03);
		} else if (len >= 2U && payload[1] == 2U) {
			send_ack(0x55, 0xe2);
		} else {
			send_ack(0x55, 0xff);
		}
		break;
	case 0x51:
		if (len >= 2U && payload[1] == 3U) {
			board_set_mode(BOARD_MODE_LAB);
			send_ack(0x51, 0x03);
		} else {
			send_ack(0x51, 0xff);
		}
		break;
	case 0x70:
		if (len >= 12U) {
			can_frame_t frame;
			can_bus_t bus = payload[1] == 1U ? CANBUS_M : CANBUS_C;
			frame.id = ((uint32_t)payload[2] << 8) | payload[3];
			frame.len = payload[4] > 8U ? 8U : payload[4];
			frame.ext = false;
			frame.rtr = false;
			for (uint8_t i = 0; i < frame.len; i++) {
				frame.data[i] = payload[5U + i];
			}
			send_ack(0x70, canbus_send(bus, &frame) ? 0x00U : 0x01U);
		} else {
			send_ack(0x70, 0xff);
		}
		break;
	default:
		send_ack(payload[0], 0xfe);
		break;
	}
}

static void parse_binary_byte(uint8_t b)
{
	if (frame_pos == 0U && b != 0xbbU) {
		return;
	}
	if (frame_pos < FRAME_MAX) {
		frame_buf[frame_pos++] = b;
	} else {
		frame_pos = 0;
		return;
	}

	if (frame_pos >= 4U) {
		uint8_t payload_len = frame_buf[3];
		uint8_t total = (uint8_t)(payload_len + 5U);

		if (frame_pos == total) {
			if (frame_buf[0] == 0xbbU && frame_buf[1] == 0x41U &&
			    frame_buf[2] == 0xa1U &&
			    checksum(frame_buf, (uint8_t)(total - 1U)) == frame_buf[total - 1U]) {
				handle_binary_payload(&frame_buf[4], payload_len);
			}
			frame_pos = 0;
		}
	}
}

static bool parse_hex_byte_pair(const char *s, uint8_t *out)
{
	int hi = hex_value((uint8_t)s[0]);
	int lo = hex_value((uint8_t)s[1]);

	if (hi < 0 || lo < 0) {
		return false;
	}
	*out = (uint8_t)((hi << 4) | lo);
	return true;
}

static bool parse_raw_tx(const char *line)
{
	can_frame_t frame = {
		.id = 0,
		.len = 0,
		.ext = false,
		.rtr = false,
		.data = {0},
	};
	can_bus_t bus;
	const char *p;

	if (!(line[0] == 't' || line[0] == 'T') && !(line[1] == 't' || line[1] == 'T')) {
		return false;
	}
	if (line[0] == '0' || line[0] == '1') {
		bus = line[0] == '1' ? CANBUS_M : CANBUS_C;
		p = &line[1];
	} else {
		bus = CANBUS_M;
		p = line;
	}

	frame.ext = p[0] == 'T';
	uint8_t id_len = frame.ext ? 8U : 3U;
	for (uint8_t i = 0; i < id_len; i++) {
		int n = hex_value((uint8_t)p[1U + i]);
		if (n < 0) {
			return false;
		}
		frame.id = (frame.id << 4) | (uint32_t)n;
	}
	int dlc = hex_value((uint8_t)p[1U + id_len]);
	if (dlc < 0 || dlc > 8) {
		return false;
	}
	frame.len = (uint8_t)dlc;
	p += 2U + id_len;
	for (uint8_t i = 0; i < frame.len; i++) {
		if (!parse_hex_byte_pair(&p[i * 2U], &frame.data[i])) {
			return false;
		}
	}

	if (canbus_send(bus, &frame)) {
		emit("OK TX\r\n");
	} else {
		emit("ERR TX\r\n");
	}
	return true;
}

static bool parse_uart_tx(const char *line)
{
	uint8_t data[32];
	uint8_t len = 0;
	const char *p;

	if (line[0] != 'u' && line[0] != 'U') {
		return false;
	}
	p = &line[1];
	while (p[0] != '\0' && p[1] != '\0') {
		if (len >= sizeof(data) || !parse_hex_byte_pair(p, &data[len])) {
			emit("ERR UART\r\n");
			return true;
		}
		len++;
		p += 2;
	}
	if (*p != '\0') {
		emit("ERR UART\r\n");
		return true;
	}
	teyes_raise_uart_send_raw(data, len);
	emit(teyes_raise_uart_ready() ? "OK UART\r\n" : "ERR UART OFF\r\n");
	return true;
}

static void print_status(void)
{
	emit("2CAN35 custom-c v");
	emit_hex8(canbox_version[0]);
	emit_hex8(canbox_version[1]);
	emit_hex8(canbox_version[2]);
	emit_hex8(canbox_version[3]);
	emit(" mode=");
	emit_u32((uint32_t)board_get_mode());
	emit(" can=");
	emit(canbus_ready(CANBUS_C) ? "1" : "0");
	emit(canbus_ready(CANBUS_M) ? "1" : "0");
	emit(" speed C=S");
	emit_u32((uint32_t)canbus_get_speed(CANBUS_C));
	emit(" M=S");
	emit_u32((uint32_t)canbus_get_speed(CANBUS_M));
	emit(" usbdrop=");
	emit_u32(usb_cdc_dropped());
	emit("\r\n");
}

static void handle_line(const char *line)
{
	if (line[0] == '\0') {
		return;
	}
	if (line[0] == '?' || line[0] == 'V' || line[0] == 'v') {
		print_status();
		emit("cmd: ?, O, C, I, mode1, mode3, 0S6, 1S3, 0t1238..., 1t1238..., uFD0505...\r\n");
		return;
	}
	if (line[0] == 'O' || line[0] == 'o') {
		logging_enabled = true;
		emit("OK OPEN\r\n");
		return;
	}
	if (line[0] == 'C' || line[0] == 'c') {
		logging_enabled = false;
		emit("OK CLOSED\r\n");
		return;
	}
	if (line[0] == 'I' || line[0] == 'i') {
		canbus_init();
		print_status();
		return;
	}
	if (line[0] == 'm' && line[1] == 'o' && line[2] == 'd' && line[3] == 'e') {
		if (line[4] == '1') {
			board_set_mode(BOARD_MODE_CANBOX);
			emit("OK mode1\r\n");
		} else if (line[4] == '3') {
			board_set_mode(BOARD_MODE_LAB);
			emit("OK mode3\r\n");
		} else {
			emit("ERR mode\r\n");
		}
		return;
	}
	if ((line[0] == '0' || line[0] == '1') && (line[1] == 'S' || line[1] == 's')) {
		int code = hex_value((uint8_t)line[2]);
		if (code == 3 || code == 4 || code == 5 || code == 6 || code == 8) {
			canbus_set_speed(line[0] == '1' ? CANBUS_M : CANBUS_C, (can_speed_code_t)code);
			print_status();
		} else {
			emit("ERR speed\r\n");
		}
		return;
	}
	if (parse_raw_tx(line)) {
		return;
	}
	if (parse_uart_tx(line)) {
		return;
	}
	emit("ERR command\r\n");
}

static void parse_ascii_byte(uint8_t b)
{
	if (b == '\r' || b == '\n') {
		line_buf[line_pos] = '\0';
		handle_line((const char *)line_buf);
		line_pos = 0;
		return;
	}
	if (line_pos < (LINE_MAX - 1U)) {
		line_buf[line_pos++] = b;
	} else {
		line_pos = 0;
	}
}

void protocol_init(protocol_write_fn_t write_fn)
{
	write_bytes = write_fn;
}

void protocol_rx_bytes(const uint8_t *data, uint16_t len)
{
	for (uint16_t i = 0; i < len; i++) {
		if (data[i] == 0xbbU || frame_pos != 0U) {
			parse_binary_byte(data[i]);
		} else {
			parse_ascii_byte(data[i]);
		}
	}
}

void protocol_poll(void)
{
}
