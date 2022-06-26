package com.bkahlert.kommons.exception

import com.bkahlert.kommons.LineSeparators.lines
import com.bkahlert.kommons.debug.Compression.Always
import com.bkahlert.kommons.debug.render
import com.bkahlert.kommons.exec.Process
import java.nio.file.Path

// TODO migrate
// TODO merge with simpleString
public fun Any?.toCompactString(): String = when (this) {
    is Path -> toUri().toString()
    is Process -> kotlin.runCatching { state.status }.fold({ it }, { it.toCompactString() })
    is java.lang.Process -> also { waitFor() }.exitValue().toString()
    else -> render { compression = Always }
}

public fun Throwable?.toCompactString(): String {
    if (this == null) return ""
    val messagePart = message?.let { ": " + it.lines()[0] } ?: ""
    return rootCause.run {
        this::class.simpleName + messagePart + stackTrace?.firstOrNull()
            ?.let { element -> " at.(${element.fileName}:${element.lineNumber})" }
    }
}

public fun Result<*>?.toCompactString(): String {
    if (this == null) return ""
    return if (isSuccess) getOrNull().toCompactString()
    else exceptionOrNull().toCompactString()
}

private val Throwable.rootCause: Throwable
    get() {
        var rootCause: Throwable = this
        while (rootCause.cause != null && rootCause.cause !== rootCause) {
            rootCause = rootCause.cause ?: error("Must not happen.")
        }
        return rootCause
    }
