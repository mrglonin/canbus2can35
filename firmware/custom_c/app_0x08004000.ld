/* Application slot used by 2CAN35/Sigma10 USB update packages. */

MEMORY
{
	rom (rx) : ORIGIN = 0x08004000, LENGTH = 48K
	ram (rwx) : ORIGIN = 0x20000000, LENGTH = 64K
}

INCLUDE libopencm3_stm32f1.ld
