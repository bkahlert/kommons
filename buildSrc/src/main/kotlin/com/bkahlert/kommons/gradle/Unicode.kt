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
        fun Int.formatHex() = "0x${toString(16)}"

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

                out.println("private inline fun a(start: Int, endInclusive: Int): IntArray = intArrayOf(start, endInclusive)")
                out.println()
                out.println("""@Suppress("LongLine")""")
                out.println("internal val unicodeCategoryRanges: Map<String, Array<IntArray>> by lazy {")
                out.println("    // @formatter:off")
                out.println("    val map=mutableMapOf<String, Array<IntArray>>()")
                val categoryRanges = data.useLines { lines ->
                    lines.asSequence()
                        .fold(mutableListOf<Pair<String, IntRange>>()) { groups, line ->
                            val (idxStr, _, cat) = line.split(';')
                            val idx = idxStr.toInt(16)

                            val (lastCat, lastRange) = groups.lastOrNull() ?: (null to null)
                            if (lastCat == cat && lastRange?.endInclusive == idx - 1) {
                                groups[groups.lastIndex] = cat to lastRange.first..idx
                            } else {
                                groups.add(cat to idx..idx)
                            }
                            groups
                        }.groupBy({ (cat, _) -> cat }) { (_, range) -> range }
                }
                categoryRanges.forEach { (cat, ranges) ->
                    out.println("""    map["$cat"]=arrayOf(""")
                    ranges.chunked(8).forEach{ chunk ->
                        out.indent(2)
                        chunk.joinTo(out) { "a(${it.start.formatHex()},${it.endInclusive.formatHex()})" }
                        out.println(",")
                    }
                    out.println("""    )""")
                }
                out.println("    map")
                out.println("    // @formatter:on")
                out.println("}")
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
