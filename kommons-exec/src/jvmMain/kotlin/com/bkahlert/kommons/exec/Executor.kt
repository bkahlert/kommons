package com.bkahlert.kommons.exec

import com.bkahlert.kommons.logging.SLF4J
import org.slf4j.Logger
import java.nio.file.Path

/** An executor that can run an [Executable] using [invoke] and [logging]. */
public interface Executor<out T : Process.State> {

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     * @param customize optional lambda to make adaptions to the underlying [ProcessBuilder]
     */
    public operator fun invoke(
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
        customize: ProcessBuilder.() -> Unit = {},
    ): T

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment] by logging all I/O using the given [logger].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     * @param logger the logger to use for logging I/O
     * @param customize optional lambda to make adaptions to the underlying [ProcessBuilder]
     */
    public fun logging(
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
        logger: Logger,
        customize: ProcessBuilder.() -> Unit = {},
    ): T = logging(
        workingDirectory = workingDirectory,
        environment = environment,
        logger = { logger },
        customize = customize,
    )

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment] by logging all I/O using the given [logger].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     * @param logger optional [Logger] factory to use for creating a I/O logger (default: [Executor] logger)
     * @param customize optional lambda to make adaptions to the underlying [ProcessBuilder]
     */
    public fun logging(
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
        logger: (Process) -> Logger = { process ->
            SLF4J.getLogger(
                buildString {
                    append(this@Executor::class.java.name)
                    process.pid?.also {
                        append('.')
                        append(it)
                    }
                }
            )
        },
        customize: ProcessBuilder.() -> Unit = {},
    ): T
}
