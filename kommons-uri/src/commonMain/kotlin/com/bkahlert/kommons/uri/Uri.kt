package com.bkahlert.kommons.uri

import kotlinx.serialization.Serializable

/**
 * URI
 * as specified in [RFC3986](https://www.rfc-editor.org/rfc/rfc3986).
 *
 * Treated as a [CharSequence], this URI yields the string representation
 * as specified in [RFC3986 section 5.3](https://www.rfc-editor.org/rfc/rfc3986#section-5.3).
 *
 * In contrast to [io.ktor.http.Url] all implementations of this interface
 * - implement [CharSequence]
 * - can be (de-)serialized with [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
 * - handle [data URIs](https://www.rfc-editor.org/rfc/rfc2397)
 *
 * URIs—except for data URIs—can be converted
 * to [io.ktor.http.Url] using [io.ktor.http.Url.toUri], and
 * back using [Uri.toUrl].
 */
@Serializable(with = UriSerializer::class)
public sealed interface Uri : CharSequence {
    /** [Schema component](https://www.rfc-editor.org/rfc/rfc3986#section-3.1) */
    public val scheme: String?

    /** [Authority component](https://www.rfc-editor.org/rfc/rfc3986#section-3.2) */
    public val authority: Authority?

    /** [Path component](https://www.rfc-editor.org/rfc/rfc3986#section-3.3) */
    public val path: String

    /** [Query component](https://www.rfc-editor.org/rfc/rfc3986#section-3.4) */
    public val query: String?

    /** [Fragment component](https://www.rfc-editor.org/rfc/rfc3986#section-3.5) */
    public val fragment: String?

    public companion object {

        /**
         * Delimiter of the [path component](https://www.rfc-editor.org/rfc/rfc3986#section-3.3).
         */
        public const val PATH_COMPONENT_DELIMITER: String = "/"

        /**
         * Separator of path segments
         * as specified in [Path component](https://www.rfc-editor.org/rfc/rfc3986#section-3.3).
         */
        public const val PATH_SEGMENTS_SEPARATOR: String = "/"

        /**
         * Delimiter of the [query component](https://www.rfc-editor.org/rfc/rfc3986#section-3.4).
         */
        public const val QUERY_COMPONENT_DELIMITER: String = "?"

        /**
         * Delimiter of the [fragment component](https://www.rfc-editor.org/rfc/rfc3986#section-3.5).
         */
        public const val FRAGMENT_COMPONENT_DELIMITER: String = "#"

        /**
         * Regular expression for parsing [Uri] instances
         * as specified in [RFC3986 Appendix B](https://www.rfc-editor.org/rfc/rfc3986#appendix-B).
         */
        public val REGEX: Regex = Regex("^(?:(?<scheme>[^:/?#]+):)?(?://(?<authority>[^/?#]*))?(?<path>[^?#]*)(?:\\?(?<query>[^#]*))?(?:#(?<fragment>.*))?")

        /**
         * Parses the specified [text] as a [Uri]
         * as specified in [RFC3986 Appendix B](https://www.rfc-editor.org/rfc/rfc3986#appendix-B).
         */
        public fun parse(text: CharSequence): Uri {
            val groupValues = requireNotNull(REGEX.matchEntire(text)) { "$text is no valid URI" }.groupValues
            return when (val scheme = groupValues[1].takeIf { it.isNotEmpty() }) {
                "data" -> DataUri.parse(text)
                else -> GenericUri(
                    scheme = scheme,
                    authority = groupValues[2].takeIf { it.isNotEmpty() }?.let { Authority.parse(it) },
                    path = groupValues[3],
                    query = groupValues[4].takeIf { it.isNotEmpty() },
                    fragment = groupValues[5].takeIf { it.isNotEmpty() },
                )
            }
        }

        /**
         * Parses the specified [text] as a [Uri]
         * as specified in [RFC3986 Appendix B](https://www.rfc-editor.org/rfc/rfc3986#appendix-B).
         */
        public fun parseOrNull(text: CharSequence): Uri? = kotlin.runCatching { parse(text) }.getOrNull()
    }
}

/**
 * Returns a generic [Uri] with the specified
 * [scheme], [authority], [path], [query], and [fragment].
 */
public fun Uri(
    scheme: String?,
    authority: Authority?,
    path: String,
    query: String? = null,
    fragment: String? = null,
): Uri = GenericUri(
    scheme = scheme,
    authority = authority,
    path = path,
    query = query,
    fragment = fragment,
)

/**
 * Generic [Uri] implementation.
 */
@Serializable(with = GenericUriSerializer::class)
internal data class GenericUri(
    override val scheme: String? = null,
    override val authority: Authority? = null,
    override val path: String = "",
    override val query: String? = null,
    override val fragment: String? = null,
) : Uri {
    private val string by lazy {
        buildString {
            scheme?.also {
                append(it)
                append(":")
            }
            authority?.also {
                append("//")
                append(it)
            }
            append(path)
            query?.also {
                append(Uri.QUERY_COMPONENT_DELIMITER)
                append(it)
            }
            fragment?.also {
                append(Uri.FRAGMENT_COMPONENT_DELIMITER)
                append(it)
            }
        }
    }

    override val length: Int get() = string.length
    override fun get(index: Int): Char = string[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.subSequence(startIndex, endIndex)

    /**
     * Returns the string representation of this URI
     * as specified in [RFC3986 section 5.3](https://www.rfc-editor.org/rfc/rfc3986#section-5.3).
     */
    override fun toString(): String = string
}
