package com.bkahlert.kommons.test

import com.bkahlert.kommons.text.capitalize
import com.bkahlert.kommons.text.indexOfOrNull
import com.bkahlert.kommons.text.withSuffix
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.readLines
import kotlin.reflect.KClass

/** The first element of this collection. Throws a [NoSuchElementException] if this collection is empty. */
private inline val <T> Iterable<T>.head: T get() = first()

/** A list containing all but the first element of this collection. */
private inline val <T> Iterable<T>.tail: List<T> get() = drop(1)


/** The [Class] containing the execution point represented by this element. */
public val StackTraceElement.`class`: Class<*> get() = Class.forName(className)

/** The [KClass] containing the execution point represented by this element. */
public val StackTraceElement.kClass: KClass<*> get() = `class`.kotlin


/**
 * Returns directory (e.g. `/home/john/dev/project/build/classes/kotlin/jvm/test`)
 * containing the classes the class represented by this Kotlin class belongs to, or `null` if it can't be located.
 */
public fun KClass<*>.findClassesDirectoryOrNull(): Path? = java.findClassesDirectoryOrNull()

/**
 * Returns directory (e.g. `/home/john/dev/project/build/classes/kotlin/jvm/test`)
 * containing the classes the class represented by this Java class belongs to, or `null` if it can't be located.
 */
public fun Class<*>.findClassesDirectoryOrNull(): Path? {
    val className = name
    val topLevelClassName = className.substringBefore('$')
    val topLevelClass = Thread.currentThread().contextClassLoader.loadClass(topLevelClassName) ?: error(buildString {
        append("error loading class $topLevelClassName")
        if (className != topLevelClassName) append(" (for $className)")
    })
    val url = topLevelClass.protectionDomain?.codeSource?.location ?: return null
    return Paths.get(url.toURI())
}


internal val defaultRelativeClassesPaths = arrayOf(
    Paths.get("out", "classes"),    // IDEA
    Paths.get("build", "classes"),  // Gradle
    Paths.get("target", "classes"), // Maven
)

/**
 * Returns the directory (e.g. `/home/john/dev/project/src/jvmTest/kotlin`)
 * containing the source code of the class represented by this Kotlin class or `null` if it can't be located.
 */
public fun KClass<*>.findSourceDirectoryOrNull(
    vararg relativeClassesPaths: Path = defaultRelativeClassesPaths,
): Path? = java.findSourceDirectoryOrNull(*relativeClassesPaths)

@Suppress("ReplaceCollectionCountWithSize")
private fun Path.contains(other: Path): Boolean =
    map { it.pathString }.windowed(other.count()).contains(other.map { it.pathString })

private fun joinCamelCase(words: Iterable<CharSequence>): String =
    buildString {
        words.firstOrNull()?.also { append(it.toString().lowercase()) }
        append(joinPascalCase(words.drop(1)))
    }

private fun joinPascalCase(words: Iterable<CharSequence>): String =
    words.joinToString("") { word ->
        when (word.length) {
            1 -> word.toString().uppercase()
            2 -> word.toString().uppercase()
            else -> word.capitalize()
        }
    }

/**
 * Returns the directory (e.g. `/home/john/dev/project/src/jvmTest/kotlin`)
 * containing the source code of the class represented by this Java class or `null` if it can't be located.
 */
public fun Class<*>.findSourceDirectoryOrNull(
    vararg relativeClassesPaths: Path = defaultRelativeClassesPaths,
): Path? {
    val classesDirectory: Path = findClassesDirectoryOrNull() ?: return null
    val buildDir: Path = relativeClassesPaths.firstOrNull { classesDirectory.contains(it) } ?: error("Unknown build directory structure")
    return classesDirectory.pathString.split(buildDir.pathString, limit = 2).run {
        val sourceRoot = Paths.get(first()) / "src"
        val suffix = Paths.get(last())
        val lang = suffix.head.pathString
        val sourceDir = suffix.map { it.pathString }.tail.let { joinCamelCase(it) }
        sourceRoot
            .resolve(sourceDir).takeIf { it.exists() }
            ?.resolve(lang)?.takeIf { it.exists() }
            ?: return null
    }
}

/**
 * Returns the file (e.g. `/home/john/dev/project/src/jvmTest/kotlin/packages/source.kt`)
 * containing the source code of the class represented by this Kotlin class or `null` if it can't be located.
 */
public fun KClass<*>.findSourceFileOrNull(
    fileNameHint: String? = null,
    vararg relativeClassesPaths: Path = defaultRelativeClassesPaths,
): Path? = java.findSourceFileOrNull(fileNameHint, *relativeClassesPaths)

/**
 * Returns the file (e.g. `/home/john/dev/project/src/jvmTest/kotlin/packages/source.kt`)
 * containing the source code of the class represented by this Java class or `null` if it can't be located.
 */
public fun Class<*>.findSourceFileOrNull(
    fileNameHint: String? = null,
    vararg relativeClassesPaths: Path = defaultRelativeClassesPaths,
): Path? {
    val sourceDir = findSourceDirectoryOrNull(*relativeClassesPaths) ?: return null
    val pkg = name.split('.').dropLast(1)
    val fileName = (fileNameHint ?: name.split('.').last().substringBefore('$')).withSuffix(".kt")
    val fileNames: List<String> = listOf(fileName, fileName.removeSuffix(".kt").withSuffix("Kt.kt"))
    val sourceFileDir = sourceDir.resolve(Paths.get(pkg.head, *pkg.tail.toTypedArray()))
    return fileNames.map { sourceFileDir.resolve(it) }.firstOrNull { it.exists() }
}

internal data class FileInfo(
    /** The source file. */
    val sourceFile: Path,
    /** The contents of the [sourceFile]. */
    val sourceFileLines: List<String> = sourceFile.readLines(),
    /**
     * The lines containing the code of interest.
     *
     * ***Note:** Line numbers are counted starting with `1`.*
     */
    val lineRange: ClosedRange<Int>,
    /** The name of the lambda. */
    val methodName: String,
) {
    /**
     * The line number that contains the [methodName].
     *
     * ***Note:** Line numbers are counted starting with `1`.*
     */
    val methodLineNumber: Int by lazy {
        for (lineIndex in lineRange.start downTo 1) {
            val line = sourceFileLines[lineIndex - 1]
            val matchingColumnIndex = line.indexOfOrNull(methodName)
                ?.takeUnless { line[methodName.length + it].isJavaIdentifierPart() }
            if (matchingColumnIndex != null) return@lazy lineIndex
        }
        lineRange.start
    }

    /**
     * The column number where [methodName] was found, or `null` otherwise.
     *
     * ***Note:** Column numbers are counted starting with `1`.*
     */
    val methodColumnNumber: Int? by lazy {
        val line = sourceFileLines[methodLineNumber - 1]
        val matchingColumnIndex = line.indexOfOrNull(methodName)
            ?.takeUnless { line[methodName.length + it].isJavaIdentifierPart() }
        (matchingColumnIndex ?: line.takeWhile { !it.isJavaIdentifierPart() }.count()) + 1
    }


    /** The lines of code containing the code of interest. */
    val lines: List<String> get() = lineRange.run { sourceFileLines.subList(start - 1, endInclusive) }

    /** The code of interest. */
    val code: String get() = lines.joinToString("\n")

    /** The code of interest as a single trimmed line. */
    val trimmedLine: String get() = lines.joinToString(separator = "") { it.trim() }.trim()

    /**
     * Returns a [FileInfo] with the [lineRange] extended so that one more previous and one further line is included.
     *
     * If [lineRange] cannot be extended further `null` is returned.
     */
    fun zoomOut(): FileInfo? {
        val zoomedOutRange = lineRange.run { (start - 1).coerceAtLeast(1)..(endInclusive + 1).coerceAtMost(sourceFileLines.size) }
        return if (zoomedOutRange == lineRange) null
        else copy(lineRange = zoomedOutRange)
    }

    /**
     * Returns a sequence starting with this file info
     * followed by [zoomOut] applied to the previously returned file info
     * until zooming out is no longer possible.
     */
    fun zoomOutSequence(): Sequence<FileInfo> {
        var next: FileInfo? = this
        return sequence {
            while (next != null) {
                next = next?.let {
                    yield(it)
                    it.zoomOut()
                }
            }
        }
    }
}

internal object FilePeekMPP {
    fun getCallerFileInfo(stackTraceElement: StackTraceElement): FileInfo? {
        val sourceFile = stackTraceElement.`class`.findSourceFileOrNull(stackTraceElement.fileName) ?: return null
        val sourceFileLines = sourceFile.readLines()
        val (lines, lineNumber) = sourceFileLines.let { lines ->
            val inlined = stackTraceElement.lineNumber >= lines.size
            if (inlined) {
                val classNames = stackTraceElement.className.split("$").map { it.substringAfterLast('.') }
                val relevantLines = classNames.fold(lines) { remainingLines, className ->
                    findBlock(remainingLines
                        .dropWhile { line -> !line.contains(className) })
                        .takeUnless { it.isEmpty() } ?: remainingLines
                }.dropWhile { !it.contains('{') }
                val fullText = lines.joinToString("\n")
                val relevantFullText = relevantLines.joinToString("\n")
                relevantLines to fullText.substringBefore(relevantFullText).lines().size
            } else {
                lines.drop(stackTraceElement.lineNumber - 1) to stackTraceElement.lineNumber
            }
        }

        return FileInfo(
            sourceFile = sourceFile,
            sourceFileLines = sourceFileLines,
            lineRange = lineNumber until lineNumber + findBlock(lines).size,
            methodName = stackTraceElement.methodName,
        )
    }

    private fun findBlock(strings: List<String>): List<String> {
        var braceDelta = 0
        return strings.takeWhileInclusive { line ->
            val openBraces = line.count { it == LambdaBody.Brackets.first }
            val closeBraces = line.count { it == LambdaBody.Brackets.second }
            braceDelta += openBraces - closeBraces
            braceDelta != 0
        }
    }

    private fun <T> List<T>.takeWhileInclusive(pred: (T) -> Boolean): List<T> {
        var shouldContinue = true
        return takeWhile {
            val result = shouldContinue
            shouldContinue = pred(it)
            result
        }
    }
}
