#include <stdint.h>

#define STR2(x) #x
#define STR(x) STR2(x)

#define SCB_VTOR_REG (*(volatile uint32_t *)0xE000ED08u)
#define SCB_CCR_REG (*(volatile uint32_t *)0xE000ED14u)
#define SCB_CCR_STKALIGN (1u << 9)

extern uint32_t _data_loadaddr;
extern uint32_t _data;
extern uint32_t _edata;
extern uint32_t _ebss;

typedef void (*funcp_t)(void);
extern funcp_t __preinit_array_start[];
extern funcp_t __preinit_array_end[];
extern funcp_t __init_array_start[];
extern funcp_t __init_array_end[];
extern funcp_t __fini_array_start[];
extern funcp_t __fini_array_end[];

void main(void);

static void call_array(funcp_t *begin, funcp_t *end)
{
    for (funcp_t *fp = begin; fp < end; ++fp) {
        (*fp)();
    }
}

void __attribute__((noreturn)) app_c_start(void)
{
    uint32_t *src = &_data_loadaddr;
    uint32_t *dest = &_data;

    while (dest < &_edata) {
        *dest++ = *src++;
    }
    while (dest < &_ebss) {
        *dest++ = 0u;
    }

    SCB_CCR_REG |= SCB_CCR_STKALIGN;

    call_array(__preinit_array_start, __preinit_array_end);
    call_array(__init_array_start, __init_array_end);

    main();

    call_array(__fini_array_start, __fini_array_end);
    for (;;) {
        __asm volatile("wfi");
    }
}

void __attribute__((naked, used, section(".entry_text"))) reset_handler(void)
{
    __asm volatile(
        "cpsid i\n"
        "ldr r0, =" STR(APP_VECTOR_BASE) "\n"
        "ldr r1, [r0]\n"
        "msr msp, r1\n"
        "movs r1, #0\n"
        "msr control, r1\n"
        "ldr r2, =0xE000ED08\n"
        "str r0, [r2]\n"
        "dsb\n"
        "isb\n"
        "ldr r0, =app_c_start\n"
        "bx r0\n"
    );
}
