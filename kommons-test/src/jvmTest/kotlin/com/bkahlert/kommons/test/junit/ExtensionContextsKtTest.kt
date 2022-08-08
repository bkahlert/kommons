package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContextException

class ExtensionContextsKtTest {

    @Test fun ancestors(extensionContext: ExtensionContext) = testAll {
        extensionContext.ancestors.map { it.uniqueId }.shouldContainExactly(
            listOf(
                "[engine:junit-jupiter]",
                "[class:com.bkahlert.kommons.test.junit.ExtensionContextsKtTest]",
            ).joinToString("/"),
            listOf(
                "[engine:junit-jupiter]",
            ).joinToString("/"),
        )
    }


    @Test fun get_store(context: ExtensionContext) = testAll {
        context.getStore<Foo>() should {
            it.get("key") shouldBe null
            it.put("key", "value")
            it.get("key") shouldBe "value"
        }
        context.getStore<Foo>("additional-part") should {
            it.get("key") shouldBe null
            it.put("key", "value")
            it.get("key") shouldBe "value"
        }
        context.getStore<Bar>() should {
            it.get("key") shouldBe null
        }
    }

    @Test fun get_test_store(context: ExtensionContext) = testAll {
        context.getStore<Foo>().put("key", "value")
        context.getTestStore<Foo>() should {
            it.get("key") shouldBe null
            it.put("key", "value")
            it.get("key") shouldBe "value"
        }
        context.getTestStore<Foo>("additional-part") should {
            it.get("key") shouldBe null
            it.put("key", "value")
            it.get("key") shouldBe "value"
        }
        context.getTestStore<Bar>() should {
            it.get("key") shouldBe null
        }
    }


    @Test fun store_get_typed(context: ExtensionContext) = testAll {
        context.getStore<Foo>() should {
            it.getTyped<String>("key") shouldBe null
            it.put("key", "value")
            val value: String? = it.getTyped("key")
            value shouldBe "value"
            it.put("key", true)
            shouldThrow<ExtensionContextException> { it.getTyped<String>("key") }
        }
    }

    @Test fun store_get_typed_or_default(context: ExtensionContext) = testAll {
        context.getStore<Foo>() should {
            it.getTypedOrDefault("key", "default") shouldBe "default"
            it.put("key", "value")
            it.getTypedOrDefault("key", "default") shouldBe "value"
            it.put("key", true)
            shouldThrow<ExtensionContextException> { it.getTypedOrDefault("key", "default") }
        }
    }

    @Test fun store_get_typed_or_compute_if_absent(context: ExtensionContext) = testAll {
        context.getStore<Foo>() should {
            it.getTypedOrComputeIfAbsent("key") { "computed" } shouldBe "computed"
            it.get("key") shouldBe "computed"
            it.put("key", "value")
            it.getTypedOrComputeIfAbsent("key") { "computed" } shouldBe "value"
            it.put("key", true)
            shouldThrow<ExtensionContextException> { it.getTypedOrComputeIfAbsent("key") { "computed" } }
        }
    }

    @Test fun store_remove_typed(context: ExtensionContext) = testAll {
        context.getStore<Foo>() should {
            it.put("key", "value")
            val removed: String = it.removeTyped("key")
            removed shouldBe "value"
            it.get("key") shouldBe null
            it.put("key", true)
            shouldThrow<ExtensionContextException> { it.removeTyped<String>("key") }
        }
    }
}

internal class Foo
internal class Bar
