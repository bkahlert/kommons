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
import koodies.docker.DockerImagePullCommandLine.Companion.CommandContext
import koodies.docker.DockerImagePullCommandLine.Options.Companion.OptionsContext

/**
 * [DockerCommandLine] that pulls the specified [image] using the specified [options].
 */
public open class DockerImagePullCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val image: DockerImage,
) : DockerImageCommandLine(
    dockerImageCommand = "pull",
    dockerImageArguments = buildArray {
        addAll(options)
        add(image.toString())
    },
) {

    public open class Options(
        /**
         * Download all tagged images in the repository
         */
        public val allTags: Boolean = false,
    ) : List<String> by (buildList {
        if (allTags) add("--all-tags")
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {

            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * Download all tagged images in the repository
                 */
                public val allTags: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(::allTags.eval())
            }
        }
    }

    public companion object : BuilderTemplate<CommandContext, DockerImagePullCommandLine>() {
        /**
         * Context for building a [DockerImagePullCommandLine].
         */

        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
            public val image: SkippableCapturingBuilderInterface<DockerImageInit, DockerImage?> by DockerImage
        }

        override fun BuildContext.build(): DockerImagePullCommandLine = ::CommandContext {
            DockerImagePullCommandLine(::options.evalOrDefault { Options() }, ::image.eval())
        }
    }
}
