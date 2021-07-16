package koodies.exec

import koodies.Koodies
import koodies.exec.ExecTerminationTestCallback.Companion.expectThatProcessAppliesTerminationCallback
import koodies.exec.Process.ExitState
import koodies.exec.Process.State.Exited.Failed
import koodies.exec.Process.State.Exited.Succeeded
import koodies.exec.Process.State.Running
import koodies.exec.ProcessingMode.Interactivity.NonInteractive
import koodies.io.Locations
import koodies.io.path.asPath
import koodies.io.path.pathString
import koodies.io.path.text
import koodies.shell.ShellScript
import koodies.test.DynamicTestsWithSubjectBuilder
import koodies.test.Smoke
import koodies.test.hasElements
import koodies.test.test
import koodies.test.tests
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.mapLines
import koodies.text.lines
import koodies.text.matchesCurlyPattern
import koodies.text.randomString
import koodies.text.toStringMatchesCurlyPattern
import koodies.tracing.TestSpan
import koodies.tracing.TraceId
import koodies.tracing.expectTraced
import koodies.tracing.rendering.Styles.None
import koodies.tracing.rendering.capturing
import koodies.tracing.spanName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class ExecutorTest {

    private val executable = CommandLine("printenv", "TEST_PROP")

    private inline val Executor<Exec>.testProp: Executor<Exec> get() = env("TEST_PROP", "TEST_VALUE")

    @Smoke @Nested
    inner class Executables {

        @Test
        fun `should exec command line`() {
            val exec = CommandLine("echo", "Hello, Command Line!").exec()
            expectThat(exec.io.output.ansiRemoved)
                .isEqualTo("Hello, Command Line!")
        }

        @Test
        fun `should exec shell script`() {
            val exec = ShellScript { "echo 'Hello, Shell Script!' | cat" }.exec()
            expectThat(exec.io.output.ansiRemoved)
                .isEqualTo("Hello, Shell Script!")
        }

        @Test
        fun `should exec using specified environment`() {
            val random = randomString()
            val exec = CommandLine("printenv", "RANDOM_PROP").exec.env("RANDOM_PROP", random)(Locations.Temp)
            expectThat(exec.io.output.ansiRemoved).isEqualTo(random)
        }

        @Test
        fun `should exec using specified working directory`() {
            val exec = CommandLine("pwd").exec(Koodies.ExecTemp)
            val tempPaths = setOf(Koodies.ExecTemp.pathString, Koodies.ExecTemp.toRealPath().pathString)
            expectThat(tempPaths).contains(exec.io.output.ansiRemoved)
        }

        @Test
        fun `should use default span name`() {
            CommandLine("echo", "Hello World!").exec()
            TraceId.current.expectTraced().hasElements(
                { spanName.isEqualTo("koodies.exec") }
            )
        }

        @Test
        fun `should use executable name if specified`() {
            CommandLine("echo", "Hello World!", name = "hello-world").exec()
            TraceId.current.expectTraced().hasElements(
                { spanName.isEqualTo("hello-world") }
            )
        }
    }

    @Nested
    inner class ExecSync {

        @Nested
        inner class ExecOnly {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.testProp.logging()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { succeeds() }
                expectThatProcess { logsSuccessfulIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec(execTerminationCallback = it) }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { fails() }
                expectThatProcess { containsDump() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec(execTerminationCallback = it) }
            }
        }

        @Nested
        inner class ExecLogging {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.testProp.logging()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { succeeds() }
                expectThatProcess { logsSuccessfulIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.testProp.logging(execTerminationCallback = it) }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.logging()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { fails() }
                expectThatProcess { containsDump() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.logging(execTerminationCallback = it) }
            }

            @TestFactory
            fun TestSpan.`should log to specified printer if specified`() = tests {
                capturing { capture ->
                    executable.exec.testProp.logging { it(copy(style = None, printer = capture)) }
                } asserting { logsSuccessfulIO() }
                capturing { capture ->
                    executable.exec.logging { it(copy(style = None, printer = capture)) }
                } asserting { containsDump() }
            }

            @Test
            fun TestSpan.`should log dump on failure`() {
                executable.exec.logging()
                expectThatRendered().containsDump()
            }

            @Test
            fun TestSpan.`should apply custom logging options`() {
                executable.exec.testProp.logging { it(copy(contentFormatter = { "!$it!" })) }
                expectThatRendered().contains("!TEST_VALUE!")
            }

            @Nested
            inner class Header {

                @Test
                fun TestSpan.`should print executable content by default`() {
                    CommandLine("echo", "short").exec.logging(style = None)
                    expectThatRendered().matchesCurlyPattern("""
                        echo short
                        short
                        ✔︎
                    """.trimIndent())
                }

                @Test
                fun TestSpan.`should print name if specified`() {
                    CommandLine("echo", "short", name = "custom name").exec.logging(style = None)
                    expectThatRendered().matchesCurlyPattern("""
                        custom name
                        echo short
                        short
                        ✔︎
                    """.trimIndent())
                }

                @Test
                fun TestSpan.`should print commandline containing URI if too long`() {
                    CommandLine("echo", "a very long argument that leads to a very long command line").exec.logging(style = None)
                    expectThatRendered {
                        matchesCurlyPattern("""
                            file://{}
                            a very long argument that leads to a very long command line
                            ✔︎
                        """.trimIndent())
                        lines().first().asPath().text.contains("""
                            'echo' 'a very long argument that leads to a very long command line'
                        """.trimIndent())
                    }
                }
            }
        }

        @Nested
        inner class ExecProcessing {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.testProp.processing { _, process -> process {} }
            }) {
                expectThatProcess { starts() }
                expectThatProcess { succeeds() }
                expectThatProcess { logsSuccessfulIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.testProp.processing(execTerminationCallback = it) { _, callback ->
                        callback {}
                    }
                }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.processing { _, process -> process {} }
            }) {
                expectThatProcess { starts() }
                expectThatProcess { fails() }
                expectThatProcess { containsDump() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.processing(execTerminationCallback = it) { _, process ->
                        process {}
                    }
                }
            }

            @Test
            fun TestSpan.`should not render`() {
                executable.exec.testProp.processing { _, process -> process {} }
                expectThatRendered().isEmpty()
            }

            @Test
            fun `should process dump on failure`() {
                var dumpProcessed = false
                executable.exec.processing { _, process -> process { io -> if (io is IO.Meta.Dump) dumpProcessed = true } }
                expectThat(dumpProcessed).isTrue()
            }

            @Test
            fun `should process IO`() {
                val processed = mutableListOf<IO>()
                executable.exec.testProp.processing { _, process -> process { io -> processed.add(io) } }
                expectThat(processed).contains(IO.Output typed "TEST_VALUE")
            }
        }
    }

    @Nested
    inner class ExecAsync {

        @Nested
        inner class ExecOnly {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.testProp.async()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.succeeds() }
                expectThatProcess { joined.logsSuccessfulIO() }
//                expectThatProcess { runsAsynchronously() } // often too fast
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.testProp.async(execTerminationCallback = it).apply { waitFor() } }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.async()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.fails() }
                expectThatProcess { joined.containsDump() }
//                expectThatProcess { runsAsynchronously() } // often too fast
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.async(execTerminationCallback = it).apply { waitFor() } }
            }
        }

        @Nested
        inner class ExecLogging {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.async.testProp.logging()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.succeeds() }
                expectThatProcess { joined.logsSuccessfulIO() }
//                expectThatProcess { runsAsynchronously() } // often too fast
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.async.testProp.logging(execTerminationCallback = it).apply { waitFor() }
                }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.async.logging()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.fails() }
                expectThatProcess { joined.containsDump() }
//                expectThatProcess { runsAsynchronously() } // often too fast
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.async.logging(execTerminationCallback = it).apply { waitFor() }
                }
            }

            @TestFactory
            fun TestSpan.`should log to specified printer if specified`() = tests {
                capturing { capture ->
                    executable.exec.testProp.async.logging { it(copy(style = None, printer = capture)) }.apply { waitFor() }
                } asserting { logsSuccessfulIO() }
                capturing { capture ->
                    executable.exec.async.logging { it(copy(style = None, printer = capture)) }.apply { waitFor() }
                } asserting { containsDump() }
            }

            @Test
            fun TestSpan.`should log dump on failure`() {
                executable.exec.async.logging().apply { waitFor() }
                expectThatRendered().containsDump()
            }

            @Test
            fun TestSpan.`should apply custom logging options`() {
                executable.exec.async.testProp.logging { it(copy(contentFormatter = { "!$it!" })) }.apply { waitFor() }
                expectThatRendered().contains("!TEST_VALUE!")
            }

            @Nested
            inner class Header {

                @Test
                fun TestSpan.`should print executable content by default`() {
                    CommandLine("echo", "short").exec.async.logging(style = None).waitFor()
                    expectThatRendered().matchesCurlyPattern("""
                        echo short
                        short
                        ✔︎
                    """.trimIndent())
                }

                @Test
                fun TestSpan.`should print name if specified`() {
                    CommandLine("echo", "short", name = "custom name").exec.async.logging(style = None).waitFor()
                    expectThatRendered().matchesCurlyPattern("""
                        custom name
                        echo short
                        short
                        ✔︎
                    """.trimIndent())
                }

                @Test
                fun TestSpan.`should print commandline containing URI if too long`() {
                    CommandLine("echo", "a very long argument that leads to a very long command line").exec.async.logging(style = None).waitFor()
                    expectThatRendered {
                        matchesCurlyPattern("""
                            file://{}
                            a very long argument that leads to a very long command line
                            ✔︎
                        """.trimIndent())
                        lines().first().asPath().text.contains("""
                            'echo' 'a very long argument that leads to a very long command line'
                        """.trimIndent())
                    }
                }
            }
        }

        @Nested
        inner class ExecProcessing {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.testProp.async.processing { _, process -> process {} }
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.succeeds() }
                expectThatProcess { joined.logsSuccessfulIO() }
//                expectThatProcess { runsAsynchronously() } // often too fast
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.async.testProp.processing(execTerminationCallback = it) { _, process -> process {} }.apply { waitFor() }
                }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.processing { _, process -> process {} }
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.fails() }
                expectThatProcess { joined.containsDump() }
//                expectThatProcess { runsAsynchronously() } // too fast
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.async.processing(execTerminationCallback = it) { _, process -> process {} }.apply { waitFor() }
                }
            }

            @Test
            fun TestSpan.`should not render`() {
                executable.exec.async.testProp.processing { _, process -> process {} }.apply { waitFor() }
                expectThatRendered().isEmpty()
            }

            @Test
            fun `should process dump on failure`() {
                var dumpProcessed = false
                executable.exec.async.processing { _, process -> process { io -> if (io is IO.Meta.Dump) dumpProcessed = true } }.apply { waitFor() }
                expectThat(dumpProcessed).isTrue()
            }

            @Test
            fun `should process IO`() {
                val processed = mutableListOf<IO>()
                executable.exec.async.testProp.processing { _, process -> process { io -> processed.add(io) } }.apply { waitFor() }
                expectThat(processed).contains(IO.Output typed "TEST_VALUE")
            }
        }

        @Smoke @Test
        fun `should apply custom processing options`() {
            val processed = mutableListOf<IO>()
            CommandLine("cat").exec.testProp.mode { async(NonInteractive("Hello Cat!$LF".byteInputStream())) }.processing { _, process ->
                process { io ->
                    processed.add(io)
                }
            }.apply { waitFor() }
            expectThat(processed).contains(IO.Output typed "Hello Cat!")
        }
    }
}

private class ExecTerminationTestCallback : ExecTerminationCallback {
    var called: Boolean = false
    var exception: Throwable? = null
    override fun invoke(exception: Throwable?) {
        called = true
        this.exception = exception
    }

    companion object {
        fun DynamicTestsWithSubjectBuilder<*>.expectThatProcessAppliesTerminationCallback(
            expectedException: Throwable?,
            exec: (ExecTerminationCallback) -> Exec,
        ) {
            val callback = ExecTerminationTestCallback()
            exec(callback)
            expecting { callback.called } that { isTrue() }
            expecting { callback.exception } that { isEqualTo(expectedException) }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun DynamicTestsWithSubjectBuilder<() -> Exec>.expectThatProcess(noinline assertions: Builder<Exec>.() -> Unit) =
    expecting { invoke() }.that(assertions)


inline fun <reified T : Exec> Builder<T>.starts(): Builder<T> =
    assert("has started") {
        kotlin.runCatching { it.pid }.fold({
            if (it != 0L) pass()
            else fail(it, "non-0 PID expected")
        }, {
            fail(cause = it)
        })
    }

inline val <reified T : Exec> Builder<T>.exited: Builder<ExitState> get() = get("exited") { onExit.get() }.isA()
inline fun <reified T : Exec> Builder<T>.logsIO(curlyPattern: String): Builder<String> = exited.io().toStringMatchesCurlyPattern(curlyPattern)

@JvmName("logsIOString")
fun Builder<String>.logsIO(ignorePrefix: String, curlyPattern: String): Builder<String> =
    get { mapLines { it.removePrefix(ignorePrefix) } }.toStringMatchesCurlyPattern(curlyPattern)


inline fun <reified T : Exec> Builder<T>.succeeds(): Builder<Succeeded> = exited.isA()
inline fun <reified T : Exec> Builder<T>.logsSuccessfulIO(): Builder<String> = logsIO(successfulIO)

@JvmName("logsSuccessfulIOString")
private fun Builder<String>.logsSuccessfulIO(ignorePrefix: String = "· "): Builder<String> =
    logsIO(ignorePrefix, "{{}}$LF$successfulIO$LF{{}}")

val successfulIO = """
    TEST_VALUE
""".trimIndent()

inline fun <reified T : Exec> Builder<T>.fails(): Builder<Failed> =
    exited.isA<Failed>().assert("unsuccessfully with non-zero exit code") {
        val actual = it.exitCode
        when (actual != 0) {
            true -> pass()
            else -> fail("completed successfully")
        }
    }

inline fun <reified T : Exec> Builder<T>.containsDump() = exited.io().containsDump()

inline val Builder<out Process>.state get() = get("exit state") { state }
inline fun <reified T : Exec> Builder<T>.runsSynchronously(): Builder<ExitState> = state.isA()
inline fun <reified T : Exec> Builder<T>.runsAsynchronously(): Builder<Running> = state.isA()


inline val <reified T : Process> Builder<T>.joined: Builder<T>
    get() = get("joined using waitFor") { also { waitFor() } }
