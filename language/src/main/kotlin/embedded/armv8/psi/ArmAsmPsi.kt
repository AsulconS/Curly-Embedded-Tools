package com.asulcons.embedded.armv8.psi

import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.asulcons.embedded.armv8.spec.A64Spec
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil

/** Common supertype so that extensions can filter for "something this plugin parsed". */
abstract class ArmAsmElement(node: ASTNode) : ASTWrapperPsiElement(node)

/** A named element that other statements can refer to: a label, or a symbol a directive introduces. */
interface ArmAsmNamedElement : PsiNameIdentifierOwner {
    val symbolName: String?
}

// -------------------------------------------------------------------------------------------------
// Statements
// -------------------------------------------------------------------------------------------------

class ArmAsmStatement(node: ASTNode) : ArmAsmElement(node) {
    val labelDefinitions: List<ArmAsmLabelDefinition>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ArmAsmLabelDefinition::class.java)

    val instruction: ArmAsmInstruction?
        get() = PsiTreeUtil.getChildOfType(this, ArmAsmInstruction::class.java)

    val directive: ArmAsmDirective?
        get() = PsiTreeUtil.getChildOfType(this, ArmAsmDirective::class.java)
}

class ArmAsmLabelDefinition(node: ASTNode) : ArmAsmElement(node), ArmAsmNamedElement {

    override fun getNameIdentifier(): PsiElement? = node.findChildByType(ArmAsmTokens.LABEL)?.psi

    override fun getName(): String? = nameIdentifier?.text

    override val symbolName: String?
        get() = name

    override fun setName(name: String): PsiElement {
        val identifier = nameIdentifier ?: return this
        identifier.replace(ArmAsmElementFactory.createLabelIdentifier(project, name))
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    /** `1:` style labels are scoped to the nearest matching `1b`/`1f` reference, not to the file. */
    val isNumeric: Boolean
        get() = name?.all { it.isDigit() } == true

    /** `.L`-prefixed labels are conventionally assembler-local and never end up in the symbol table. */
    val isAssemblerLocal: Boolean
        get() = name?.startsWith(".L") == true
}

class ArmAsmInstruction(node: ASTNode) : ArmAsmElement(node) {
    val mnemonicElement: PsiElement?
        get() = node.findChildByType(ArmAsmTokens.MNEMONIC)?.psi

    val mnemonic: String?
        get() = mnemonicElement?.text

    val operands: List<ArmAsmOperand>
        get() {
            val list = PsiTreeUtil.getChildOfType(this, ArmAsmOperandList::class.java) ?: return emptyList()
            return PsiTreeUtil.getChildrenOfTypeAsList(list, ArmAsmOperand::class.java)
        }

    /**
     * A macro invocation points back at its definition.
     *
     * The reference hangs off the instruction rather than off the mnemonic token, with its range
     * narrowed to the mnemonic: a bare leaf is not a reference host, whereas Ctrl+Click walks up from
     * the caret looking for an element whose reference range covers the offset.
     */
    override fun getReference(): PsiReference? = ArmAsmMacroReference.of(this)

    override fun getReferences(): Array<PsiReference> =
        reference?.let { arrayOf(it) } ?: PsiReference.EMPTY_ARRAY
}

class ArmAsmOperandList(node: ASTNode) : ArmAsmElement(node)

class ArmAsmDirective(node: ASTNode) : ArmAsmElement(node) {
    val directiveElement: PsiElement?
        get() = node.findChildByType(ArmAsmTokens.DIRECTIVE)?.psi

    /** Lower-cased so that callers can compare against [A64Spec] without repeating the normalisation. */
    val directiveName: String?
        get() = directiveElement?.text?.lowercase()

    val arguments: List<ArmAsmDirectiveArgument>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ArmAsmDirectiveArgument::class.java)
}

/**
 * One comma-separated chunk of a directive's argument list.
 *
 * When it is the first argument of `.equ`, `.set`, `.macro` and friends it also *declares* a name, so
 * it doubles as a named element and can be renamed and navigated to like a label.
 */
class ArmAsmDirectiveArgument(node: ASTNode) : ArmAsmElement(node), ArmAsmNamedElement {

    private val declaringDirective: ArmAsmDirective?
        get() {
            val directive = parent as? ArmAsmDirective ?: return null
            val name = directive.directiveName ?: return null
            if (name !in A64Spec.SYMBOL_DEFINING_DIRECTIVES) return null
            return if (directive.arguments.firstOrNull() === this) directive else null
        }

    val isSymbolDeclaration: Boolean
        get() = declaringDirective != null

    /** `.macro push reg` puts the macro name and its parameters in one argument; only the head declares. */
    override fun getNameIdentifier(): PsiElement? {
        if (declaringDirective == null) return null
        var child = firstChild
        while (child != null) {
            val type = child.node.elementType
            if (type === ArmAsmTokens.IDENTIFIER || type === ArmAsmTokens.MNEMONIC) return child
            if (type !== TokenType.WHITE_SPACE && !ArmAsmTokens.COMMENTS.contains(type)) return null
            child = child.nextSibling
        }
        return null
    }

    override fun getName(): String? = nameIdentifier?.text

    override val symbolName: String?
        get() = name

    override fun setName(name: String): PsiElement {
        val identifier = nameIdentifier ?: return this
        identifier.replace(ArmAsmElementFactory.createSymbolIdentifier(project, name))
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}

class ArmAsmPreprocessorStatement(node: ASTNode) : ArmAsmElement(node)

// -------------------------------------------------------------------------------------------------
// Operands and expressions
// -------------------------------------------------------------------------------------------------

open class ArmAsmOperand(node: ASTNode) : ArmAsmElement(node)

class ArmAsmRegisterOperand(node: ASTNode) : ArmAsmOperand(node) {
    val registerElement: PsiElement?
        get() = node.findChildByType(ArmAsmTokens.REGISTER)?.psi

    val registerName: String?
        get() = registerElement?.text
}

class ArmAsmImmediateOperand(node: ASTNode) : ArmAsmOperand(node) {
    /** The literal value when the immediate is a plain number, or `null` for symbolic expressions. */
    val constantValue: Long?
        get() = ArmAsmNumbers.evaluate(this)
}

class ArmAsmMemoryOperand(node: ASTNode) : ArmAsmOperand(node) {
    val isWriteBack: Boolean
        get() = node.findChildByType(ArmAsmTokens.EXCL) != null
}

class ArmAsmRegisterList(node: ASTNode) : ArmAsmOperand(node) {
    val registers: List<ArmAsmRegisterOperand>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ArmAsmRegisterOperand::class.java)
}

class ArmAsmShiftOperand(node: ASTNode) : ArmAsmOperand(node) {
    val operatorElement: PsiElement?
        get() = node.findChildByType(ArmAsmTokens.IDENTIFIER)?.psi

    val operatorName: String?
        get() = operatorElement?.text?.lowercase()

    /**
     * The shift amount.
     *
     * Evaluated from the expression after the `#`, not from the operand as a whole: the operand opens
     * with the shift keyword, and a leading identifier is precisely what marks an expression as
     * non-constant, so folding the whole operand always yielded `null`.
     */
    val amount: Long?
        get() = amountElement?.let { ArmAsmNumbers.evaluate(it) }

    private val amountElement: PsiElement?
        get() {
            var child = node.findChildByType(ArmAsmTokens.HASH)?.treeNext
            while (child != null &&
                (child.elementType === TokenType.WHITE_SPACE || ArmAsmTokens.COMMENTS.contains(child.elementType))
            ) {
                child = child.treeNext
            }
            return child?.psi
        }
}

class ArmAsmLiteralOperand(node: ASTNode) : ArmAsmOperand(node)

class ArmAsmExpressionOperand(node: ASTNode) : ArmAsmOperand(node)

class ArmAsmBinaryExpression(node: ASTNode) : ArmAsmElement(node)

class ArmAsmUnaryExpression(node: ASTNode) : ArmAsmElement(node)

class ArmAsmParenthesizedExpression(node: ASTNode) : ArmAsmElement(node)

class ArmAsmRelocatedExpression(node: ASTNode) : ArmAsmElement(node) {
    val specifier: String?
        get() = node.findChildByType(ArmAsmTokens.RELOCATION)?.text?.trim(':')
}

class ArmAsmLiteral(node: ASTNode) : ArmAsmElement(node)

/** A symbol used as an operand — the thing that resolves to a label or a `.equ`. */
class ArmAsmSymbol(node: ASTNode) : ArmAsmElement(node) {
    val symbolName: String
        get() = text

    override fun getReference(): PsiReference = ArmAsmSymbolReference(this)

    override fun getReferences(): Array<PsiReference> = arrayOf(reference)
}

/** `1b` / `2f`: a reference to the closest numeric label backwards or forwards. */
class ArmAsmLocalLabelReference(node: ASTNode) : ArmAsmElement(node) {
    val labelNumber: String
        get() = text.dropLast(1)

    val searchesBackwards: Boolean
        get() = text.lastOrNull() == 'b'

    override fun getReference(): PsiReference = ArmAsmLocalLabelPsiReference(this)

    override fun getReferences(): Array<PsiReference> = arrayOf(reference)
}
