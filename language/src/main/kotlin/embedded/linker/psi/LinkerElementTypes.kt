package com.asulcons.embedded.linker.psi

import com.asulcons.embedded.linker.LinkerScriptLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

class LinkerElementType(debugName: String) : IElementType(debugName, LinkerScriptLanguage) {
    override fun toString(): String = "Ld:" + super.toString()
}

object LinkerElementTypes {
    @JvmField val FILE = IFileElementType(LinkerScriptLanguage)

    /** A top-level command such as `ENTRY(_start)` or `OUTPUT_ARCH(aarch64)`. */
    @JvmField val COMMAND = LinkerElementType("COMMAND")

    @JvmField val MEMORY_BLOCK = LinkerElementType("MEMORY_BLOCK")
    @JvmField val MEMORY_REGION = LinkerElementType("MEMORY_REGION")
    @JvmField val REGION_ATTRIBUTES = LinkerElementType("REGION_ATTRIBUTES")
    @JvmField val REGION_PROPERTY = LinkerElementType("REGION_PROPERTY")

    @JvmField val SECTIONS_BLOCK = LinkerElementType("SECTIONS_BLOCK")
    @JvmField val OUTPUT_SECTION = LinkerElementType("OUTPUT_SECTION")
    @JvmField val SECTION_HEADER = LinkerElementType("SECTION_HEADER")
    @JvmField val SECTION_BODY = LinkerElementType("SECTION_BODY")
    @JvmField val SECTION_TRAILER = LinkerElementType("SECTION_TRAILER")
    @JvmField val INPUT_SECTION = LinkerElementType("INPUT_SECTION")

    @JvmField val PHDRS_BLOCK = LinkerElementType("PHDRS_BLOCK")
    @JvmField val VERSION_BLOCK = LinkerElementType("VERSION_BLOCK")

    @JvmField val ASSIGNMENT = LinkerElementType("ASSIGNMENT")

    /** The name after `>` or `AT>`, which must match a region declared in `MEMORY`. */
    @JvmField val REGION_REFERENCE = LinkerElementType("REGION_REFERENCE")

    @JvmField val BINARY_EXPRESSION = LinkerElementType("BINARY_EXPRESSION")
    @JvmField val UNARY_EXPRESSION = LinkerElementType("UNARY_EXPRESSION")
    @JvmField val TERNARY_EXPRESSION = LinkerElementType("TERNARY_EXPRESSION")
    @JvmField val PARENTHESIZED_EXPRESSION = LinkerElementType("PARENTHESIZED_EXPRESSION")
    @JvmField val FUNCTION_CALL = LinkerElementType("FUNCTION_CALL")
    @JvmField val ARGUMENT_LIST = LinkerElementType("ARGUMENT_LIST")
    @JvmField val SYMBOL = LinkerElementType("SYMBOL")
    @JvmField val LITERAL = LinkerElementType("LITERAL")
}
