/*
 * Minimal AArch64 reset vector for `qemu-system-aarch64 -machine virt`.
 *
 * Everything above the "deliberate mistakes" banner should be clean: no warnings, no squiggles.
 *
 * Ctrl+Click `SAVE_PAIR` and `PUSH_PAIR` below to jump to where they are defined; Alt+F7 on either
 * definition finds the call sites, and Shift+F6 renames both ends together.
 */
#include <asm/config.h>
#define PUSH_PAIR(a, b) stp a, b, [sp, #-16]!

        .arch   armv8-a
        .section .text.boot, "ax", %progbits
        .global _start
        .type   _start, %function

.macro  SAVE_PAIR r1, r2
        stp     \r1, \r2, [sp, #-16]!
.endm

_start:
        mrs     x0, mpidr_el1                   // which core are we on?
        and     x0, x0, #0xff
        cbnz    x0, park                        // secondary cores just spin

        ldr     x1, =__stack_top
        mov     sp, x1

        /* Zero the BSS. */
        adrp    x2, __bss_start
        add     x2, x2, :lo12:__bss_start
        adrp    x3, __bss_end
        add     x3, x3, :lo12:__bss_end
1:      cmp     x2, x3
        b.ge    2f
        str     xzr, [x2], #8
        b       1b
2:
        SAVE_PAIR x29, x30                      // Ctrl+Click -> the .macro above
        PUSH_PAIR x27, x28                      // Ctrl+Click -> the #define at the top
        bl      kernel_main

park:
        wfe
        b       park

        .size   _start, . - _start

/* ==========================================================================
 * Deliberate mistakes. Every line below should be reported.
 * ========================================================================== */

broken:
        mvo     x0, x1                          // unknown instruction  -> Alt+Enter offers `mov`
        mov     x31, x1                         // x31 is not spellable -> offers `sp` / `xzr`
        add     x0, x1, #8192                   // will not fit the 12-bit immediate field
        movz    x0, #0x1234, lsl #8             // a wide move shifts by 0, 16, 32 or 48
        tbz     w0, #40, park                   // bit 40 does not exist in a 32-bit operand
        b       9f                              // there is no `9:` after this point
        ldr     x0, [x1                         // unclosed bracket -> syntax error
        .secton .text                           // unknown directive -> offers `.section`
broken:                                         // this name is already taken
        .endm                                   // closes a `.macro` that was never opened
