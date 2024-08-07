// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.debugger.evaluate

import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.synthetic.JavaSyntheticPropertiesScope
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class DebuggerFieldExpressionCodegenExtension : ExpressionCodegenExtension {
    override fun applyProperty(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
        val propertyDescriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return null

        if (propertyDescriptor is DebuggerFieldPropertyDescriptor) {
            return StackValue.StackValueWithSimpleReceiver.field(
                c.typeMapper.mapType(propertyDescriptor.type),
                propertyDescriptor.ownerType(c.codegen.state),
                propertyDescriptor.fieldName,
                false,
                receiver
            )
        }

        if (propertyDescriptor is JavaPropertyDescriptor) {
            val containingClass = propertyDescriptor.containingDeclaration as? JavaClassDescriptor
            if (containingClass != null) {
                val correspondingGetter =
                    JavaSyntheticPropertiesScope(
                        LockBasedStorageManager.NO_LOCKS,
                        LookupTracker.DO_NOTHING,
                        KotlinTypeRefiner.Default,
                        supportJavaRecords = true
                    )
                        .getSyntheticExtensionProperties(listOf(containingClass.defaultType), NoLookupLocation.FROM_BACKEND)
                        .firstOrNull { it.name == propertyDescriptor.name }

                if (correspondingGetter != null) {
                    return c.codegen.intermediateValueForProperty(
                        correspondingGetter, false, false,
                        c.codegen.getSuperCallTarget(resolvedCall.call),
                        false, receiver, resolvedCall, false
                    )
                }
            }
        }

        return null
    }
}
