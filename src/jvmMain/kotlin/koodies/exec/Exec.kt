package koodies.exec

import koodies.collections.synchronizedListOf
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.META
import koodies.concurrent.process.IO.META.DUMP
import koodies.concurrent.process.IOSequence
import koodies.exception.dump
import koodies.exec.Process.ExitState
import koodies.text.LineSeparators.LF
import java.nio.file.Path

/**
 * A [Process] with advanced features like
 * the possibility to access a copy of the process's [IO].
 */
public interface Exec : Process {

    public companion object {

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
 * A callback that is invoked the moment an [Exec] terminated.
 */
public typealias ExecTerminationCallback = (Throwable?) -> Unit

/**
 * In compliance to [Process.outputStream] this type of stream
 * consists of [META] about a [Process].
 */
public class MetaStream(vararg listeners: (META) -> Unit) {
    private val history: MutableList<META> = synchronizedListOf()
    private val listeners: MutableList<(META) -> Unit> = synchronizedListOf(*listeners)

    public fun subscribe(listener: (META) -> Unit) {
        history.forEach { listener(it) }
        listeners.add(listener)
    }

    public fun emit(meta: META) {
        history.add(meta)
        listeners.forEach { it(meta) }
    }
}
