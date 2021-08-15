package com.bkahlert.kommons.docker

import com.bkahlert.kommons.builder.buildList
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.ConnectivityProblem
import com.bkahlert.kommons.docker.DockerRunCommandLine.Options
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.Exec
import com.bkahlert.kommons.exec.Exec.Companion.fallbackExitStateHandler
import com.bkahlert.kommons.exec.ExecTerminationCallback
import com.bkahlert.kommons.exec.Executable
import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.resolveBetweenFileSystems
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.text.splitAndMap
import com.bkahlert.kommons.text.takeUnlessBlank
import com.bkahlert.kommons.text.withRandomSuffix
import com.bkahlert.kommons.toBaseName
import org.codehaus.plexus.util.cli.shell.BourneShell
import java.nio.file.Path

/**
 * Integration of [Docker] `run` that runs the specified [executable] using the specified [image] using the specified [options].
 */
public class DockerRunCommandLine(

    /**
     * The image used to run the [DockerRunCommandLine].
     *
     * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#extended-description"
     * >Docker run: Extended Description</a>
     */
    public val image: DockerImage,

    /**
     * The options used to run the [DockerRunCommandLine].
     *
     * @see Options
     * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#options"
     * >Docker run: Options</a>
     */
    options: Options = Options(),

    private val executable: Executable<Exec> = CommandLine(""),
) : Executable<DockerExec> {

    public constructor(image: DockerImage, executable: Executable<Exec>) : this(image, Options(), executable)

    override val name: CharSequence? = executable.name?.let { "$it 🐳 ${image.toString(includeSpecifier = false)}" }

    private val fallbackName = executable.content.toBaseName().withRandomSuffix()
    public val options: Options = options.withFallbackName(fallbackName).withFixedEntryPoint(executable)

    override val content: CharSequence = toCommandLine().content

    override fun toCommandLine(
        environment: Map<String, String>,
        workingDirectory: Path?,
        transform: (String) -> String,
    ): CommandLine =
        CommandLine("docker", buildList {
            add("run")

            environment.forEach { (key, value) ->
                add("--env")
                add("$key=$value")
            }

            val wdFixedOptions = options
            addAll(wdFixedOptions)

            add(image.toString())

            val runCommandLine = if (workingDirectory != null) {
                executable.toCommandLine(environment, workingDirectory) { arg ->
                    wdFixedOptions.remapPathsInArguments(workingDirectory,
                        arg,
                        executable is ShellScript)
                }
            } else {
                executable.toCommandLine(environment, workingDirectory)
            }

            if (executable is ShellScript) {
                addAll(DEFAULT_SHELL_ARGUMENTS)
                add(runCommandLine.arguments.last())
            } else {
                val command = runCommandLine.command.takeUnlessBlank()
                if (command != null && options.entryPoint == null) add(command)
                addAll(runCommandLine.arguments)
            }
        }.map(transform), name = name)

    override fun toExec(
        redirectErrorStream: Boolean,
        environment: Map<String, String>,
        workingDirectory: Path?,
        execTerminationCallback: ExecTerminationCallback?,
    ): DockerExec {
        val commandLine = toCommandLine(environment, workingDirectory).warnOnConnectivityProblem()
        val container = options.name ?: error("Missing name in $options; at least would have expected $fallbackName")
        return DockerExec(container, commandLine.toExec(redirectErrorStream, environment, workingDirectory, execTerminationCallback))
    }

    override fun toString(): String = toCommandLine().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DockerRunCommandLine

        if (executable != other.executable) return false

        return true
    }

    override fun hashCode(): Int = executable.hashCode()

    /**
     * The options used to run the [DockerRunCommandLine].
     *
     * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#options"
     * >Docker run: Options</a>
     */
    public data class Options(

        /**
         * Whether to run the [DockerRunCommandLine] in detached or in
         * foreground mode.
         *
         * @see <a href="https://docs.docker.com/engine/reference/run/#detached--d"
         * >Docker run reference: Detached (-d)</a>
         */
        val detached: Boolean = false,

        /**
         * If set, the entry point the Dockerfile image default should be overridden
         * with.
         *
         * @see <a href="https://docs.docker.com/engine/reference/run/#entrypoint-default-command-to-execute-at-runtime"
         * >Docker run reference: ENTRYPOINT (default command to execute at runtime)</a>
         */
        val entryPoint: String? = null,

        /**
         * The name the container created by the [DockerRunCommandLine] should
         * be identified by.
         *
         * @see <a href="https://docs.docker.com/engine/reference/run/#name---name"
         * >Docker run reference: Name (--name)</a>
         */
        val name: DockerContainer? = null,

        /**
         * Publish a container's port or a range of ports to the host
         * format: `ip:hostPort:containerPort` or `ip::containerPort` or
         * `hostPort:containerPort` or `containerPort`
         *
         * Both hostPort and containerPort can be specified as a
         * range of ports. When specifying ranges for both, the
         * number of container ports in the range must match the
         * number of host ports in the range, for example:
         * `1234-1236:1234-1236/tcp`
         *
         * When specifying a range for hostPort only, the
         * containerPort must not be a range.  In this case the
         * container port is published somewhere within the
         * specified hostPort range. (e.g., `1234-1236:1234/tcp`)
         *
         * @see <a href="https://docs.docker.com/engine/reference/run/#expose-incoming-ports"
         * >Docker run reference: EXPOSE (incoming ports)</a>
         */
        val publish: List<String> = emptyList(),

        /**
         * Whether to run the [DockerRunCommandLine] with extended privileges.
         *
         * By default, Docker containers are “unprivileged” and cannot, for example,
         * run a Docker daemon inside a Docker container.
         *
         * @see <a href="https://docs.docker.com/engine/reference/run/#runtime-privilege-and-linux-capabilities"
         * >Docker run reference: Runtime privilege and Linux capabilities</a>
         */
        val privileged: Boolean = false,

        /**
         * The working directory to run the [DockerRunCommandLine] with.
         *
         * The default working directory for running binaries within a container is the root directory (/).
         *
         * @see <a href="https://docs.docker.com/engine/reference/run/#workdir"
         * >Docker run reference: WORKDIR</a>
         */
        val workingDirectory: ContainerPath? = null,

        /**
         * Whether to automatically remove the container when it exits.
         *
         * By default a container’s file system persists even after the container exits.
         *
         * @see <a href="https://docs.docker.com/engine/reference/run/#clean-up---rm"
         * >Docker run reference: Clean up (--rm)</a>
         */
        val autoCleanup: Boolean = true,

        /**
         * Whether to keep STDIN open even if not attached.
         *
         * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#options"
         * >Docker run: Options</a>
         */
        val interactive: Boolean = true,

        /**
         * Whether to allocate a pseudo-TTY.
         *
         * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#options"
         * >Docker run: Options</a>
         */
        val pseudoTerminal: Boolean = false,

        /**
         * The mounts the [DockerRunCommandLine] should run with.
         *
         * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#add-bind-mounts-or-volumes-using-the---mount-flag"
         * >Docker run: Add bind mounts or volumes using the --mount flag</a>
         */
        val mounts: MountOptions = MountOptions(emptyList()),

        /**
         * Custom options the [DockerRunCommandLine] should be run with.
         *
         * The elements of this list are treated as arguments and
         * appended to the command line unchanged.
         *
         * ***Note:** This feature should only be used if the needed option
         * is not implemented.*
         */
        val custom: List<String> = emptyList(),
    ) : List<String> by (buildList {
        detached.takeIf { it }?.also { add("-d") }
        entryPoint?.also { add("--entrypoint", entryPoint) }
        name?.also { add("--name", name.name) }
        publish.forEach { p -> add("-p", p) }
        privileged.takeIf { it }?.also { add("--privileged") }
        workingDirectory?.also { add("--workdir", it.asString()) }
        autoCleanup.takeIf { it }?.also { add("--rm") }
        interactive.takeIf { it }?.also { add("--interactive") }
        pseudoTerminal.takeIf { it }?.also { add("--tty") }
        mounts.addAll { this }
        custom.forEach { add(it) }
    }) {
        /**
         * Checks if [hostPath] represents a path accessible by one of the [mounts]
         * and if so, returns the mapped [ContainerPath].
         */
        public fun mapToContainerPathOrNull(hostPath: HostPath): ContainerPath? =
            kotlin.runCatching { mounts.mapToContainerPath(hostPath) }.getOrNull()

        /**
         * Tries to find all paths found inside [arg] and remaps all those
         * that are still accessible through the specified [mounts].
         *
         * Relative paths are resolved using the [hostWorkingDirectory] and if specified
         * mapped backed to a relative [ContainerPath] using [Options.workingDirectory].
         *
         * Arguments not containing paths are left unchanged.
         *
         * Arguments of the form `a=b` get mapped with key and value treated separately.
         */
        public fun remapPathsInArguments(hostWorkingDirectory: Path, arg: String, fulltextStrategy: Boolean): String =
            if (arg.count { it == '=' } > 1) arg
            else arg.splitAndMap("=") {
                if (fulltextStrategy) {
                    val pathString = hostWorkingDirectory.pathString
                    val baseDirRegex = Regex.escape(pathString).toRegex()
                    baseDirRegex.replace(this) {
                        remapArgumentAsPath(hostWorkingDirectory, it.value.asPath()) ?: this
                    }
                } else {
                    fun String.remap(): String {
                        val argAsPath = asHostPath() // e.g. /a/b resp. b
                        if (!argAsPath.isAbsolute) return this // skip -arg
                        return remapArgumentAsPath(hostWorkingDirectory, argAsPath) ?: this
                    }

                    splitAndMap(" ") { remap() }
                }
            }

        private fun remapArgumentAsPath(hostWorkingDirectory: Path, argAsPath: HostPath): String? {
            val argAsAbsPath = hostWorkingDirectory.resolveBetweenFileSystems(argAsPath) // e.g. /a/b resp. /a/b (if pwd=/a)
            val mapped = mapToContainerPathOrNull(argAsAbsPath)?.let { mappedPath ->   // e.g. /c/d
                workingDirectory
                    ?.takeIf { !argAsPath.isAbsolute }
                    ?.let { mappedPath.relativeTo(it) } // e.g. b (if container pwd=/c)
                    ?: mappedPath.asString() // e.g. /c/d
            }
            return mapped
        }

        /**
         * Returns [Options] consisting of all currently set options and
         * the unchanged [name] if it's already set or the specified
         * [fallbackName] otherwise.
         */
        public fun withFallbackName(fallbackName: String): Options =
            takeUnless { it.name == null } ?: copy(name = DockerContainer.from(fallbackName))

        /**
         * Returns [Options] consisting of all currently set options and
         * the unchanged [workingDirectory] if it's already set or the specified
         * [fallbackWorkingDirectory] mapped using [mapToContainerPathOrNull].
         */
        public fun withFallbackWorkingDirectory(fallbackWorkingDirectory: HostPath): Options =
            takeUnless { it.workingDirectory == null } ?: run {
                val mappedFallbackWorkingDirectory = mapToContainerPathOrNull(fallbackWorkingDirectory)
                takeUnless { mappedFallbackWorkingDirectory != null } ?: copy(workingDirectory = mappedFallbackWorkingDirectory)
            }

        /**
         * Adapts the [entryPoint] to allow the specified [executable] to be
         * executed in a container.
         */
        public fun withFixedEntryPoint(executable: Executable<*>): Options =
            takeUnless { executable is ShellScript } ?: copy(entryPoint = DEFAULT_SHELL_COMMAND)
    }

    public companion object {

        private val DEFAULT_SHELL = BourneShell()
        public val DEFAULT_SHELL_COMMAND: String = DEFAULT_SHELL.shellCommand
        public val DEFAULT_SHELL_ARGUMENTS: List<String> = DEFAULT_SHELL.shellArgsList

        private fun CommandLine.warnOnConnectivityProblem() = CommandLine(command, arguments, null) { pid, exitCode, io ->
            kotlin.runCatching {
                with(DockerExitStateHandler) { handle(pid, exitCode, io).takeIf { exitState -> exitState is ConnectivityProblem } }
            }.getOrNull() ?: run {
                with(fallbackExitStateHandler()) { handle(pid, exitCode, io) }
            }
        }
    }
}

/**
 * Returns a [DockerRunCommandLine] that runs `this` [Executable]
 * using the [DockerImage] built by [image]
 * and optional [options] (default: [Options.autoCleanup], [Options.interactive] and [Options.name] derived from [CommandLine.content]).
 */
public fun Executable<Exec>.dockerized(options: Options = Options(), image: DockerImageInit): DockerRunCommandLine =
    DockerRunCommandLine(DockerImage(image), options, this)

/**
 * Returns a [DockerRunCommandLine] that runs `this` [Executable]
 * using the specified [image]
 * and optional [options] (default: [Options.autoCleanup], [Options.interactive] and [Options.name] derived from [CommandLine.content]).
 */
public fun Executable<Exec>.dockerized(image: DockerImage, options: Options = Options()): DockerRunCommandLine =
    DockerRunCommandLine(image, options, this)
