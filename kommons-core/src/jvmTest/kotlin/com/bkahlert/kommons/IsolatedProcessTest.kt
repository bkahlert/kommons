package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess

class IsolatedProcessTest {

    @Test
    fun `should execute main`(@TempDir tempDir: Path) = testAll {
        val file = tempDir / "file"
        val result = IsolatedProcess.exec(TestClass::class, file.pathString)
        result shouldBe 0
        file.readText() shouldBe "foo"
    }

    @Test
    fun `should return non-zero exit code on error`(@TempDir tempDir: Path) = testAll {
        val file = tempDir / "not-existing-dir" / "file"
        val result = IsolatedProcess.exec(TestClass::class, file.pathString)
        result shouldBe 1
    }

    @Test
    fun `should throw on missing main`() = testAll {
        shouldThrow<IllegalArgumentException> { IsolatedProcess.exec(IsolatedProcessTest::class) }
    }
}

class TestClass {

    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            kotlin.runCatching {
                val file = Paths.get(args.first())
                file.writeText("foo")
            }.onFailure { exitProcess(1) }
        }
    }
}
