package com.bkahlert.kommons.io.path

import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.writeLines

public object StandardOpenOptions {
    /**
     * Default [StandardOpenOption] arguments for append operations such as [Path.writeLines].
     *
     * The effect is that a file will be created if it does not exist and otherwise overridden.
     */
    public val DEFAULT_WRITE_OPTIONS: Array<StandardOpenOption> =
        arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

    /**
     * Default [StandardOpenOption] arguments for append operations such as [Path.appendLines].
     *
     * The effect is that a file will be created if it does not exist and otherwise appended to.
     */
    public val DEFAULT_APPEND_OPTIONS: Array<StandardOpenOption> =
        arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
}
