package com.asulcons.embedded.gdb.injection

import com.asulcons.embedded.gdb.psi.GdbRawBody
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * Hands the body of a `python … end` block to the Python plugin.
 *
 * GDB embeds real Python there, so rather than approximating it with a second lexer this injects the
 * actual language and gets its highlighting, completion and error reporting for free.
 *
 * The Python plugin is looked up by ID at runtime and never declared as a dependency: not every
 * IntelliJ-based IDE ships one, and a `.gdbinit` has to keep working in the ones that do not. Without
 * it the body simply stays the opaque region it already was.
 */
class GdbPythonInjector : MultiHostInjector {

    override fun elementsToInjectIn(): List<Class<out PsiElement>> = listOf(GdbRawBody::class.java)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val body = context as? GdbRawBody ?: return
        if (!body.isValidHost) return
        if (body.textLength == 0) return

        val python = findPython() ?: return
        registrar.startInjecting(python)
            .addPlace(null, null, body, TextRange(0, body.textLength))
            .doneInjecting()
    }

    /** `python-ce` and the Ultimate Python plugin both register the language under this ID. */
    private fun findPython(): Language? = Language.findLanguageByID("Python")
}
