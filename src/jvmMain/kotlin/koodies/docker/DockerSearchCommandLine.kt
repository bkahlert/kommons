package koodies.docker

import koodies.builder.BooleanBuilder
import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.OnOff.Context
import koodies.builder.BuilderTemplate
import koodies.builder.PairBuilder
import koodies.builder.SkippableBuilder
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerSearchCommandLine.Companion.CommandContext
import koodies.docker.DockerSearchCommandLine.Options.Companion.OptionsContext

/**
 * Search one or more stopped containers.
 */
public open class DockerSearchCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val term: String,
) : DockerCommandLine(
    dockerCommand = "search",
    arguments = buildArray {
        addAll(options)
        add(term)
    },
) {

    public open class Options(
        /**
         * Filter output based on conditions provided
         */
        public val filters: List<Pair<String, String>> = emptyList(),
        /**
         * Pretty-print search using a Go template
         */
        public val format: String?,
        /**
         * Max number of search results
         */
        public val limit: Int? = 25,
    ) : List<String> by (buildList {
        filters.forEach { (key, value) -> +"--filter" + "$key=$value" }
        format?.also { +"--format" + it }
        limit.also { +"--limit" + "$limit" }
    }) {

        public companion object : BuilderTemplate<OptionsContext, Options>() {
            /**
             * Context for building [Options].
             */

            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {

                /**
                 * Filter output based on conditions provided
                 */
                public val filter: SkippableCapturingBuilderInterface<() -> Pair<String, String>, Pair<String, String>?> by PairBuilder()

                /**
                 * Filter output based on stars
                 */
                public val stars: SkippableBuilder<() -> Int, Int, Unit>
                    by builder<Int>() then { "stars" to it.toString() } then filter

                /**
                 * Filter output based on whether image is automated
                 */
                public val isAutomated: SkippableBuilder<Context.() -> BooleanValue, Boolean, Unit>
                    by BooleanBuilder.OnOff then { "is-automated" to it.toString() } then filter

                /**
                 * Filter output based on whether image is official
                 */
                public val isOfficial: SkippableBuilder<Context.() -> BooleanValue, Boolean, Unit>
                    by (BooleanBuilder.OnOff then { "is-official" to it.toString() }) then filter

                /**
                 * Pretty-print search using a Go template
                 */
                public val format: SkippableCapturingBuilderInterface<() -> String, String?> by builder<String>()

                /**
                 * Max number of search results
                 */
                public val limit: SkippableCapturingBuilderInterface<() -> Int, Int> by builder<Int>() default 25
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(::filter.evalAll(), ::format.eval(), ::limit.eval())
            }
        }
    }

    public companion object : BuilderTemplate<CommandContext, DockerSearchCommandLine>() {
        /**
         * Context for building a [DockerSearchCommandLine].
         */

        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {

            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
            public val term: SkippableCapturingBuilderInterface<() -> String, String?> by builder()
        }

        override fun BuildContext.build(): DockerSearchCommandLine = ::CommandContext {
            DockerSearchCommandLine(::options.eval(), ::term.eval())
        }
    }
}
