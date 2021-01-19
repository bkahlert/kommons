@file:Suppress("ClassName")

package koodies.text

/**
 * Returns true if this char sequence matches the given SLF4J / Logback style [curlyPattern], like `I {} you have to {}`.
 *
 * @sample Samples.singleLineMatches
 * @sample Samples.multiLineMatches
 */
fun <T : CharSequence> T.matchesCurlyPattern(curlyPattern: String, placeholder: String = "{}"): Boolean =
    this.matches(Regex(curlyPattern.split(placeholder).joinToString(".*") { Regex.escape(it) }))


private object Samples {
    val singleLineMatches = "this is a test".matchesCurlyPattern("this is a {}")
    val multiLineMatches =
        """
        Executing [sh, -c, >&1 echo "test output"
        >&2 echo "test error"] in /Users/bkahlert/Development/com.imgcstmzr.
        Started Process[pid=72692, exitValue=0]
        Process[pid=72692, exitValue=0] stopped with exit code 0
        """.trimIndent()
            .matchesCurlyPattern(
                """
                Executing [sh, -c, >&1 echo "test output"
                >&2 echo "test error"] in {}
                Started Process[pid={}, exitValue={}]
                Process[pid={}, exitValue={}] stopped with exit code {}
                """.trimIndent())
}
