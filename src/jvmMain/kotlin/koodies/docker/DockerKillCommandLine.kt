package koodies.docker

import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerKillCommandLine.Companion.CommandContext
import koodies.docker.DockerKillCommandLine.Options.Companion.OptionsContext

/**
 * [DockerCommandLine] that kills the specified [containers] using the specified [options].
 */
public open class DockerKillCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "kill",
    arguments = buildArray {
        addAll(options)
        addAll(containers)
    },
) {
    public open class Options(
        /**
         * 	Signal to send to the container (default: KILL)
         */
        public val signal: String? = null,
    ) : List<String> by (buildList {
        signal?.let {
            add("--signal")
            add(it)
        }
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {


            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {

                /**
                 * 	Signal to send to the container (default: KILL)
                 */
                public val signal: SkippableCapturingBuilderInterface<() -> String, String?> by builder()
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(::signal.eval())
            }
        }
    }

    public companion object : BuilderTemplate<CommandContext, DockerKillCommandLine>() {
        /**
         * Context for building a [DockerKillCommandLine].
         */

        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
            public val containers: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
        }

        override fun BuildContext.build(): DockerKillCommandLine = ::CommandContext {
            DockerKillCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }
}
