package com.bkahlert.kommons.uri

/**
 * Authority component of a [Uri]
 * as specified in [RFC3986 section 3.2](https://www.rfc-editor.org/rfc/rfc3986#section-3.2).
 */
public data class Authority(
    /** [User information subcomponent](https://www.rfc-editor.org/rfc/rfc3986#section-3.2.1) */
    public val userInfo: String?,
    /** [Host subcomponent](https://www.rfc-editor.org/rfc/rfc3986#section-3.2.2) */
    public val host: String,
    /** [Port subcomponent](https://www.rfc-editor.org/rfc/rfc3986#section-3.2.3) */
    public val port: Int?,
) {
    override fun toString(): String = buildString {
        userInfo?.also {
            append(it)
            append("@")
        }
        append(host)
        port?.also {
            append(":")
            append(it)
        }
    }

    public companion object {

        /**
         * Regular expression for parsing the authority component of [Uri] instances
         * as described in [RFC3986 section 3.2](https://www.rfc-editor.org/rfc/rfc3986#section-3.2).
         */
        public val REGEX: Regex = Regex("^(?:(?<userInfo>[^@]+)@)?(?<host>[^:]+)[^?#]*?(?::(?<port>\\d+))?$")

        /**
         * Parses the specified [text] as an [Authority].
         */
        public fun parse(text: String): Authority {
            val groupValues = requireNotNull(REGEX.matchEntire(text)) { "$text is no valid URI authority" }.groupValues
            return Authority(
                userInfo = groupValues[1].takeIf { it.isNotEmpty() },
                host = groupValues[2],
                port = groupValues[3].takeIf { it.isNotEmpty() }?.toInt(),
            )
        }

        /**
         * Parses the specified [text] as an [Authority].
         */
        public fun parseOrNull(text: String): Authority? = kotlin.runCatching { parse(text) }.getOrNull()
    }
}
