package koodies.exec

import koodies.collections.synchronizedListOf
import koodies.exec.ProcessingMode.Interactivity.Interactive
import koodies.exec.ProcessingMode.Interactivity.NonInteractive
import koodies.exec.ProcessingMode.Synchronicity.Async
import koodies.exec.ProcessingMode.Synchronicity.Sync
import koodies.io.Koodies
import koodies.junit.UniqueId
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import koodies.text.matchesCurlyPattern
import koodies.time.seconds
import koodies.tracing.KoodiesAttributes
import koodies.tracing.RenderingAttributes
import koodies.tracing.TestSpan
import koodies.tracing.TraceId
import koodies.tracing.eventText
import koodies.tracing.events
import koodies.tracing.expectTraced
import koodies.tracing.rendering.Renderable
import koodies.tracing.spanAttributes
import koodies.tracing.spanName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import kotlin.time.Duration
import kotlin.time.measureTime

class ProcessorsKtTest {

    @Nested
    inner class SynchronousProcessing {

        @Test
        fun TestSpan.`should trace`() {
            CommandLine("cat").toExec().process(ProcessingMode(Sync, NonInteractive("Hello Cat!${LF}".byteInputStream())),
                TracingOptions(
                    attributes = mapOf(
                        KoodiesAttributes.EXEC_NAME.key to "exec-name",
                        KoodiesAttributes.EXEC_EXECUTABLE.key to CommandLine("cat"),
                        RenderingAttributes.name(Renderable.of("span-name")),
                    ),
                    renderer = { it(this) }
                )) { }
            expectThatRendered().matchesCurlyPattern("""
                    ╭──╴span-name
                    │
                    │   Hello Cat!            
                    │
                    ╰──╴✔︎
                """.trimIndent())
            TraceId.current.expectTraced().hasSize(1) and {
                with(get(0)) {
                    spanName.isEqualTo("koodies.exec")
                    spanAttributes.get { execName }.isEqualTo("exec-name")
                    spanAttributes.get { execExecutable }.isEqualTo("'cat'")
                    events.hasSize(1) and { get(0).eventText.isEqualTo("Hello Cat!") }
                }
            }
        }

        @Nested
        inner class NonInteractively {

            @Test
            fun `should process with no input`() {
                val log = mutableListOf<IO>()
                CommandLine("echo", "Hello World!").toExec().process(ProcessingMode(Sync, NonInteractive(null))) { io ->
                    log.add(io)
                }
                expectThat(log)
                    .with({ size }) { isEqualTo(1) }
                    .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello World!") }
            }

            @Test
            fun `should process with input`() {
                val log = mutableListOf<IO>()
                CommandLine("cat").toExec().process(ProcessingMode(Sync, NonInteractive("Hello Cat!$LF".byteInputStream()))) { io ->
                    log.add(io)
                }
                expectThat(log)
                    .with({ size }) { isEqualTo(1) }
                    .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Cat!") }
            }
        }


        @Nested
        inner class Interactively {

            @Test
            fun `should process with non-blocking reader`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val log = mutableListOf<IO>()
                CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\"").toExec()
                    .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                    .process(ProcessingMode(Sync, Interactive(nonBlocking = true))) { io ->
                        log.add(io)
                    }
                expectThat(log)
                    .with({ size }) { isEqualTo(2) }
                    .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                    .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
            }

            @Test
            fun `should process with blocking reader`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val log = mutableListOf<IO>()
                CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\"").toExec()
                    .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                    .process(ProcessingMode(Sync, Interactive(nonBlocking = false))) { io ->
                        log.add(io)
                    }

                expectThat(log)
                    .with({ size }) { isEqualTo(2) }
                    .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                    .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
            }
        }
    }

    @Nested
    inner class AsynchronousProcessing {

        @Test
        fun TestSpan.`should trace`() {
            CommandLine("cat").toExec().process(ProcessingMode(Async, NonInteractive("Hello Cat!${LF}".byteInputStream())),
                TracingOptions(
                    attributes = mapOf(
                        KoodiesAttributes.EXEC_NAME.key to "exec-name",
                        KoodiesAttributes.EXEC_EXECUTABLE.key to CommandLine("cat"),
                        RenderingAttributes.name(Renderable.of("span-name")),
                    ),
                    renderer = { it(this) }
                )) { }.waitFor()
            expectThatRendered().matchesCurlyPattern("""
                    ╭──╴span-name
                    │
                    │   Hello Cat!            
                    │
                    ╰──╴✔︎
                """.trimIndent())
            TraceId.current.expectTraced().hasSize(1) and {
                with(get(0)) {
                    spanName.isEqualTo("koodies.exec")
                    spanAttributes.get { execName }.isEqualTo("exec-name")
                    spanAttributes.get { execExecutable }.isEqualTo("'cat'")
                    events.hasSize(1) and { get(0).eventText.isEqualTo("Hello Cat!") }
                }
            }
        }

        @Nested
        inner class NonInteractively {

            @Nested
            inner class WaitingForTermination {

                @Test
                fun `should process with no input`() {
                    val log = synchronizedListOf<IO>()
                    CommandLine("echo", "Hello World!").toExec()
                        .process(ProcessingMode(Async, NonInteractive(null))) { io -> log.add(io) }
                        .waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(1) }
                        .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello World!") }
                }

                @Test
                fun `should process with input`() {
                    val log = synchronizedListOf<IO>()
                    CommandLine("cat").toExec()
                        .process(ProcessingMode(Async, NonInteractive("Hello Cat!$LF".byteInputStream()))) { io -> log.add(io) }
                        .waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(1) }
                        .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Cat!") }
                }
            }

            @Nested
            inner class NotWaitingForTermination {

                @Test
                fun `should process with no input`() {
                    val timePassed = measureTime {
                        CommandLine("sleep", "10").toExec().process(ProcessingMode(Async, NonInteractive(null))) { }
                    }
                    expectThat(timePassed).isLessThan(0.5.seconds)
                }

                @Test
                fun `should process with input`() {
                    val timePassed = measureTime {
                        CommandLine("cat").toExec().process(ProcessingMode(Async,
                            NonInteractive("Hello Cat!$LF".byteInputStream()))) { }
                    }
                    expectThat(timePassed).isLessThan(0.5.seconds)
                }
            }
        }


        @Nested
        inner class Interactively {

            @Nested
            inner class WaitingForTermination {

                @Test
                fun `should process with non-blocking reader`() {
                    val log = synchronizedListOf<IO>()
                    CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\"").toExec()
                        .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                        .process(ProcessingMode(Async, Interactive(nonBlocking = true))) { io ->
                            log.add(io)
                        }.waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(2) }
                        .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                        .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                }

                @Test
                fun `should process with blocking reader`() {
                    val log = synchronizedListOf<IO>()
                    CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\"").toExec()
                        .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                        .process(ProcessingMode(Async, Interactive(nonBlocking = false))) { io ->
                            log.add(io)
                        }.waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(2) }
                        .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                        .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                }
            }

            @Nested
            inner class NotWaitingForTermination {

                @Test
                fun `should process with non-blocking reader`() {
                    val timePassed = measureTime {
                        CommandLine("sleep", "10").toExec()
                            .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                            .process(ProcessingMode(Async, Interactive(nonBlocking = true))) { }
                    }
                    expectThat(timePassed).isLessThan(.5.seconds)
                }

                @Test
                fun `should process with blocking reader`() {
                    val timePassed = measureTime {
                        CommandLine("sleep", "10").toExec()
                            .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                            .process(ProcessingMode(Async, Interactive(nonBlocking = false))) {}
                    }
                    expectThat(timePassed).isLessThan(.5.seconds)
                }
            }
        }
    }
}

private fun CommandLine.toExec() = toExec(false, emptyMap(), Koodies.InternalTemp, null)
