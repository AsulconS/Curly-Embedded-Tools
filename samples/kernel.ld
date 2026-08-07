/*
 * Linker script for the QEMU `virt` board: RAM starts at 0x40000000.
 *
 * Ctrl+click (or Ctrl+B) on any `> FLASH` / `AT> RAM` below to jump to its MEMORY declaration.
 */
OUTPUT_FORMAT("elf64-littleaarch64")
OUTPUT_ARCH(aarch64)
ENTRY(_start)

MEMORY
{
    FLASH (rx)  : ORIGIN = 0x00000000, LENGTH = 2M
    RAM   (rwx) : ORIGIN = 0x40000000, LENGTH = 128M
}

SECTIONS
{
    .text : ALIGN(8)
    {
        KEEP(*(.text.boot))
        *(.text .text.*)
        *(.rodata .rodata.*)
    } > FLASH

    .data : ALIGN(8)
    {
        __data_start = .;
        *(.data .data.*)
        __data_end = .;
    } > RAM AT> FLASH

    .bss (NOLOAD) : ALIGN(16)
    {
        __bss_start = .;
        *(.bss .bss.* COMMON)
        . = ALIGN(16);
        __bss_end = .;
    } > RAM

    . = ALIGN(16);
    __stack_top = ORIGIN(RAM) + LENGTH(RAM);

    /DISCARD/ : { *(.comment) *(.note.*) }
}

/* ==========================================================================
 * Deliberate mistakes. Every line below should be reported.
 * ========================================================================== */

MEMORY
{
    SRAM (rw) : ORIGIN = 0x50000000              /* LENGTH is not optional */
    RAM  (rw) : ORIGIN = 0, LENGTH = 1K          /* RAM is already declared above */
}

SECTIONS
{
    .heap : { *(.heap) } > SRMA                  /* unknown region -> Alt+Enter offers `SRAM` */
    .stack { *(.stack) } > RAM                   /* missing ':' after the section name */
    __end = .                                    /* missing ';' */
}
