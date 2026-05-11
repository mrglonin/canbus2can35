#include "board.h"

#include <libopencm3/cm3/common.h>
#include <libopencm3/cm3/scb.h>
#include <libopencm3/cm3/systick.h>
#include <libopencm3/stm32/f1/flash.h>
#include <libopencm3/stm32/otg_fs.h>
#include <libopencm3/stm32/f1/gpio.h>
#include <libopencm3/stm32/rcc.h>

const uint8_t canbox_uid[CANBOX_UID_LEN] = {
	0x37, 0xff, 0xda, 0x05, 0x42, 0x47, 0x30, 0x38, 0x59, 0x41, 0x22, 0x43,
};

#ifndef CANBOX_VERSION_B0
#define CANBOX_VERSION_B0 0x04U
#endif
#ifndef CANBOX_VERSION_B1
#define CANBOX_VERSION_B1 0x35U
#endif
#ifndef CANBOX_VERSION_B2
#define CANBOX_VERSION_B2 0x10U
#endif
#ifndef CANBOX_VERSION_B3
#define CANBOX_VERSION_B3 0x00U
#endif

const uint8_t canbox_version[CANBOX_VERSION_LEN] = {
	CANBOX_VERSION_B0, CANBOX_VERSION_B1, CANBOX_VERSION_B2, CANBOX_VERSION_B3,
};

#define STK_CSR_REG    MMIO32(0xE000E010U)
#define NVIC_ICER_BASE 0xE000E180U
#define NVIC_ICPR_BASE 0xE000E280U
#define SCB_AIRCR_REG  MMIO32(0xE000ED0CU)
#define RCC_CR_REG     MMIO32(0x40021000U)
#define RCC_CFGR_REG   MMIO32(0x40021004U)
#define RCC_CFGR2_REG  MMIO32(0x4002102CU)
#define RCC_APB1ENR_REG MMIO32(0x4002101CU)
#define PWR_CR_REG      MMIO32(0x40007000U)
#define BKP_DR1_REG     MMIO16(0x40006C04U)
#define IWDG_KR_REG     MMIO32(0x40003000U)

#define BOARD_RCC_APB1ENR_BKPEN (1U << 27)
#define BOARD_RCC_APB1ENR_PWREN (1U << 28)
#define BOARD_PWR_CR_DBP        (1U << 8)
#define BOARD_SCB_AIRCR_VECTKEY (0x5FAU << 16)
#define BOARD_SCB_AIRCR_PRIGROUP_MASK (7U << 8)
#define BOARD_SCB_AIRCR_SYSRESETREQ (1U << 2)
#define UPDATE_MAGIC            0xBEEFU
#define IWDG_REFRESH_KEY        0xAAAAU

#define STOCK_BEEP_INIT  ((void (*)(void))0x0800057d)
#define STOCK_BEEP_START ((void (*)(void))0x0800064d)
#define STOCK_BEEP_STOP  ((void (*)(void))0x08000629)

static volatile uint32_t ms_ticks;
static volatile board_mode_t active_mode = BOARD_MODE_CANBOX;

static void delay_cycles(volatile uint32_t count)
{
	while (count--) {
		__asm volatile ("nop");
	}
}

void sys_tick_handler(void)
{
	board_watchdog_kick();
	ms_ticks++;
}

void board_watchdog_kick(void)
{
	IWDG_KR_REG = IWDG_REFRESH_KEY;
}

void board_runtime_sanitize(void)
{
	board_watchdog_kick();
	__asm volatile ("cpsid i");
	STK_CSR_REG = 0;
	for (uint32_t i = 0; i < 3U; i++) {
		MMIO32(NVIC_ICER_BASE + i * 4U) = 0xffffffffU;
		MMIO32(NVIC_ICPR_BASE + i * 4U) = 0xffffffffU;
	}
	SCB_VTOR = APP_VECTOR_BASE;
	__asm volatile ("cpsie i");
}

bool board_clock_setup(void)
{
	board_watchdog_kick();
#if CANBOX_HSE_MHZ == 8
	rcc_clock_setup_in_hse_8mhz_out_72mhz();
#elif CANBOX_HSE_MHZ == 16
	rcc_clock_setup_in_hse_16mhz_out_72mhz();
#else
#error "Unsupported CANBOX_HSE_MHZ"
#endif
	board_watchdog_kick();
	return true;
}

void board_systick_setup(void)
{
	systick_set_clocksource(STK_CSR_CLKSOURCE_AHB);
	systick_set_reload(72000U - 1U);
	systick_interrupt_enable();
	systick_counter_enable();
}

uint32_t board_millis(void)
{
	return ms_ticks;
}

void board_delay_ms(uint32_t ms)
{
	uint32_t start = board_millis();

	while ((board_millis() - start) < ms) {
		board_watchdog_kick();
		__asm volatile ("nop");
	}
}

void board_usb_force_connect(void)
{
	OTG_FS_GCCFG &= ~(OTG_GCCFG_NOVBUSSENS | OTG_GCCFG_VBUSASEN | OTG_GCCFG_VBUSBSEN);
	OTG_FS_GCCFG |= OTG_GCCFG_NOVBUSSENS | OTG_GCCFG_PWRDWN;
	OTG_FS_DCTL |= OTG_DCTL_SDIS;
	delay_cycles(720000);
	OTG_FS_DCTL &= ~OTG_DCTL_SDIS;
}

void board_reverse_output_init(void)
{
#if ENABLE_REVERSE_OUT
	rcc_periph_clock_enable(RCC_GPIOC);
	gpio_clear(GPIOC, GPIO14);
	gpio_set_mode(GPIOC, GPIO_MODE_OUTPUT_2_MHZ, GPIO_CNF_OUTPUT_PUSHPULL, GPIO14);
#endif
}

void board_reverse_output_set(bool on)
{
#if ENABLE_REVERSE_OUT
	if (on) {
		gpio_set(GPIOC, GPIO14);
	} else {
		gpio_clear(GPIOC, GPIO14);
	}
#else
	(void)on;
#endif
}

void board_beep(uint8_t count)
{
#if USE_STOCK_BEEP
	STOCK_BEEP_INIT();
	while (count--) {
		STOCK_BEEP_START();
		delay_cycles(1200000);
		STOCK_BEEP_STOP();
		delay_cycles(600000);
	}
#else
	(void)count;
#endif
}

void board_reboot(void)
{
	__asm volatile ("dsb");
	SCB_AIRCR_REG = BOARD_SCB_AIRCR_VECTKEY |
	                (SCB_AIRCR_REG & BOARD_SCB_AIRCR_PRIGROUP_MASK) |
	                BOARD_SCB_AIRCR_SYSRESETREQ;
	__asm volatile ("dsb");
	while (1) {
	}
}

static void backup_domain_enable(void)
{
	RCC_APB1ENR_REG |= BOARD_RCC_APB1ENR_BKPEN | BOARD_RCC_APB1ENR_PWREN;
	PWR_CR_REG |= BOARD_PWR_CR_DBP;
}

void board_enter_update_request(void)
{
	active_mode = BOARD_MODE_UPDATE;
	backup_domain_enable();
	BKP_DR1_REG = UPDATE_MAGIC;
	board_reboot();
}

void board_set_mode(board_mode_t mode)
{
	active_mode = mode;
	board_beep((uint8_t)mode);
}

board_mode_t board_get_mode(void)
{
	return active_mode;
}
