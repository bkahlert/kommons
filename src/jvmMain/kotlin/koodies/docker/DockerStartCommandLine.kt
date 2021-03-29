package koodies.docker

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BooleanBuilder.YesNo.Context
import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.ListBuilder
import koodies.builder.buildArray
import koodies.builder.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.execute
import koodies.concurrent.process.ManagedProcess
import koodies.docker.DockerStartCommandLine.Companion.CommandContext
import koodies.docker.DockerStartCommandLine.Options.Companion.OptionsContext
import koodies.logging.RenderingLogger
import koodies.text.Semantics.formattedAs

/**
 * [DockerCommandLine] that starts the specified [containers] using the specified [options].
 */
public open class DockerStartCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    public val options: Options,
    public val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "start",
    arguments = buildArray {
        addAll(options)
        addAll(containers)
    },
) {
    public open class Options(
        /**
         * Attach STDOUT/STDERR and forward signals
         */
        public val attach: Boolean = true,
        /**
         * Attach container's STDIN
         */
        public val interactive: Boolean = false,
    ) : List<String> by (buildList {
        if (attach) +"--attach"
        if (interactive) +"--interactive"
    }) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @DockerCommandLineDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {

                /**
                 * Attach STDOUT/STDERR and forward signals
                 */
                public val attach: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default true

                /**
                 * Attach container's STDIN
                 */
                public val interactive: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
            }

            override fun BuildContext.build(): Options = ::OptionsContext {
                Options(::attach.evalOrDefault(true), ::interactive.evalOrDefault(false))
            }
        }
    }

    public companion object : BuilderTemplate<CommandContext, DockerStartCommandLine>() {
        /**
         * Context for building a [DockerStartCommandLine].
         */
        @DockerCommandLineDsl
        public class CommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, Options?> by Options
            public val containers: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
        }

        override fun BuildContext.build(): DockerStartCommandLine = ::CommandContext {
            DockerStartCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }
}

/**
 * Starts `this` [DockerContainer] from the locally stored containers using the
 * [DockerStartCommandLine.Options] built with the given [OptionsContext] [Init].
 * and prints the [DockerCommandLine]'s execution to [System.out].
 */
public val DockerContainer.start: (Init<OptionsContext>) -> ManagedProcess
    get() = {
        DockerStartCommandLine {
            options(it)
            containers by listOf(this@start.name)
        }.execute {
            summary("Startping ${this@start.formattedAs.input}")
            null
        }
    }

/**
 * Starts `this` [DockerContainer] from the locally stored containers using the
 * [DockerStartCommandLine.Options] built with the given [OptionsContext] [Init].
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.start: DockerContainer.(Init<OptionsContext>) -> ManagedProcess
    get() = {
        val thisContainer: DockerContainer = this
        DockerStartCommandLine {
            options(it)
            containers by listOf(thisContainer.name)
        }.execute {
            summary("Startping ${thisContainer.formattedAs.input}")
            null
        }
    }
