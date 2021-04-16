package koodies.concurrent.process

import koodies.collections.synchronizedListOf
import koodies.concurrent.process
import koodies.concurrent.process.ProcessingMode.Interactivity.Interactive
import koodies.concurrent.process.ProcessingMode.Interactivity.NonInteractive
import koodies.concurrent.process.ProcessingMode.Synchronicity.Async
import koodies.concurrent.process.ProcessingMode.Synchronicity.Sync
import koodies.concurrent.process.UserInput.enter
import koodies.test.UniqueId
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import kotlin.time.measureTime
import kotlin.time.milliseconds

@Execution(SAME_THREAD)
class ProcessorsKtTest {

    @Nested
    inner class SynchronousProcessing {

        @Nested
        inner class NonInteractively {

            @Test
            fun `should process with no input`() {
                val log = mutableListOf<IO>()
                process(CommandLine("echo", "Hello World!"), null).process(ProcessingMode(Sync, NonInteractive(null))) { io -> log.add(io) }
                expectThat(log)
                    .with({ size }) { isEqualTo(3) }
                    .with({ get(0) }) { isA<IO.META.STARTING>() }
                    .with({ get(1) }) { isA<IO.OUT>().toStringIsEqualTo("Hello World!") }
                    .with({ get(2) }) { isA<IO.META.TERMINATED>() }
            }

            @Test
            fun `should process with input`() {
                val log = mutableListOf<IO>()
                process(CommandLine("cat"), null).process(ProcessingMode(Sync, NonInteractive("Hello Cat!$LF".byteInputStream()))) { io -> log.add(io) }
                expectThat(log)
                    .with({ size }) { isEqualTo(3) }
                    .with({ get(0) }) { isA<IO.META.STARTING>() }
                    .with({ get(1) }) { isA<IO.OUT>().toStringIsEqualTo("Hello Cat!") }
                    .with({ get(2) }) { isA<IO.META.TERMINATED>() }
            }
        }


        @Nested
        inner class Interactively {

            @Test
            fun `should process with non-blocking reader`(uniqueId: UniqueId) = withTempDir(uniqueId){
                val log = mutableListOf<IO>()
                process(CommandLine(this, "/bin/sh", "-c", "read input; echo \"\$input you, too\""), null)
                    .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                    .process(ProcessingMode(Sync, Interactive(nonBlocking = true))) { io ->
                        log.add(io)
                    }
                expectThat(log)
                    .with({ size }) { isEqualTo(5) }
                    .with({ get(0) }) { isA<IO.META.STARTING>() }
                    .with({ get(1) }) { isA<IO.META.FILE>() }
                    .with({ get(2) }) { isA<IO.OUT>().toStringIsEqualTo("Hello Back!") }
                    .with({ get(3) }) { isA<IO.OUT>().toStringIsEqualTo(" you, too") }
                    .with({ get(4) }) { isA<IO.META.TERMINATED>() }
            }

            @Test
            fun `should process with blocking reader`(uniqueId: UniqueId)  = withTempDir(uniqueId){
                val log = mutableListOf<IO>()
                process(CommandLine(this, "/bin/sh", "-c", "read input; echo \"\$input you, too\""), null)
                    .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                    .process(ProcessingMode(Sync, Interactive(nonBlocking = false))) { io ->
                        log.add(io)
                    }

                expectThat(log)
                    .with({ size }) { isEqualTo(5) }
                    .with({ get(0) }) { isA<IO.META.STARTING>() }
                    .with({ get(1) }) { isA<IO.META.FILE>() }
                    .with({ get(2) }) { isA<IO.OUT>().toStringIsEqualTo("Hello Back!") }
                    .with({ get(3) }) { isA<IO.OUT>().toStringIsEqualTo(" you, too") }
                    .with({ get(4) }) { isA<IO.META.TERMINATED>() }
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
                    process(CommandLine("echo", "Hello World!"), null)
                        .process(ProcessingMode(Async, NonInteractive(null))) { io -> log.add(io) }
                        .waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(3) }
                        .with({ get(0) }) { isA<IO.META.STARTING>() }
                        .with({ get(1) }) { isA<IO.OUT>().toStringIsEqualTo("Hello World!") }
                        .with({ get(2) }) { isA<IO.META.TERMINATED>() }
                }

                @Test
                fun `should process with input`() {
                    val log = synchronizedListOf<IO>()
                    process(CommandLine("cat"), null)
                        .process(ProcessingMode(Async, NonInteractive("Hello Cat!$LF".byteInputStream()))) { io -> log.add(io) }
                        .waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(3) }
                        .with({ get(0) }) { isA<IO.META.STARTING>() }
                        .with({ get(1) }) { isA<IO.OUT>().toStringIsEqualTo("Hello Cat!") }
                        .with({ get(2) }) { isA<IO.META.TERMINATED>() }
                }
            }

            @Nested
            inner class NotWaitingForTermination {

                @Test
                fun `should process with no input`() {
                    val timePassed = measureTime {
                        process(CommandLine("sleep", "10"), null).process(ProcessingMode(Async, NonInteractive(null))) { }
                    }
                    expectThat(timePassed).isLessThan(250.milliseconds)
                }

                @Test
                fun `should process with input`() {
                    val timePassed = measureTime {
                        process(CommandLine("cat"), null).process(ProcessingMode(Async, NonInteractive("Hello Cat!$LF".byteInputStream()))) { }
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
                    process(CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\""), null)
                        .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                        .process(ProcessingMode(Async, Interactive(nonBlocking = true))) { io ->
                            log.add(io)
                        }.waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(5) }
                        .with({ get(0) }) { isA<IO.META.STARTING>() }
                        .with({ get(1) }) { isA<IO.META.FILE>() }
                        .with({ get(2) }) { isA<IO.OUT>().toStringIsEqualTo("Hello Back!") }
                        .with({ get(3) }) { isA<IO.OUT>().toStringIsEqualTo(" you, too") }
                        .with({ get(4) }) { isA<IO.META.TERMINATED>() }
                }

                @Test
                fun `should process with blocking reader`() {
                    val log = synchronizedListOf<IO>()
                    process(CommandLine("/bin/sh", "-c", "read input; echo \"\$input you, too\""), null)
                        .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                        .process(ProcessingMode(Async, Interactive(nonBlocking = false))) { io ->
                            log.add(io)
                        }.waitFor()
                    expectThat(log)
                        .with({ size }) { isEqualTo(5) }
                        .with({ get(0) }) { isA<IO.META.STARTING>() }
                        .with({ get(1) }) { isA<IO.META.FILE>() }
                        .with({ get(2) }) { isA<IO.OUT>().toStringIsEqualTo("Hello Back!") }
                        .with({ get(3) }) { isA<IO.OUT>().toStringIsEqualTo(" you, too") }
                        .with({ get(4) }) { isA<IO.META.TERMINATED>() }
                }
            }

            @Nested
            inner class NotWaitingForTermination {

                @Test
                fun `should process with non-blocking reader`() {
                    val timePassed = measureTime {
                        process(CommandLine("sleep", "10"), null)
                            .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                            .process(ProcessingMode(Async, Interactive(nonBlocking = true))) { }
                    }
                    expectThat(timePassed).isLessThan(250.milliseconds)
                }

                @Test
                fun `should process with blocking reader`() {
                    val timePassed = measureTime {
                        process(CommandLine("sleep", "10"), null)
                            .also { it.enter("Hello Back!", delay = 0.milliseconds) }
                            .process(ProcessingMode(Async, Interactive(nonBlocking = false))) {}
                    }
                    expectThat(timePassed).isLessThan(250.milliseconds)
                }
            }
        }
    }
}
