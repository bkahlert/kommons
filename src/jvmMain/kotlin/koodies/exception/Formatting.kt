package koodies.exception

import koodies.concurrent.process.Process
import koodies.text.LineSeparators.lines
import java.nio.file.Path

fun Any?.toCompactString(): String = when (this) {
    is Array<*> -> toList().toCompactString()
    is Path -> toUri().toString()
    is Process -> also { waitFor() }.exitValue.toString()
    is java.lang.Process -> also { waitFor() }.exitValue().toString()
    else -> if (this == null || this == Unit) "" else toString()
}

fun Throwable?.toCompactString(): String {
    if (this == null) return ""
    val messagePart = message?.let { ": " + it.lines()[0] } ?: ""
    return rootCause.run {
        this::class.simpleName + messagePart + stackTrace?.firstOrNull()
            ?.let { element -> " at.(${element.fileName}:${element.lineNumber})" }
    }
}

fun Result<*>?.toCompactString(): String {
    if (this == null) return ""
    return if (isSuccess) getOrNull().toCompactString()
    else exceptionOrNull().toCompactString()
}
