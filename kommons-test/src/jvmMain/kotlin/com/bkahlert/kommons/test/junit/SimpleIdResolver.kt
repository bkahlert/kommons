package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import org.junit.platform.engine.UniqueId

/** [ParameterResolver] that resolves the [SimpleId] of the current test or container.*/
public class SimpleIdResolver : TypeBasedParameterResolver<SimpleId>() {
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): SimpleId =
        extensionContext.simpleId

    public companion object {

        /** The [SimpleId] of the current test or container. */
        public val ExtensionContext.simpleId: SimpleId get(): SimpleId = SimpleId.from(UniqueId.parse(uniqueId))
    }
}
