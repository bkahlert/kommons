package koodies.docker

import koodies.Either
import koodies.Either.Left
import koodies.Either.Right
import koodies.collections.head
import koodies.collections.tail
import koodies.concurrent.process.IO.META.STARTING
import koodies.concurrent.process.IO.OUT
import koodies.docker.DockerExitStateHandler.Failure
import koodies.exec.Exec
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.Fatal
import koodies.exec.Process.ExitState.Success
import koodies.or
import koodies.text.takeUnlessBlank

public const val NONE: String = "<none>"

public inline fun <reified T> Exec.dockerDaemonParse(
    numColumns: Int,
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
                .map { it.unformatted }
                .map { it.split("\t") }
                .filter { it.size == numColumns }
                .map { it.map { field -> if (field == NONE) "" else field } }
                .mapNotNull { columns ->
                    kotlin.runCatching { transform(columns) }.recover {
                        throw IllegalStateException("Error parsing $columns", it)
                    }.getOrThrow()
                }.toList())
        }
    }
}

public inline class ColumnParseHelper(public val process: Exec) {
    public inline fun <reified T> columns(num: Int, crossinline lineParser: (List<String>) -> T?): Either<List<T>, Failure> =
        process.dockerDaemonParse(num) { lineParser(it) }
}

public inline val Exec.parse: ColumnParseHelper get() = ColumnParseHelper(this)

public fun Exec.parseImages(): List<DockerImage> {
    return parse.columns(3) { (repoAndPath, tag, digest) ->
        val (repository, path) = repoAndPath.split("/").let { it.head to it.tail }
        repository.takeUnlessBlank()?.let { repo ->
            DockerImage(repo, path, tag.takeUnlessBlank(), digest.takeUnlessBlank())
        }
    }.or { emptyList() }
}
