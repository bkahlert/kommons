package filepeek

import koodies.io.path.addExtensions
import koodies.io.path.asPath
import koodies.io.path.extensionOrNull
import koodies.text.convertKebabCaseToCamelCase
import koodies.text.withoutPrefix
import java.io.File

data class FileInfo(
    val lineNumber: Int,
    val sourceFileName: String,
    val line: String,
    val methodName: String,
)

private val FS = File.separator

class FilePeek2(
    private val ignoredPackages: List<String> = emptyList(),
    private val sourceRoots: List<String> = listOf("src${FS}test${FS}kotlin", "src${FS}test${FS}java"),
) {

    fun getCallerFileInfo(
    ): FileInfo {
        val stackTrace = RuntimeException().stackTrace

        val callerStackTraceElement = stackTrace.first { el ->
            ignoredPackages
                .none { el.className.startsWith(it) }
        }
        val className = callerStackTraceElement.className.substringBefore('$')
        val clazz = javaClass.classLoader.loadClass(className)!!
        val classFilePath = File(clazz.protectionDomain.codeSource.location.path)
            .absolutePath

        val (matchDir, buildDir) = when {
            classFilePath.contains("${FS}out${FS}") -> "${FS}out${FS}" to "out${FS}test${FS}classes" // running inside IDEA
            classFilePath.contains("build${FS}classes${FS}java") -> "build${FS}classes${FS}java" to "build${FS}classes${FS}java${FS}test" // gradle 4.x java source
            classFilePath.contains("build${FS}classes${FS}kotlin") -> "build${FS}classes${FS}kotlin" to "build${FS}classes${FS}kotlin${FS}test" // gradle 4.x kotlin sources
            classFilePath.contains("target${FS}classes") -> "target${FS}classes" to "target${FS}classes" // maven
            else -> "build${FS}classes${FS}test" to "build${FS}classes${FS}test" // older gradle
        }

        val sourceFileCandidates: List<File> = this.sourceRoots
            .map { sourceRoot ->
                val replace = classFilePath.replace(buildDir, sourceRoot)
                val sourceFileWithoutExtension =
                    replace
                        .plus(FS + className.replace(".", FS))

                File(sourceFileWithoutExtension).parentFile
                    .resolve(callerStackTraceElement.fileName!!)
            }.let {

                val (baseDir, suffix) = classFilePath.split(matchDir).let {
                    val baseDir = it.first()
                    val suffixDir = it.last().withoutPrefix(FS).replace(FS, "-").convertKebabCaseToCamelCase()
                    baseDir to suffixDir
                }
                val sourceDir = baseDir.asPath().resolve("src").resolve(suffix).resolve("kotlin")
                val sourceFileWithoutExtension = sourceDir.resolve(className.replace(".", FS))

                val candidate1 = sourceFileWithoutExtension.addExtensions(callerStackTraceElement.fileName?.asPath()?.extensionOrNull ?: "kt").toFile()
                if (candidate1.exists()) {
                    it + candidate1
                } else {
                    val candidate2 = sourceFileWithoutExtension.parent.resolve(callerStackTraceElement.fileName!!).toFile()
                    it + candidate2
                }
            }
        val sourceFile =
            sourceFileCandidates.singleOrNull(File::exists) ?: throw SourceFileNotFoundException(
                classFilePath,
                className,
                sourceFileCandidates
            )

        val callerLine = sourceFile.bufferedReader().useLines { lines ->
            var braceDelta = 0
            lines.drop(callerStackTraceElement.lineNumber - 1)
                .takeWhileInclusive { line ->
                    val openBraces = line.count { it == '{' }
                    val closeBraces = line.count { it == '}' }
                    braceDelta += openBraces - closeBraces
                    braceDelta != 0
                }.map { it.trim() }.joinToString(separator = "")
        }

        return FileInfo(
            callerStackTraceElement.lineNumber,
            sourceFileName = sourceFile.absolutePath,
            line = callerLine.trim(),
            methodName = callerStackTraceElement.methodName

        )
    }
}

internal fun <T> Sequence<T>.takeWhileInclusive(pred: (T) -> Boolean): Sequence<T> {
    var shouldContinue = true
    return takeWhile {
        val result = shouldContinue
        shouldContinue = pred(it)
        result
    }
}

class SourceFileNotFoundException(classFilePath: String, className: String, candidates: List<File>) :
    java.lang.RuntimeException("did not find source file for class $className loaded from $classFilePath. tried: ${candidates.joinToString { it.path }}")


class LambdaBody(methodName: String, line: String) {
    val body: String

    init {
        val firstPossibleBracket = line.indexOf(methodName) + methodName.length
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
