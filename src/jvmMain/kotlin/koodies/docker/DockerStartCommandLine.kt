package koodies.docker

import koodies.builder.ArrayBuilder
import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BooleanBuilder.YesNo.Context
import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerStartCommandLine.Companion.StartContext
import koodies.docker.DockerStartCommandLine.Options.Companion.StartOptionsContext

/**
 * Start one or more stopped containers.
 */
public open class DockerStartCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "start",
    arguments = ArrayBuilder.buildArray {
        addAll(options)
        addAll(containers)
    },
) {
    public companion object : BuilderTemplate<StartContext, DockerStartCommandLine>() {
        /**
         * Context for building a [DockerStartCommandLine].
         */
        @DockerCommandLineDsl
        public class StartContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<StartOptionsContext.() -> Unit, Options?> by Options
            public val containers: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
        }

        override fun BuildContext.build(): DockerStartCommandLine = ::StartContext {
            DockerStartCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }

    public open class Options(
        /**
         * Attach STDOUT/STDERR and forward signals
         */
        public val attach: Boolean = true,
        /**
         * Attach container's STDIN
         */
        public val interactive: Boolean = false,
    ) : List<String> by (buildList {
        attach.also { +"--attach" + "$attach" }
        interactive.also { +"--interactive" + "$interactive" }
    }) {
        public companion object : BuilderTemplate<StartOptionsContext, Options>() {
            /**
             * Context for building [Options].
             */
            @DockerCommandLineDsl
            public class StartOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * Attach STDOUT/STDERR and forward signals
                 */
                public val attach: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default true

                /**
                 * Attach container's STDIN
                 */
                public val interactive: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
            }

            override fun BuildContext.build(): Options = ::StartOptionsContext {
                Options(::attach.evalOrDefault(true), ::interactive.evalOrDefault(false))
            }
        }
    }

}

