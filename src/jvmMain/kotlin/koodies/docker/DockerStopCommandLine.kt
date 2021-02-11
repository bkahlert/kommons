package koodies.docker

import koodies.builder.ArrayBuilder
import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.docker.DockerStopCommandLine.Companion.StopContext
import koodies.docker.DockerStopCommandLine.Options.Companion.StopOptionsContext

/**
 * [DockerCommandLine] that stops the specified [containers] using the specified [options].
 */
open class DockerStopCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    val options: Options,
    val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "stop",
    arguments = ArrayBuilder.buildArray {
        addAll(options)
        addAll(containers)
    },
) {
    open class Options(
        /**
         * 	Seconds to wait for stop before killing it
         */
        val time: Int? = null,
    ) : List<String> by (buildList {
        time?.also { +"--time" + "$time" }
    }) {
        companion object : BuilderTemplate<StopOptionsContext, Options>() {
            /**
             * Context for building [Options].
             */
            @DockerCommandLineDsl
            class StopOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * 	Seconds to wait for stop before killing it
                 */
                val time by builder<Int>()
            }

            override fun BuildContext.build() = withContext(::StopOptionsContext) {
                Options(::time.eval())
            }
        }
    }

    companion object : BuilderTemplate<StopContext, DockerStopCommandLine>() {
        /**
         * Context for building a [DockerStopCommandLine].
         */
        @DockerCommandLineDsl
        class StopContext(override val captures: CapturesMap) : CapturingContext() {
            val options by Options
            val containers by ListBuilder<String>()
        }

        override fun BuildContext.build() = withContext(::StopContext) {
            DockerStopCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }
}

