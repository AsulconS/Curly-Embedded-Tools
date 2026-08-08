package com.asulcons.embedded.gdb.stubs

import com.asulcons.embedded.gdb.lexer.GdbTokens
import com.asulcons.embedded.gdb.psi.GdbBlock
import com.asulcons.embedded.gdb.psi.GdbFile
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import java.io.IOException

/**
 * Makes `import gdb` resolvable inside a script's `python … end` blocks.
 *
 * Two things are needed and neither works alone. The stub itself, because GDB's `gdb` module lives
 * inside the interpreter GDB embeds and cannot be installed from anywhere — so no interpreter will
 * ever supply it. And its directory on the Python path, because an injected fragment's file is a
 * window into the host document rather than a file on disk, so the sibling-module rule that would
 * normally cover this does not reach it.
 */
object GdbPythonStubs {

    const val STUB_NAME: String = "gdb.pyi"

    private const val STUB_RESOURCE = "/stubs/gdb.pyi"

    /**
     * The GDB script [element] belongs to, looking through a language injection.
     *
     * Without the injection hop the offer would be missing from the one place a user would go for it:
     * the caret sits on the unresolved `import gdb`, which is Python, not GDB script.
     */
    fun gdbFileFor(element: PsiElement): GdbFile? {
        (element.containingFile as? GdbFile)?.let { return it }
        val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element)
        return host?.containingFile as? GdbFile
    }

    fun hasPythonBlock(file: GdbFile): Boolean =
        PsiTreeUtil.findChildrenOfType(file, GdbBlock::class.java)
            .any { it.keywordType === GdbTokens.KW_PYTHON }

    /** Where the stub belongs for [file], or `null` for a file with no directory of its own. */
    fun directoryFor(file: GdbFile): VirtualFile? = file.virtualFile?.parent

    /** True when either half of the setup is still missing, so the offer stays useful either way. */
    fun needsSetup(project: Project, directory: VirtualFile): Boolean =
        !isStubInstalled(directory) || !isOnPythonPath(project, directory)

    fun isStubInstalled(directory: VirtualFile): Boolean = directory.findChild(STUB_NAME) != null

    fun isOnPythonPath(project: Project, directory: VirtualFile): Boolean =
        ProjectFileIndex.getInstance(project).isInSourceContent(directory)

    /**
     * Writes the stub and puts its directory on the Python path. Both steps are skipped individually
     * if already done, so this is safe to run twice. Must run inside a write action.
     */
    fun setUp(project: Project, directory: VirtualFile) {
        installStub(directory)
        markAsSourceRoot(project, directory)
    }

    private fun installStub(directory: VirtualFile) {
        if (isStubInstalled(directory)) return
        val contents = javaClass.getResourceAsStream(STUB_RESOURCE)?.use { it.readBytes() }
            ?: throw IncorrectOperationException("the bundled $STUB_NAME is missing from the plugin")
        try {
            directory.createChildData(this, STUB_NAME).setBinaryContent(contents)
        } catch (e: IOException) {
            // `e as Throwable` is load-bearing, not noise. 2026.1 carries both
            // `IncorrectOperationException(String, Throwable)` and `(String, Exception)`; an IOException
            // binds to the more specific `(String, Exception)`, which 2026.2 removed. That compiles
            // clean against 261 and throws NoSuchMethodError on 262 — the Plugin Verifier is the only
            // thing that catches it. Upcasting pins the overload that exists in both.
            throw IncorrectOperationException(
                "cannot write $STUB_NAME to ${directory.presentableUrl}",
                e as Throwable,
            )
        }
    }

    /**
     * Adds [directory] as a source folder of whichever content entry already contains it.
     *
     * Deliberately does not create a content entry when none covers the directory: that would mean
     * pulling a folder from outside the project into it, which is well beyond what installing a stub
     * should decide on the user's behalf.
     */
    private fun markAsSourceRoot(project: Project, directory: VirtualFile) {
        if (isOnPythonPath(project, directory)) return
        val module = ProjectFileIndex.getInstance(project).getModuleForFile(directory) ?: return

        val model = ModuleRootManager.getInstance(module).modifiableModel
        var committed = false
        try {
            val entry = model.contentEntries.firstOrNull { content ->
                content.file?.let { VfsUtilCore.isAncestor(it, directory, false) } == true
            } ?: return
            entry.addSourceFolder(directory, false)
            model.commit()
            committed = true
        } finally {
            if (!committed) model.dispose()
        }
    }
}
