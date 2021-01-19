package koodies.text

import java.nio.charset.Charset
import kotlin.text.toByteArray as toKotlinByteArray

fun CharSequence.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray = "$this".toKotlinByteArray(charset)
