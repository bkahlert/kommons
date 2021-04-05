package koodies.docker

import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.Execution
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors.loggingProcessor
import koodies.concurrent.process.process
import koodies.concurrent.process.terminationLoggingProcessor
import koodies.concurrent.toManagedProcess
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.DockerizedExecution.DockerizedExecutionOptions.Companion.OptionsContext
import koodies.logging.RenderingLogger
import koodies.logging.runLogging
import koodies.shell.ShellExecutable
import koodies.text.Semantics.formattedAs
import koodies.text.withRandomSuffix
import koodies.toBaseName

/**
 * Helper to collect an optional [RenderingLogger], build [DockerRunCommandLineExecutionOptions] and an optional [Processor]
 * to [execute] the given [DockerRunCommandLine].
 */
public class DockerizedExecution(
    private val parentLogger: RenderingLogger?,
    private val image: DockerImage,
    private val commandLine: CommandLine,
) {
    private var processor: Processor<DockerProcess>? = null

    public fun executeWithOptionalProcessor(init: (OptionsContext.() -> Processor<DockerProcess>?)?): DockerProcess =
        executeWithOptionallyStoredProcessor { init?.let { processor = it() } }

    private fun executeWithOptionallyStoredProcessor(init: Init<OptionsContext>): DockerProcess {
        val options = DockerizedExecutionOptions(init)
        val dockerOptions = options.dockerOptions.withDefaultName(commandLine.summary.toBaseName().withRandomSuffix())
        val dockerRunCommandLine = DockerRunCommandLine(image, dockerOptions, commandLine)

        return with(options.executionOptions) {
            val processLogger = loggingOptions.newLogger(parentLogger, "Executing dockerized with ${image.formattedAs.input}: ${commandLine.summary}")
            val dockerProcess = dockerRunCommandLine.toManagedProcess(processTerminationCallback)
            if (processingMode.isSync) {
                processLogger.runLogging {
                    dockerProcess.process(processingMode, processor = processor ?: loggingProcessor(processLogger))
                }
            } else {
                processLogger.logResult { Result.success(dockerProcess) }
                dockerProcess.process(processingMode, processor = processor ?: dockerProcess.terminationLoggingProcessor(processLogger))
            }
        }
    }

    private fun Options.withDefaultName(name: String) =
        takeUnless { it.name == null } ?: copy(name = DockerContainer.from(name))

    /**
     * Options used to [executeDockerized] a [CommandLine].
     */
    public data class DockerizedExecutionOptions(
        val dockerOptions: Options = Options(),
        val executionOptions: Execution.Options = Execution.Options(),
    ) {
        public companion object : BuilderTemplate<OptionsContext, DockerizedExecutionOptions>() {
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val dockerOptions: SkippableCapturingBuilderInterface<Options.Companion.OptionsContext.() -> Unit, Options?> by Options
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
public val ShellExecutable.executeDockerized: (DockerImage, (OptionsContext.() -> Processor<DockerProcess>?)?) -> DockerProcess
    get() = { image, optionsInit ->
        DockerizedExecution(null, image, this.toCommandLine()).executeWithOptionalProcessor(optionsInit)
    }

/**
 * Runs `this` [CommandLine] using the
 * given [DockerImage] and the
 * [DockerRunCommandLine.Options] built with the given [OptionsContext] [Init].
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.executeDockerized: ShellExecutable.(DockerImage, (OptionsContext.() -> Processor<DockerProcess>?)?) -> DockerProcess
    get() = { image, optionsInit ->
        DockerizedExecution(this@executeDockerized, image, this.toCommandLine()).executeWithOptionalProcessor(optionsInit)
    }
