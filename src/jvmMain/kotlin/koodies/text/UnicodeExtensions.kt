package koodies.text

import koodies.collections.Dictionary
import koodies.collections.dictOf
import koodies.number.ApproximationMode
import koodies.runtime.asSystemResourceUrl
import koodies.text.Unicode.Emojis.Emoji
import koodies.text.Unicode.Emojis.FullHoursDictionary
import koodies.text.Unicode.Emojis.HalfHoursDictionary
import koodies.text.Unicode.UnicodeBlockMeta
import koodies.time.Now
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.streams.toList

private object UnicodeDict : Dictionary<Long, String> by
dictOf("unicode.dict.tsv".asSystemResourceUrl().loadTabSeparatedValues(skipLines = 1), { "\\u${it.toString(16)}!!${it.toChar().category.name}" })

operator fun Unicode.get(codePoint: Long): String = UnicodeDict.get(codePoint)

/**
 * Returns this character's [Unicode name](https://unicode.org/charts/charindex.html).
 */
val Char.unicodeName: String get() = UnicodeDict[this.toLong()]

fun Instant.asEmoji(approximationMode: ApproximationMode = ApproximationMode.Ceil): Emoji {
    val zonedDateTime: ZonedDateTime = atZone(ZoneId.systemDefault())
    val hour = zonedDateTime.hour
    val minute = zonedDateTime.minute
    val closest = (approximationMode.calc(minute.toDouble(), 30.0) / 30.0).toInt()
    return listOf(FullHoursDictionary[hour - 1], HalfHoursDictionary[hour - 1], FullHoursDictionary[hour])[closest]
}

val Now.emoji get() = instant.asEmoji()

fun <T> UnicodeBlockMeta<T>.asTable(): String where T : Unicode.UnicodeBlock<T>, T : Enum<T> = with(unicodeBlock) {
    check(isValid) { "Unicode block must have the same number of values ($valueCount) as code points ($codePointCount)." }
    val table = StringBuilder()
    for (codePoint in range) {
        table.append("$codePoint\t${codePoint.unicodeName}\n")
    }
    "$table"
}

private fun URL.loadTabSeparatedValues(skipLines: Long) = openStream().bufferedReader().lines().skip(skipLines).map { row ->
    row.split("\t").let { java.lang.Long.parseLong(it.first(), 16) to it.last() }
}.toList().toMap()
