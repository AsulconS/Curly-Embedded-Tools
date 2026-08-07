package com.asulcons.embedded.gdb.psi

import com.asulcons.embedded.gdb.GdbLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

class GdbElementType(debugName: String) : IElementType(debugName, GdbLanguage) {
    override fun toString(): String = "Gdb:" + super.toString()
}

object GdbElementTypes {
    @JvmField val FILE = IFileElementType(GdbLanguage)

    /** A single command line, e.g. `target remote :1234`. */
    @JvmField val COMMAND = GdbElementType("COMMAND")

    /** `define`/`if`/`while`/`commands`/`document`/`python` … `end`. */
    @JvmField val BLOCK = GdbElementType("BLOCK")

    /** The head line of a block, kept separate so the body can be indented against it. */
    @JvmField val BLOCK_HEADER = GdbElementType("BLOCK_HEADER")

    @JvmField val BLOCK_BODY = GdbElementType("BLOCK_BODY")

    /** The verbatim body of a `python` or `document` block; a language-injection host when Python. */
    @JvmField val RAW_BODY = GdbElementType("RAW_BODY")

    @JvmField val ARGUMENTS = GdbElementType("ARGUMENTS")
}
