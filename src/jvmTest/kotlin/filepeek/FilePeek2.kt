package filepeek

import koodies.collections.head
import koodies.collections.tail
import koodies.io.path.asPath
import koodies.io.path.pathString
import koodies.text.LineSeparators.LF
import koodies.text.joinToCamelCase
import koodies.text.withSuffix
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

data class FileInfo(
    val lineNumber: Int,
    val sourceFileName: String,
    val line: String,
    val methodName: String,
)

private fun Path.contains(other: Path): Boolean =
    other.map { it.pathString }.let { otherStrings ->
        map { it.pathString }.windowed(otherStrings.size).contains(otherStrings)
    }

class FilePeek2(
    private val stackTraceElement: StackTraceElement,
    private val classesToSourceMappings: List<Path> = listOf(
        Path.of("out", "classes"), // IDEA
        Path.of("build", "classes"), // Gradle
        Path.of("target", "classes"), // Maven
    ),
) {

    private val classLoader = javaClass.classLoader

    fun getCallerFileInfo(): FileInfo {

        val classesDirectory: Path = stackTraceElement.run {
            val baseClassName = className.substringBefore('$')
            val baseClass = classLoader.loadClass(baseClassName) ?: error("Error loading base class $baseClassName of $className")
            Path.of(baseClass.protectionDomain.codeSource.location.toURI())
        }

        val buildDir: Path = classesToSourceMappings.firstOrNull { classesDirectory.contains(it) } ?: error("Unknown build directory structure")
        val sourceDir = classesDirectory.pathString.split(buildDir.pathString, limit = 2).run {
            val sourceRoot = first().asPath().resolve("src")
            val suffix = last().asPath()
            val lang = suffix.head.pathString
            val sourceDir = suffix.map { it.pathString }.tail.joinToCamelCase()
            sourceRoot.resolve(sourceDir).resolve(lang)
        }

        val pkg = stackTraceElement.className.split(".").dropLast(1)
        val fileName = stackTraceElement.fileName ?: error("Unknown filename in $stackTraceElement")
        val fileNames: List<String> = listOf(fileName, fileName.removeSuffix(".kt").withSuffix("Kt.kt"))
        val sourceFileDir = sourceDir.resolve(Path.of(pkg.head, *pkg.tail.toTypedArray()))
        val sourceFile: Path = fileNames.map { sourceFileDir.resolve(it) }.single { it.exists() }

        val (lines, lineNumber) = sourceFile.readLines().let { lines ->
            if (stackTraceElement.lineNumber < lines.size) {
                // looks like not inlined
                lines.drop(stackTraceElement.lineNumber - 1) to stackTraceElement.lineNumber
            } else {
                // obviously inlined since line number > available lines
                val classNames = stackTraceElement.className.split("$").map { it.substringAfterLast('.') }
                val relevantLines = classNames.fold(lines) { remainingLines, className ->
                    remainingLines
                        .dropWhile { line -> !line.contains(className) }
                        .findBlock()
                        .takeUnless { it.isEmpty() } ?: remainingLines
                }.dropWhile { !it.contains('{') }
                val fullText = lines.joinToString(LF)
                val relevantFullText = relevantLines.joinToString(LF)
                relevantLines to fullText.substringBefore(relevantFullText).lines().size
            }
        }

        val callerLine: String = lines.findBlock().joinToString(separator = "") { it.trim() }

        return FileInfo(
            lineNumber,
            sourceFileName = sourceFile.pathString,
            line = callerLine.trim(),
            methodName = stackTraceElement.methodName
        )
    }
}

private fun List<String>.findBlock(): List<String> {
    var braceDelta = 0
    return takeWhileInclusive { line ->
        val openBraces = line.count { it == '{' }
        val closeBraces = line.count { it == '}' }
        braceDelta += openBraces - closeBraces
        braceDelta != 0
    }
}

internal fun <T> List<T>.takeWhileInclusive(pred: (T) -> Boolean): List<T> {
    var shouldContinue = true
    return takeWhile {
        val result = shouldContinue
        shouldContinue = pred(it)
        result
    }
}


class LambdaBody(methodName: String, line: String) {
    val body: String

    init {
        val firstPossibleBracket = line.indexOf(methodName).takeIf { it >= 0 }?.let { it + methodName.length } ?: 0
        val firstBracket = line.indexOf('{', firstPossibleBracket) + 1
        val subjectEnd = findMatchingClosingBracket(line, firstBracket)
        body = line.substring(firstBracket, subjectEnd).trim()
    }

    private fun findMatchingClosingBracket(condition: String, start: Int): Int {
        val len = condition.length
        var bracketLevel = 0
        var pos = start
        while (pos < len) {
            when (condition[pos]) {
                '{' -> bracketLevel += 1
                '}' -> if (bracketLevel == 0) return pos else bracketLevel -= 1
            }
            pos += 1
        }
        error("could not find matching brackets in $condition")
    }
}
