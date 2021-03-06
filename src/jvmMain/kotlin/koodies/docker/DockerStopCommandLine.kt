package koodies.docker

import koodies.builder.ArrayBuilder
import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerStopCommandLine.Companion.StopContext
import koodies.docker.DockerStopCommandLine.Options.Companion.StopOptionsContext

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
    arguments = ArrayBuilder.buildArray {
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
        public companion object : BuilderTemplate<StopOptionsContext, Options>() {
            /**
             * Context for building [Options].
             */
            @DockerCommandLineDsl
            public class StopOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * 	Seconds to wait for stop before killing it
                 */
                public val time: SkippableCapturingBuilderInterface<() -> Int, Int?> by builder<Int>()
            }

            override fun BuildContext.build(): Options = ::StopOptionsContext {
                Options(::time.eval())
            }
        }
    }

    public companion object : BuilderTemplate<StopContext, DockerStopCommandLine>() {
        /**
         * Context for building a [DockerStopCommandLine].
         */
        @DockerCommandLineDsl
        public class StopContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<StopOptionsContext.() -> Unit, Options?> by Options
            public val containers: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder()
        }

        override fun BuildContext.build(): DockerStopCommandLine = ::StopContext {
            DockerStopCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }
}

