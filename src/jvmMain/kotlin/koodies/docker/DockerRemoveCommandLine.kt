package koodies.docker

import koodies.builder.ArrayBuilder
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.docker.DockerRemoveCommandLine.Companion.RemoveContext
import koodies.docker.DockerRemoveCommandLine.Options.Companion.RemoveOptionsContext

/**
 * [DockerCommandLine] that removes the specified [containers] using the specified [options].
 */
open class DockerRemoveCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    val options: Options,
    val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "rm",
    arguments = ArrayBuilder.buildArray {
        addAll(options)
        addAll(containers)
    },
) {
    open class Options(
        /**
         * 	Force the removal of a running container (uses SIGKILL)
         */
        val force: Boolean = false,
        /**
         * 	Remove the specified link
         */
        val link: String? = null,
        /**
         * 	Remove anonymous volumes associated with the container
         */
        val volumes: List<String> = emptyList(),
    ) : List<String> by (buildList {
        if (force) add("--force")
        link?.also { add("--link", it) }
        volumes.forEach { add("--volumes", it) }
    }) {
        companion object : BuilderTemplate<RemoveOptionsContext, Options>() {
            @DockerCommandLineDsl
            class RemoveOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                /**
                 * 	Force the removal of a running container (uses SIGKILL)
                 */
                val force by YesNo default false

                /**
                 * 	Remove the specified link
                 */
                val link by builder<String>()

                /**
                 * 	Remove anonymous volumes associated with the container
                 */
                val volumes by listBuilder<String>()
            }

            override fun BuildContext.build() = ::RemoveOptionsContext {
                val link1: String? = ::link.eval<String?>()
                Options(::force.eval(), link1, ::volumes.eval())
            }
        }
    }

    companion object : BuilderTemplate<RemoveContext, DockerRemoveCommandLine>() {
        /**
         * Context for building a [DockerRemoveCommandLine].
         */
        @DockerCommandLineDsl
        class RemoveContext(override val captures: CapturesMap) : CapturingContext() {
            val options by Options
            val containers by ListBuilder<String>()
        }

        override fun BuildContext.build() = ::RemoveContext {
            DockerRemoveCommandLine(
                ::options.evalOrDefault { Options() },
                ::containers.eval(),
            )
        }
    }
}

