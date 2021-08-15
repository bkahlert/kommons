package com.bkahlert.kommons.text

import com.bkahlert.kommons.collections.Dictionary
import com.bkahlert.kommons.collections.dictOf
import com.bkahlert.kommons.math.RoundingMode
import com.bkahlert.kommons.math.RoundingMode.CEILING
import com.bkahlert.kommons.runtime.asSystemResourceUrl
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.Unicode.Emojis.Emoji
import com.bkahlert.kommons.text.Unicode.Emojis.FullHoursDictionary
import com.bkahlert.kommons.text.Unicode.Emojis.HalfHoursDictionary
import com.bkahlert.kommons.text.Unicode.UnicodeBlockMeta
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.streams.toList

private object UnicodeDict : Dictionary<Long, String> by
dictOf("unicode.dict.tsv".asSystemResourceUrl().loadTabSeparatedValues(skipLines = 1), { "\\u${it.toString(16)}!!${it.toInt().toChar().category.name}" })

public operator fun Unicode.get(codePoint: Long): String = UnicodeDict.get(codePoint)

public fun Instant.asEmoji(roundingMode: RoundingMode = CEILING): Emoji {
    val zonedDateTime: ZonedDateTime = atZone(ZoneId.systemDefault())
    val hour = zonedDateTime.hour
    val minute = zonedDateTime.minute
    val closest = (roundingMode(minute.toDouble(), 30.0) / 30.0).toInt()
    return listOf(FullHoursDictionary[hour - 1], HalfHoursDictionary[hour - 1], FullHoursDictionary[hour])[closest]
}

public fun <T> UnicodeBlockMeta<T>.asTable(): String where T : Unicode.UnicodeBlock<T>, T : Enum<T> = with(unicodeBlock) {
    check(isValid) { "Unicode block must have the same number of values ($valueCount) as code points ($codePointCount)." }
    val table = StringBuilder()
    for (codePoint in range) {
        table.append("$codePoint\t${codePoint.unicodeName}$LF")
    }
    "$table"
}

private fun URL.loadTabSeparatedValues(skipLines: Long) = openStream().bufferedReader().lines().skip(skipLines).map { row ->
    row.split("\t").let { java.lang.Long.parseLong(it.first(), 16) to it.last() }
}.toList().toMap()
