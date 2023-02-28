package com.bkahlert.kommons.uri

import com.bkahlert.kommons.uri.DataUri.Companion.DEFAULT_MEDIA_TYPE
import io.ktor.http.ContentType
import io.ktor.http.charset
import io.ktor.http.decodeURLPart
import io.ktor.http.quote
import io.ktor.http.withCharset
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.Serializable

/**
 * Data URI
 * as specified in [RFC2397](https://www.rfc-editor.org/rfc/rfc2397).
 *
 * Treated as a [CharSequence], this URI yields the string representation
 * as specified in [RFC2397 section 2](https://www.rfc-editor.org/rfc/rfc2397#section-2).
 */
@Serializable(with = DataUriSerializer::class)
public data class DataUri(
    /** The internet media type of [data]. Implicitly defaults to [DEFAULT_MEDIA_TYPE]. */
    public val mediaType: ContentType?,
    /** The decoded data. */
    public val data: ByteArray,
) : Uri {

    /** The [mediaType] as a string. */
    public val mediaTypeString: String? by lazy {
        if (mediaType == null) return@lazy null
        buildString {
            append(mediaType.contentType)
            append("/")
            append(mediaType.contentSubtype)
            mediaType.parameters.forEach { (name, value, escapeValue) ->
                append(";")
                append(name)
                append("=")
                append(if (escapeValue) value.quote() else value)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DataUri

        if (mediaType != other.mediaType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mediaType?.hashCode() ?: 0
        result = 31 * result + data.contentHashCode()
        return result
    }

    private val uri by lazy {
        GenericUri(scheme = SCHEMA, path = buildString {
            mediaTypeString?.also { append(it) }
            append(";base64")
            append(",")
            append(data.encodeBase64().dropLastWhile { it == '=' })
        })
    }

    override val scheme: String? get() = uri.scheme
    override val authority: Authority? get() = uri.authority
    override val path: String get() = uri.path
    override val query: String? get() = uri.query
    override val fragment: String? get() = uri.fragment
    override val length: Int get() = uri.length
    override fun get(index: Int): Char = uri[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = uri.subSequence(startIndex, endIndex)

    /**
     * Returns the string representation of this URI
     * as specified in [RFC2397 section 2](https://www.rfc-editor.org/rfc/rfc2397#section-2).
     */
    override fun toString(): String = uri.toString()

    public companion object {

        /**
         * Default media type for [DataUri] instances
         * as specified in [RFC2397 section 2](https://www.rfc-editor.org/rfc/rfc2397#section-2).
         */
        public val DEFAULT_MEDIA_TYPE: ContentType = ContentType.Text.Plain.withCharset(kotlin.runCatching {
            Charset.forName("US-ASCII") // Surprisingly, not supported in Ktor/JS ...
        }.getOrDefault(Charsets.UTF_8))

        /**
         * URL schema
         * as specified in [RFC2397 section 2](https://www.rfc-editor.org/rfc/rfc2397#section-3).
         */
        public val SCHEMA: String = "data"

        /**
         * Regular expression for parsing [DataUri] instances
         * as described in [RFC2397 section 3](https://www.rfc-editor.org/rfc/rfc2397#section-3).
         */
        public val REGEX: Regex = Regex("^$SCHEMA:(?<mediaType>[-\\w]+/[-+\\w.]+(?:;\\w+=[-\\w]+)*)?(?<base64>;base64)?,(?<data>.*)")

        /**
         * Parses the specified [text] as a [DataUri]
         * as specified in [RFC3986 Appendix B](https://www.rfc-editor.org/rfc/rfc3986#appendix-B).
         */
        public fun parse(text: CharSequence): DataUri {
            val groupValues = requireNotNull(REGEX.matchEntire(text)) { "$text is no valid data URI" }.groupValues
            val mediaType = groupValues[1].takeIf { it.isNotEmpty() }?.let { ContentType.parse(it) }
            val charset = mediaType?.charset() ?: Charsets.UTF_8
            return DataUri(
                mediaType = mediaType,
                data = when (groupValues[2]) {
                    "" -> groupValues[3].decodeURLPart(charset = charset).toByteArray(charset)
                    else -> groupValues[3].decodeBase64Url()
                },
            )
        }

        /**
         * Parses the specified [text] as a [Uri]
         * as specified in [RFC3986 Appendix B](https://www.rfc-editor.org/rfc/rfc3986#appendix-B).
         */
        public fun parseOrNull(text: CharSequence): DataUri? = kotlin.runCatching { parse(text) }.getOrNull()

        private fun String.decodeBase64Url(): ByteArray = replace("%2B", "+")
            .replace("%2F", "/")
            .replace("%3D", "=")
            .decodeBase64Bytes()
    }
}
