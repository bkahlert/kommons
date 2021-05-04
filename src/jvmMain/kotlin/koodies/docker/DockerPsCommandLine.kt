package koodies.docker

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BooleanBuilder.YesNo.Context
import koodies.builder.BuilderTemplate
import koodies.builder.PairBuilder
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerPsCommandLine.Options.Companion.OptionsContext

/**
 * [DockerCommandLine] that lists locally available instances of [DockerContainer].
 */
public open class DockerPsCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
) : DockerCommandLine(
    dockerCommand = "ps",
    arguments = buildArray {
        addAll(options)
        add("--no-trunc")
        add("--format")
        add("{{.Names}}\t{{.State}}\t{{.Status}}")
    },
) {
    public open class Options(
        /**
         * Show all images (default hides intermediate images)
         */
        public val all: Boolean = false,

        /**
         * Filter output based on conditions provided
         */
        public val filters: List<Pair<String, String>> = emptyList(),
    ) : List<String> by (buildList {
        if (all) add("--all")
        filters.forEach { (key, value) -> add("--filter"); add("$key=$value") }
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {

            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * Show all containers (default shows just running)
                 */
                public val all: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false

                /**
                 * Filter output based on conditions provided
                 */
                public val filter: SkippableCapturingBuilderInterface<() -> Pair<String, String>, Pair<String, String>?> by PairBuilder()

                /**
                 * Filter output based on container’s exact name
                 */
                public fun exactName(name: String): Unit = filter { "name" to "^$name${'$'}" }
            }

            override fun BuildContext.build(): Options = Companion::OptionsContext {
                Options(::all.eval(), ::filter.evalAll())
            }
        }
    }

    public companion object : BuilderTemplate<Companion.CommandContext, DockerPsCommandLine>() {
        /**
         * Context for building a [DockerPsCommandLine].
         */

        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
        }

        override fun BuildContext.build(): DockerPsCommandLine = Companion::CommandContext {
            DockerPsCommandLine(::options.evalOrDefault { Options() })
        }
    }
}
