package com.asulcons.embedded

import com.intellij.ui.IconManager
import javax.swing.Icon

object EmbeddedIcons {
    @JvmField
    val ArmAsmFile: Icon = load("/icons/armAsmFile.svg")

    @JvmField
    val GdbInitFile: Icon = load("/icons/gdbInitFile.svg")

    @JvmField
    val LinkerScriptFile: Icon = load("/icons/linkerScriptFile.svg")

    private fun load(path: String): Icon =
        IconManager.getInstance().getIcon(path, EmbeddedIcons::class.java.classLoader)
}
