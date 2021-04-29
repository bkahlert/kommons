package koodies.docker

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BooleanBuilder.YesNo.Context
import koodies.builder.BuilderTemplate
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.DockerImageRemoveCommandLine.Options.Companion.OptionsContext

/**
 * [DockerImageCommandLine] that removes the specified [images] using the specified [options].
 */
public open class DockerImageRemoveCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val images: List<DockerImage>,
) : DockerImageCommandLine(
    dockerImageCommand = "rm",
    dockerImageArguments = buildArray {
        addAll(options)
        images.forEach { add(it.toString()) }
    },
) {
    public open class Options(
        /**
         * Force removal of the image
         */
        public val force: Boolean = false,
    ) : List<String> by (buildList {
        if (force) add("--force")
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @DockerCommandLineDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * Force removal of the image
                 */
                public val force: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
            }

            override fun BuildContext.build(): Options = Companion::OptionsContext {
                Options(::force.eval())
            }
        }
    }

    public companion object : BuilderTemplate<Companion.CommandContext, DockerImageRemoveCommandLine>() {
        /**
         * Context for building a [DockerImagePullCommandLine].
         */
        @DockerCommandLineDsl
        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
            public val image: SkippableCapturingBuilderInterface<DockerImageInit, DockerImage?> by DockerImage
        }

        override fun BuildContext.build(): DockerImageRemoveCommandLine = Companion::CommandContext {
            DockerImageRemoveCommandLine(::options.evalOrDefault { Options() }, ::image.evalAll())
        }
    }
}
