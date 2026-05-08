#pragma once

#include <stdbool.h>
#include <stdint.h>

#define CANBOX_UID_LEN 12U
#define CANBOX_VERSION_LEN 4U

extern const uint8_t canbox_uid[CANBOX_UID_LEN];
extern const uint8_t canbox_version[CANBOX_VERSION_LEN];

typedef enum {
	BOARD_MODE_CANBOX = 1,
	BOARD_MODE_UPDATE = 2,
	BOARD_MODE_LAB = 3,
} board_mode_t;

void board_runtime_sanitize(void);
void board_clock_setup(void);
void board_systick_setup(void);
uint32_t board_millis(void);
void board_delay_ms(uint32_t ms);
void board_usb_force_connect(void);
void board_reverse_output_init(void);
void board_reverse_output_set(bool on);
void board_beep(uint8_t count);
void board_reboot(void) __attribute__((noreturn));
void board_enter_update_request(void);
void board_set_mode(board_mode_t mode);
board_mode_t board_get_mode(void);
