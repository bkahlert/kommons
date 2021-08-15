package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.junit.UniqueId.Companion.id
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver

class UniqueIdResolver : TypeBasedParameterResolver<UniqueId>() {
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): UniqueId =
        extensionContext.id
}
