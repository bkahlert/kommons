package com.bkahlert.kommons.test


import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


class JvmFilePeekKtTest {

    @Test fun find_classes_directory_or_null() = testAll(
        { this::class.findClassesDirectoryOrNull() },
        { javaClass.findClassesDirectoryOrNull() },
        { StaticClass::class.findClassesDirectoryOrNull() },
        { StaticClass::class.java.findClassesDirectoryOrNull() },
        { InnerClass::class.findClassesDirectoryOrNull() },
        { InnerClass::class.java.findClassesDirectoryOrNull() },
    ) { compute ->
        compute().shouldNotBeNull() should { dir ->
            dir.shouldBeADirectory()
            dir.shouldEndWith("kotlin", "jvm", "test")
        }

        String::class.findClassesDirectoryOrNull().shouldBeNull()
    }

    @Test fun find_source_directory_or_null() = testAll(
        { this::class.findSourceDirectoryOrNull() },
        { javaClass.findSourceDirectoryOrNull() },
        { StaticClass::class.findSourceDirectoryOrNull() },
        { StaticClass::class.java.findSourceDirectoryOrNull() },
        { InnerClass::class.findSourceDirectoryOrNull() },
        { InnerClass::class.java.findSourceDirectoryOrNull() },
    ) { compute ->
        compute().shouldNotBeNull() should { dir ->
            dir.shouldBeADirectory()
            dir.shouldEndWith("src", "jvmTest", "kotlin")
        }

        String::class.findSourceDirectoryOrNull().shouldBeNull()
    }

    @Test fun find_source_file_or_null() = testAll(
        { this::class.findSourceFileOrNull() },
        { javaClass.findSourceFileOrNull() },
        { StaticClass::class.findSourceFileOrNull() },
        { StaticClass::class.java.findSourceFileOrNull() },
        { InnerClass::class.findSourceFileOrNull() },
        { InnerClass::class.java.findSourceFileOrNull() },
    ) { compute ->
        compute().shouldNotBeNull() should { file ->
            file.shouldBeAFile()
            file.shouldEndWith("src", "jvmTest", "kotlin", "com", "bkahlert", "kommons", "test", "JvmFilePeekKtTest.kt")
        }

        String::class.findSourceFileOrNull().shouldBeNull()
    }


    inner class StaticClass
    inner class InnerClass


    @Test fun get_caller_file_info() = testAll {
        FilePeekMPP.getCallerFileInfo(raiseStackTraceElement {
            throw RuntimeException()
        }).shouldNotBeNull() should { fileInfo ->
            fileInfo.sourceFile should { file ->
                file.shouldEndWith("src", "jvmTest", "kotlin", "com", "bkahlert", "kommons", "test", "JvmFilePeekKtTest.kt")
                file.shouldExist()
            }
            fileInfo.sourceFileLines shouldHaveAtLeastSize 100
            fileInfo.lineRange shouldBe 73..73
            fileInfo.methodLineNumber shouldBe 71
            fileInfo.methodColumnNumber shouldBe 15
            fileInfo.methodName shouldBe "get_caller_file_info"
            fileInfo.lines.shouldContainExactly("            throw RuntimeException()")
            fileInfo.code shouldBe "            throw RuntimeException()"
            fileInfo.trimmedLine shouldBe "throw RuntimeException()"
            fileInfo.zoomOut().shouldNotBeNull() should { zoomedOutFileInfo ->
                zoomedOutFileInfo.sourceFile shouldBe fileInfo.sourceFile
                zoomedOutFileInfo.sourceFileLines shouldBe fileInfo.sourceFileLines
                zoomedOutFileInfo.lineRange shouldBe 72..74
                zoomedOutFileInfo.methodLineNumber shouldBe 71
                zoomedOutFileInfo.methodColumnNumber shouldBe 15
                zoomedOutFileInfo.methodName shouldBe fileInfo.methodName
                zoomedOutFileInfo.lines.shouldContainExactly(
                    "        FilePeekMPP.getCallerFileInfo(raiseStackTraceElement {",
                    "            throw RuntimeException()",
                    "        }).shouldNotBeNull() should { fileInfo ->",
                )
                zoomedOutFileInfo.code shouldBe """
                    FilePeekMPP.getCallerFileInfo(raiseStackTraceElement {
                        throw RuntimeException()
                    }).shouldNotBeNull() should { fileInfo ->
                """.trimIndent().prependIndent("        ")
                zoomedOutFileInfo.trimmedLine shouldBe """
                    FilePeekMPP.getCallerFileInfo(raiseStackTraceElement {throw RuntimeException()}).shouldNotBeNull() should { fileInfo ->
                """.trimIndent()
            }
        }
    }

    @Test fun get_caller_file_info__fallback() {
        FilePeekMPP.getCallerFileInfo(raiseStackTraceElement {
            foo { bar { throw RuntimeException() } }
        }).shouldNotBeNull() should { fileInfo ->
            fileInfo.sourceFile should { file ->
                file.shouldEndWith("src", "jvmTest", "kotlin", "com", "bkahlert", "kommons", "test", "JvmFilePeekKtTest.kt")
                file.shouldExist()
            }
            fileInfo.sourceFileLines shouldHaveAtLeastSize 100
            fileInfo.lineRange shouldBe 113..113
            fileInfo.methodLineNumber shouldBe 113
            fileInfo.methodColumnNumber shouldBe 13
            fileInfo.methodName shouldBe "invoke"
            fileInfo.lines.shouldContainExactly("            foo { bar { throw RuntimeException() } }")
            fileInfo.code shouldBe "            foo { bar { throw RuntimeException() } }"
            fileInfo.trimmedLine shouldBe "foo { bar { throw RuntimeException() } }"
            fileInfo.zoomOut().shouldNotBeNull() should { zoomedOutFileInfo ->
                zoomedOutFileInfo.sourceFile shouldBe fileInfo.sourceFile
                zoomedOutFileInfo.sourceFileLines shouldBe fileInfo.sourceFileLines
                zoomedOutFileInfo.lineRange shouldBe 112..114
                zoomedOutFileInfo.methodLineNumber shouldBe 112
                zoomedOutFileInfo.methodColumnNumber shouldBe 9
                zoomedOutFileInfo.methodName shouldBe fileInfo.methodName
                zoomedOutFileInfo.lines.shouldContainExactly(
                    "        FilePeekMPP.getCallerFileInfo(raiseStackTraceElement {",
                    "            foo { bar { throw RuntimeException() } }",
                    "        }).shouldNotBeNull() should { fileInfo ->",
                )
                zoomedOutFileInfo.code shouldBe """
                    FilePeekMPP.getCallerFileInfo(raiseStackTraceElement {
                        foo { bar { throw RuntimeException() } }
                    }).shouldNotBeNull() should { fileInfo ->
                """.trimIndent().prependIndent("        ")
                zoomedOutFileInfo.trimmedLine shouldBe """
                    FilePeekMPP.getCallerFileInfo(raiseStackTraceElement {foo { bar { throw RuntimeException() } }}).shouldNotBeNull() should { fileInfo ->
                """.trimIndent()
            }
        }
    }
}

internal inline fun raiseStackTraceElement(block: () -> Nothing): StackTraceElement =
    raise(block).stackTrace.first()

internal inline fun raise(block: () -> Nothing): Throwable =
    try {
        block()
    } catch (e: Throwable) {
        e
    }

internal fun <R> foo(block: () -> R): R = block()
internal fun <R> bar(block: () -> R): R = block()
