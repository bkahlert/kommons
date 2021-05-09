package koodies.io.file

import koodies.io.path.pathString
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path
import java.nio.file.Paths

class PathStringTest {

    @TestFactory
    fun `path string`() = listOf(
        "/absolute/path",
        "file.ext",
        "dir",
        "..",
        ".",
        "",
    ).map { path ->
        dynamicContainer("\"$path\"", listOf(
            dynamicContainer("as generic path", Paths.get(path).let { genericPath ->
                listOf(
                    dynamicTest("should have identical serialization") {
                        expectThat(genericPath).pathStringIsEqualTo(path)
                    },
                )
            }),
            dynamicContainer("as path with overwritten toString", (object : Path by Paths.get(path) {
                override fun toString(): String = "custom toString"
            }).let { customPath ->
                listOf(
                    dynamicTest("should have custom toString") {
                        expectThat("$customPath").isEqualTo("custom toString")
                    },
                    dynamicTest("should have identical serialization") {
                        expectThat(customPath).pathStringIsEqualTo(path)
                    },
                    dynamicTest("should have get equal path from serialized path") {
                        expectThat(Paths.get(customPath.pathString)).isEqualTo(Paths.get(path))
                    },
                )
            }),
        ))
    }
}

fun <T : Path> Assertion.Builder<T>.pathStringIsEqualTo(path: String) =
    assert("equals $path") {
        val actual = it.pathString
        if (actual == path) pass()
        else fail()
    }
