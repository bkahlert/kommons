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
import koodies.concurrent.process.errors
import koodies.docker.DockerRemoveCommandLine.Companion.CommandContext
import koodies.docker.DockerRemoveCommandLine.Options.Companion.OptionsContext
import koodies.logging.RenderingLogger
import koodies.text.Semantics.formattedAs

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
                public val link: SkippableCapturingBuilderInterface<() -> String, String?> by builder<String>()

                /**
                 * 	Remove anonymous volumes associated with the container
                 */
                public val volumes: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>> by listBuilder()
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


private fun ManagedProcess.dockerDaemonParse(): Boolean = errors().let {
    it.isBlank()
        || it.contains("no such container", ignoreCase = true)
        && !it.contains("cannot remove a running container", ignoreCase = true)
}


/**
 * Removes `this` [DockerContainer] from the locally stored containers using the
 * [DockerRemoveCommandLine.Options] built with the given [OptionsContext] [Init].
 * and prints the [DockerCommandLine]'s execution to [System.out].
 */
public val DockerContainer.remove: (Init<OptionsContext>) -> Boolean
    get() = {
        val dockerRemoveCommandLine = DockerRemoveCommandLine {
            options(it)
            containers by listOf(this@remove.name)
        }
        val forcefully = if (dockerRemoveCommandLine.options.force) " forcefully".formattedAs.warning else ""
        dockerRemoveCommandLine.execute {
            summary("Removing$forcefully ${this@remove.formattedAs.input}")
            null
        }.dockerDaemonParse()
    }

/**
 * Removes `this` [DockerContainer] from the locally stored containers using the
 * [DockerRemoveCommandLine.Options] built with the given [OptionsContext] [Init].
 * and logs the [DockerCommandLine]'s execution using `this` [RenderingLogger].
 */
public val RenderingLogger?.remove: DockerContainer.(Init<OptionsContext>) -> Boolean
    get() = {
        val thisContainer: DockerContainer = this
        val dockerRemoveCommandLine = DockerRemoveCommandLine {
            options(it)
            containers by listOf(thisContainer.name)
        }
        val forcefully = if (dockerRemoveCommandLine.options.force) " forcefully".formattedAs.warning else ""
        dockerRemoveCommandLine.execute {
            summary("Removing$forcefully ${thisContainer.formattedAs.input}")
            null
        }.dockerDaemonParse()
    }
