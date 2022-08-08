package com.bkahlert.kommons.test

import kotlin.streams.asSequence

internal enum class UnicodeFont(
    private val capitalLetters: IntArray,
    private val smallLetters: IntArray,
    private val digits: IntArray? = null,
) {
    Bold("𝐀".."𝐙", "𝐚".."𝐳", "𝟎".."𝟗"),
    Italic(("𝐴".."𝑍").toIntArray(), "𝑎𝑏𝑐𝑑𝑒𝑓𝑔ℎ𝑖𝑗𝑘𝑙𝑚𝑛𝑜𝑝𝑞𝑟𝑠𝑡𝑢𝑣𝑤𝑥𝑦𝑧".codePoints),
    BoldItalic("𝑨".."𝒁", "𝒂".."𝒛"),
    Script("𝒜ℬ𝒞𝒟ℰℱ𝒢ℋℐ𝒥𝒦ℒℳ𝒩𝒪𝒫𝒬ℛ𝒮𝒯𝒰𝒱𝒲𝒳𝒴𝒵".codePoints, "𝒶𝒷𝒸𝒹ℯ𝒻ℊ𝒽𝒾𝒿𝓀𝓁𝓂𝓃ℴ𝓅𝓆𝓇𝓈𝓉𝓊𝓋𝓌𝓍𝓎𝓏".codePoints),
    BoldScript("𝓐".."𝓩", "𝓪".."𝔃"),
    Fraktur("𝔄𝔅ℭ𝔇𝔈𝔉𝔊ℌℑ𝔍𝔎𝔏𝔐𝔑𝔒𝔓𝔔ℜ𝔖𝔗𝔘𝔙𝔚𝔛𝔜ℨ".codePoints, ("𝔞".."𝔷").toIntArray()),
    BoldFraktur("𝕬".."𝖅", "𝖆".."𝖟"),
    DoubleStruck("𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ".codePoints, ("𝕒".."𝕫").toIntArray(), ("𝟘".."𝟡").toIntArray()),
    SansSerif("𝖠".."𝖹", "𝖺".."𝗓", "𝟢".."𝟫"),
    SansSerifBold("𝗔".."𝗭", "𝗮".."𝘇", "𝟬".."𝟵"),
    SansSerifItalic("𝘈".."𝘡", "𝘢".."𝘻"),
    SansSerifBoldItalic("𝘼".."𝙕", "𝙖".."𝙯"),
    Monospace("𝙰".."𝚉", "𝚊".."𝚣", "𝟶".."𝟿"),
    ;

    init {
        check(capitalLetters.size == 26) { "26 capital letters needed" }
        check(smallLetters.size == 26) { "26 small letters needed" }
        digits?.also { check(it.size == 10) { "either no or 10 digits needed" } }
    }

    constructor(capitalLetters: IntRange, smallLetters: IntRange, digits: IntRange? = null) : this(
        capitalLetters.toIntArray(), smallLetters.toIntArray(), digits?.toIntArray(),
    )

    fun convertCodePointOrNull(codePoint: Int): Int? =
        capitalLetters.getOrNull(codePoint - capitalLetterACodePoint)
            ?: smallLetters.getOrNull(codePoint - smallLetterACodePoint)
            ?: digits?.getOrNull(codePoint - digitZeroCodePoint)

    fun format(text: String, onFailure: (codePoint: Int) -> Int = { it }): String {
        return buildString {
            text.codePoints().forEach {
                val codePoint = convertCodePointOrNull(it) ?: onFailure(it)
                appendCodePoint(codePoint)
            }
        }
    }

    companion object {
        const val capitalLetterACodePoint = 'A'.code
        const val smallLetterACodePoint = 'a'.code
        const val digitZeroCodePoint = '0'.code
    }
}

private val String.codePoints: IntArray get() = codePoints().asSequence().toList().toIntArray()
private val String.codePoint: Int get() = codePoints.single()
private operator fun String.rangeTo(other: String): IntRange = codePoint..other.codePoint
private fun IntRange.toIntArray(): IntArray = toList().toIntArray()
