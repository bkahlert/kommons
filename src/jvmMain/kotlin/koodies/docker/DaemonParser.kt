package koodies.docker

import koodies.Either
import koodies.Either.Left
import koodies.Either.Right
import koodies.collections.head
import koodies.collections.tail
import koodies.concurrent.process.IO.META.STARTING
import koodies.concurrent.process.IO.OUT
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process.ExitState
import koodies.concurrent.process.Process.ExitState.Fatal
import koodies.concurrent.process.Process.ExitState.Success
import koodies.concurrent.process.io
import koodies.docker.DockerExitStateHandler.Failure
import koodies.or
import koodies.text.ANSI.ansiRemoved
import koodies.text.takeUnlessBlank

public const val NONE: String = "<none>"

public inline fun <reified T> ManagedProcess.dockerDaemonParse(
    numColumns: Int,
    removeAnsi: Boolean = true,
    columnSeparator: String = "\t",
    crossinline transform: (List<String>) -> T?,
): Either<List<T>, Failure> {
    return when (val exitState = waitFor()) {
        is Fatal -> {
            val commandLine = io.filterIsInstance<STARTING>().singleOrNull()?.run { commandLine.summary } ?: "docker command"
            error("Error running $commandLine: $exitState")
        }

        is Failure -> Right(exitState)
        is ExitState.Failure -> error("Unmapped ${Failure::class.simpleName} ${exitState::class.simpleName}: $exitState")

        is Success -> {
            Left(exitState.io.asSequence()
                .filterIsInstance<OUT>()
                .map { it.ansiRemoved }
                .map { it.split("\t") }
                .filter { it.size == numColumns }
                .map { it.map { field -> if (field == NONE) "" else field } }
                .mapNotNull { transform(it) }
                .toList())
        }
    }
}


//public inline fun <reified T> ManagedProcess.columns(num: Int, crossinline lineParser: (List<String>) -> T?): List<T> =
//    dockerDaemonParse(num) { lineParser(it) }
//private val pipeline: (Sequence<IO>) -> List<String> =
//    { it.filterIsInstance<OUT>().map { it.ansiRemoved }.map { it.split("\t") }.filter { it.size == 3 }.toList() }

public inline class ColumnParseHelper(public val process: ManagedProcess) {
    public inline fun <reified T> columns(num: Int, crossinline lineParser: (List<String>) -> T?): Either<List<T>, Failure> =
        process.dockerDaemonParse(num) { lineParser(it) }
}

public inline val ManagedProcess.parse: ColumnParseHelper get() = ColumnParseHelper(this)

public fun ManagedProcess.parseImages(): List<DockerImage> {
    return parse.columns(3) { (repoAndPath, tag, digest) ->
        val (repository, path) = repoAndPath.split("/").let { it.head to it.tail }
        repository.takeUnlessBlank()?.let { repo ->
            DockerImage(repo, path, tag.takeUnlessBlank(), digest.takeUnlessBlank())
        }
    }.or { emptyList() }
}
