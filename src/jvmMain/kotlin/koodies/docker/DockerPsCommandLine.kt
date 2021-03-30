package koodies.docker

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BooleanBuilder.YesNo.Context
import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.PairBuilder
import koodies.builder.SkippableBuilder
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.execute
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.output
import koodies.docker.DockerPsCommandLine.Options.Companion.OptionsContext
import koodies.logging.RenderingLogger
import koodies.text.Semantics.formattedAs
import koodies.text.quoted

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
        add("{{.Names}}".quoted)
    },
) {
    public open class Options(
        /**
         * 	Show all images (default hides intermediate images)
         */
        public val all: Boolean = false,

        /**
         * Filter output based on conditions provided
         */
        public val filters: List<Pair<String, String>> = emptyList(),
    ) : List<String> by (buildList {
        if (all) add("--all")
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @DockerCommandLineDsl
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
                 * 	Filter output based on container’s exact name
                 */
                public val exactName: SkippableBuilder<() -> String, String, Pair<String, String>>
                    by builder<String>() then { "name" to "name=^$it${'$'}" }
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
        @DockerCommandLineDsl
        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
        }

        override fun BuildContext.build(): DockerPsCommandLine = Companion::CommandContext {
            DockerPsCommandLine(::options.evalOrDefault { Options() })
        }
    }
}

private fun ManagedProcess.parseContainers(): List<DockerContainer> = output().lines().filter { it.isNotBlank() }
    .map { DockerContainer(it) }

/**
 * Lists locally available instances of [DockerContainer] using the
 * [DockerPsCommandLine.Options] built with the given [OptionsContext] [Init].
 * and prints the [DockerCommandLine]'s execution to [System.out].
 */
@Suppress("unused")
public val Docker.ps: (Init<OptionsContext>) -> List<DockerContainer>
    get() = {
        DockerPsCommandLine {
            options(it)
        }.execute {
            noDetails("Listing containers")
            null
        }.parseContainers()
    }

/**
 * Lists locally available instances of [DockerContainer] using the
 * [DockerPsCommandLine.Options] built with the given [OptionsContext] [Init]
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.ps: Docker.(Init<OptionsContext>) -> List<DockerContainer>
    get() = {
        DockerPsCommandLine {
            options(it)
        }.execute {
            noDetails("Listing containers")
            null
        }.parseContainers()
    }

/**
 * Checks if `this` [DockerContainer] is running using the
 * [DockerPsCommandLine.Options] built with the given [OptionsContext] [Init].
 * and prints the [DockerCommandLine]'s execution to [System.out].
 */
public val DockerContainer.isRunning: () -> Boolean
    get() = {
        DockerPsCommandLine {
            options { exactName by this@isRunning.name }
        }.execute {
            noDetails("Checking if ${this@isRunning.formattedAs.input} is running")
            null
        }.parseContainers().isNotEmpty()
    }

/**
 * Lists locally available instances `this` [DockerImage] using the
 * [DockerPsCommandLine.Options] built with the given [OptionsContext] [Init]
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.isRunning: DockerContainer.() -> Boolean
    get() = {
        val thisContainer = this
        DockerPsCommandLine {
            options { exactName by thisContainer.name }
        }.execute {
            noDetails("Checking if ${thisContainer.formattedAs.input} is running")
            null
        }.parseContainers().isNotEmpty()
    }
