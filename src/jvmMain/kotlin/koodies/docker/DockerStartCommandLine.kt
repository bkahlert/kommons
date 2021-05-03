package koodies.docker

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BooleanBuilder.YesNo.Context
import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerStartCommandLine.Companion.CommandContext
import koodies.docker.DockerStartCommandLine.Options.Companion.OptionsContext

/**
 * [DockerCommandLine] that starts the specified [containers] using the specified [options].
 */
public open class DockerStartCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "start",
    arguments = buildArray {
        addAll(options)
        addAll(containers)
    },
) {
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
        if (attach) +"--attach"
        if (interactive) +"--interactive"
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {

            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {

                /**
                 * Attach STDOUT/STDERR and forward signals
                 */
                public val attach: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default true

                /**
                 * Attach container's STDIN
                 */
                public val interactive: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(::attach.evalOrDefault(true), ::interactive.evalOrDefault(false))
            }
        }
    }

    public companion object : BuilderTemplate<CommandContext, DockerStartCommandLine>() {
        /**
         * Context for building a [DockerStartCommandLine].
         */

        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
            public val containers: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
        }

        override fun BuildContext.build(): DockerStartCommandLine = ::CommandContext {
            DockerStartCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }
}
