package koodies.exec

import koodies.collections.synchronizedListOf
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.META
import koodies.concurrent.process.IO.META.DUMP
import koodies.concurrent.process.merge
import koodies.exception.dump
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.text.LineSeparators.LF
import java.nio.file.Path

/**
 * A process that logs its own [IO].
 */
public interface Exec : Process {

    public companion object {

        public fun from(
            commandLine: CommandLine,
            exitStateHandler: ExitStateHandler? = null,
            execTerminationCallback: ExecTerminationCallback? = null,
        ): Exec = JavaExec(
            commandLine = commandLine,
            exitStateHandler = exitStateHandler,
            execTerminationCallback = execTerminationCallback)

        /**
         * Dumps the [IO] of [process] individualized with the given [errorMessage]
         * to the process's [workingDirectory] and returns the same dump as a string.
         *
         * The given error messages are concatenated with a line break.
         */
        public fun Exec.createDump(vararg errorMessage: String): String {
            metaStream.emit(META typed errorMessage.joinToString(LF))
            return workingDirectory.dump(null) { io.merge<IO>(removeEscapeSequences = false) }.also { dump -> metaStream.emit(DUMP(dump)) }
        }
    }

    /**
     * Contains the so far logged I/O of this process.
     */
    public val io: Sequence<IO>

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


public typealias ExecTerminationCallback = (Throwable?) -> Unit

// TODO
///**
// * Returns whether [start] was called.
// *
// * Contrary to [alive] this property will never return `false` once [start] was called.
// */
//public val Process.started: Boolean
//    get() = when (state) {
//        is Prepared -> false
//        is Running -> true
//        is Terminated -> true
//    }


public class MetaStream(vararg listeners: (META) -> Unit) {
    private val history: MutableList<META> = synchronizedListOf()
    private val listeners: MutableList<(META) -> Unit> = synchronizedListOf(*listeners)

    public fun subscribe(listener: (META) -> Unit): Unit {
        history.forEach { listener(it) }
        listeners.add(listener)
    }

    public fun emit(meta: META): Unit {
        history.add(meta)
        listeners.forEach { it(meta) }
    }
}
