package koodies.docker

import koodies.builder.BuilderTemplate
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerInfoCommandLine.Options.Companion.OptionsContext

/**
 * [DockerCommandLine] that displays system wide information regarding the Docker installation.
 */
public open class DockerInfoCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
) : DockerCommandLine(
    dockerCommand = "info",
    arguments = buildArray {
        addAll(options)
    },
) {
    public open class Options(
        /**
         * Format the output using the given Go template.
         */
        public val format: String? = null,
    ) : List<String> by (buildList {
        format?.also {
            add("--format")
            add(it)
        }
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @DockerCommandLineDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {

                /**
                 * Format the output using the given Go template.
                 */
                public val format: SkippableCapturingBuilderInterface<() -> String, String?> by builder()
            }

            override fun BuildContext.build(): Options = Companion::OptionsContext {
                Options(::format.evalOrNull())
            }
        }
    }

    public companion object : BuilderTemplate<Companion.CommandContext, DockerInfoCommandLine>() {
        /**
         * Context for building a [DockerInfoCommandLine].
         */
        @DockerCommandLineDsl
        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
        }

        override fun BuildContext.build(): DockerInfoCommandLine = Companion::CommandContext {
            DockerInfoCommandLine(::options.evalOrDefault { Options() })
        }
    }
}
