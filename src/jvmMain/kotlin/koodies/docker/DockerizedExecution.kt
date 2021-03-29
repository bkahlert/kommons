package koodies.docker

import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.Execution
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.Processor
import koodies.concurrent.process.attach
import koodies.concurrent.process.process
import koodies.concurrent.toManagedProcess
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.DockerizedExecution.DockerizedExecutionOptions
import koodies.docker.DockerizedExecution.DockerizedExecutionOptions.Companion.OptionsContext
import koodies.logging.RenderingLogger
import koodies.shell.ShellExecutable
import koodies.text.Semantics.formattedAs
import koodies.time.sleep
import koodies.toBaseName
import kotlin.time.seconds

/**
 * Helper to collect an optional [RenderingLogger], build [DockerRunCommandLineExecutionOptions] and an optional [Processor]
 * to [execute] the given [DockerRunCommandLine].
 */
public class DockerizedExecution(
    private val logger: RenderingLogger?,
    private val image: DockerImage,
    private val commandLine: CommandLine,
) {
    private var processor: Processor<DockerProcess>? = null

    public fun executeWithOptionalProcessor(init: (DockerizedExecutionOptions.Companion.OptionsContext.() -> Processor<DockerProcess>?)?): DockerProcess =
        executeWithOptionallyStoredProcessor {
            init?.let {
                processor = it()
            } ?: run {
                dockerOptions { name by commandLine.summary.toBaseName() }
            }
        }

    private fun executeWithOptionallyStoredProcessor(init: Init<OptionsContext>): DockerProcess {
        val options = DockerizedExecutionOptions(init)
        val dockerRunCommandLine = DockerRunCommandLine(image, options.dockerOptions, commandLine)

        return with(options.executionOptions) {
            loggingOptions.render(logger, "Executing dockerized with ${image.formattedAs.input}: ${commandLine.summary}") {
                dockerRunCommandLine.toManagedProcess(expectedExitValue, processTerminationCallback)
                    .also { it.start() } // TODO remove
                    .also { 5.seconds.sleep() }
                    .let { it.process(processingMode, processor = processor ?: it.attach(this)) }
            }
        }
    }

    /**
     * Options used to [executeDockerized] a [CommandLine].
     */
    public data class DockerizedExecutionOptions(
        val dockerOptions: Options = Options(),
        val executionOptions: Execution.Options = Execution.Options(),
    ) {
        public companion object : BuilderTemplate<OptionsContext, DockerizedExecutionOptions>() {
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val dockerOptions: SkippableCapturingBuilderInterface<koodies.docker.DockerRunCommandLine.Options.Companion.OptionsContext.() -> Unit, Options?> by Options
                public val executionOptions: SkippableCapturingBuilderInterface<Execution.Options.Companion.OptionsContext.() -> Unit, Execution.Options?> by Execution.Options
            }

            override fun BuildContext.build(): DockerizedExecutionOptions = Companion::OptionsContext {
                DockerizedExecutionOptions(::dockerOptions.evalOrDefault { Options() }, ::executionOptions.evalOrDefault { Execution.Options() })
            }
        }
    }
}

/**
 * Runs `this` [CommandLine] using the
 * given [DockerImage] and the
 * [DockerRunCommandLine.Options] built with the given [OptionsContext] [Init].
 * and prints the [DockerCommandLine]'s execution to [System.out].
 */
public val ShellExecutable.executeDockerized: (DockerImage, (DockerizedExecutionOptions.Companion.OptionsContext.() -> Processor<DockerProcess>?)?) -> DockerProcess
    get() = { image, optionsInit ->
        DockerizedExecution(null, image, this.toCommandLine()).executeWithOptionalProcessor(optionsInit)
    }

/**
 * Runs `this` [CommandLine] using the
 * given [DockerImage] and the
 * [DockerRunCommandLine.Options] built with the given [OptionsContext] [Init].
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.executeDockerized: ShellExecutable.(DockerImage, (DockerizedExecutionOptions.Companion.OptionsContext.() -> Processor<DockerProcess>?)?) -> DockerProcess
    get() = { image, optionsInit ->
        DockerizedExecution(this@executeDockerized, image, this.toCommandLine()).executeWithOptionalProcessor(optionsInit)
    }
