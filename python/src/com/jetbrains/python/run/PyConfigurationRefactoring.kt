/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.python.run

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.UndoRefactoringElementAdapter

/**
 * Tools to support refactoring for configurations
 * that implements [com.intellij.execution.configurations.RefactoringListenerProvider]
 */


/**
 * @see CompositeRefactoringElementListener
 */
abstract class UndoRefactoringElementCompositable : UndoRefactoringElementAdapter() {
  public abstract override fun refactored(element: PsiElement, oldQualifiedName: String?)
}

/**
 * Chains several [com.intellij.refactoring.listeners.RefactoringElementListener]
 */
class CompositeRefactoringElementListener(vararg private val listeners: UndoRefactoringElementCompositable) : UndoRefactoringElementAdapter() {
  override fun refactored(element: PsiElement, oldQualifiedName: String?) {
    listeners.forEach { it.refactored(element, oldQualifiedName) }
  }

  /**
   * Creates new listener adding provided one
   */
  fun plus(listener: UndoRefactoringElementCompositable) = CompositeRefactoringElementListener(*arrayOf(listener) + listeners)
}


/**
 * Renames working directory if folder physically renamed
 */
class PyWorkingDirectoryRenamer(private val workingDirectoryFile: VirtualFile?,
                                private val conf: AbstractPythonRunConfiguration<*>) : UndoRefactoringElementCompositable() {
  override fun refactored(element: PsiElement, oldQualifiedName: String?) {
    if (workingDirectoryFile != null) {
      conf.setWorkingDirectory(workingDirectoryFile.path)
    }
  }
}

