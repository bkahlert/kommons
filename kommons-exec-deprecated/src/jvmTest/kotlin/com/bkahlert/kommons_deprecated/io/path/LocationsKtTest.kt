package com.bkahlert.kommons_deprecated.io.path

import com.bkahlert.kommons.io.isSubPathOf
import strikt.api.Assertion.Builder
import java.nio.file.Path

fun Builder<Path>.isSubPathOf(path: Path): Builder<Path> =
    assert("is inside $path") {
        when (it.isSubPathOf(path)) {
            true -> pass()
            false -> fail()
        }
    }
