package koodies.concurrent.process

import koodies.builder.ListBuilderInit
import koodies.builder.MapBuilderInit
import koodies.builder.buildMapTo
import koodies.builder.buildTo
import koodies.io.path.Locations
import java.nio.file.Path

abstract class CommandLineBuilder {
    companion object {
        fun build(command: String, init: CommandLineBuilder.() -> Unit): CommandLine {
            val redirects = mutableListOf<String>()
            val environment = mutableMapOf<String, String>()
            val workingDirectory = Locations.Temp
            val args = mutableListOf<String>()
            object : CommandLineBuilder() {
                override val redirects
                    get() = redirects
                override val environment
                    get() = environment
                override val workingDirectory: Path
                    get() = workingDirectory
                override val command = command
                override val args
                    get() = args
            }.apply(init)
            return CommandLine(
                redirects = redirects,
                environment = environment,
                workingDirectory = workingDirectory,
                command = command,
                arguments = args.toList()
            )
        }
    }

    protected abstract val redirects: MutableList<String>
    protected abstract val environment: MutableMap<String, String>
    protected abstract val workingDirectory: Path
    protected abstract val command: String
    protected abstract val args: MutableList<String>

    fun redirects(init: ListBuilderInit<String>) = init.buildTo(redirects)
    fun environment(init: MapBuilderInit<String, String>) = init.buildMapTo(environment)
    fun arguments(init: ListBuilderInit<String>) = init.buildTo(args)
}
