package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver

/** [ParameterResolver] that resolves the [DisplayName] of the current test or container.*/
public class DisplayNameResolver : TypeBasedParameterResolver<DisplayName>() {
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): DisplayName =
        DisplayName(extensionContext)
}
