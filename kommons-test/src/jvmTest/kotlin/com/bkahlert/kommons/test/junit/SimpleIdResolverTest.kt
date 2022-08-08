package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.junit.SimpleIdResolver.Companion.simpleId
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.engine.UniqueId

class SimpleIdResolverTest {

    @Test fun test_name(simpleId: SimpleId, uniqueId: UniqueId) = testAll {
        simpleId shouldBe SimpleId.from(uniqueId)
    }

    @Test fun extension(simpleId: SimpleId, extensionContext: ExtensionContext) = testAll {
        simpleId shouldBe extensionContext.simpleId
    }

    @Nested
    inner class NestedTest {

        @Test fun test_name(simpleId: SimpleId, uniqueId: UniqueId) = testAll {
            simpleId shouldBe SimpleId.from(uniqueId)
        }

        @Test fun extension(simpleId: SimpleId, extensionContext: ExtensionContext) = testAll {
            simpleId shouldBe extensionContext.simpleId
        }
    }
}
