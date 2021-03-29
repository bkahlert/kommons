package koodies.docker

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.OnOff
import koodies.builder.BooleanBuilder.OnOff.Context
import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLine.Companion.CommandLineContext
import koodies.docker.DockerRunCommandLine.Companion.CommandContext
import koodies.docker.DockerRunCommandLine.Options.Companion.OptionsContext
import koodies.docker.MountOptions.Companion.CollectingMountOptionsContext
import koodies.io.file.resolveBetweenFileSystems
import koodies.text.splitAndMap
import java.nio.file.Path

/**
 * [DockerCommandLine] that runs a command specified by its [redirects], [environment], [workingDirectory],
 * [command] and [arguments] using the specified [image] using the specified [options]
 */
public open class DockerRunCommandLine private constructor(
    /**
     * The image used to run this command line.
     */
    public val image: DockerImage,
    /**
     * Options that specify how this command line is run.
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


    public data class Options(
        /**
         * [Detached (-d)][https://docs.docker.com/engine/reference/run/#detached--d]
         */
        val detached: Boolean = false,
        val entryPoint: String? = null,
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
         * @see <a href="https://docs.docker.com/engine/reference/run/#expose-incoming-ports">EXPOSE (incoming ports)</a>
         */
        val publish: List<String> = emptyList(),
        val privileged: Boolean = false,
        val workingDirectory: ContainerPath? = null,
        val autoCleanup: Boolean = true,
        val interactive: Boolean = true,
        val pseudoTerminal: Boolean = false,
        val mounts: MountOptions = MountOptions(emptyList()),
        val custom: List<String> = emptyList(),
    ) : List<String> by (buildList {
        detached.takeIf { it }?.also { add("-d") }
        entryPoint?.also { add("--entrypoint", entryPoint) }
        name?.also { add("--name", name.sanitized) }
        publish.forEach { p -> add("-p", p) }
        privileged.takeIf { it }?.also { add("--privileged") }
        workingDirectory?.also { add("-w", it.asString()) }
        autoCleanup.takeIf { it }?.also { add("--rm") }
        interactive.takeIf { it }?.also { add("-i") }
        pseudoTerminal.takeIf { it }?.also { add("-t") }
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

        public fun withFallbackWorkingDirectory(fallbackWorkingDirectory: HostPath): Options {
            if (workingDirectory == null) {
                val mappedFallbackWorkingDirectory = mapToContainerPathOrNull(fallbackWorkingDirectory)
                if (mappedFallbackWorkingDirectory != null) return copy(workingDirectory = mappedFallbackWorkingDirectory)
            }
            return this
        }

        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @DockerCommandLineDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * [Detached (-d)][https://docs.docker.com/engine/reference/run/#detached--d]
                 */
                public val detached: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
                public val entrypoint: SkippableCapturingBuilderInterface<() -> String, String?> by builder<String>()
                public val name: SkippableCapturingBuilderInterface<() -> String, String?> by builder<String>()

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
                 * @see <a href="https://docs.docker.com/engine/reference/run/#expose-incoming-ports">EXPOSE (incoming ports)</a>
                 */
                public val publish: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
                public val container: SkippableCapturingBuilderInterface<() -> DockerContainer, DockerContainer?> by builder<DockerContainer>()
                public val privileged: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
                public val workingDirectory: SkippableCapturingBuilderInterface<() -> ContainerPath, ContainerPath?> by builder<ContainerPath>()
                public val autoCleanup: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
                public val interactive: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
                public val pseudoTerminal: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
                public val mounts: SkippableCapturingBuilderInterface<CollectingMountOptionsContext.() -> Unit, MountOptions?> by MountOptions
                public val custom: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(
                    ::detached.evalOrDefault(false),
                    ::entrypoint.evalOrNull(),
                    ::name.evalOrNull<String>()?.let { DockerContainer(it) } ?: ::container.evalOrNull<DockerContainer>(),
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
        @DockerCommandLineDsl
        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val image: SkippableCapturingBuilderInterface<DockerImage.ImageContext.() -> DockerImage, DockerImage?> by DockerImage
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
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
