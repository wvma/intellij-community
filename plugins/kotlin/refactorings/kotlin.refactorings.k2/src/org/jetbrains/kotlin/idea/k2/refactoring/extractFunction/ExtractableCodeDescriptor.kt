// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.k2.refactoring.extractFunction

import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * A class keeping information for the extracted declaration.
 *
 * To set up annotations to be added to the extracted declaration, you have two options:
 *  1. Set [renderedAnnotations]. In this case, [annotationsText] that will be finally added to the declaration
 *  will be a simple joined string of [renderedAnnotations].
 *  For example, when creating an extracted function `@Anno1(X, Y) @Anno2(Z, W, V) fun foo() {..}`, we can set
 *  `[renderedAnnotations] = listOf("@Anno1(X, Y)", "@Anno2(Z, W, V)")`.
 *
 *  2. Set [annotationClassIds]. In this case, [annotationsText] will be the same text of annotations that
 *  the container PSI from [extractionData] has whose class ids must exist in [annotationClassIds].
 *  For example, when creating `@Anno1(X, Y) @Anno2(Z, W, V) fun foo() {..}` extracted from
 *  `@Anno1(X, Y) @Anno2(Z, W, V) fun bar() {..}`, we can set
 *  `[annotationClassIds] = listOf("com.packageForAnno1.Anno1", "com.packageForAnno2.Anno2")`.
 */
data class ExtractableCodeDescriptor(
    val context: KtElement,
    override val extractionData: ExtractionData,
    override val suggestedNames: List<String>,
    override val visibility: KtModifierKeywordToken?,
    override val parameters: List<Parameter>,
    override val receiverParameter: Parameter?,
    override val typeParameters: List<TypeParameter>,
    override val replacementMap: MultiMap<KtSimpleNameExpression, IReplacement<KaType>>,
    override val controlFlow: ControlFlow<KaType>,
    override val returnType: KaType,
    override val modifiers: List<KtKeywordToken> = emptyList(),
    override val optInMarkers: List<FqName> = emptyList(),
    val annotationClassIds: Set<ClassId> = emptySet(),
    val renderedAnnotations: Set<String> = emptySet(),
) : IExtractableCodeDescriptor<KaType> {
    override val name: String get() = suggestedNames.firstOrNull() ?: ""

    override val duplicates: List<DuplicateInfo<KaType>> by lazy { findDuplicates() }

    private val isUnitReturn: Boolean = analyze(context) { returnType.isUnitType }

    override fun isUnitReturnType(): Boolean = isUnitReturn

    @OptIn(KaExperimentalApi::class)
    override val annotationsText: String
        get() {
            if (renderedAnnotations.isNotEmpty()) {
                return renderedAnnotations.joinToString(separator = "\n", postfix = "\n")
            }
            if (annotationClassIds.isEmpty()) return ""
            val container = extractionData.commonParent.getStrictParentOfType<KtNamedFunction>() ?: return ""
            return analyze(container) {
                val filteredRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES.annotationRenderer.with {
                    annotationFilter = annotationFilter.and(object : KaRendererAnnotationsFilter {
                        override fun filter(
                            analysisSession: KaSession,
                            annotation: KaAnnotation,
                            owner: KaAnnotated
                        ): Boolean {
                            return annotation.classId in annotationClassIds
                        }
                    })

                }
                val printer = PrettyPrinter()
                filteredRenderer.renderAnnotations(useSiteSession, container.symbol, printer)
                printer.toString() + "\n"
            }
        }
}

internal fun getPossibleReturnTypes(cfg: ControlFlow<KaType>): List<KaType> {
    return cfg.possibleReturnTypes
}

data class ExtractableCodeDescriptorWithConflicts(
    override val descriptor: ExtractableCodeDescriptor,
    override val conflicts: MultiMap<PsiElement, String>
) : ExtractableCodeDescriptorWithConflictsResult(), IExtractableCodeDescriptorWithConflicts<KaType>
