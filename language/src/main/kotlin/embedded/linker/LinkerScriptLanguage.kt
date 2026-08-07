package com.asulcons.embedded.linker

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.EmbeddedIcons
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object LinkerScriptLanguage : Language("GnuLinkerScript", "text/x-ld-script") {
    private fun readResolve(): Any = LinkerScriptLanguage

    override fun getDisplayName(): String = "GNU Linker Script"

    override fun isCaseSensitive(): Boolean = true
}

object LinkerScriptFileType : LanguageFileType(LinkerScriptLanguage) {
    override fun getName(): String = "GNU Linker Script"

    override fun getDescription(): String = EmbeddedBundle.message("filetype.linker.description")

    override fun getDefaultExtension(): String = "ld"

    override fun getIcon(): Icon = EmbeddedIcons.LinkerScriptFile
}
