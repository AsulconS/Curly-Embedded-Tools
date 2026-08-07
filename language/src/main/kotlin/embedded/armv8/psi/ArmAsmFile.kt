package com.asulcons.embedded.armv8.psi

import com.asulcons.embedded.armv8.ArmAsmFileType
import com.asulcons.embedded.armv8.ArmAsmLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil

class ArmAsmFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ArmAsmLanguage) {

    override fun getFileType(): FileType = ArmAsmFileType

    override fun toString(): String = "ARMv8 Assembly File"

    val statements: List<ArmAsmStatement>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ArmAsmStatement::class.java)

    /**
     * Every label and directive-declared symbol in this file, grouped by name.
     *
     * Resolution stops at the file boundary on purpose: an assembly symbol that is not defined here is
     * usually defined in C, in another translation unit, or by the linker script, and chasing it would
     * mean guessing. The duplicate-label inspection and Go To Declaration both work off this map.
     */
    val symbolDefinitions: Map<String, List<ArmAsmNamedElement>>
        get() = cached(SYMBOLS) {
            val definitions = ArrayList<ArmAsmNamedElement>()
            definitions += PsiTreeUtil.findChildrenOfType(this, ArmAsmLabelDefinition::class.java)
            definitions += PsiTreeUtil.findChildrenOfType(this, ArmAsmDirectiveArgument::class.java)
                .filter { it.isSymbolDeclaration }
            definitions.filter { it.symbolName != null }.groupBy { it.symbolName!! }
        }

    /** Names introduced by `.macro`, which are indistinguishable from instructions at the token level. */
    val macroNames: Set<String>
        get() = cached(MACROS) {
            directives(".macro").mapNotNullTo(HashSet()) { it.arguments.firstOrNull()?.name }
        }

    /**
     * Register aliases introduced by `count .req x9`.
     *
     * The lexer sees `count` in statement-head position and calls it a mnemonic, so the alias shows up
     * as an instruction whose sole operand is the `.req` word.
     */
    val registerAliases: Set<String>
        get() = cached(ALIASES) {
            statements.mapNotNullTo(HashSet()) { statement ->
                val instruction = statement.instruction ?: return@mapNotNullTo null
                val head = instruction.operands.firstOrNull()?.text?.trim() ?: return@mapNotNullTo null
                if (!head.equals(".req", ignoreCase = true)) return@mapNotNullTo null
                instruction.mnemonic
            }
        }

    /**
     * Names introduced by `#define` in a `.S` file.
     *
     * A `#define`d instruction-like macro is invoked exactly like a real mnemonic, so without this the
     * unknown-instruction inspection would flag every use of one.
     */
    val preprocessorMacroNames: Set<String>
        get() = cached(PREPROCESSOR_MACROS) {
            PsiTreeUtil.findChildrenOfType(this, ArmAsmPreprocessorStatement::class.java)
                .mapNotNullTo(HashSet()) { DEFINE.find(it.text)?.groupValues?.get(1) }
        }

    val numericLabels: List<ArmAsmLabelDefinition>
        get() = cached(NUMERIC_LABELS) {
            PsiTreeUtil.findChildrenOfType(this, ArmAsmLabelDefinition::class.java).filter { it.isNumeric }
        }

    fun directives(name: String): List<ArmAsmDirective> =
        PsiTreeUtil.findChildrenOfType(this, ArmAsmDirective::class.java).filter { it.directiveName == name }

    /** The `.macro` argument that declares [name], if this file defines such a macro. */
    fun macroDefinition(name: String): ArmAsmDirectiveArgument? =
        directives(".macro")
            .mapNotNull { it.arguments.firstOrNull() }
            .firstOrNull { it.name == name }

    /** The `#define` line that introduces [name], for `.S` files that build instructions out of cpp. */
    fun preprocessorDefinition(name: String): ArmAsmPreprocessorStatement? =
        PsiTreeUtil.findChildrenOfType(this, ArmAsmPreprocessorStatement::class.java)
            .firstOrNull { DEFINE.find(it.text)?.groupValues?.get(1) == name }

    /**
     * Depends on the project-wide PSI modification count rather than on this file.
     *
     * A file dependency would be tighter, but it makes the platform ask whether the holder is
     * "very physical", which drags in language-injection services that a light PSI environment does
     * not have. The modification count is the conservative choice and is what these whole-file
     * summaries want anyway.
     */
    private fun <T : Any> cached(key: Key<CachedValue<T>>, compute: () -> T): T =
        CachedValuesManager.getCachedValue(this, key) {
            CachedValueProvider.Result.create(compute(), PsiModificationTracker.MODIFICATION_COUNT)
        }

    private companion object {
        val DEFINE = Regex("^#\\s*define\\s+([A-Za-z_][A-Za-z0-9_]*)")

        val SYMBOLS = Key.create<CachedValue<Map<String, List<ArmAsmNamedElement>>>>("armAsm.symbolDefinitions")
        val MACROS = Key.create<CachedValue<Set<String>>>("armAsm.macroNames")
        val ALIASES = Key.create<CachedValue<Set<String>>>("armAsm.registerAliases")
        val PREPROCESSOR_MACROS = Key.create<CachedValue<Set<String>>>("armAsm.preprocessorMacros")
        val NUMERIC_LABELS = Key.create<CachedValue<List<ArmAsmLabelDefinition>>>("armAsm.numericLabels")
    }
}
