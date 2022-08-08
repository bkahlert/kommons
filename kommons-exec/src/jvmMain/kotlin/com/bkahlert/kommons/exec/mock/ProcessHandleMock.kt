package com.bkahlert.kommons.exec.mock

import com.bkahlert.kommons.Now
import java.lang.ProcessHandle.Info
import java.time.Duration
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import kotlin.time.toJavaDuration

public class ProcessHandleMock(
    private val processMock: JavaProcessMock,
    private val start: Instant = Now,
) : ProcessHandle {
    override fun compareTo(other: ProcessHandle?): Int = other?.compareTo(this) ?: 0
    override fun pid(): Long = processMock.pid()
    override fun parent(): Optional<ProcessHandle> = Optional.empty()
    override fun children(): Stream<ProcessHandle> = Stream.empty()
    override fun descendants(): Stream<ProcessHandle> = Stream.empty()
    override fun info(): Info = object : Info {
        override fun command(): Optional<String> = Optional.empty()
        override fun commandLine(): Optional<String> = Optional.empty()
        override fun arguments(): Optional<Array<String>> = Optional.empty()
        override fun startInstant(): Optional<Instant> = Optional.of(start)
        override fun totalCpuDuration(): Optional<Duration> = Optional.of(kotlin.time.Duration.ZERO.toJavaDuration())
        override fun user(): Optional<String> = Optional.empty()
    }

    override fun onExit(): CompletableFuture<ProcessHandle> = CompletableFuture.completedFuture(this)
    override fun supportsNormalTermination(): Boolean = true
    override fun destroy(): Boolean = true
    override fun destroyForcibly(): Boolean = true
    override fun isAlive(): Boolean = processMock.isAlive
}
