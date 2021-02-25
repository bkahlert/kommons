package koodies.docker

import koodies.builder.ArrayBuilder
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.docker.DockerStartCommandLine.Companion.StartContext
import koodies.docker.DockerStartCommandLine.Options.Companion.StartOptionsContext

/**
 * Start one or more stopped containers.
 */
open class DockerStartCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    val options: Options,
    val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "start",
    arguments = ArrayBuilder.buildArray {
        addAll(options)
        addAll(containers)
    },
) {
    companion object : BuilderTemplate<StartContext, DockerStartCommandLine>() {
        /**
         * Context for building a [DockerStartCommandLine].
         */
        @DockerCommandLineDsl
        class StartContext(override val captures: CapturesMap) : CapturingContext() {
            val options by Options
            val containers by ListBuilder<String>()
        }

        override fun BuildContext.build() = withContext(::StartContext) {
            DockerStartCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }

    open class Options(
        /**
         * Attach STDOUT/STDERR and forward signals
         */
        val attach: Boolean = true,
        /**
         * Attach container's STDIN
         */
        val interactive: Boolean = false,
    ) : List<String> by (buildList {
        attach.also { +"--attach" + "$attach" }
        interactive.also { +"--interactive" + "$interactive" }
    }) {
        companion object : BuilderTemplate<StartOptionsContext, Options>() {
            /**
             * Context for building [Options].
             */
            @DockerCommandLineDsl
            class StartOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * Attach STDOUT/STDERR and forward signals
                 */
                val attach by YesNo default true

                /**
                 * Attach container's STDIN
                 */
                val interactive by YesNo default false
            }

            override fun BuildContext.build() = withContext(::StartOptionsContext) {
                Options(::attach.evalOrDefault(true), ::interactive.evalOrDefault(false))
            }
        }
    }

}

