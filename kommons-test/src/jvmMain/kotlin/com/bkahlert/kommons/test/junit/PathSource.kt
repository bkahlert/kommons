package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.FileInfo
import com.bkahlert.kommons.test.FilePeekMPP
import com.bkahlert.kommons.test.KommonsTest
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSystemSource
import java.io.File
import java.net.URI
import java.nio.file.Path

/** Path based [FileSystemSource]. */
public data class PathSource(
    /** The source file. */
    public val path: Path,
    /** The line number of the source file. */
    public val lineNumber: Int,
    /** The optional column number of the source file. */
    public val columnNumber: Int? = null,
) : FileSystemSource {
    /** Creates a [PathSource] from the specified [fileInfo]. */
    internal constructor(fileInfo: FileInfo) : this(fileInfo.sourceFile, fileInfo.methodLineNumber, fileInfo.methodColumnNumber)

    /** Returns the source file as a [File]. */
    override fun getFile(): File = path.toFile()

    /**
     * Returns an [URI] that can be used to describe (test) source locations,
     * i.e. in combination with [DynamicContainer] and [DynamicTest]
     * @see FilePosition.fromQuery
     */
    override fun getUri(): URI = buildString {
        append(path.toUri())
        append("?line=$lineNumber")
        columnNumber?.let { append("&column=$it") }
    }.let(::URI)

    override fun toString(): String = uri.toString()

    public companion object {

        /** Returns a [PathSource] from the specified [stackTraceElement], or `null` if the corresponding file cannot be located. */
        public fun fromOrNull(stackTraceElement: StackTraceElement): PathSource? =
            FilePeekMPP.getCallerFileInfo(stackTraceElement)?.let(::PathSource)

        /** Returns a [PathSource] from the specified [exception], or `null` if the corresponding file cannot be located. */
        public fun fromOrNull(exception: Throwable): PathSource? =
            fromOrNull(exception.stackTrace.first())

        /** [URI] of the [PathSource] located using this [StackTraceElement], or `null` if the corresponding file cannot be located. */
        public val StackTraceElement.sourceUri: URI? get() = fromOrNull(this)?.uri

        /** [URI] of the [PathSource] located using this [Throwable], or `null` if the corresponding file cannot be located. */
        public val Throwable.sourceUri: URI? get() = fromOrNull(this)?.uri

        /** [PathSource] pointing to the current location, or `null` if the corresponding file cannot be located. */
        public val current: PathSource? get() = fromOrNull(KommonsTest.locateCall())

        /** [URI] of the [PathSource] pointing to the current location, or `null` if the corresponding file cannot be located. */
        public val currentUri: URI? get() = current?.uri
    }
}
