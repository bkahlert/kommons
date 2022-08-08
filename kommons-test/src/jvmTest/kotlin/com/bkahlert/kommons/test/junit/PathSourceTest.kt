package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.bar
import com.bkahlert.kommons.test.findSourceFileOrNull
import com.bkahlert.kommons.test.foo
import com.bkahlert.kommons.test.junit.PathSource.Companion.sourceUri
import com.bkahlert.kommons.test.raise
import com.bkahlert.kommons.test.raiseStackTraceElement
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.div

class PathSourceTest {

    @Test fun from(@TempDir tempDir: Path) = testAll {
        PathSource(tempDir / "file.kt", 10).toString() should {
            it.shouldStartWith("file:")
            it.shouldEndWith("file.kt?line=10")
        }

        PathSource(tempDir / "file.kt", 10, 20).toString() should {
            it.shouldStartWith("file:")
            it.shouldEndWith("file.kt?line=10&column=20")
        }
    }

    @Test fun to_string(@TempDir tempDir: Path) = testAll {
        val pathSource = PathSource(tempDir / "file.kt", 10)
        pathSource.toString() shouldBe pathSource.uri.toString()
    }

    @Test fun get_uri(@TempDir tempDir: Path) = testAll {
        val path = tempDir / "file.kt"
        PathSource(path, 10).uri shouldBe URI("${path.toUri()}?line=10")
    }

    @Test fun get_file(@TempDir tempDir: Path) = testAll {
        val path = tempDir / "file.kt"
        PathSource(path, 10).file shouldBe path.toFile()
    }

    @Test fun from_or_null() = testAll {
        val sourceFile = checkNotNull(javaClass.findSourceFileOrNull())
        val methodLineNumber = 52 // must be equal to line number of this line

        PathSource.fromOrNull(raiseStackTraceElement { foo { bar { throw RuntimeException() } } })
            .shouldBe(PathSource(sourceFile, methodLineNumber + 2, 9))
        PathSource.fromOrNull(raise { foo { bar { throw RuntimeException() } } })
            .shouldBe(PathSource(sourceFile, methodLineNumber + 4, 9))

        PathSource.fromOrNull(raiseStackTraceElement {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }).shouldBe(PathSource(sourceFile, methodLineNumber + 11, 21))
        PathSource.fromOrNull(raise {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }).shouldBe(PathSource(sourceFile, methodLineNumber + 19, 21))
    }

    @Test fun source_uri() = testAll {
        val sourceFile = checkNotNull(javaClass.findSourceFileOrNull())
        val methodLineNumber = 79 // must be equal to line number of this line

        raiseStackTraceElement { foo { bar { throw RuntimeException() } } }.sourceUri
            .shouldBe(PathSource(sourceFile, methodLineNumber + 2, 9).uri)
        raise { foo { bar { throw RuntimeException() } } }.sourceUri
            .shouldBe(PathSource(sourceFile, methodLineNumber + 4, 9).uri)

        raiseStackTraceElement {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }.sourceUri.shouldBe(PathSource(sourceFile, methodLineNumber + 11, 21).uri)
        raise {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }.sourceUri.shouldBe(PathSource(sourceFile, methodLineNumber + 19, 21).uri)
    }

    @Test fun current() = testAll {
        val sourceFile = checkNotNull(javaClass.findSourceFileOrNull())
        val methodLineNumber = 106 // must be equal to line number of this line

        foo { bar { PathSource.current } }
            .shouldBe(PathSource(sourceFile, methodLineNumber + 2, 9))

        foo {
            bar {
                @Suppress("UNUSED_VARIABLE") val now = Instant.now()
                PathSource.current
            }
        }
            .shouldBe(PathSource(sourceFile, methodLineNumber + 8, 17))
    }

    @Test fun current_uri() = testAll {
        val sourceFile = checkNotNull(javaClass.findSourceFileOrNull())
        val methodLineNumber = 122 // must be equal to line number of this line

        foo { bar { PathSource.currentUri } }
            .shouldBe(PathSource(sourceFile, methodLineNumber + 2, 9).uri)

        foo {
            bar {
                @Suppress("UNUSED_VARIABLE") val now = Instant.now()
                PathSource.currentUri
            }
        }
            .shouldBe(PathSource(sourceFile, methodLineNumber + 8, 17).uri)
    }
}
