package com.bkahlert.kommons.io.path

import java.nio.file.Files
import java.nio.file.Path

public var Path.executable: Boolean
    get() = Files.isExecutable(this)
    set(value) {
        toFile().setExecutable(value)
    }
