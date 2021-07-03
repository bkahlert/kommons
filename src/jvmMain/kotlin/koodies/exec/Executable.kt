package koodies.exec

import koodies.tracing.rendering.Renderable
import java.nio.file.Path

/**
 * An executable is something that can be executed
 * using [exec] or any of the various options
 * provided by [Exec].
 */
public interface Executable<out E : Exec> {

    /**
     * Optional name of this executable.
     */
    public val name: CharSequence?

    /**
     * Brief description of that this executable is doing.
     */
    public val summary: Renderable

    /**
     * Creates a [CommandLine] to run this executable.
     *
     * @param environment the environment to be exposed to the [Exec] during execution
     * @param workingDirectory the working directory to be used during execution
     * @param transform applied to each argument before used to form the [CommandLine]
     */
    public fun toCommandLine(
        environment: Map<String, String> = emptyMap(),
        workingDirectory: Path? = null,
        transform: (String) -> String = { it },
    ): CommandLine

    /**
     * Creates a [Exec] to run this executable.
     *
     * @param redirectErrorStream whether standard error is redirected to standard output during execution
     * @param environment the environment to be exposed to the [Exec] during execution
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     */
    public fun toExec(
        redirectErrorStream: Boolean,
        environment: Map<String, String>,
        workingDirectory: Path?,
        execTerminationCallback: ExecTerminationCallback?,
    ): E

    /**
     * Executor that allows to execute this [Executable]
     * in three ways:
     * 1. [Executor.invoke] just executes this [Executable] with no special handling.
     * 2. [Executor.logging] executes the [Executable] and logs the execution with the configured [Executor.loggingOptions].
     * 3. [Executor.processing] executes the [Executable] by passing the [Exec]'s [IO] to the configured [Executor.processor].
     */
    public val exec: Executor<out E> get() = Executor(this)
}
