package koodies.docker

import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.ListBuilder
import koodies.builder.SkippableBuilder
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.execute
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.errors
import koodies.docker.DockerStopCommandLine.Companion.CommandContext
import koodies.docker.DockerStopCommandLine.Options.Companion.OptionsContext
import koodies.logging.RenderingLogger
import koodies.text.Semantics.formattedAs
import koodies.time.toIntMilliseconds
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * [DockerCommandLine] that stops the specified [containers] using the specified [options].
 */
public open class DockerStopCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "stop",
    arguments = buildArray {
        addAll(options)
        addAll(containers)
    },
) {
    public open class Options(
        /**
         * 	Seconds to wait for stop before killing it
         */
        public val time: Int? = null,
    ) : List<String> by (buildList {
        time?.also { +"--time" + "$time" }
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @DockerCommandLineDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * 	Seconds to wait for stop before killing it
                 */
                public val time: SkippableCapturingBuilderInterface<() -> Int, Int?> by builder<Int>()

                /**
                 * [Duration] to wait for stop before killing it. Timeouts only support a resolution of 1 second.
                 * Fractions are rounded according to [roundToInt].
                 */
                public val timeout: SkippableBuilder<() -> Duration, Duration, Unit> by builder<Duration>() then {
                    it.toIntMilliseconds().div(1000.0).roundToInt()
                } then time
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(::time.eval())
            }
        }
    }

    public companion object : BuilderTemplate<CommandContext, DockerStopCommandLine>() {
        /**
         * Context for building a [DockerStopCommandLine].
         */
        @DockerCommandLineDsl
        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
            public val containers: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
        }

        override fun BuildContext.build(): DockerStopCommandLine = ::CommandContext {
            DockerStopCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }
}


private fun ManagedProcess.parseResponse(): Boolean = errors().let {
    it.isBlank()
        || it.contains("no such container", ignoreCase = true)
        && !it.contains("cannot remove a running container", ignoreCase = true)
}

/**
 * Stops `this` [DockerContainer] from the locally stored containers using the
 * [DockerStopCommandLine.Options] built with the given [OptionsContext] [Init].
 * and prints the [DockerCommandLine]'s execution to [System.out].
 */
public val DockerContainer.stop: (Init<OptionsContext>) -> Boolean
    get() = {
        DockerStopCommandLine {
            options(it)
            containers by listOf(this@stop.name)
        }.execute {
            summary("Stopping ${this@stop.formattedAs.input}")
            null
        }.parseResponse()
    }

/**
 * Stops `this` [DockerContainer] from the locally stored containers using the
 * [DockerStopCommandLine.Options] built with the given [OptionsContext] [Init].
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.stop: DockerContainer.(Init<OptionsContext>) -> Boolean
    get() = {
        val thisContainer: DockerContainer = this
        DockerStopCommandLine {
            options(it)
            containers by listOf(thisContainer.name)
        }.execute {
            summary("Stopping ${thisContainer.formattedAs.input}")
            null
        }.parseResponse()
    }
