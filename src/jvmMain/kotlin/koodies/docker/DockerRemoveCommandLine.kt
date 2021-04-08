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
import koodies.docker.DockerRemoveCommandLine.Companion.CommandContext
import koodies.docker.DockerRemoveCommandLine.Options.Companion.OptionsContext

/**
 * [DockerCommandLine] that removes the specified [containers] using the specified [options].
 */
public open class DockerRemoveCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "rm",
    arguments = buildArray {
        addAll(options)
        addAll(containers)
    },
) {
    public open class Options(
        /**
         * 	Force the removal of a running container (uses SIGKILL)
         */
        public val force: Boolean = false,
        /**
         * 	Remove the specified link associated with the container.
         */
        public val link: Boolean = false,
        /**
         * 	Remove anonymous volumes associated with the container
         */
        public val volumes: Boolean = false,
    ) : List<String> by (buildList {
        if (force) add("--force")
        if (link) add("--link")
        if (volumes) add("--volumes")
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {

            @DockerCommandLineDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * 	Force the removal of a running container (uses SIGKILL)
                 */
                public val force: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false

                /**
                 * 	Remove the specified link
                 */
                public val link: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false

                /**
                 * 	Remove anonymous volumes associated with the container
                 */
                public val volumes: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(::force.eval(), ::link.eval(), ::volumes.eval())
            }
        }
    }

    public companion object : BuilderTemplate<CommandContext, DockerRemoveCommandLine>() {
        /**
         * Context for building a [DockerRemoveCommandLine].
         */
        @DockerCommandLineDsl
        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
            public val containers: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
        }

        override fun BuildContext.build(): DockerRemoveCommandLine = ::CommandContext {
            DockerRemoveCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }
}
