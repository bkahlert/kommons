package koodies.concurrent


import koodies.builder.ArrayBuilder
import koodies.builder.Builder
import koodies.builder.BuildingContext
import koodies.builder.HigherOrderBuilder
import koodies.builder.Init
import koodies.builder.ListBuilder
import koodies.builder.ListBuilder.Companion.buildList
import koodies.docker.DockerCommandLine
import kotlin.reflect.KClass

//interface BuildingContext2<BC : BuildingContext<BC, T>, T> : Builder<BC, T>

inline fun <reified BC : BuildingContext<BC, T>, reified T> KClass<BC>.xxxxxxx(): BC {
    return this.java.constructors.filter { it.parameterCount == 0 }.firstOrNull()?.newInstance()?.let { it as BC } ?: error("No zero-arg constructor found.")
}

inline val <reified BC : BuildingContext<BC, T>, reified T> Pair<BC, T>.buildxx get() = BC::class


val y = DockerStopCommandLine { options {} }

object a1 : Z<DockerStopCommandLineBuilder, DockerStopCommandLine>({ DockerStopCommandLineBuilder() })

val a11 = a1.build { options {} }
val a12 = a1 { options {} }


inline operator fun <reified ZZZ : Z<BC, T>, reified BC : BuildingContext<BC, T>, reified T> ZZZ.invoke(noinline init: Init<BC>): T {
    return build(init)
}

open class Z<BC : BuildingContext<BC, T>, T>(protected val buildingContextProvider: () -> BC) {
    //    val build by xx
    fun build(init: Init<BC>): T {
        return Builder.build(init, buildingContextProvider)
    }
}

val z = DockerStopCommandLine { options {} }


/**
 * [DockerCommandLine] that stops the specified [containers] using the specified [options].
 */
open class DockerStopCommandLine(
    /**
     * Options that specify how this command line is run.
     */
    val options: DockerStopCommandLineOptions,
    val containers: List<String>,
) : DockerCommandLine(
    dockerCommand = "stop",
    arguments = ArrayBuilder.buildArray {
        +options
        +containers
    },
) {
    constructor(vararg containers: String) : this(DockerStopCommandLineOptions(), containers.toList())

    companion object : Z<DockerStopCommandLineBuilder, DockerStopCommandLine>({ DockerStopCommandLineBuilder() })
//, X<DockerStopCommandLineBuilder, DockerStopCommandLine> by { DockerStopCommandLineBuilder() }.c()
}

open class DockerStopCommandLineBuilder : HigherOrderBuilder<DockerStopCommandLineBuilder, DockerStopCommandLine>({
    DockerStopCommandLine(this::options.accessValue() ?: DockerStopCommandLineOptions(), this::containers.accessValue() ?: emptyList())
}) {
    val options by building { DockerStopCommandLineOptionsBuilder() }
    val containers by building { ListBuilder<String>() }
}

open class DockerStopCommandLineOptions(
    /**
     * 	Seconds to wait for stop before killing it
     */
    val time: Int? = null,
) : List<String> by (buildList {
    time?.also { +"--time" + "$time" }
})

open class DockerStopCommandLineOptionsBuilder : HigherOrderBuilder<DockerStopCommandLineOptionsBuilder, DockerStopCommandLineOptions>({
    DockerStopCommandLineOptions(::time.accessValue())
}) {
    /**
     * 	Seconds to wait for stop before killing it
     */
    val time by providing<Int?>()
}
