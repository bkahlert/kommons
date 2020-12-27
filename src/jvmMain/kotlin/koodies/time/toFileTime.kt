package koodies.time

import java.nio.file.attribute.FileTime
import java.time.Instant

/**
 * Returns a [FileTime] representing the same point of time value
 * on the time-line as this instant.
 */
fun Instant.toFileTime(): FileTime = FileTime.from(this)
