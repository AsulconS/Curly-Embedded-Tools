package com.asulcons.embedded.linker.spec

/** The GNU `ld` script vocabulary, split by where each word may appear. */
object LinkerKeywords {

    /** Commands that may stand at the top level of a script. */
    val TOP_LEVEL_COMMANDS: Map<String, String> = mapOf(
        "ENTRY" to "Set the entry point symbol: `ENTRY(_start)`.",
        "INCLUDE" to "Include another linker script.",
        "INPUT" to "Add object files to the link.",
        "GROUP" to "Like `INPUT`, but the archives are searched repeatedly until no new references resolve.",
        "AS_NEEDED" to "Mark the listed libraries as DT_NEEDED only if they are actually used.",
        "OUTPUT" to "Name the output file.",
        "SEARCH_DIR" to "Add a directory to the library search path.",
        "STARTUP" to "Link this file first.",
        "OUTPUT_FORMAT" to "Set the BFD output format, e.g. `elf64-littleaarch64`.",
        "OUTPUT_ARCH" to "Set the output architecture, e.g. `aarch64`.",
        "TARGET" to "Set the BFD input format.",
        "ASSERT" to "Fail the link unless an expression is non-zero: `ASSERT(cond, \"message\")`.",
        "EXTERN" to "Force a symbol to be entered as undefined.",
        "FORCE_COMMON_ALLOCATION" to "Allocate space for common symbols even when producing relocatable output.",
        "INHIBIT_COMMON_ALLOCATION" to "Leave common symbols unallocated.",
        "NOCROSSREFS" to "Report an error if the named sections reference each other.",
        "NOCROSSREFS_TO" to "Report an error on references into the first named section.",
        "REGION_ALIAS" to "Give a memory region a second name: `REGION_ALIAS(\"RODATA\", FLASH)`.",
        "MEMORY" to "Declare the target's memory regions and their origins and lengths.",
        "SECTIONS" to "Describe how input sections map onto output sections.",
        "PHDRS" to "Declare ELF program headers explicitly.",
        "VERSION" to "Declare symbol version nodes.",
        "PROVIDE" to "Define a symbol only if the link references it and nothing else defines it.",
        "PROVIDE_HIDDEN" to "Like `PROVIDE`, but the symbol gets hidden ELF visibility.",
        "HIDDEN" to "Define a symbol with hidden ELF visibility.",
        "INSERT" to "Insert this script's contents into the default one.",
    )

    /** Words that appear inside `SECTIONS` and inside an output section body. */
    val SECTION_KEYWORDS: Map<String, String> = mapOf(
        "AT" to "Set the load address (LMA): `AT(lma)` before the body, or `AT> REGION` after it.",
        "ALIGN" to "Round an address up: `. = ALIGN(8);` or `ALIGN(16)` on an output section.",
        "ALIGN_WITH_INPUT" to "Keep the LMA/VMA difference the same as in the input.",
        "SUBALIGN" to "Override the alignment of the input sections placed here.",
        "KEEP" to "Protect the matched input sections from `--gc-sections`.",
        "SORT" to "Sort matching input sections by name. A synonym for `SORT_BY_NAME`.",
        "SORT_BY_NAME" to "Sort matching input sections by name.",
        "SORT_BY_ALIGNMENT" to "Sort matching input sections by alignment, largest first.",
        "SORT_BY_INIT_PRIORITY" to "Sort by the numeric priority encoded in the section name.",
        "SORT_NONE" to "Suppress sorting for the matched sections.",
        "EXCLUDE_FILE" to "Exclude files from a wildcard match: `*(EXCLUDE_FILE(*crt*) .text)`.",
        "CREATE_OBJECT_SYMBOLS" to "Define a symbol at the start of each input file's contribution.",
        "CONSTRUCTORS" to "Place a.out style constructor and destructor tables here.",
        "BYTE" to "Emit one byte.",
        "SHORT" to "Emit two bytes.",
        "LONG" to "Emit four bytes.",
        "QUAD" to "Emit eight bytes.",
        "SQUAD" to "Emit eight bytes, sign-extending a 32-bit expression.",
        "FILL" to "Set the fill pattern for gaps in this section.",
        "OVERLAY" to "Describe sections that share a virtual address but have distinct load addresses.",
        "NOLOAD" to "Mark the output section as not loaded at run time.",
        "READONLY" to "Mark the output section read-only.",
        "DSECT" to "Legacy output section type.",
        "COPY" to "Legacy output section type.",
        "INFO" to "Legacy output section type.",
        "ONLY_IF_RO" to "Emit the output section only if all its input sections are read-only.",
        "ONLY_IF_RW" to "Emit the output section only if all its input sections are writable.",
        "SPECIAL" to "Legacy output section type.",
    )

    /** Words that only make sense inside `MEMORY`. */
    val MEMORY_KEYWORDS: Map<String, String> = mapOf(
        "ORIGIN" to "Start address of a memory region. Abbreviates to `org` or `o`.",
        "LENGTH" to "Size of a memory region. Abbreviates to `len` or `l`.",
    )

    /** Built-in functions usable in linker-script expressions. */
    val FUNCTIONS: Map<String, String> = mapOf(
        "ABSOLUTE" to "Turn a section-relative value into an absolute one.",
        "ADDR" to "Virtual address (VMA) of a named section.",
        "ALIGNOF" to "Alignment of a named section, or 0 if it was not allocated.",
        "BLOCK" to "Deprecated synonym for `ALIGN`.",
        "CONSTANT" to "`CONSTANT(MAXPAGESIZE)` or `CONSTANT(COMMONPAGESIZE)`.",
        "DATA_SEGMENT_ALIGN" to "Align for the data segment, honouring `-z relro`.",
        "DATA_SEGMENT_END" to "Mark the end of the data segment.",
        "DATA_SEGMENT_RELRO_END" to "Mark the end of the RELRO region.",
        "DEFINED" to "1 if the symbol is defined and in the global symbol table, otherwise 0.",
        "LENGTH" to "Length of a memory region: `LENGTH(RAM)`.",
        "LOADADDR" to "Load address (LMA) of a named section.",
        "LOG2CEIL" to "Base-2 logarithm, rounded up.",
        "MAX" to "Larger of two expressions.",
        "MIN" to "Smaller of two expressions.",
        "NEXT" to "Next unallocated address that is a multiple of the argument.",
        "ORIGIN" to "Origin of a memory region: `ORIGIN(RAM)`.",
        "SEGMENT_START" to "Start of a named segment, overridable from the command line.",
        "SIZEOF" to "Size of a named section: `SIZEOF(.data)`.",
        "SIZEOF_HEADERS" to "Size of the output file's headers.",
    )

    /** Region attribute letters accepted in `MEMORY { NAME (rwx) : … }`. */
    const val REGION_ATTRIBUTES: String = "rwxail"

    val ALL: Map<String, String> =
        TOP_LEVEL_COMMANDS + SECTION_KEYWORDS + MEMORY_KEYWORDS + FUNCTIONS

    val KEYWORD_NAMES: Set<String> = ALL.keys

    /** `ORIGIN`/`LENGTH` may be written in three ways each inside a `MEMORY` block. */
    val ORIGIN_SPELLINGS: Set<String> = setOf("ORIGIN", "org", "o")

    val LENGTH_SPELLINGS: Set<String> = setOf("LENGTH", "len", "l")

    fun describe(name: String): String? = ALL[name]
}
