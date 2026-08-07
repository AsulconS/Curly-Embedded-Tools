package com.asulcons.embedded.gdb.stubs

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.gdb.psi.GdbFile
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import java.util.function.Function
import javax.swing.JComponent

/**
 * Offers the GDB Python stubs on the editor of a script that needs them.
 *
 * The intention alone was not enough: it is only found by someone who already knows it exists, and the
 * first sign of trouble — an unresolved `import gdb` — gives no hint that this plugin has anything to
 * say about it. The banner shows only on a GDB script that actually has a `python` block and has no
 * `gdb.pyi` beside it, and it can be dismissed for good.
 */
class GdbPythonStubNotificationProvider : EditorNotificationProvider, DumbAware {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (PropertiesComponent.getInstance(project).getBoolean(DISMISSED_KEY, false)) return null

        val script = PsiManager.getInstance(project).findFile(file) as? GdbFile ?: return null
        if (!GdbPythonStubs.hasPythonBlock(script)) return null
        val directory = GdbPythonStubs.directoryFor(script) ?: return null
        if (!GdbPythonStubs.needsSetup(project, directory)) return null

        return Function { editor -> createPanel(project, editor, directory) }
    }

    private fun createPanel(
        project: Project,
        editor: FileEditor,
        directory: VirtualFile,
    ): EditorNotificationPanel {
        val panel = EditorNotificationPanel(editor, EditorNotificationPanel.Status.Info)
        panel.text = EmbeddedBundle.message("gdb.stubs.banner.text")
        panel.createActionLabel(EmbeddedBundle.message("gdb.stubs.banner.install")) {
            WriteCommandAction.runWriteCommandAction(
                project,
                EmbeddedBundle.message("gdb.stubs.banner.install"),
                null,
                { GdbPythonStubs.setUp(project, directory) },
            )
            EditorNotifications.getInstance(project).updateAllNotifications()
        }
        panel.createActionLabel(EmbeddedBundle.message("gdb.stubs.banner.dismiss")) {
            PropertiesComponent.getInstance(project).setValue(DISMISSED_KEY, true)
            EditorNotifications.getInstance(project).updateAllNotifications()
        }
        return panel
    }

    private companion object {
        const val DISMISSED_KEY = "com.asulcons.embedded.gdb.stubs.dismissed"
    }
}
