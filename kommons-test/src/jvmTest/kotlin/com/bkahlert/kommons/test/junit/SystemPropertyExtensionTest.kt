package com.bkahlert.kommons.test.junit

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class SystemPropertyExtensionTest {

    companion object {
        private const val testProperty = "foo"
        private val read = mutableListOf<String>()
    }

    @BeforeAll
    fun prepare() {
        System.setProperty(testProperty, "init")
    }

    @AfterAll
    fun cleanup() {
        read.shouldContainExactlyInAnyOrder("1", "2", "3", "4", "5")
        System.clearProperty(testProperty)
    }

    @Test
    @SystemProperty(name = "foo", value = "1")
    @SystemProperty(name = "bar", value = "baz-1")
    fun test1() {
        read.add(System.getProperty("foo"))
        System.getProperty("bar") shouldBe "baz-1"
    }

    @Test
    @SystemProperty(name = "foo", value = "2")
    @SystemProperty(name = "bar", value = "baz-2")
    fun test2() {
        read.add(System.getProperty("foo"))
        System.getProperty("bar") shouldBe "baz-2"
    }

    @Test
    @SystemProperty(name = "foo", value = "3")
    @SystemProperty(name = "bar", value = "baz-3")
    fun test3() {
        read.add(System.getProperty("foo"))
        System.getProperty("bar") shouldBe "baz-3"
    }

    @Test
    @SystemProperties(
        SystemProperty(name = "foo", value = "4"),
        SystemProperty(name = "bar", value = "baz-4")
    )
    fun test4() {
        read.add(System.getProperty("foo"))
        System.getProperty("bar") shouldBe "baz-4"
    }

    @Test
    @SystemProperties(
        SystemProperty(name = "foo", value = "5"),
        SystemProperty(name = "bar", value = "baz-5")
    )
    fun test5() {
        read.add(System.getProperty("foo"))
        System.getProperty("bar") shouldBe "baz-5"
    }
}
