package koodies.test

import strikt.api.Assertion
import strikt.assertions.fileName
import strikt.assertions.isEqualTo
import java.nio.file.Path

fun <T : Path> Assertion.Builder<T>.hasSameFileName(expected: Path) =
    fileName.isEqualTo(expected.fileName)
