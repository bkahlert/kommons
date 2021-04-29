package koodies.exec

import koodies.Either
import koodies.Either.Left
import koodies.Either.Right
import koodies.collections.synchronizedListOf
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.META
import koodies.concurrent.process.IO.META.DUMP
import koodies.concurrent.process.IO.META.FILE
import koodies.concurrent.process.IO.META.STARTING
import koodies.concurrent.process.IO.META.TERMINATED
import koodies.concurrent.process.IO.OUT
import koodies.concurrent.process.IOSequence
import koodies.exception.dump
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.exec.Process.ExitState.Failure
import koodies.exec.Process.ExitState.Fatal
import koodies.exec.Process.ExitState.Success
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs
import java.nio.file.Path

/**
 * A [Process] with advanced features like
 * the possibility to access a copy of the process's [IO].
 */
public interface Exec : Process {

    public companion object {

        /**
         * Returns an [ExitStateHandler] that interprets `this` [Exec]
         * once terminated as a [Success] if it exits with code 0
         * and as a [Failure] otherwise.
         */
        public fun Exec.fallbackExitStateHandler(relevantFiles: List<Path>): ExitStateHandler = ExitStateHandler { terminated ->
            if (terminated.exitCode == 0) {
                metaStream.emit(TERMINATED(this))
                Success(terminated.pid, terminated.io)
            } else {
                val dump = createDump(
                    "Process ${terminated.pid.formattedAs.input} terminated with exit code ${terminated.exitCode.formattedAs.input}",
                    *relevantFiles.map { FILE(it).formatted }.toTypedArray()
                )
                Failure(terminated.exitCode, terminated.pid, relevantFiles.map { it.toUri() }, dump, terminated.io)
            }
        }

        /**
         * Dumps the [IO] of `this` [Exec] individualized with the given [errorMessage]
         * to the process's [workingDirectory] and returns the same dump as a string.
         *
         * The given error messages are concatenated with a line break.
         */
        public fun Exec.createDump(vararg errorMessage: String): String {
            metaStream.emit(META typed errorMessage.joinToString(LF))
            return workingDirectory.dump(null) { io.ansiKept }.also { dump -> metaStream.emit(DUMP(dump)) }
        }
    }

    /**
     * Contains the so far logged I/O of this process.
     */
    public val io: IOSequence<IO>

    /**
     * The working directory of this process.
     */
    public val workingDirectory: Path

    override fun start(): Exec

    /**
     * Registers the given [callback] in a thread-safe manner
     * to be called before the process termination is handled.
     */
    public fun addPreTerminationCallback(callback: Exec.() -> Unit): Exec

    /**
     * Registers the given [callback] in a thread-safe manner
     * to be called after the process termination is handled.
     */
    public fun addPostTerminationCallback(callback: Exec.(ExitState) -> Unit): Exec
}

/**
 * Factory capable of creating an [Exec] from a [CommandLine].
 */
public fun interface ExecFactory<out E : Exec> {

    /**
     * Creates a [Exec] to run this executable with the specified [execTerminationCallback]
     * that is called the moment the [Exec] terminated—no matter if the [Exec] succeeds or fails.
     */
    public fun toProcess(commandLine: CommandLine, execTerminationCallback: ExecTerminationCallback?): E

    public companion object {

        /**
         * Factory for [Exec] instances based on [Process].
         */
        public val NATIVE: ExecFactory<Exec> = ExecFactory { commandLine, execTerminationCallback ->
            JavaExec(commandLine, null, execTerminationCallback)
        }
    }
}

/**
 * A callback that is invoked the moment an [Exec] terminated.
 */
public typealias ExecTerminationCallback = (Throwable?) -> Unit

/**
 * In compliance to [Process.outputStream] this type of stream
 * consists of [META] about an [Exec].
 */
public class MetaStream(vararg listeners: (META) -> Unit) {
    private val history: MutableList<META> = synchronizedListOf()
    private val listeners: MutableList<(META) -> Unit> = synchronizedListOf(*listeners)

    /**
     * Subscribes the given [listener] to this meta stream, that is,
     * already emitted and future messages are sent to the [listener].
     */
    public fun subscribe(listener: (META) -> Unit) {
        history.forEach { listener(it) }
        listeners.add(listener)
    }

    /**
     * Exits the given [message] to all subscribed [listeners].
     */
    public fun emit(message: META) {
        history.add(message)
        listeners.forEach { it(message) }
    }
}

/**
 * Contains a columns-based parser that can be used to
 * map the output lines of `this` [Exec].
 */
public val Exec.parse: ColumnParser get() = ColumnParser(this)

/**
 * Column-based parser that maps output line of an [Exec].
 */
public inline class ColumnParser(
    /**
     * [Exec] to parse the output of.
     */
    public val exec: Exec,
) {

    /**
     * If [exec] terminated successfully, this method
     * returns all non-`null` elements created by passing
     * each line split into [num] tab-separated columns to
     * the given [lineParser].
     *
     * Otherwise the [Failure] [Exec.exitState] is returned.
     */
    public inline fun <T : Any, reified E : Failure> columns(num: Int, crossinline lineParser: (List<String>) -> T?): Either<List<T>, E> =
        when (val exitState = exec.waitFor()) {
            is Fatal -> {
                val commandLine = exec.io.filterIsInstance<STARTING>().singleOrNull()?.run { commandLine.summary } ?: "docker command"
                error("Error running $commandLine: $exitState")
            }

            is E -> Right(exitState)
            is Failure -> error("Unmapped ${E::class.simpleName} ${exitState::class.simpleName}: $exitState")

            is Success -> {
                Left(exitState.io.asSequence()
                    .filterIsInstance<OUT>()
                    .map { it.unformatted }
                    .map { it.split("\t") }
                    .filter { it.size == num }
                    .map { it.map { field -> if (field == "<none>") "" else field } }
                    .mapNotNull { columns ->
                        kotlin.runCatching { lineParser(columns) }.recover {
                            throw IllegalStateException("Error parsing $columns", it)
                        }.getOrThrow()
                    }.toList())
            }
        }
}
