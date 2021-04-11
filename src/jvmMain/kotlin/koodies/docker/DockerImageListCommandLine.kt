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
import koodies.docker.DockerImage.ImageContext
import koodies.docker.DockerImageListCommandLine.Options.Companion.OptionsContext

/**
 * [DockerImageCommandLine] that lists locally available instances of [DockerImage].
 */
public open class DockerImageListCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val image: DockerImage?,
) : DockerImageCommandLine(
    dockerImageCommand = "ls",
    dockerImageArguments = buildArray {
        addAll(options)
        add("--no-trunc")
        add("--format")
        add("{{.Repository}}\t{{.Tag}}\t{{.Digest}}")
        image?.also { add(it.toString()) }
    },
) {
    public open class Options(
        /**
         * 	Show all images (default hides intermediate images)
         */
        public val all: Boolean = false,
    ) : List<String> by (buildList {
        if (all) add("--all")
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @DockerCommandLineDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * Show all images (default hides intermediate images)
                 */
                public val all: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
            }

            override fun BuildContext.build(): Options = Companion::OptionsContext {
                Options(::all.eval())
            }
        }
    }

    public companion object : BuilderTemplate<Companion.CommandContext, DockerImageListCommandLine>() {
        /**
         * Context for building a [DockerImageListCommandLine].
         */
        @DockerCommandLineDsl
        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
            public val image: SkippableCapturingBuilderInterface<ImageContext.() -> DockerImage, DockerImage?> by DockerImage
        }

        override fun BuildContext.build(): DockerImageListCommandLine = Companion::CommandContext {
            DockerImageListCommandLine(::options.evalOrDefault { Options() }, ::image.evalOrNull())
        }
    }
}
