package koodies.exec

import koodies.collections.synchronizedListOf
import koodies.exec.ProcessingMode.Interactivity.Interactive
import koodies.exec.ProcessingMode.Interactivity.NonInteractive
import koodies.exec.ProcessingMode.Synchronicity.Async
import koodies.exec.ProcessingMode.Synchronicity.Sync
import koodies.io.path.Locations.Temp
import koodies.test.UniqueId
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import koodies.time.seconds
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import kotlin.time.Duration
import kotlin.time.measureTime

class ProcessorsKtTest {

    @Nested
    inner class SynchronousProcessing {

        @Nested
        inner class NonInteractively {

            @Test
            fun `should process with no input`() {
                val log = mutableListOf<IO>()
                CommandLine("echo", "Hello World!").toExec().process(ProcessingMode(Sync, NonInteractive(null))) { io ->
                    log.add(io)
                }
                expectThat(log)
                    .with({ size }) { isEqualTo(3) }
                    .with({ get(0) }) { isA<IO.Meta.Starting>() }
                    .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo("Hello World!") }
                    .with({ get(2) }) { isA<IO.Meta.Terminated>() }
            }

            @Test
            fun `should process with input`() {
                val log = mutableListOf<IO>()
                CommandLine("cat").toExec().process(ProcessingMode(Sync, NonInteractive("Hello Cat!$LF".byteInputStream()))) { io ->
                    log.add(io)
                }
                expectThat(log)
                    .with({ size }) { isEqualTo(3) }
                    .with({ get(0) }) { isA<IO.Meta.Starting>() }
                    .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo("Hello Cat!") }
                    .with({ get(2) }) { isA<IO.Meta.Terminated>() }
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
                    .with({ size }) { isEqualTo(4) }
                    .with({ get(0) }) { isA<IO.Meta.Starting>() }
                    .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                    .with({ get(2) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                    .with({ get(3) }) { isA<IO.Meta.Terminated>() }
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
                    .with({ size }) { isEqualTo(4) }
                    .with({ get(0) }) { isA<IO.Meta.Starting>() }
                    .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                    .with({ get(2) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                    .with({ get(3) }) { isA<IO.Meta.Terminated>() }
            }
        }
    }

    @Nested
    inner class AsynchronousProcessing {

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
                        .with({ size }) { isEqualTo(3) }
                        .with({ get(0) }) { isA<IO.Meta.Starting>() }
                        .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo("Hello World!") }
                        .with({ get(2) }) { isA<IO.Meta.Terminated>() }
                }

                @Test
                fun `should process with input`() {
                    val log = synchronizedListOf<IO>()
                    CommandLine("cat").toExec()
                        .process(ProcessingMode(Async, NonInteractive("Hello Cat!$LF".byteInputStream()))) { io -> log.add(io) }
                        .waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(3) }
                        .with({ get(0) }) { isA<IO.Meta.Starting>() }
                        .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo("Hello Cat!") }
                        .with({ get(2) }) { isA<IO.Meta.Terminated>() }
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
                        .with({ size }) { isEqualTo(4) }
                        .with({ get(0) }) { isA<IO.Meta.Starting>() }
                        .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                        .with({ get(2) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                        .with({ get(3) }) { isA<IO.Meta.Terminated>() }
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
                        .with({ size }) { isEqualTo(4) }
                        .with({ get(0) }) { isA<IO.Meta.Starting>() }
                        .with({ get(1) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                        .with({ get(2) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                        .with({ get(3) }) { isA<IO.Meta.Terminated>() }
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
                    expectThat(timePassed).isLessThan(0.25.seconds)
                }

                @Test
                fun `should process with blocking reader`() {
                    val timePassed = measureTime {
                        CommandLine("sleep", "10").toExec()
                            .also { it.enter("Hello Back!", delay = Duration.ZERO) }
                            .process(ProcessingMode(Async, Interactive(nonBlocking = false))) {}
                    }
                    expectThat(timePassed).isLessThan(0.25.seconds)
                }
            }
        }
    }
}

private fun CommandLine.toExec() = toExec(false, emptyMap(), Temp, null)
