package koodies.test

import strikt.api.Assertion
import strikt.assertions.isEqualTo
import strikt.java.fileName
import java.nio.file.Path

fun <T : Path> Assertion.Builder<T>.hasSameFileName(expected: Path) =
    fileName.isEqualTo(expected.fileName)
