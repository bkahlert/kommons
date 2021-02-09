package koodies.docker

import koodies.builder.Init
import koodies.builder.ListBuilder
import koodies.builder.MapBuilderInit
import koodies.builder.build
import koodies.builder.buildMapTo
import koodies.builder.buildMultipleTo
import koodies.builder.context.ElementAddingContext
import koodies.concurrent.process.ManagedProcess
import koodies.io.path.Locations
import java.nio.file.Path

/**
 * Builder to create instances of [DockerCommandLine].
 */
open class DockerCommandLineBuilder(

    /**
     * Redirects like `2>&1` to be used when running this command line.
     */
    protected val redirects: MutableList<String> = mutableListOf(),
    /**
     * The environment to be exposed to the [ManagedProcess] that runs this
     * command line.
     */
    protected val environment: MutableMap<String, String> = mutableMapOf(),
    /**
     * The working directory of the [ManagedProcess] that runs this
     * command line.
     */
    protected var workingDirectory: Path = Locations.Temp,
    /**
     * The arguments to be passed to [DockerCommandLine.command].
     */
    protected val arguments: MutableList<String> = mutableListOf(),
) {
    companion object {
        /**
         * Builds a [DockerCommandLine] using [init] that runs using the specified [command].
         */
        fun build(command: String, init: DockerCommandLineBuilder.() -> Unit): DockerCommandLine =
            DockerCommandLineBuilder().apply(init).run {
                DockerCommandLine(redirects, environment, workingDirectory, command, arguments.toList())
            }
    }

    /**
     * Specifies the redirects like `2>&1` to be used when running this built command line.
     */
    fun redirects(init: Init<ElementAddingContext<String>, Unit>) = ListBuilder<String>().buildMultipleTo(init, redirects)

    /**
     * Specifies the environment to be exposed to the [ManagedProcess] that runs this built
     * command line.
     */
    fun environment(init: MapBuilderInit<String, String>) = init.buildMapTo(environment)

    /**
     * Specifies the working directory of the [ManagedProcess] that runs this built
     * command line.
     */
    fun workingDirectory(init: () -> Path) = init.build { workingDirectory = this }

    /**
     * Specifies the arguments to be passed to [command].
     */
    fun arguments(init: Init<ElementAddingContext<String>, Unit>) = ListBuilder<String>().buildMultipleTo(init, arguments)
}
