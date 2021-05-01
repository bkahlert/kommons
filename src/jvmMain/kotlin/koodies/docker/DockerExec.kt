package koodies.docker

import koodies.asString
import koodies.builder.Init
import koodies.docker.DockerContainer.State.Error
import koodies.docker.DockerContainer.State.Existent.Created
import koodies.docker.DockerContainer.State.Existent.Dead
import koodies.docker.DockerContainer.State.Existent.Exited
import koodies.docker.DockerContainer.State.Existent.Paused
import koodies.docker.DockerContainer.State.Existent.Removing
import koodies.docker.DockerContainer.State.Existent.Restarting
import koodies.docker.DockerContainer.State.Existent.Running
import koodies.docker.DockerContainer.State.NotExistent
import koodies.docker.DockerExitStateHandler.Failure
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.DockerRunCommandLine.Options.Companion.OptionsContext
import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.Exec.Companion.createDump
import koodies.exec.ExecFactory
import koodies.exec.ExecTerminationCallback
import koodies.exec.Executor
import koodies.exec.JavaExec
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.Fatal
import koodies.exec.Process.ProcessState
import koodies.exec.Process.ProcessState.Prepared
import koodies.exec.asScriptFileOrNull
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.formattedAs
import koodies.text.withRandomSuffix
import koodies.toBaseName
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * An [Exec] representing a running a [Docker] container.
 */
public open class DockerExec private constructor(
    public val container: DockerContainer,
    private val exec: Exec,
) : Exec by exec {

    override var exitState: ExitState? = null
    override val state: ProcessState
        get() = when (container.state) {
            is NotExistent -> exitState ?: Prepared()
            is Created -> Prepared(format())
            is Restarting, is Running, is Removing, is Paused -> ProcessState.Running(pid, format())
                .also { onExit } // hack to trigger lazy on exit register an exit state storing future
            is Exited, is Dead, is Error -> exitState ?: run {
                val message = "Backed Docker exec no more running but no exit state is known."
                val dump = createDump(message)
                Fatal(IllegalStateException(dump.ansiRemoved), -1, pid, dump, io, message).also { exitState = it }
            }
        }

    override val onExit: CompletableFuture<out ExitState> by lazy {
        exec.onExit.apply {
            thenAccept {
                exitState = kotlin.runCatching { DockerExitStateHandler.handle(it) }.getOrNull()
                    ?.takeIf { it is Failure.ConnectivityProblem }
                    ?: it
            }
        }
    }

    override fun start(): DockerExec = also { exec.start() }

    /**
     * Stops this [Exec] by stopping its container.
     */
    override fun stop(): DockerExec = stop(null)

    /**
     * Kills this [Exec] by killing its container.
     */
    override fun kill(): DockerExec = kill(null)

    /**
     * Stops this process by stopping its container with the optionally specified [timeout] (default: 5 seconds).
     */
    public fun stop(timeout: Duration? = 5.seconds): DockerExec =
        also { container.stop(timeout = timeout) }.also { exec.stop() }

    /**
     * Kills this process by killing its container with the optionally specified [signal] (default: KILL).
     */
    public fun kill(signal: String?): DockerExec =
        also { container.kill(signal = signal) }.also { exec.kill() }

    override fun toString(): String = asString(::container, ::exec)

    public companion object {

        /**
         * Factory for [DockerExec].
         */
        public val NATIVE_DOCKER_EXEC_WRAPPED: ExecFactory<DockerExec> =
            ExecFactory { redirectErrorStream, environment, workingDirectory, commandLine, execTerminationCallback ->
                val exec = JavaExec(redirectErrorStream, environment, workingDirectory, commandLine, null, execTerminationCallback)
                DockerExec(DockerContainer.from(commandLine.dockerFallbackName), exec)
            }
    }
}

/**
 * Executes the [Executor.executable] with the current configuration.
 *
 * @param workingDirectory the working directory to be used during execution
 * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
 */
public fun Executor<DockerExec>.exec(
    workingDirectory: Path? = null,
    execTerminationCallback: ExecTerminationCallback? = null,
): DockerExec = invoke(workingDirectory, execTerminationCallback)

/**
 * Returns an [Executor] that runs `this` executor's [Executor.executable]
 * using the [DockerImage] built by [image]
 * and optional [options] (default: [Options.autoCleanup], [Options.interactive] and [Options.name] derived from [Executor.executable]).
 */
public fun Executor<Exec>.dockerized(options: Options = Options(), image: DockerImageInit): Executor<DockerExec> =
    dockerize(DockerImage(image), options)

/**
 * Returns an [Executor] that runs `this` executor's [Executor.executable]
 * using the [DockerImage] built by [image]
 * and the [Options] built by [options].
 */
public fun Executor<Exec>.dockerized(image: DockerImageInit, options: Init<OptionsContext>): Executor<DockerExec> =
    dockerize(DockerImage(image), Options(options))

/**
 * Returns an [Executor] that runs `this` executor's [Executor.executable]
 * using the specified [image]
 * and optional [options] (default: [Options.autoCleanup], [Options.interactive] and [Options.name] derived from [Executor.executable]).
 */
public fun Executor<Exec>.dockerized(image: DockerImage, options: Options = Options()): Executor<DockerExec> =
    dockerize(image, options)

/**
 * Returns an [Executor] that runs `this` executor's [Executor.executable]
 * using the specified [image]
 * and the [Options] built by [options].
 */
public fun Executor<Exec>.dockerized(image: DockerImage, options: Init<OptionsContext>): Executor<DockerExec> =
    dockerize(image, Options(options))

private fun Executor<Exec>.dockerize(
    image: DockerImage,
    options: Options,
): Executor<DockerExec> {
    val commandLine = executable.toCommandLine()
    val scriptFile = commandLine.asScriptFileOrNull()
    val dockerRunCommandLine: DockerRunCommandLine = when (scriptFile) {
        null -> DockerRunCommandLine(
            image = image,
            options = options
                .withFallbackName(commandLine),
            commandLine = commandLine,
        )
        // TODO require shebang
        else -> DockerRunCommandLine(
            image = image,
            options = options
                .withFallbackName(commandLine)
                .withMappedScriptFile(scriptFile),
            commandLine = CommandLine(""),
        )
    }

    return copy(
        executable = dockerRunCommandLine,
        caption = "Executing dockerized with ${image.formattedAs.input}: ${commandLine.summary}",
    ).with(DockerExec.NATIVE_DOCKER_EXEC_WRAPPED)
}

private fun Options.withMappedScriptFile(scriptFile: Path): Options {
    val mountedScriptLocation = "/${scriptFile.fileName}"
    val scriptMountOption = MountOption(scriptFile, mountedScriptLocation.asContainerPath())
    return copy(
        mounts = mounts + scriptMountOption,
        entryPoint = mountedScriptLocation,
    )
}

private val CommandLine.dockerFallbackName: String
    get() = (this as? DockerRunCommandLine)
        ?.let { it.options.name?.name }
        ?: summary.toBaseName().withRandomSuffix()

private fun Options.withFallbackName(commandLine: CommandLine): Options {
    return withFallbackName(commandLine.dockerFallbackName)
}
