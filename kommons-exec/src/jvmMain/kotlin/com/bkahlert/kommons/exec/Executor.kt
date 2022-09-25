package com.bkahlert.kommons.exec

import com.bkahlert.kommons.logging.SLF4J
import org.slf4j.Logger
import java.nio.file.Path

/** An executor that can run an [Executable] using [invoke] and [logging]. */
public interface Executor<out T> {

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     */
    public operator fun invoke(
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
    ): T = invoke({}, workingDirectory, *environment)

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment].
     *
     * @param customize optional lambda to make adaptions to the underlying [ProcessBuilder]
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     */
    public operator fun invoke(
        customize: ProcessBuilder.() -> Unit,
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
    ): T

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment] by logging all I/O using the given [logger].
     *
     * @param logger the logger to use for logging I/O
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     */
    public fun logging(
        logger: (Process) -> Logger = { it.ioLogger() },
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
    ): T = logging(logger, {}, workingDirectory, *environment)

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment] by logging all I/O using the given [logger].
     *
     * @param logger the logger to use for logging I/O
     * @param customize optional lambda to make adaptions to the underlying [ProcessBuilder]
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     */
    @OverloadResolutionByLambdaReturnType
    public fun logging(
        logger: (Process) -> Logger = { it.ioLogger() },
        customize: ProcessBuilder.() -> Unit,
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
    ): T

    public companion object {
        /** Returns a [Logger] to log the I/O of `this` [Process]. */
        public fun Process.ioLogger(): Logger = SLF4J.getLogger(
            buildString {
                append(this@ioLogger::class.java.name)
                this@ioLogger.pid?.also {
                    append('.')
                    append(it)
                }
            }
        )
    }
}
