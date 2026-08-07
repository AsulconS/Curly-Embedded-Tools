package com.asulcons.embedded.armv8

import com.intellij.lang.Language

object ArmAsmLanguage : Language("ARMv8Assembly", "text/x-asm", "text/x-arm-asm") {
    private fun readResolve(): Any = ArmAsmLanguage

    override fun getDisplayName(): String = "ARMv8 Assembly"

    override fun isCaseSensitive(): Boolean = true
}
