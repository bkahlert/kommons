package com.bkahlert.kommons.gradle

import java.io.File
import java.io.OutputStream
import java.io.PrintWriter
import java.net.URL

enum class Unicode(
    vararg val path: String,
    baseUrl: URL = URL("https://www.unicode.org/Public/"),
    val url: URL = URL(buildString {
        append(baseUrl)
        path.joinTo(this, "/")
    }),
) {
    UnicodeData("UCD", "latest", "ucd", "UnicodeData.txt") {
        val sourceDirRegex = Regex("[a-z]+(?:Main|Test)")
        fun String.formatHex() = "0x${trimStart('0').takeUnless { it.isEmpty() } ?: "0"}"

        fun PrintWriter.indent(levels: Int = 1) {
            check(levels > 0)
            for (i in 0 until levels) {
                print("    ")
            }
        }

        override fun processTo(
            data: File,
            sourceFile: File,
        ) {
            sourceFile.printWriter().use { out ->
                out.print("package ")
                sourceFile.parentFile.toPath().map { it.toString() }
                    .dropWhile { !it.matches(sourceDirRegex) }
                    .dropWhile { it.matches(sourceDirRegex) }
                    .drop(1) // "kotlin"
                    .joinTo(out, ".")
                out.println()
                out.println()

                val generalCategories = mutableSetOf<String>()
                data.useLines { lines ->
                    out.println("// @formatter:off")
                    out.println("internal val UnicodeData: Map<Int, GeneralCategory> by lazy {")
                    out.indent()
                    out.println("HashMap<Int, GeneralCategory>().apply {")
                    lines.forEach { line ->
                        val (codePoints, _, generalCategory) = line.split(';')
                        generalCategories.add(generalCategory)
                        out.indent(2)
                        out.print("put(")
                        codePoints.split("..").joinTo(out, "..") { it.formatHex() }
                        out.print(", ")
                        out.print("GC.$generalCategory")
                        out.println(")")
                    }
                    out.indent()
                    out.println("}")
                    out.println("}")
                    out.println("// @formatter:on")
                    out.println()
                }

                out.println("internal enum class GeneralCategory {")
                out.indent()
                generalCategories.joinTo(out, ", ")
                out.println(";")
                out.indent()
                out.println("companion object")
                out.println("}")
                out.println()
                out.println("private typealias GC = GeneralCategory")
            }
        }
    },
    ;

    protected abstract fun processTo(
        data: File,
        sourceFile: File,
    )

    /**
     * Processes the specified [file] to the specified [sourceFile].
     */
    fun processTo(
        file: File,
        sourceFile: File = file.resolveSibling(file.name.substringBeforeLast('.') + ".kt"),
        overwrite: Boolean = false,
    ): File {
        if (sourceFile.exists()) {
            println(sourceFile.exists())
            if (overwrite) sourceFile.delete()
            else return sourceFile
        }
        println(sourceFile.exists())

        processTo(file, sourceFile)

        return sourceFile
    }

    /**
     * Downloads this Unicode file to the specified [directory],
     * using the specified [filename].
     */
    fun downloadTo(
        directory: File,
        filename: String = path.last(),
        overwrite: Boolean = false,
    ): File {
        val destination: File = directory.resolve(filename)

        if (destination.exists()) {
            if (overwrite) destination.delete()
            else return destination
        }

        url.openStream().use { inputStream ->
            destination.outputStream().use { outputStream: OutputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return destination
    }

    fun generate(
        sourceFile: File,
        overwrite: Boolean = false,
        removeDownload: Boolean = true,
    ): File {
        if (sourceFile.exists()) {
            if (overwrite) sourceFile.delete()
            else return sourceFile
        }

        val download = downloadTo(
            directory = sourceFile.parentFile,
            overwrite = overwrite,
        )

        return processTo(
            file = download,
            sourceFile = sourceFile,
            overwrite = overwrite,
        ).also {
            if (removeDownload) {
                download.delete()
            }
        }
    }
}
