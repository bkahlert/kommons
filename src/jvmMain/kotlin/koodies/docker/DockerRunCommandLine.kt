package koodies.docker

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.OnOff
import koodies.builder.BooleanBuilder.OnOff.Context
import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.ListBuilder
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerRunCommandLine.Companion.CommandContext
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.DockerRunCommandLine.Options.Companion.OptionsContext
import koodies.exec.CommandLine
import koodies.exec.CommandLine.Companion.CommandLineContext
import koodies.io.file.resolveBetweenFileSystems
import koodies.text.splitAndMap
import java.nio.file.Path

/**
 * [DockerCommandLine] that runs a command specified by its [redirects], [environment], [workingDirectory],
 * [command] and [arguments] using the specified [image] using the specified [options]
 */
public open class DockerRunCommandLine private constructor(

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
    public val options: Options,
    redirects: List<String>,
    environment: Map<String, String>,
    workingDirectory: Path,
    command: String,
    arguments: List<String>,
) : DockerCommandLine(
    redirects = redirects,
    environment = environment,
    workingDirectory = workingDirectory,
    dockerCommand = "run",
    arguments = buildList {
        environment.addAll { listOf("--env", "$key=$value") }
        addAll(options)
        add(image.toString())
        if (command.isNotBlank() && options.entryPoint == null) add(command)
        addAll(options.remapPathsInArguments(workingDirectory, arguments))
    },
) {
    public constructor(image: DockerImage, options: Options = Options(), commandLine: CommandLine) : this(
        image = image,
        options = options.withFallbackWorkingDirectory(commandLine.workingDirectory),
        redirects = commandLine.redirects,
        environment = commandLine.environment,
        workingDirectory = commandLine.workingDirectory,
        command = commandLine.command,
        arguments = commandLine.arguments,
    )

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
        workingDirectory?.also { add("-w", it.asString()) }
        autoCleanup.takeIf { it }?.also { add("--rm") }
        interactive.takeIf { it }?.also { add("--interactive") }
        pseudoTerminal.takeIf { it }?.also { add("--tty") }
        mounts.addAll { this }
        custom.forEach { add(it) }
    }) {
        /**
         * Checks if this strings represents a path accessible by one of the [MountOptions]
         * of the specified [Options] and if so, returns the mapped [ContainerPath].
         */
        public fun mapToContainerPathOrNull(hostPath: HostPath): ContainerPath? =
            kotlin.runCatching { mounts.mapToContainerPath(hostPath) }.getOrNull()

        /**
         * Tries to find all paths found inside [arguments] and remaps all those
         * that are still accessible through the specified [mounts].
         *
         * Relative paths are resolved using the [argumentsWorkingDirectory] and if specified
         * mapped backed to a relative [ContainerPath] using [Options.workingDirectory].
         *
         * Arguments not containing paths are left unchanged.
         *
         * Arguments of the form `a=b` get mapped with key and value treated separately.
         */
        public fun remapPathsInArguments(argumentsWorkingDirectory: Path, arguments: List<String>): List<String> = arguments.map { arg ->
            if (arg.count { it == '=' } > 1) return@map arg
            arg.splitAndMap("=") {
                val originalPath = asHostPath() // e.g. /a/b resp. b
                val absoluteOriginalPath = argumentsWorkingDirectory.resolveBetweenFileSystems(originalPath) // e.g. /a/b resp. /a/b (if pwd=/a)
                mapToContainerPathOrNull(absoluteOriginalPath)?.let { mappedPath ->   // e.g. /c/d
                    workingDirectory
                        ?.takeIf { !originalPath.isAbsolute }
                        ?.let { mappedPath.relativeTo(it) } // e.g. b (if container pwd=/c)
                        ?: mappedPath.asString() // e.g. /c/d
                } ?: this
            }
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
         * [fallbackWorkingDirectory] otherwise.
         */
        public fun withFallbackWorkingDirectory(fallbackWorkingDirectory: HostPath): Options =
            takeUnless { it.workingDirectory == null } ?: run {
                val mappedFallbackWorkingDirectory = mapToContainerPathOrNull(fallbackWorkingDirectory)
                takeUnless { mappedFallbackWorkingDirectory != null } ?: copy(workingDirectory = mappedFallbackWorkingDirectory)
            }

        public companion object : BuilderTemplate<OptionsContext, Options>() {

            /**
             * Context to build [Options].
             */
            @DockerCommandLineDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {

                /**
                 * Whether to run the [DockerRunCommandLine] in detached or in
                 * foreground mode.
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/run/#detached--d"
                 * >Docker run reference: Detached (-d)</a>
                 */
                public val detached: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff

                /**
                 * If set, the entry point the Dockerfile image default should be overridden
                 * with.
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/run/#entrypoint-default-command-to-execute-at-runtime"
                 * >Docker run reference: ENTRYPOINT (default command to execute at runtime)</a>
                 */
                public val entrypoint: SkippableCapturingBuilderInterface<() -> String, String?> by builder()

                /**
                 * The name the container created by the [DockerRunCommandLine] should
                 * be identified by.
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/run/#name---name"
                 * >Docker run reference: Name (--name)</a>
                 */
                public val name: SkippableCapturingBuilderInterface<() -> String, String?> by builder()

                /**
                 * The container representing the container created by the [DockerRunCommandLine].
                 *
                 * This method is an alternative to [name] for the case a [DockerContainer]
                 * is already present and its name used.
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/run/#name---name"
                 * >Docker run reference: Name (--name)</a>
                 */
                public val container: SkippableCapturingBuilderInterface<() -> DockerContainer, DockerContainer?> by builder()

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
                public val publish: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder()

                /**
                 * Whether to run the [DockerRunCommandLine] with extended privileges.
                 *
                 * By default, Docker containers are “unprivileged” and cannot, for example,
                 * run a Docker daemon inside a Docker container.
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/run/#runtime-privilege-and-linux-capabilities"
                 * >Docker run reference: Runtime privilege and Linux capabilities</a>
                 */
                public val privileged: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff

                /**
                 * The working directory to run the [DockerRunCommandLine] with.
                 *
                 * The default working directory for running binaries within a container is the root directory (/).
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/run/#workdir"
                 * >Docker run reference: WORKDIR</a>
                 */
                public val workingDirectory: SkippableCapturingBuilderInterface<() -> ContainerPath, ContainerPath?> by builder()

                /**
                 * Whether to automatically remove the container when it exits.
                 *
                 * By default a container’s file system persists even after the container exits.
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/run/#clean-up---rm"
                 * >Docker run reference: Clean up (--rm)</a>
                 */
                public val autoCleanup: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff

                /**
                 * Whether to keep STDIN open even if not attached.
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#options"
                 * >Docker run: Options</a>
                 */
                public val interactive: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff

                /**
                 * Whether to allocate a pseudo-TTY.
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#options"
                 * >Docker run: Options</a>
                 */
                public val pseudoTerminal: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff

                /**
                 * The mounts the [DockerRunCommandLine] should run with.
                 *
                 * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#add-bind-mounts-or-volumes-using-the---mount-flag"
                 * >Docker run: Add bind mounts or volumes using the --mount flag</a>
                 */
                public val mounts: SkippableCapturingBuilderInterface<Init<MountOptionContext<Unit>>, MountOptions?> by MountOptions

                /**
                 * Custom options the [DockerRunCommandLine] should be run with.
                 *
                 * The elements of this list are treated as arguments and
                 * appended to the command line unchanged.
                 *
                 * ***Note:** This feature should only be used if the needed option
                 * is not implemented.*
                 */
                public val custom: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder()
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(
                    ::detached.evalOrDefault(false),
                    ::entrypoint.evalOrNull(),
                    ::name.evalOrNull<String>()?.let { DockerContainer.from(it) } ?: ::container.evalOrNull<DockerContainer>(),
                    ::publish.evalOrDefault(emptyList()),
                    ::privileged.evalOrDefault(false),
                    ::workingDirectory.evalOrNull(),
                    ::autoCleanup.evalOrDefault(true),
                    ::interactive.evalOrDefault(true),
                    ::pseudoTerminal.evalOrDefault(false),
                    ::mounts.evalOrDefault { MountOptions() },
                    ::custom.evalOrDefault(emptyList()),
                )
            }
        }
    }

    public companion object : BuilderTemplate<CommandContext, DockerRunCommandLine>() {

        /**
         * Context to build a [DockerRunCommandLine].
         */
        @DockerCommandLineDsl
        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {

            /**
             * The image used to run the [DockerRunCommandLine].
             *
             * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#extended-description"
             * >Docker run: Extended Description</a>
             */
            public val image: SkippableCapturingBuilderInterface<DockerImageInit, DockerImage?> by DockerImage

            /**
             * The options used to run the [DockerRunCommandLine].
             *
             * @see Options
             * @see <a href="https://docs.docker.com/engine/reference/commandline/run/#options"
             * >Docker run: Options</a>
             */
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options

            /**
             * The command line the [DockerRunCommandLine] should run.
             */
            public val commandLine: SkippableCapturingBuilderInterface<CommandLineContext.() -> Unit, CommandLine?> by CommandLine
        }

        override fun BuildContext.build(): DockerRunCommandLine = ::CommandContext {
            DockerRunCommandLine(
                ::image.eval(),
                ::options.evalOrDefault { Options() },
                ::commandLine.evalOrDefault { CommandLine("") },
            )
        }
    }
}
