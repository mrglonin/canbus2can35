/* Mode3 lab slot used by the 2CAN35 wrapper package. */

MEMORY
{
	rom (rx) : ORIGIN = 0x08009000, LENGTH = 16K
	ram (rwx) : ORIGIN = 0x20000000, LENGTH = 64K
}

INCLUDE libopencm3_stm32f1.ld
