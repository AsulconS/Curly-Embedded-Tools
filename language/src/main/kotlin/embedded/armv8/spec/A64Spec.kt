package com.asulcons.embedded.armv8.spec

/**
 * The instruction/directive vocabulary the inspections and completion work from.
 *
 * It deliberately covers the A64 base set, the Advanced SIMD & FP set, the crypto extensions and the
 * commonly written SVE subset rather than trying to be the whole Arm ARM: an over-eager "unknown
 * mnemonic" report is worse than a missed one, which is why [isKnownMnemonic] also forgives the
 * mechanical spellings (condition suffixes, the `2` upper-half variants).
 */
object A64Spec {

    val CONDITION_CODES: Set<String> = setOf(
        "eq", "ne", "cs", "hs", "cc", "lo", "mi", "pl", "vs", "vc",
        "hi", "ls", "ge", "lt", "gt", "le", "al", "nv",
    )

    val SHIFT_OPERATORS: Set<String> = setOf("lsl", "lsr", "asr", "ror", "msl")

    val EXTEND_OPERATORS: Set<String> = setOf(
        "uxtb", "uxth", "uxtw", "uxtx", "sxtb", "sxth", "sxtw", "sxtx",
    )

    /** Operand keywords for `dmb`/`dsb`/`isb`, `prfm` and `tlbi`/`at`/`dc`/`ic`. */
    val OPERAND_KEYWORDS: Set<String> = setOf(
        // barrier options
        "sy", "st", "ld", "ish", "ishst", "ishld", "nsh", "nshst", "nshld", "osh", "oshst", "oshld",
        // prefetch operations
        "pldl1keep", "pldl1strm", "pldl2keep", "pldl2strm", "pldl3keep", "pldl3strm",
        "plil1keep", "plil1strm", "plil2keep", "plil2strm", "plil3keep", "plil3strm",
        "pstl1keep", "pstl1strm", "pstl2keep", "pstl2strm", "pstl3keep", "pstl3strm",
        // cache / TLB operations
        "ialluis", "iallu", "ivau", "zva", "cvac", "cvau", "civac", "isw", "csw", "cisw",
        "vmalle1", "vmalle1is", "vae1", "vae1is", "aside1", "aside1is", "alle1", "alle2", "alle3",
        // PSTATE fields for `msr`
        "daifset", "daifclr", "spsel", "uao", "pan", "dit", "ssbs", "tco", "sm", "za",
    )

    /** Frequently written system registers; the pattern `*_elN` is accepted wholesale as well. */
    val SYSTEM_REGISTERS: Set<String> = setOf(
        "nzcv", "daif", "fpcr", "fpsr", "spsel", "currentel", "pan", "uao", "dit", "ssbs", "tco",
        "midr_el1", "mpidr_el1", "sctlr_el1", "sctlr_el2", "sctlr_el3",
        "actlr_el1", "cpacr_el1", "cptr_el2", "cptr_el3", "scr_el3", "hcr_el2",
        "ttbr0_el1", "ttbr1_el1", "ttbr0_el2", "ttbr0_el3", "tcr_el1", "tcr_el2", "tcr_el3",
        "mair_el1", "mair_el2", "mair_el3", "amair_el1",
        "vbar_el1", "vbar_el2", "vbar_el3", "isr_el1",
        "elr_el1", "elr_el2", "elr_el3", "spsr_el1", "spsr_el2", "spsr_el3",
        "sp_el0", "sp_el1", "sp_el2", "esr_el1", "esr_el2", "esr_el3",
        "far_el1", "far_el2", "far_el3", "par_el1",
        "tpidr_el0", "tpidr_el1", "tpidr_el2", "tpidr_el3", "tpidrro_el0",
        "cntfrq_el0", "cntpct_el0", "cntvct_el0",
        "cntp_ctl_el0", "cntp_cval_el0", "cntp_tval_el0",
        "cntv_ctl_el0", "cntv_cval_el0", "cntv_tval_el0",
        "id_aa64pfr0_el1", "id_aa64pfr1_el1", "id_aa64isar0_el1", "id_aa64isar1_el1",
        "id_aa64mmfr0_el1", "id_aa64mmfr1_el1", "id_aa64mmfr2_el1", "id_aa64dfr0_el1",
        "ctr_el0", "dczid_el0", "clidr_el1", "ccsidr_el1", "csselr_el1", "revidr_el1",
        "mdscr_el1", "oslar_el1", "osdlr_el1", "vttbr_el2", "vtcr_el2", "hstr_el2", "vpidr_el2", "vmpidr_el2",
    )

    /** Relocation specifiers written as `:name:` in front of a symbol. */
    val RELOCATION_SPECIFIERS: List<String> = listOf(
        "lo12", "abs_g0", "abs_g0_nc", "abs_g1", "abs_g1_nc", "abs_g2", "abs_g2_nc", "abs_g3",
        "prel_g0", "prel_g0_nc", "prel_g1", "prel_g1_nc", "prel_g2", "prel_g2_nc", "prel_g3",
        "pg_hi21", "pg_hi21_nc", "got", "got_lo12", "got_page_lo15", "gotpage_lo15",
        "gottprel", "gottprel_lo12", "gottprel_g0_nc", "gottprel_g1",
        "tprel_hi12", "tprel_lo12", "tprel_lo12_nc", "tprel_g0", "tprel_g0_nc",
        "tprel_g1", "tprel_g1_nc", "tprel_g2",
        "tlsdesc", "tlsdesc_lo12", "tlsgd", "tlsgd_lo12", "tlsld", "tlsldm", "dtprel_hi12", "dtprel_lo12",
    )

    val BASE_MNEMONICS: Set<String> = setOf(
        "abs", "adc", "adcs", "add", "addg", "adds", "adr", "adrp", "and", "ands", "asr", "asrv",
        "at", "autda", "autdza", "autdb", "autdzb", "autia", "autia1716", "autiasp", "autiaz",
        "autiza", "autib", "autib1716", "autibsp", "autibz", "autizb", "axflag",
        "b", "bc", "bfc", "bfi", "bfm", "bfxil", "bic", "bics", "bl", "blr", "blraa", "blraaz",
        "blrab", "blrabz", "br", "braa", "braaz", "brab", "brabz", "brk", "bti",
        "cas", "casa", "casab", "casah", "casal", "casalb", "casalh", "casb", "cash",
        "casp", "caspa", "caspal", "caspl", "cbnz", "cbz", "ccmn", "ccmp", "cfinv",
        "chkfeat", "cinc", "cinv", "clrex", "cls", "clz", "cmn", "cmp", "cmpp", "cneg",
        "crc32b", "crc32cb", "crc32ch", "crc32cw", "crc32cx", "crc32h", "crc32w", "crc32x",
        "csdb", "csel", "cset", "csetm", "csinc", "csinv", "csneg",
        "dc", "dcps1", "dcps2", "dcps3", "dgh", "dmb", "drps", "dsb",
        "eon", "eor", "eret", "eretaa", "eretab", "esb", "extr",
        "gmi", "hint", "hlt", "hvc", "ic", "isb",
        "ldadd", "ldadda", "ldaddal", "ldaddl", "ldaddb", "ldaddab", "ldaddalb", "ldaddlb",
        "ldaddh", "ldaddah", "ldaddalh", "ldaddlh",
        "ldapr", "ldaprb", "ldaprh", "ldapur", "ldapurb", "ldapurh", "ldapursb", "ldapursh", "ldapursw",
        "ldar", "ldarb", "ldarh", "ldaxp", "ldaxr", "ldaxrb", "ldaxrh",
        "ldclr", "ldclra", "ldclral", "ldclrl", "ldclrb", "ldclrh",
        "ldeor", "ldeora", "ldeoral", "ldeorl", "ldeorb", "ldeorh",
        "ldg", "ldgm", "ldlar", "ldlarb", "ldlarh", "ldnp", "ldp", "ldpsw",
        "ldr", "ldraa", "ldrab", "ldrb", "ldrh", "ldrsb", "ldrsh", "ldrsw",
        "ldset", "ldseta", "ldsetal", "ldsetl", "ldsetb", "ldseth",
        "ldsmax", "ldsmaxa", "ldsmaxal", "ldsmaxl", "ldsmin", "ldsmina", "ldsminal", "ldsminl",
        "ldtr", "ldtrb", "ldtrh", "ldtrsb", "ldtrsh", "ldtrsw",
        "ldumax", "ldumaxa", "ldumaxal", "ldumaxl", "ldumin", "ldumina", "lduminal", "lduminl",
        "ldur", "ldurb", "ldurh", "ldursb", "ldursh", "ldursw", "ldxp", "ldxr", "ldxrb", "ldxrh",
        "lsl", "lslv", "lsr", "lsrv",
        "madd", "mneg", "mov", "movk", "movn", "movz", "mrs", "msr", "msub", "mul", "mvn",
        "neg", "negs", "ngc", "ngcs", "nop",
        "orn", "orr",
        "pacda", "pacdza", "pacdb", "pacdzb", "pacga", "pacia", "pacia1716", "paciasp", "paciaz",
        "paciza", "pacib", "pacib1716", "pacibsp", "pacibz", "pacizb",
        "prfm", "prfum", "psb", "pssbb",
        "rbit", "ret", "retaa", "retab", "rev", "rev16", "rev32", "rev64", "rmif", "ror", "rorv",
        "sb", "sbc", "sbcs", "sbfiz", "sbfm", "sbfx", "sdiv", "setf8", "setf16", "sev", "sevl",
        "smaddl", "smc", "smnegl", "smsubl", "smulh", "smull", "ssbb",
        "st2g", "stadd", "staddb", "staddh", "staddl", "stclr", "stclrb", "stclrh",
        "steor", "steorb", "steorh", "stg", "stgm", "stgp",
        "stllr", "stllrb", "stllrh", "stlr", "stlrb", "stlrh", "stlur", "stlurb", "stlurh",
        "stlxp", "stlxr", "stlxrb", "stlxrh", "stnp", "stp", "str", "strb", "strh",
        "stset", "stsetb", "stseth", "stsmax", "stsmin", "sttr", "sttrb", "sttrh",
        "stumax", "stumin", "stur", "sturb", "sturh", "stxp", "stxr", "stxrb", "stxrh",
        "stz2g", "stzg", "stzgm",
        "sub", "subg", "subp", "subps", "subs", "svc", "swp", "swpa", "swpal", "swpl",
        "swpb", "swph", "sxtb", "sxth", "sxtw", "sys", "sysl",
        "tbnz", "tbz", "tlbi", "tst", "tsb",
        "ubfiz", "ubfm", "ubfx", "udf", "udiv", "umaddl", "umnegl", "umsubl", "umulh", "umull",
        "uxtb", "uxth", "uxtw",
        "wfe", "wfi", "wfet", "wfit",
        "xaflag", "xpacd", "xpaci", "xpaclri", "yield",
    )

    val SIMD_MNEMONICS: Set<String> = setOf(
        "abs", "add", "addhn", "addp", "addv", "aesd", "aese", "aesimc", "aesmc",
        "and", "bcax", "bic", "bif", "bit", "bsl",
        "cmeq", "cmge", "cmgt", "cmhi", "cmhs", "cmle", "cmlt", "cmtst", "cnt",
        "dup", "eor", "eor3", "ext",
        "fabd", "fabs", "facge", "facgt", "fadd", "faddp", "fcadd", "fccmp", "fccmpe",
        "fcmeq", "fcmge", "fcmgt", "fcmla", "fcmle", "fcmlt", "fcmp", "fcmpe", "fcsel",
        "fcvt", "fcvtas", "fcvtau", "fcvtl", "fcvtms", "fcvtmu", "fcvtn", "fcvtns", "fcvtnu",
        "fcvtps", "fcvtpu", "fcvtxn", "fcvtzs", "fcvtzu", "fdiv", "fjcvtzs",
        "fmadd", "fmax", "fmaxnm", "fmaxnmp", "fmaxnmv", "fmaxp", "fmaxv",
        "fmin", "fminnm", "fminnmp", "fminnmv", "fminp", "fminv",
        "fmla", "fmlal", "fmls", "fmlsl", "fmov", "fmsub", "fmul", "fmulx",
        "fneg", "fnmadd", "fnmsub", "fnmul",
        "frecpe", "frecps", "frecpx", "frint32x", "frint32z", "frint64x", "frint64z",
        "frinta", "frinti", "frintm", "frintn", "frintp", "frintx", "frintz",
        "frsqrte", "frsqrts", "fsqrt", "fsub",
        "ins", "ld1", "ld1r", "ld2", "ld2r", "ld3", "ld3r", "ld4", "ld4r",
        "mla", "mls", "mov", "movi", "mul", "mvn", "mvni",
        "neg", "not", "orn", "orr", "pmul", "pmull",
        "raddhn", "rax1", "rbit", "rev16", "rev32", "rev64", "rshrn", "rsubhn",
        "saba", "sabal", "sabd", "sabdl", "sadalp", "saddl", "saddlp", "saddlv", "saddw",
        "scvtf", "sdot",
        "sha1c", "sha1h", "sha1m", "sha1p", "sha1su0", "sha1su1",
        "sha256h", "sha256h2", "sha256su0", "sha256su1",
        "sha512h", "sha512h2", "sha512su0", "sha512su1",
        "shadd", "shl", "shll", "shrn", "shsub", "sli",
        "sm3partw1", "sm3partw2", "sm3ss1", "sm3tt1a", "sm3tt1b", "sm3tt2a", "sm3tt2b",
        "sm4e", "sm4ekey",
        "smax", "smaxp", "smaxv", "smin", "sminp", "sminv", "smlal", "smlsl", "smmla", "smov", "smull",
        "sqabs", "sqadd", "sqdmlal", "sqdmlsl", "sqdmulh", "sqdmull", "sqneg",
        "sqrdmlah", "sqrdmlsh", "sqrdmulh", "sqrshl", "sqrshrn", "sqrshrun",
        "sqshl", "sqshlu", "sqshrn", "sqshrun", "sqsub", "sqxtn", "sqxtun",
        "srhadd", "sri", "srshl", "srshr", "srsra", "sshl", "sshll", "sshr", "ssra",
        "ssubl", "ssubw", "st1", "st2", "st3", "st4", "sub", "subhn", "sudot", "suqadd", "sxtl",
        "tbl", "tbx", "trn1", "trn2",
        "uaba", "uabal", "uabd", "uabdl", "uadalp", "uaddl", "uaddlp", "uaddlv", "uaddw",
        "ucvtf", "udot", "uhadd", "uhsub",
        "umax", "umaxp", "umaxv", "umin", "uminp", "uminv", "umlal", "umlsl", "ummla", "umov", "umull",
        "uqadd", "uqrshl", "uqrshrn", "uqshl", "uqshrn", "uqsub", "uqxtn",
        "urecpe", "urhadd", "urshl", "urshr", "ursqrte", "ursra", "usdot",
        "ushl", "ushll", "ushr", "usmmla", "usqadd", "usra", "usubl", "usubw", "uxtl",
        "uzp1", "uzp2", "xar", "xtn", "zip1", "zip2",
    )

    /** The most commonly hand-written SVE/SVE2 instructions. */
    val SVE_MNEMONICS: Set<String> = setOf(
        "addpl", "addvl", "cntb", "cntd", "cnth", "cntp", "cntw",
        "decb", "decd", "dech", "decp", "decw", "incb", "incd", "inch", "incp", "incw",
        "index", "ld1b", "ld1d", "ld1h", "ld1w", "ld1sb", "ld1sh", "ld1sw", "ldff1b", "ldff1d",
        "ldnf1b", "ldr", "pfalse", "pnext", "prfb", "prfd", "prfh", "prfw",
        "ptest", "ptrue", "ptrues", "rdvl", "sel", "setffr", "st1b", "st1d", "st1h", "st1w",
        "whilele", "whilelo", "whilels", "whilelt", "wrffr", "rdffr",
    )

    val ALL_MNEMONICS: Set<String> = BASE_MNEMONICS + SIMD_MNEMONICS + SVE_MNEMONICS

    val DIRECTIVES: Set<String> = setOf(
        ".abort", ".align", ".altmacro", ".arch", ".arch_extension", ".arm", ".ascii", ".asciz",
        ".balign", ".balignl", ".balignw", ".bss", ".byte",
        ".cfi_adjust_cfa_offset", ".cfi_def_cfa", ".cfi_def_cfa_offset", ".cfi_def_cfa_register",
        ".cfi_endproc", ".cfi_escape", ".cfi_lsda", ".cfi_offset", ".cfi_personality",
        ".cfi_register", ".cfi_remember_state", ".cfi_restore", ".cfi_restore_state",
        ".cfi_return_column", ".cfi_rel_offset", ".cfi_same_value", ".cfi_sections",
        ".cfi_signal_frame", ".cfi_startproc", ".cfi_undefined", ".cfi_val_offset", ".cfi_window_save",
        ".comm", ".cpu", ".data", ".dc", ".dcb", ".ds", ".double", ".dword",
        ".eabi_attribute", ".egroup", ".eject", ".else", ".elseif", ".end", ".endfunc", ".endif",
        ".endm", ".endr", ".equ", ".equiv", ".eqv", ".err", ".error", ".exitm", ".extern",
        ".fail", ".file", ".fill", ".float", ".func",
        ".global", ".globl", ".gnu_attribute", ".hidden", ".hword",
        ".ident", ".if", ".ifb", ".ifc", ".ifdef", ".ifeq", ".ifeqs", ".ifge", ".ifgt", ".ifle",
        ".iflt", ".ifnb", ".ifnc", ".ifndef", ".ifne", ".ifnes", ".ifnotdef",
        ".inst", ".include", ".incbin", ".int", ".internal", ".irp", ".irpc",
        ".lcomm", ".line", ".linkonce", ".list", ".ln", ".loc", ".loc_mark_labels", ".local", ".long",
        ".ltorg", ".macro", ".mri",
        ".noaltmacro", ".nolist", ".octa", ".offset", ".org",
        ".p2align", ".p2alignl", ".p2alignw", ".pool", ".popsection", ".previous", ".print",
        ".protected", ".psize", ".purgem", ".pushsection",
        ".quad", ".rept", ".req", ".rodata",
        ".sbttl", ".scl", ".section", ".seh_endproc", ".seh_proc", ".set", ".short", ".single",
        ".size", ".skip", ".sleb128", ".space", ".string", ".string8", ".string16",
        ".struct", ".subsection", ".symver",
        ".text", ".title", ".tlsdescseq", ".type", ".uleb128", ".unreq", ".variant_pcs",
        ".version", ".vsave", ".warning", ".weak", ".weakref", ".word", ".xword", ".zero",
    )

    /** Directives whose first argument introduces a symbol name. */
    val SYMBOL_DEFINING_DIRECTIVES: Set<String> = setOf(
        ".equ", ".equiv", ".eqv", ".set", ".macro", ".comm", ".lcomm", ".req",
    )

    /** Directives that open a region terminated by the matching entry in [REGION_CLOSERS]. */
    val REGION_OPENERS: Map<String, String> = mapOf(
        ".macro" to ".endm",
        ".rept" to ".endr",
        ".irp" to ".endr",
        ".irpc" to ".endr",
        ".if" to ".endif",
        ".ifdef" to ".endif",
        ".ifndef" to ".endif",
        ".ifb" to ".endif",
        ".ifnb" to ".endif",
        ".ifc" to ".endif",
        ".ifnc" to ".endif",
        ".ifeq" to ".endif",
        ".ifne" to ".endif",
        ".ifeqs" to ".endif",
        ".ifnes" to ".endif",
        ".ifge" to ".endif",
        ".ifgt" to ".endif",
        ".ifle" to ".endif",
        ".iflt" to ".endif",
        ".ifnotdef" to ".endif",
        ".func" to ".endfunc",
        ".pushsection" to ".popsection",
    )

    val REGION_CLOSERS: Set<String> = REGION_OPENERS.values.toSet()

    /** `.else`/`.elseif` are legal only between an `.if`-family opener and its `.endif`. */
    val REGION_CONTINUATIONS: Set<String> = setOf(".else", ".elseif")

    private val CONDITIONAL_PREFIXES = setOf("b", "bc")

    /**
     * True when [rawMnemonic] names something GNU `as` would accept, allowing for the spellings that
     * are generated rather than listed: `b.<cond>`, the `2` upper-half SIMD forms, and the `w`/`x`
     * width suffixes some assemblers tolerate.
     */
    fun isKnownMnemonic(rawMnemonic: String): Boolean {
        val mnemonic = rawMnemonic.lowercase()
        if (mnemonic in ALL_MNEMONICS) return true

        val dot = mnemonic.indexOf('.')
        if (dot > 0) {
            val head = mnemonic.substring(0, dot)
            val tail = mnemonic.substring(dot + 1)
            if (head in CONDITIONAL_PREFIXES && tail in CONDITION_CODES) return true
            // `ld1.16b`-style separators used by some assemblers
            if (head in ALL_MNEMONICS) return true
        }

        // Upper-half SIMD variants: `saddl2`, `sqdmull2`, `xtn2`, …
        if (mnemonic.endsWith("2") && mnemonic.dropLast(1) in SIMD_MNEMONICS) return true

        return false
    }

    fun isKnownDirective(directive: String): Boolean {
        val lower = directive.lowercase()
        if (lower in DIRECTIVES) return true
        // `.cfi_*` and `.seh_*` families keep growing; do not fight the toolchain over them.
        return lower.startsWith(".cfi_") || lower.startsWith(".seh_")
    }

    fun isSystemRegister(name: String): Boolean {
        val lower = name.lowercase()
        if (lower in SYSTEM_REGISTERS) return true
        // Generic encodings (`s3_0_c1_c0_0`) and any `<name>_elN` follow a predictable shape.
        if (lower.matches(Regex("s[0-3]_[0-7]_c(1[0-5]|[0-9])_c(1[0-5]|[0-9])_[0-7]"))) return true
        return lower.matches(Regex("[a-z0-9_]+_el[0-3]"))
    }
}
