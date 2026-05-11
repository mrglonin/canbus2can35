/* 2CAN35 app package with stock-like mode1 entry geometry.
 * Vector table stays at 0x08004000, reset handler is placed at 0x08008CE0.
 */

MEMORY
{
	rom (rx) : ORIGIN = 0x08004000, LENGTH = 48K
	ram (rwx) : ORIGIN = 0x20000000, LENGTH = 64K
}

EXTERN(vector_table)
ENTRY(reset_handler)

SECTIONS
{
	.text ORIGIN(rom) : {
		KEEP(*(.vectors))
		. = 0x4CE0;
		KEEP(*(.entry_text))
		*(.text*)
		. = ALIGN(4);
		*(.rodata*)
		. = ALIGN(4);
	} >rom

	.preinit_array : {
		. = ALIGN(4);
		__preinit_array_start = .;
		KEEP (*(.preinit_array))
		__preinit_array_end = .;
	} >rom

	.init_array : {
		. = ALIGN(4);
		__init_array_start = .;
		KEEP (*(SORT(.init_array.*)))
		KEEP (*(.init_array))
		__init_array_end = .;
	} >rom

	.fini_array : {
		. = ALIGN(4);
		__fini_array_start = .;
		KEEP (*(.fini_array))
		KEEP (*(SORT(.fini_array.*)))
		__fini_array_end = .;
	} >rom

	.ARM.extab : {
		*(.ARM.extab*)
	} >rom

	.ARM.exidx : {
		__exidx_start = .;
		*(.ARM.exidx*)
		__exidx_end = .;
	} >rom

	. = ALIGN(4);
	_etext = .;

	.data : {
		_data = .;
		*(.data*)
		. = ALIGN(4);
		_edata = .;
	} >ram AT >rom
	_data_loadaddr = LOADADDR(.data);

	.bss : {
		*(.bss*)
		*(COMMON)
		. = ALIGN(4);
		_ebss = .;
	} >ram

	/DISCARD/ : { *(.eh_frame) }

	. = ALIGN(4);
	end = .;
}

PROVIDE(_stack = ORIGIN(ram) + LENGTH(ram));
ASSERT(reset_handler == 0x08008CE0, "reset_handler must match stock mode1 entry")
