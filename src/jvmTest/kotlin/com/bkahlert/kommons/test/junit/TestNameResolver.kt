package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver

class TestNameResolver : TypeBasedParameterResolver<TestName>() {
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): TestName =
        TestName.from(extensionContext)
}
