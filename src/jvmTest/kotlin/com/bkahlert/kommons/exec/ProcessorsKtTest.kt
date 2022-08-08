package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.collections.synchronizedListOf
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.test.hasElements
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.tracing.TraceId
import com.bkahlert.kommons.tracing.eventText
import com.bkahlert.kommons.tracing.events
import com.bkahlert.kommons.tracing.expectTraced
import com.bkahlert.kommons.tracing.hasSpanAttribute
import com.bkahlert.kommons.tracing.spanName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class ProcessorsKtTest {

    @Nested
    inner class SynchronousProcessing {

        @Nested
        inner class SpanningProcessor {

            @Test
            fun `should trace`() {
                CommandLine("tee", "/dev/fd/2").toExec()
                    .process(
                        ProcessingMode(async = false, "Hello Cat!${LF}".byteInputStream()), Processors.spanningProcessor(
                            ExecAttributes.NAME to "exec-name",
                            ExecAttributes.EXECUTABLE to CommandLine("cat"),
                        )
                    )

                TraceId.current.expectTraced().hasElements(
                    {
                        spanName.isEqualTo("exec-name")
                        hasSpanAttribute(ExecAttributes.NAME, "exec-name")
                        hasSpanAttribute(ExecAttributes.EXECUTABLE, "cat")
                        events.hasElements(
                            { eventText.isEqualTo("Hello Cat!") },
                            { eventText.isEqualTo("Hello Cat!") },
                        )
                    }
                )
            }
        }

        @Test
        fun `should process`() {
            val log = mutableListOf<IO>()
            CommandLine("echo", "Hello World!").toExec()
                .process(ProcessingMode(async = false)) { _: Exec, callback: ((IO) -> Unit) -> ExitState ->
                    callback { log.add(it) }
                }
            expectThat(log)
                .with({ size }) { isEqualTo(1) }
                .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello World!") }
        }

        @Test
        fun `should process input stream`() {
            val log = mutableListOf<IO>()
            CommandLine("cat").toExec()
                .process(ProcessingMode(async = false, "Hello Cat!$LF".byteInputStream())) { _: Exec, callback: ((IO) -> Unit) -> ExitState ->
                    callback { log.add(it) }
                }
            expectThat(log)
                .with({ size }) { isEqualTo(1) }
                .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Cat!") }
        }

        @Test
        fun `should interact`(simpleId: SimpleId) = withTempDir(simpleId) {
            val log = mutableListOf<IO>()
            CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\"").toExec()
                .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                .process(ProcessingMode(async = false)) { _: Exec, callback: ((IO) -> Unit) -> ExitState ->
                    callback { log.add(it) }
                }
            expectThat(log)
                .with({ size }) { isEqualTo(2) }
                .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
        }
    }

    @Nested
    inner class AsynchronousProcessing {

        @Nested
        inner class SpanningProcessor {

            @Test
            fun `should trace`() {
                CommandLine("tee", "/dev/fd/2").toExec()
                    .process(
                        ProcessingMode(async = true, "Hello Cat!$LF".repeat(3).byteInputStream()), Processors.spanningProcessor(
                            ExecAttributes.NAME to "exec-name",
                            ExecAttributes.EXECUTABLE to CommandLine("cat"),
                        )
                    ).waitFor()

                TraceId.current.expectTraced().hasElements(
                    {
                        spanName.isEqualTo("exec-name")
                        hasSpanAttribute(ExecAttributes.NAME, "exec-name")
                        hasSpanAttribute(ExecAttributes.EXECUTABLE, "cat")
                        events.hasElements(
                            { eventText.isEqualTo("Hello Cat!") },
                            { eventText.isEqualTo("Hello Cat!") },
                        )
                    }
                )
            }
        }


        @Nested
        inner class WaitingForTermination {

            @Test
            fun `should process`() {
                val log = synchronizedListOf<IO>()
                CommandLine("echo", "Hello World!").toExec()
                    .process(ProcessingMode(async = true)) { _: Exec, callback: ((IO) -> Unit) -> ExitState ->
                        callback { log.add(it) }
                    }.waitFor()
                expectThat(log)
                    .with({ size }) { isEqualTo(1) }
                    .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello World!") }
            }

            @Test
            fun `should process input stream`() {
                val log = synchronizedListOf<IO>()
                CommandLine("cat").toExec()
                    .process(
                        ProcessingMode(
                            async = true,
                            "Hello Cat!$LF".byteInputStream()
                        )
                    ) { _: Exec, callback: ((IO) -> Unit) -> ExitState ->
                        callback { log.add(it) }
                    }.waitFor()
                expectThat(log)
                    .with({ size }) { isEqualTo(1) }
                    .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Cat!") }
            }

            @Test
            fun `should wait for termination`() {
                val log = synchronizedListOf<IO>()
                CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\"").toExec()
                    .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                    .process(ProcessingMode(async = true)) { _: Exec, callback: ((IO) -> Unit) -> ExitState ->
                        callback { log.add(it) }
                    }.waitFor()
                expectThat(log)
                    .with({ size }) { isEqualTo(2) }
                    .with({ get(0) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                    .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
            }
        }

        @Isolated
        @Nested
        inner class NotWaitingForTermination {

            @Test
            fun `should process`() {
                val timePassed = measureTime {
                    CommandLine("sleep", "10").toExec().process(ProcessingMode(async = true))
                }
                expectThat(timePassed).isLessThan(0.5.seconds)
            }

            @Test
            fun `should process input stream`() {
                val timePassed = measureTime {
                    CommandLine("cat").toExec().process(ProcessingMode(async = true, "Hello Cat!$LF".byteInputStream()))
                }
                expectThat(timePassed).isLessThan(0.5.seconds)
            }

            @Test
            fun `should return immediately`() {
                val timePassed = measureTime {
                    CommandLine("sleep", "10").toExec()
                        .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                        .process(ProcessingMode(async = true))
                }
                expectThat(timePassed).isLessThan(.5.seconds)
            }
        }
    }

    @Nested
    inner class SpanningProcessor {

        @Test
        fun `should process exec and IO`() {
            lateinit var capturedExec: Exec
            lateinit var capturedIO: IO
            val commandLine = CommandLine("echo", "Hello World!")
            commandLine.toExec().process(processor = Processors.spanningProcessor { exec, io ->
                capturedExec = exec
                capturedIO = io
            })
            expectThat(capturedExec.commandLine).isEqualTo(commandLine)
            expectThat(capturedIO).isEqualTo(IO.Output typed "Hello World!")
        }
    }
}

private fun CommandLine.toExec() = toExec(false, emptyMap(), Kommons.InternalTemp, null)
