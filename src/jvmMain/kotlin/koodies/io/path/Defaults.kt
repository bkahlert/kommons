package koodies.io.path

import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.writeLines


object Defaults {
    /**
     * Default [StandardOpenOption] arguments for append operations such as [Path.writeLines].
     *
     * The effect is that a file will be created if it does not exist and otherwise overridden.
     */
    val DEFAULT_WRITE_OPTIONS: Array<StandardOpenOption> =
        arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

    /**
     * Default [StandardOpenOption] arguments for append operations such as [Path.appendLines].
     *
     * The effect is that a file will be created if it does not exist and otherwise appended to.
     */
    val DEFAULT_APPEND_OPTIONS: Array<StandardOpenOption> =
        arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)

    val OWNER_ALL_PERMISSIONS = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)
    val GROUP_ALL_PERMISSIONS = setOf(PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE)
    val OTHERS_ALL_PERMISSIONS = setOf(PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE)
}
