package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import org.junit.platform.engine.UniqueId

/** [ExtensionContext] that resolves the [UniqueId] of the current test or container.*/
public class ExtensionContextResolver : TypeBasedParameterResolver<ExtensionContext>() {
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): ExtensionContext =
        extensionContext
}
