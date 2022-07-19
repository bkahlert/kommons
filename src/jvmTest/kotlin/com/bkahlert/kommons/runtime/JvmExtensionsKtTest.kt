package com.bkahlert.kommons.runtime

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import kotlin.reflect.jvm.javaMethod

class JvmExtensionsKtTest {

    @Nested
    inner class Ancestors {

        @Test
        fun `should resolve class ancestor`() {
            class InnerTestClass
            expectThat(InnerTestClass::class.java.ancestor).isEqualTo(
                Ancestors::class.java
            )
        }

        @Test
        fun `should resolve class ancestors`() {
            class InnerTestClass
            expectThat(InnerTestClass::class.java.ancestors).containsExactly(
                InnerTestClass::class.java,
                Ancestors::class.java,
                JvmExtensionsKtTest::class.java,
            )
        }

        @Test
        fun `should resolve method ancestor`() {
            val method = ::`should resolve method ancestor`.javaMethod ?: fail("Error getting Java method.")
            expectThat(method.ancestorx).isEqualTo(
                Ancestors::class.java,
            )
        }

        @Test
        fun `should resolve method ancestors`() {
            val method = ::`should resolve method ancestors`.javaMethod ?: fail("Error getting Java method.")
            expectThat(method.ancestorsx).containsExactly(
                method,
                Ancestors::class.java,
                JvmExtensionsKtTest::class.java,
            )
        }
    }
}
