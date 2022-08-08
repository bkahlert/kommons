package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import org.junit.platform.engine.UniqueId

/** [ParameterResolver] that resolves the [UniqueId] of the current test or container.*/
public class UniqueIdResolver : TypeBasedParameterResolver<UniqueId>() {
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): UniqueId =
        UniqueId.parse(extensionContext.uniqueId)
}
