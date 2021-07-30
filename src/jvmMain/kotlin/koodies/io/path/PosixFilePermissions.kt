package koodies.io.path

import java.nio.file.attribute.PosixFilePermission

public object PosixFilePermissions {

    /**
     * `0700`
     */
    public val OWNER_ALL_PERMISSIONS: Set<PosixFilePermission> =
        setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)

    /**
     * `0070`
     */
    public val GROUP_ALL_PERMISSIONS: Set<PosixFilePermission> =
        setOf(PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE)

    /**
     * `0007`
     */
    public val OTHERS_ALL_PERMISSIONS: Set<PosixFilePermission> =
        setOf(PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE)
}
