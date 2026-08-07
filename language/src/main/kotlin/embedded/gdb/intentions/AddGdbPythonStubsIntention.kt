package com.asulcons.embedded.gdb.intentions

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.gdb.stubs.GdbPythonStubs
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * Offers the bundled GDB Python stubs from inside a `python … end` block.
 *
 * Registered without a `<language>` so that it is also reachable from the *injected* Python — the
 * caret is on the unresolved `import gdb` when a user goes looking for this, and that element belongs
 * to the Python file, not to the GDB script. [GdbPythonStubs.gdbFileFor] does the hop back.
 */
class AddGdbPythonStubsIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = EmbeddedBundle.message("intention.gdb.addStubs.family")

    override fun getText(): String = familyName

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val file = GdbPythonStubs.gdbFileFor(element) ?: return false
        if (!GdbPythonStubs.hasPythonBlock(file)) return false
        val directory = GdbPythonStubs.directoryFor(file) ?: return false
        return GdbPythonStubs.needsSetup(project, directory)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = GdbPythonStubs.gdbFileFor(element) ?: return
        val directory = GdbPythonStubs.directoryFor(file) ?: return
        GdbPythonStubs.setUp(project, directory)
    }

    override fun startInWriteAction(): Boolean = true
}
