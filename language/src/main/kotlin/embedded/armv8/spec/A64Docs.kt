package com.asulcons.embedded.armv8.spec

/** One-line summaries surfaced by Quick Documentation and by the completion popup. */
object A64Docs {

    val MNEMONICS: Map<String, String> = mapOf(
        "adc" to "Add with carry.",
        "add" to "Add. `add Xd, Xn, #imm12` or `add Xd, Xn, Xm{, shift}`.",
        "adds" to "Add and set the NZCV condition flags.",
        "adr" to "Form a PC-relative address, ±1 MiB.",
        "adrp" to "Form a PC-relative address to a 4 KiB page, ±4 GiB. Pair it with `:lo12:` to reach the byte.",
        "and" to "Bitwise AND.",
        "ands" to "Bitwise AND, setting the condition flags.",
        "asr" to "Arithmetic shift right (sign preserving).",
        "at" to "Address translate: run a stage of the MMU walk and report the result in `PAR_EL1`.",
        "b" to "Unconditional branch, ±128 MiB.",
        "bfi" to "Bitfield insert.",
        "bfxil" to "Bitfield extract and insert low.",
        "bic" to "Bit clear: `Rd = Rn AND NOT Rm`.",
        "bl" to "Branch with link; the return address goes to X30.",
        "blr" to "Branch with link to a register.",
        "br" to "Branch to a register.",
        "brk" to "Breakpoint: raises a debug exception.",
        "cbnz" to "Compare and branch if not zero.",
        "cbz" to "Compare and branch if zero.",
        "ccmn" to "Conditional compare negative.",
        "ccmp" to "Conditional compare; feeds NZCV without a branch.",
        "clz" to "Count leading zeros.",
        "cmn" to "Compare negative — `adds` discarding the result.",
        "cmp" to "Compare — `subs` discarding the result.",
        "csel" to "Conditional select: `Rd = cond ? Rn : Rm`.",
        "cset" to "Conditional set: `Rd = cond ? 1 : 0`.",
        "csetm" to "Conditional set mask: `Rd = cond ? -1 : 0`.",
        "csinc" to "Conditional select increment.",
        "csinv" to "Conditional select invert.",
        "csneg" to "Conditional select negate.",
        "dc" to "Data cache maintenance, e.g. `dc civac, x0`.",
        "dmb" to "Data memory barrier; orders memory accesses without waiting for completion.",
        "dsb" to "Data synchronization barrier; blocks until earlier accesses complete.",
        "eor" to "Bitwise exclusive OR.",
        "eret" to "Exception return: restores PC from ELR_ELn and PSTATE from SPSR_ELn.",
        "extr" to "Extract a register pair as a 64-bit rotate.",
        "hvc" to "Hypervisor call.",
        "ic" to "Instruction cache maintenance, e.g. `ic iallu`.",
        "isb" to "Instruction synchronization barrier; flushes the pipeline.",
        "ldar" to "Load-acquire register.",
        "ldaxr" to "Load-acquire exclusive register; pairs with `stlxr`.",
        "ldp" to "Load pair of registers.",
        "ldr" to "Load register. `ldr Xt, =sym` asks the assembler for a literal-pool entry.",
        "ldrb" to "Load byte, zero-extending.",
        "ldrh" to "Load halfword, zero-extending.",
        "ldrsw" to "Load word, sign-extending to 64 bits.",
        "ldur" to "Load register with an unscaled signed offset, ±256 bytes.",
        "ldxr" to "Load exclusive register; pairs with `stxr`.",
        "lsl" to "Logical shift left.",
        "lsr" to "Logical shift right.",
        "madd" to "Multiply-add: `Rd = Ra + Rn * Rm`.",
        "mov" to "Move register or immediate (an alias of `orr`/`add`/`movz`).",
        "movk" to "Move 16-bit immediate, keeping the other bits. Build 64-bit constants with `movz` + `movk`.",
        "movn" to "Move the bitwise NOT of a 16-bit immediate.",
        "movz" to "Move a 16-bit immediate, zeroing the rest.",
        "mrs" to "Move a system register into a general-purpose register.",
        "msr" to "Move to a system register, or set a PSTATE field such as `daifset`.",
        "msub" to "Multiply-subtract: `Rd = Ra - Rn * Rm`.",
        "mul" to "Multiply.",
        "mvn" to "Bitwise NOT.",
        "neg" to "Negate.",
        "nop" to "No operation.",
        "orr" to "Bitwise inclusive OR.",
        "prfm" to "Prefetch memory, e.g. `prfm pldl1keep, [x0]`.",
        "ret" to "Return from subroutine; branches to X30 unless another register is given.",
        "rev" to "Reverse byte order.",
        "ror" to "Rotate right.",
        "sbfx" to "Signed bitfield extract.",
        "sdiv" to "Signed divide; division by zero yields zero rather than trapping.",
        "smc" to "Secure monitor call.",
        "smull" to "Signed multiply long (32 × 32 → 64).",
        "stlr" to "Store-release register.",
        "stlxr" to "Store-release exclusive register; returns 0 on success.",
        "stp" to "Store pair of registers. `stp x29, x30, [sp, #-16]!` is the standard prologue.",
        "str" to "Store register.",
        "strb" to "Store byte.",
        "strh" to "Store halfword.",
        "stur" to "Store register with an unscaled signed offset, ±256 bytes.",
        "stxr" to "Store exclusive register; returns 0 on success.",
        "sub" to "Subtract.",
        "subs" to "Subtract and set the condition flags.",
        "svc" to "Supervisor call — the syscall instruction.",
        "sxtb" to "Sign-extend byte.",
        "sxth" to "Sign-extend halfword.",
        "sxtw" to "Sign-extend word.",
        "sys" to "Execute a system instruction by its CRn/CRm/op encoding.",
        "tbnz" to "Test a single bit and branch if it is set.",
        "tbz" to "Test a single bit and branch if it is clear.",
        "tlbi" to "TLB invalidate, e.g. `tlbi vmalle1is`.",
        "tst" to "Test bits — `ands` discarding the result.",
        "ubfx" to "Unsigned bitfield extract.",
        "udiv" to "Unsigned divide.",
        "umull" to "Unsigned multiply long (32 × 32 → 64).",
        "uxtb" to "Zero-extend byte.",
        "uxth" to "Zero-extend halfword.",
        "wfe" to "Wait for event; a low-power spin used to park secondary cores.",
        "wfi" to "Wait for interrupt.",
        "yield" to "Hint that this thread is in a spin loop.",
        // Advanced SIMD & FP
        "fadd" to "Floating-point add.",
        "fcmp" to "Floating-point compare, setting NZCV.",
        "fcvt" to "Convert between floating-point precisions.",
        "fcvtzs" to "Convert floating-point to signed integer, rounding toward zero.",
        "fdiv" to "Floating-point divide.",
        "fmadd" to "Fused multiply-add.",
        "fmov" to "Move between FP/SIMD and general-purpose registers, or load an FP immediate.",
        "fmul" to "Floating-point multiply.",
        "fsqrt" to "Floating-point square root.",
        "fsub" to "Floating-point subtract.",
        "dup" to "Duplicate a scalar or element across a vector.",
        "ld1" to "Load one-element structures to one or more vector registers.",
        "st1" to "Store one-element structures from one or more vector registers.",
        "movi" to "Move an immediate into every lane of a vector.",
        "scvtf" to "Convert a signed integer to floating point.",
        "ucvtf" to "Convert an unsigned integer to floating point.",
        "umov" to "Move a vector element to a general-purpose register.",
        "ins" to "Insert a value into a vector element.",
        "tbl" to "Table lookup across a vector register list.",
        "zip1" to "Interleave the lower halves of two vectors.",
        "zip2" to "Interleave the upper halves of two vectors.",
    )

    val DIRECTIVES: Map<String, String> = mapOf(
        ".align" to "Align the location counter. On AArch64 the operand is a power of two, like `.p2align`.",
        ".arch" to "Select the target architecture, e.g. `.arch armv8-a+crc`.",
        ".ascii" to "Emit a string without a trailing NUL.",
        ".asciz" to "Emit a NUL-terminated string. Same as `.string`.",
        ".balign" to "Align the location counter to a byte multiple.",
        ".bss" to "Switch to the `.bss` section.",
        ".byte" to "Emit 8-bit values.",
        ".comm" to "Declare a common symbol of a given size.",
        ".cpu" to "Select the target CPU, e.g. `.cpu cortex-a53`.",
        ".data" to "Switch to the `.data` section.",
        ".double" to "Emit 64-bit floating-point values.",
        ".else" to "Alternative branch of a `.if` block.",
        ".endif" to "Close a `.if` block.",
        ".endm" to "Close a `.macro` definition.",
        ".endr" to "Close a `.rept`, `.irp` or `.irpc` block.",
        ".equ" to "Define a symbol: `.equ NAME, expression`.",
        ".extern" to "Declare a symbol as defined elsewhere. GNU `as` ignores it; it documents intent.",
        ".fill" to "Emit `count` copies of a value: `.fill count, size, value`.",
        ".float" to "Emit 32-bit floating-point values.",
        ".global" to "Make a symbol visible to the linker. Spelled `.globl` as well.",
        ".globl" to "Make a symbol visible to the linker. Spelled `.global` as well.",
        ".hidden" to "Give a symbol hidden ELF visibility.",
        ".hword" to "Emit 16-bit values.",
        ".if" to "Begin conditional assembly.",
        ".incbin" to "Include a file verbatim as binary data.",
        ".include" to "Include another assembly source file.",
        ".irp" to "Repeat a block once per argument, substituting a macro parameter.",
        ".lcomm" to "Reserve local (`.bss`) storage for a symbol.",
        ".ltorg" to "Dump the pending literal pool here — needed before it drifts out of `ldr` range.",
        ".macro" to "Begin a macro definition: `.macro name, params…`, closed by `.endm`.",
        ".org" to "Advance the location counter to an absolute offset.",
        ".p2align" to "Align the location counter to a power-of-two boundary.",
        ".popsection" to "Return to the section saved by `.pushsection`.",
        ".pushsection" to "Switch section, remembering the current one for `.popsection`.",
        ".quad" to "Emit 64-bit values. Same as `.xword`.",
        ".rept" to "Repeat a block a fixed number of times, closed by `.endr`.",
        ".req" to "Define a register alias: `name .req x9`.",
        ".section" to "Switch to a named section: `.section .text.boot, \"ax\", %progbits`.",
        ".set" to "Define or redefine a symbol: `.set NAME, expression`.",
        ".size" to "Record a symbol's size, usually `.size name, .-name`.",
        ".skip" to "Reserve zero-filled bytes. Same as `.space`.",
        ".space" to "Reserve zero-filled bytes. Same as `.skip`.",
        ".string" to "Emit a NUL-terminated string.",
        ".text" to "Switch to the `.text` section.",
        ".type" to "Record a symbol's ELF type, e.g. `.type name, %function`.",
        ".unreq" to "Drop a register alias created by `.req`.",
        ".weak" to "Mark a symbol as weak so a strong definition can override it.",
        ".word" to "Emit 32-bit values.",
        ".xword" to "Emit 64-bit values. Same as `.quad`.",
        ".zero" to "Emit a run of zero bytes.",
    )

    fun forRegister(rawName: String): String? {
        val name = rawName.lowercase()
        val base = name.substringBefore('.').substringBefore('/')
        return when {
            name == "sp" -> "Stack pointer. Must stay 16-byte aligned whenever it is used as a base address."
            name == "wsp" -> "Low 32 bits of the stack pointer."
            name == "xzr" -> "64-bit zero register: reads as 0, discards writes."
            name == "wzr" -> "32-bit zero register: reads as 0, discards writes."
            name == "lr" -> "Link register (X30). Holds the return address set by `bl`."
            name == "fp" -> "Frame pointer (X29) by AAPCS64 convention."
            name == "pc" -> "Program counter. Readable only through `adr`/`adrp` and branches."
            base.startsWith("x") -> "64-bit general-purpose register. ${roleOf(base.drop(1).toIntOrNull())}"
            base.startsWith("w") -> "Low 32 bits of the matching X register. ${roleOf(base.drop(1).toIntOrNull())}"
            base.startsWith("v") -> "128-bit SIMD register, viewed through an arrangement such as `.16b` or `.4s`."
            base.startsWith("q") -> "128-bit view of a SIMD register."
            base.startsWith("d") -> "64-bit (double-precision) view of a SIMD register."
            base.startsWith("s") -> "32-bit (single-precision) view of a SIMD register."
            base.startsWith("h") -> "16-bit (half-precision) view of a SIMD register."
            base.startsWith("b") -> "8-bit view of a SIMD register."
            base.startsWith("z") -> "SVE scalable vector register."
            base.startsWith("p") -> "SVE predicate register; `/z` zeroes and `/m` merges inactive lanes."
            else -> null
        }
    }

    /** AAPCS64 role of a general-purpose register, which is what a caller usually wants to know. */
    private fun roleOf(index: Int?): String = when (index) {
        null -> ""
        in 0..7 -> "Argument and result register (AAPCS64), caller-saved."
        8 -> "Indirect result location register, caller-saved."
        in 9..15 -> "Temporary register, caller-saved."
        16, 17 -> "Intra-procedure-call scratch register (IP0/IP1); a linker veneer may clobber it."
        18 -> "Platform register — reserved on some ABIs, avoid it in portable code."
        in 19..28 -> "Callee-saved register: save it in the prologue if you use it."
        29 -> "Frame pointer."
        30 -> "Link register."
        else -> ""
    }
}
