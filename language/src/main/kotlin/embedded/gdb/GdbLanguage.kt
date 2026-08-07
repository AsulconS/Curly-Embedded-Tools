package com.asulcons.embedded.gdb

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.EmbeddedIcons
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object GdbLanguage : Language("GdbScript", "text/x-gdb-script") {
    private fun readResolve(): Any = GdbLanguage

    override fun getDisplayName(): String = "GDB Script"

    override fun isCaseSensitive(): Boolean = true
}

object GdbFileType : LanguageFileType(GdbLanguage) {
    override fun getName(): String = "GDB Script"

    override fun getDescription(): String = EmbeddedBundle.message("filetype.gdb.description")

    override fun getDefaultExtension(): String = "gdb"

    override fun getIcon(): Icon = EmbeddedIcons.GdbInitFile
}
