package koodies.io.path

import java.nio.file.Files
import java.nio.file.Path

var Path.executable: Boolean
    get() = Files.isExecutable(this)
    set(value) {
        toFile().setExecutable(true)
    }
