package koodies.concurrent.process

import koodies.builder.ListBuilderInit
import koodies.builder.MapBuilderInit
import koodies.builder.build
import koodies.builder.buildListTo
import koodies.builder.buildMapTo
import koodies.io.path.Locations
import java.nio.file.Path

class CommandLineBuilder(
    private val redirects: MutableList<String> = mutableListOf(),
    private val environment: MutableMap<String, String> = mutableMapOf(),
    private var workingDirectory: Path = Locations.Temp,
    private val args: MutableList<String> = mutableListOf(),
) {
    companion object {
        fun build(command: String, init: CommandLineBuilder.() -> Unit): CommandLine =
            CommandLineBuilder().apply(init).run {
                CommandLine(redirects, environment, workingDirectory, command, args.toList())
            }
    }

    fun redirects(init: ListBuilderInit<String>) = init.buildListTo(redirects)
    fun environment(init: MapBuilderInit<String, String>) = init.buildMapTo(environment)
    fun workingDirectory(init: () -> Path) = init.build { workingDirectory = this }
    fun arguments(init: ListBuilderInit<String>) = init.buildListTo(args)
}
