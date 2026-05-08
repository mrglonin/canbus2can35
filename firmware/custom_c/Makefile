ROOT ?= /Users/legion/Downloads/2CAN35_CANBOX_WORK_2026-05-08
TOOLCHAIN ?= $(ROOT)/toolchains/arm-gnu-toolchain-15.2.rel1-darwin-arm64-arm-none-eabi/bin
PREFIX ?= $(TOOLCHAIN)/arm-none-eabi
CC := $(PREFIX)-gcc
OBJCOPY := $(PREFIX)-objcopy
SIZE := $(PREFIX)-size

OPENCM3 ?= $(ROOT)/canbox-fw-lab/canbox/libopencm3
PYTHON ?= python3

BUILD := build
TARGET := 2can35_custom
APP_BASE ?= 0x08004000
CANBOX_HSE_MHZ ?= 16
ENABLE_CAN_HW ?= 0
USE_STOCK_BEEP ?= 0
ENABLE_REVERSE_OUT ?= 0
ENABLE_TEYES_UART ?= 0
TEYES_UART_BAUD ?= 19200

UID_HEX ?= 37FFDA054247303859412243
VERSION_HEX ?= 04351000
UPDATE_KEY_A ?= 0x04
UPDATE_KEY_B ?= 0x5b

CFLAGS := -Os -std=gnu99 -Wall -Wextra -Wshadow -Wundef -Wstrict-prototypes \
	-mcpu=cortex-m3 -mthumb -mfix-cortex-m3-ldrd -msoft-float \
	-DSTM32F1 -DCANBOX_HSE_MHZ=$(CANBOX_HSE_MHZ) \
	-DAPP_VECTOR_BASE=$(APP_BASE) -DENABLE_CAN_HW=$(ENABLE_CAN_HW) \
	-DUSE_STOCK_BEEP=$(USE_STOCK_BEEP) \
	-DENABLE_REVERSE_OUT=$(ENABLE_REVERSE_OUT) \
	-DENABLE_TEYES_UART=$(ENABLE_TEYES_UART) \
	-DTEYES_UART_BAUD=$(TEYES_UART_BAUD) \
	-Iinclude -I$(OPENCM3)/include \
	-ffunction-sections -fdata-sections

LDFLAGS := -Tapp_0x08004000.ld -L$(OPENCM3)/lib/stm32/f1 \
	-mcpu=cortex-m3 -mthumb -mfix-cortex-m3-ldrd -msoft-float \
	-nostartfiles --specs=nosys.specs -Wl,--gc-sections -Wl,-Map,$(BUILD)/$(TARGET).map

LIBOPENCM3_OBJS := \
	$(OPENCM3)/lib/cm3/vector.stm32f1 \
	$(OPENCM3)/lib/cm3/sync.stm32f1 \
	$(OPENCM3)/lib/cm3/nvic.stm32f1 \
	$(OPENCM3)/lib/cm3/systick.stm32f1 \
	$(OPENCM3)/lib/stm32/f1/gpio.stm32f1 \
	$(OPENCM3)/lib/stm32/f1/rcc.stm32f1 \
	$(OPENCM3)/lib/stm32/f1/flash.stm32f1 \
	$(OPENCM3)/lib/stm32/common/gpio_common_all.stm32f1 \
	$(OPENCM3)/lib/stm32/common/rcc_common_all.stm32f1 \
	$(OPENCM3)/lib/stm32/common/flash_common_f01.stm32f1 \
	$(OPENCM3)/lib/stm32/can.stm32f1 \
	$(OPENCM3)/lib/usb/usb.o \
	$(OPENCM3)/lib/usb/usb_control.o \
	$(OPENCM3)/lib/usb/usb_standard.o \
	$(OPENCM3)/lib/usb/usb_fx07_common.o \
	$(OPENCM3)/lib/usb/usb_f107.o

SRC := \
	src/board.c \
	src/canbus.c \
	src/kia_profile.c \
	src/main.c \
	src/protocol.c \
	src/ringbuf.c \
	src/teyes_raise_uart.c \
	src/usb_cdc.c

OBJ := $(SRC:src/%.c=$(BUILD)/%.o)

.PHONY: all clean package sim size

all: package

$(BUILD):
	mkdir -p $(BUILD)

$(BUILD)/%.o: src/%.c | $(BUILD)
	$(CC) $(CFLAGS) -c $< -o $@

$(BUILD)/$(TARGET).elf: $(OBJ) $(LIBOPENCM3_OBJS) app_0x08004000.ld
	$(CC) $(OBJ) $(LIBOPENCM3_OBJS) $(LDFLAGS) -o $@

$(BUILD)/$(TARGET).bin: $(BUILD)/$(TARGET).elf
	$(OBJCOPY) -Obinary $< $@
	$(SIZE) $<

package: $(BUILD)/$(TARGET).bin
	$(PYTHON) tools/package_update.py \
		--app-bin $< \
		--out $(BUILD)/$(TARGET)_$(VERSION_HEX)_update.bin \
		--uid $(UID_HEX) \
		--version $(VERSION_HEX) \
		--key-a $(UPDATE_KEY_A) \
		--key-b $(UPDATE_KEY_B) \
		--report $(BUILD)/$(TARGET)_$(VERSION_HEX)_report.json \
		--meta ENABLE_CAN_HW=$(ENABLE_CAN_HW) \
		--meta USE_STOCK_BEEP=$(USE_STOCK_BEEP) \
		--meta ENABLE_REVERSE_OUT=$(ENABLE_REVERSE_OUT) \
		--meta ENABLE_TEYES_UART=$(ENABLE_TEYES_UART) \
		--meta TEYES_UART_BAUD=$(TEYES_UART_BAUD) \
		--meta CANBOX_HSE_MHZ=$(CANBOX_HSE_MHZ)

sim:
	$(PYTHON) tools/sim_protocol.py
	$(PYTHON) tools/sim_teyes_raise.py

size: $(BUILD)/$(TARGET).elf
	$(SIZE) $<

clean:
	rm -rf $(BUILD)
