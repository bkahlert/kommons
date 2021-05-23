package koodies.io

import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.getPosixFilePermissions

val Assertion.Builder<Path>.permissions: DescribeableBuilder<Set<PosixFilePermission>>
    get() = get("POSIX file permissions") { getPosixFilePermissions() }
