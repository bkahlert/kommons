package koodies.concurrent.process

import koodies.collections.synchronizedListOf
import koodies.concurrent.process.ProcessingMode.Interactivity.Interactive
import koodies.concurrent.process.ProcessingMode.Interactivity.NonInteractive
import koodies.concurrent.process.ProcessingMode.Synchronicity.Async
import koodies.concurrent.process.ProcessingMode.Synchronicity.Sync
import koodies.concurrent.process.UserInput.enter
import koodies.exec.CommandLine
import koodies.exec.JavaExec
import koodies.io.path.Locations.Temp
import koodies.test.UniqueId
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import kotlin.time.measureTime
import kotlin.time.milliseconds

class ProcessorsKtTest {

    @Nested
    inner class SynchronousProcessing {

        @Nested
        inner class NonInteractively {

            @Test
            fun `should process with no input`() {
                val log = mutableListOf<IO>()
                JavaExec(false, emptyMap(), Temp, CommandLine("echo", "Hello World!")).process(ProcessingMode(Sync, NonInteractive(null))) { io ->
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
                JavaExec(false, emptyMap(), Temp, CommandLine("cat")).process(ProcessingMode(Sync, NonInteractive("Hello Cat!$LF".byteInputStream()))) { io ->
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
                JavaExec(false, emptyMap(), this, CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\""))
                    .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                    .process(ProcessingMode(Sync, Interactive(nonBlocking = true))) { io ->
                        log.add(io)
                    }
                expectThat(log)
                    .with({ size }) { isEqualTo(5) }
                    .with({ get(0) }) { isA<IO.Meta.Starting>() }
                    .with({ get(1) }) { isA<IO.Meta.File>() }
                    .with({ get(2) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                    .with({ get(3) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                    .with({ get(4) }) { isA<IO.Meta.Terminated>() }
            }

            @Test
            fun `should process with blocking reader`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val log = mutableListOf<IO>()
                JavaExec(false, emptyMap(), this, CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\""))
                    .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                    .process(ProcessingMode(Sync, Interactive(nonBlocking = false))) { io ->
                        log.add(io)
                    }

                expectThat(log)
                    .with({ size }) { isEqualTo(5) }
                    .with({ get(0) }) { isA<IO.Meta.Starting>() }
                    .with({ get(1) }) { isA<IO.Meta.File>() }
                    .with({ get(2) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                    .with({ get(3) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                    .with({ get(4) }) { isA<IO.Meta.Terminated>() }
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
                    JavaExec(false, emptyMap(), null, CommandLine("echo", "Hello World!"))
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
                    JavaExec(false, emptyMap(), null, CommandLine("cat"))
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
                        JavaExec(false, emptyMap(), null, CommandLine("sleep", "10")).process(ProcessingMode(Async, NonInteractive(null))) { }
                    }
                    expectThat(timePassed).isLessThan(250.milliseconds)
                }

                @Test
                fun `should process with input`() {
                    val timePassed = measureTime {
                        JavaExec(false, emptyMap(), null, CommandLine("cat")).process(ProcessingMode(Async,
                            NonInteractive("Hello Cat!$LF".byteInputStream()))) { }
                    }
                    expectThat(timePassed).isLessThan(250.milliseconds)
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
                    JavaExec(false, emptyMap(), null, CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\""))
                        .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                        .process(ProcessingMode(Async, Interactive(nonBlocking = true))) { io ->
                            log.add(io)
                        }.waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(5) }
                        .with({ get(0) }) { isA<IO.Meta.Starting>() }
                        .with({ get(1) }) { isA<IO.Meta.File>() }
                        .with({ get(2) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                        .with({ get(3) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                        .with({ get(4) }) { isA<IO.Meta.Terminated>() }
                }

                @Test
                fun `should process with blocking reader`() {
                    val log = synchronizedListOf<IO>()
                    JavaExec(false, emptyMap(), null, CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\""))
                        .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                        .process(ProcessingMode(Async, Interactive(nonBlocking = false))) { io ->
                            log.add(io)
                        }.waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(5) }
                        .with({ get(0) }) { isA<IO.Meta.Starting>() }
                        .with({ get(1) }) { isA<IO.Meta.File>() }
                        .with({ get(2) }) { isA<IO.Output>().toStringIsEqualTo("Hello Back!") }
                        .with({ get(3) }) { isA<IO.Output>().toStringIsEqualTo(" you, too") }
                        .with({ get(4) }) { isA<IO.Meta.Terminated>() }
                }
            }

            @Nested
            inner class NotWaitingForTermination {

                @Test
                fun `should process with non-blocking reader`() {
                    val timePassed = measureTime {
                        JavaExec(false, emptyMap(), null, CommandLine("sleep", "10"))
                            .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                            .process(ProcessingMode(Async, Interactive(nonBlocking = true))) { }
                    }
                    expectThat(timePassed).isLessThan(250.milliseconds)
                }

                @Test
                fun `should process with blocking reader`() {
                    val timePassed = measureTime {
                        JavaExec(false, emptyMap(), null, CommandLine("sleep", "10"))
                            .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                            .process(ProcessingMode(Async, Interactive(nonBlocking = false))) {}
                    }
                    expectThat(timePassed).isLessThan(250.milliseconds)
                }
            }
        }
    }
}
