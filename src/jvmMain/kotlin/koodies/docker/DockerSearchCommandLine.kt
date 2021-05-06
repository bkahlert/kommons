package koodies.docker

import koodies.asString
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
import koodies.docker.DockerExitStateHandler.Failure
import koodies.docker.DockerSearchCommandLine.Companion.CommandContext
import koodies.docker.DockerSearchCommandLine.Options.Companion.OptionsContext
import koodies.exec.parse
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.RenderingLogger
import koodies.or
import koodies.text.Semantics.formattedAs

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
        add("--no-trunc")
        add("--format")
        add("{{.Name}}\t{{.Description}}\t{{.StarCount}}\t{{.IsOfficial}}\t{{.IsAutomated}}")
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
         * Max number of search results
         */
        public val limit: Int? = 25,
    ) : List<String> by (buildList {
        filters.forEach { (key, value) -> +"--filter" + "$key=$value" }
        limit.also { +"--limit" + "$limit" }
    }) {
        override fun toString(): String {
            return asString(::filters, ::limit)
        }

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
                 * Max number of search results
                 */
                public val limit: SkippableCapturingBuilderInterface<() -> Int, Int> by builder<Int>() default 25
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(::filter.evalAll(), ::limit.eval())
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

        /**
         * Searches for at most [limit] Docker images matching [term],
         * having at least the given number of [stars] and being [automated] and/or [official].
         */
        public fun search(
            term: String,
            stars: Int? = null,
            automated: Boolean? = null,
            official: Boolean? = null,
            limit: Int = 100,
            logger: RenderingLogger = BACKGROUND,
        ): List<DockerSeachResult> {
            val commandLine = DockerSearchCommandLine {
                options {
                    stars?.also { this.stars by it }
                    automated?.also { isAutomated by it }
                    official?.also { isOfficial by official }
                    this.limit by limit
                }
                this.term by term
            }

            return commandLine.exec.logging(logger) {
                errorsOnly("Searching up to ${limit.formattedAs.input} images with filters ${commandLine.options.filters.formattedAs.input}")
            }.parse.columns<DockerSeachResult, Failure>(5) { (name, description, starCount, isOfficial, isAutomated) ->
                DockerSeachResult(DockerImage { name }, description, starCount.toIntOrNull() ?: 0, isOfficial.isNotBlank(), isAutomated.isNotBlank())
            } or { emptyList() }
        }
    }

    public data class DockerSeachResult(
        public val image: DockerImage,
        public val description: String,
        public val stars: Int,
        public val official: Boolean,
        public val automated: Boolean,
    )
}
