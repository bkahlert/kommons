package com.bkahlert.kommons.text

import com.bkahlert.kommons.ClassPath
import com.bkahlert.kommons.CodePoint
import com.bkahlert.kommons.useBufferedReader

private val mappings: Map<Int, String> = ClassPath("unicode.dict.tsv").useBufferedReader { reader ->
    reader
        .lineSequence()
        .dropWhile { line -> line.startsWith("#") }
        .associate { line ->
            val columns = line.split("\t")
            val index = columns.first().toInt(16)
            val name = columns.last()
            index to name
        }
}


internal object UnicodeDict : Map<Int, String> by mappings {

    override fun get(key: Int): String {
        return mappings[key] ?: "\\u${key.toString(16)}!!${key.toChar().category.name}"
    }
}

/** The Unicode name of this code point, e.g. `LINE SEPARATOR` */
public val CodePoint.unicodeName: String get() = UnicodeDict[index]

/** The formatted Unicode name of this code point, e.g. `❲LINE SEPARATOR❳` */
public val CodePoint.formattedName: String get() = "❲$unicodeName❳"
