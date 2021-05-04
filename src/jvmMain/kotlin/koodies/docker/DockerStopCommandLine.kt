package koodies.docker

import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.SkippableBuilder
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerStopCommandLine.Companion.CommandContext
import koodies.docker.DockerStopCommandLine.Options.Companion.OptionsContext
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
         * Seconds to wait for stop before killing it
         */
        public val time: Int? = null,
    ) : List<String> by (buildList {
        time?.also { +"--time" + "$time" }
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {

            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {

                /**
                 * Seconds to wait for stop before killing it
                 */
                public val time: SkippableCapturingBuilderInterface<() -> Int?, Int?> by builder()

                /**
                 * [Duration] to wait for stop before killing it. Timeouts only support a resolution of 1 second.
                 * Fractions are rounded according to [roundToInt].
                 */
                public val timeout: SkippableBuilder<() -> Duration?, Duration?, Unit> by builder<Duration?>() then {
                    it?.toIntMilliseconds()?.div(1000.0)?.roundToInt()
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
