package com.bkahlert.kommons

/**
 * # Semantic Version
 *
 * Given a version number [major].[minor].[patch], increment the:
 * - [major] version when you make incompatible API changes,
 * - [minor] version when you add functionality in a backwards compatible manner, and
 * - [patch] version when you make backwards compatible bug fixes.
 * @see <a href="https://semver.org">Semantic Versioning 2.0.0</a>
 */
public data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val build: String? = null,
) {

    override fun toString(): String =
        StringBuilder().apply {
            append(major)
            append('.')
            append(minor)
            append('.')
            append(patch)
            preRelease?.also {
                append('-')
                append(it)
            }
            build?.also {
                append('+')
                append(it)
            }
        }.toString()

    public companion object {

        private val regex = Regex("" +
            "(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)" +
            "(?<preRelease>-(?:\\w+\\.)*\\w+)?" +
            "(?<build>\\+(?:\\w+\\.)*\\w+)?" +
            "")

        public fun parse(version: String): SemVer =
            regex.matchEntire(version)?.run {
                SemVer(
                    major = groupValues[1].toInt(),
                    minor = groupValues[2].toInt(),
                    patch = groupValues[3].toInt(),
                    preRelease = groupValues.drop(4).firstOrNull { it.startsWith("-") }?.drop(1),
                    build = groupValues.drop(4).firstOrNull { it.startsWith("+") }?.drop(1),
                )
            } ?: throw IllegalArgumentException("$version is not valid semantic version")
    }
}
