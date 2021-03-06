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
import koodies.docker.DockerRemoveCommandLine.Companion.RemoveContext
import koodies.docker.DockerRemoveCommandLine.Options.Companion.RemoveOptionsContext

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
    arguments = ArrayBuilder.buildArray {
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
         * 	Remove the specified link
         */
        public val link: String? = null,
        /**
         * 	Remove anonymous volumes associated with the container
         */
        public val volumes: List<String> = emptyList(),
    ) : List<String> by (buildList {
        if (force) add("--force")
        link?.also { add("--link", it) }
        volumes.forEach { add("--volumes", it) }
    }) {
        public companion object : BuilderTemplate<RemoveOptionsContext, Options>() {
            @DockerCommandLineDsl
            public class RemoveOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * 	Force the removal of a running container (uses SIGKILL)
                 */
                public val force: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false

                /**
                 * 	Remove the specified link
                 */
                public val link: SkippableCapturingBuilderInterface<() -> String, String?> by builder<String>()

                /**
                 * 	Remove anonymous volumes associated with the container
                 */
                public val volumes: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>> by listBuilder()
            }

            override fun BuildContext.build(): Options = ::RemoveOptionsContext {
                val link1: String? = ::link.eval<String?>()
                Options(::force.eval(), link1, ::volumes.eval())
            }
        }
    }

    public companion object : BuilderTemplate<RemoveContext, DockerRemoveCommandLine>() {
        /**
         * Context for building a [DockerRemoveCommandLine].
         */
        @DockerCommandLineDsl
        public class RemoveContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<RemoveOptionsContext.() -> Unit, Options?> by Options
            public val containers: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
        }

        override fun BuildContext.build(): DockerRemoveCommandLine = ::RemoveContext {
            DockerRemoveCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }
}

