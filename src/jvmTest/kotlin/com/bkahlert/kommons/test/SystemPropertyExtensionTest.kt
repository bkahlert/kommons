package com.bkahlert.kommons.test

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SystemPropertyExtensionTest {

    companion object {
        const val testProperty = "SystemPropertyExtensionTest"
    }

    var test1DidRun = false

    @BeforeAll
    fun prepare() {
        System.setProperty(testProperty, "important system value")
    }

    @AfterAll
    fun cleanup() {
        System.clearProperty(testProperty)
    }

    @Order(1)
    @Test
    @SystemProperties(
        SystemProperty(name = "foo", value = "bar"),
        SystemProperty(name = "testProperty", value = "baz")
    )
    fun `should set system property`() {
        expectThat(test1DidRun).isFalse()
        expectThat(System.getProperty("foo")).isEqualTo("bar")
        test1DidRun = true
    }

    @Order(2)
    @Test
    fun `should restore system property`() {
        expectThat(test1DidRun).isTrue()
        expectThat(System.getProperty("foo")).isNull()
        expectThat(System.getProperty(testProperty)).isEqualTo("important system value")
    }
}
