package koodies.docker

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BooleanBuilder.YesNo.Context
import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.collections.head
import koodies.collections.tail
import koodies.concurrent.execute
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.output
import koodies.docker.DockerImage.ImageContext
import koodies.docker.DockerImageListCommandLine.Options.Companion.OptionsContext
import koodies.logging.RenderingLogger
import koodies.text.Semantics.formattedAs
import koodies.text.quoted
import koodies.text.takeUnlessBlank

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
        add("{{.Repository}}\t{{.Tag}}\t{{.Digest}}".quoted)
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

private fun ManagedProcess.parseImages(): List<DockerImage> = output().lines().filter { it.isNotBlank() }
    .mapNotNull { line ->
        val (repoAndPath, tag, digest) = line.replace("<none>", "").split("\t")
        val (repository, path) = repoAndPath.split("/").let { it.head to it.tail }
        repository.takeUnlessBlank()?.let { repo -> DockerImage(repo, path, tag.takeUnlessBlank(), digest.takeUnlessBlank()) }
    }

/**
 * Lists locally available instances of [DockerImage] using the
 * [DockerImageListCommandLine.Options] built with the given [OptionsContext] [Init].
 * and prints the [DockerCommandLine]'s execution to [System.out].
 */
public val DockerImage.Companion.list: (Init<OptionsContext>) -> List<DockerImage>
    get() = {
        DockerImageListCommandLine {
            options(it)
        }.execute {
            noDetails("Listing images")
            null
        }.parseImages()
    }

/**
 * Lists locally available instances of [DockerImage] using the
 * [DockerImageListCommandLine.Options] built with the given [OptionsContext] [Init]
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.list: DockerImage.Companion.(Init<OptionsContext>) -> List<DockerImage>
    get() = {
        DockerImageListCommandLine {
            options(it)
        }.execute {
            noDetails("Listing images")
            null
        }.parseImages()
    }

/**
 * Lists locally available instances of `this` [DockerImage] using the
 * [DockerImageListCommandLine.Options] built with the given [OptionsContext] [Init].
 * and prints the [DockerCommandLine]'s execution to [System.out].
 */
public val DockerImage.listImages: (Init<OptionsContext>) -> List<DockerImage>
    get() = {
        DockerImageListCommandLine {
            options(it)
            image by this@listImages
        }.execute {
            summary("Listing ${this@listImages.formattedAs.input} images")
            null
        }.parseImages()
    }

/**
 * Lists locally available instances `this` [DockerImage] using the
 * [DockerImageListCommandLine.Options] built with the given [OptionsContext] [Init]
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.listImages: DockerImage.(Init<OptionsContext>) -> List<DockerImage>
    get() = {
        val thisImage = this
        DockerImageListCommandLine {
            options(it)
            image by thisImage
        }.execute {
            summary("Listing ${thisImage.formattedAs.input} images")
            null
        }.parseImages()
    }

/**
 * Checks if `this` [DockerImage] is pulled
 * and prints the [DockerCommandLine]'s execution to [System.out].
 */
public val DockerImage.isPulled: () -> Boolean
    get() = {
        DockerImageListCommandLine {
            image by this@isPulled
        }.execute {
            summary("Checking if ${this@isPulled.formattedAs.input} is pulled")
            null
        }.parseImages().isNotEmpty()
    }

/**
 * Checks if `this` [DockerImage] is pulled
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.isPulled: DockerImage.() -> Boolean
    get() = {
        val thisImage = this
        DockerImageListCommandLine {
            image by thisImage
        }.execute {
            summary("Checking if ${thisImage.formattedAs.input} is pulled")
            null
        }.parseImages().isNotEmpty()
    }
