/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.reactiveidea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import com.jetbrains.reactivemodel.Path
import com.jetbrains.reactivemodel.util.Lifetime
import java.util.HashMap

public class DocumentsSynchronizer(val project: Project) : ProjectComponent {
  val lifetime = Lifetime.create(Lifetime.Eternal)
  var bJavaHost: EditorHost? = null
  var aTxtHost: EditorHost? = null


  override fun getComponentName(): String = "DocumentsSynchronizer"

  private val messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect()

  override fun initComponent() {

    UIUtil.invokeLaterIfNeeded {
      val aTxt = StandardFileSystems.local().findFileByPath("/Users/jetzajac/IdeaProjects/untitled/src/A.txt")
      val bJava = StandardFileSystems.local().findFileByPath("/Users/jetzajac/IdeaProjects/untitled2/src/B.java")


      FileEditorManager.getInstance(project).getSelectedTextEditor()
      messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
          object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
              val editor = (FileEditorManager.getInstance(project).getAllEditors(file).first() as TextEditor).getEditor()
              if (!isClient()) {
                if (file.equals(aTxt)) {
                  serverModel(lifetime.lifetime, 12346) { m ->
                    aTxtHost = EditorHost(lifetime.lifetime, m, Path("editor"), editor, false)
                  }
                }
              } else {
                if (file.equals(bJava)) {
                  val clientModel = clientModel("http://localhost:12346", Lifetime.Eternal)

                  bJavaHost = EditorHost(lifetime.lifetime, clientModel, Path("editor"), editor, true)
                }
              }
            }

            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {

            }

            override fun selectionChanged(event: FileEditorManagerEvent) {

            }

          })
    }
  }

  private fun isClient(): Boolean = System.getProperty("com.jetbrains.reactiveidea.client") == "true"


  override fun disposeComponent() {
    lifetime.terminate()
  }

  override fun projectOpened() {

  }

  override fun projectClosed() {

  }

}
