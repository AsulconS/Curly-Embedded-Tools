package com.asulcons.embedded

import com.intellij.lang.PsiBuilder
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet

/**
 * All three languages here are line oriented, yet none of them wants newlines to be real tokens:
 * making `\n` a token would take the line break out of the whitespace between blocks and the
 * formatter's [com.intellij.formatting.Indent] machinery would never see a line to indent.
 *
 * So newlines stay ordinary whitespace and the parsers ask the builder about them directly, walking
 * the *raw* token stream backwards over whatever the builder skipped on its way to the current token.
 */
fun PsiBuilder.hasLineBreakBefore(skippable: TokenSet = TokenSet.EMPTY): Boolean {
    val text = originalText
    var steps = -1
    while (true) {
        // `null` means we walked past the beginning of the file, which counts as a line start.
        val type = rawLookup(steps) ?: return true
        if (type !== TokenType.WHITE_SPACE && !skippable.contains(type)) return false
        val start = rawTokenTypeStart(steps)
        val end = rawTokenTypeStart(steps + 1)
        if (start < 0 || end < start || end > text.length) return false
        for (i in start until end) {
            if (text[i] == '\n') return true
        }
        steps--
    }
}
