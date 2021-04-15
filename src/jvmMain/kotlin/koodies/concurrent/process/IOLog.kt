package koodies.concurrent.process

import koodies.asString
import koodies.exception.persistDump
import koodies.exec.Exec
import koodies.io.ByteArrayOutputStream
import koodies.text.INTERMEDIARY_LINE_PATTERN
import koodies.text.LineSeparators
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.Semantics.Symbols
import koodies.text.truncate
import koodies.unit.Size
import koodies.unit.bytes
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An I/O log can be used to log what a [Exec] received and produces as data.
 *
 * In order to log I/O only [plus] must be called.
 */
public class IOLog : Sequence<IO> {

    private val lock = ReentrantLock()

    /**
     * Returns an [Iterator] that iterates over all currently logged I/O
     * of the corresponding [Exec].
     */
    override fun iterator(): Iterator<IO> =
        lock.withLock { log.toList() }.iterator()

    /**
     * Contains the currently logged I/O. See [copy] for more details.
     */
    private val log = mutableListOf<IO>()

    /**
     * Adds the specified [META] [IO] to this log.
     */
    public operator fun plus(meta: IO.META) {
        lock.withLock { log.add(meta) }
    }

    /**
     * Assembles [IO.INPUT] chunks and adds successfully reconstructed ones
     * to this log.
     */
    public val input: IOAssembler = IOAssembler { lines ->
        lock.withLock {
            lines.forEach { log.add(IO.INPUT typed it) }
        }
    }

    /**
     * Assembles [IO.OUT] chunks and adds successfully reconstructed ones
     * to this log.
     */
    public val out: IOAssembler = IOAssembler { lines ->
        lock.withLock {
            lines.forEach { log.add(IO.OUT typed it) }
        }
    }

    /**
     * Assembles [IO.ERR] chunks and adds successfully reconstructed ones
     * to this log.
     */
    public val err: IOAssembler = IOAssembler { lines ->
        lock.withLock {
            lines.forEach { log.add(IO.ERR typed it) }
        }
    }

    /**
     * For each type of [IO] all so far saved incomplete strings are treated
     * as if they were complete, that is, appended to the list of completed strings.
     */
    public fun flush(): Unit {
        input.flush()
        out.flush()
        err.flush()
    }

    /**
     * Returns a dump of the logged I/O log.
     */
    public fun dump(): String = merge<IO>(removeEscapeSequences = false)

    /**
     * Dumps the logged I/O log in the specified [directory] using the name scheme `koodies.process.{PID}.{RANDOM}.log".
     */
    public fun dump(directory: Path, pid: Int): Map<String, Path> = persistDump(directory.resolve("koodies.process.$pid.log")) { dump() }

    override fun toString(): String = asString {
        Symbols.OK to joinToString { it.truncate() }
        "OUT" to out.incompleteBytes
        "ERR" to err.incompleteBytes
    }
}

public class IOAssembler(public val lineCompletedCallback: (List<String>) -> Unit) {

    private val lock = ReentrantLock()

    /**
     * Contains not yet fully logged I/O, that is, data not yet terminated by one of the [LineSeparators].
     */
    private val incomplete: ByteArrayOutputStream = ByteArrayOutputStream()

    public val incompleteBytes: Size get() = incomplete.size().bytes

    /**
     * Takes the given [bytes] and attempts to re-construct complete text lines
     * based on already stored bytes.
     */
    public operator fun plus(bytes: ByteArray): Unit {
        lock.withLock {
            incomplete.write(bytes)
            while (true) {
                val justCompletedLines = incomplete.hasCompletedLines()
                if (justCompletedLines != null) {
                    val completedLines = incomplete.removeCompletedLines()
                    lineCompletedCallback(completedLines)
                } else break
            }
        }
    }

    private fun ByteArrayOutputStream.hasCompletedLines(): String? =
        toString(Charsets.UTF_8).takeIf { it.matches(LineSeparators.INTERMEDIARY_LINE_PATTERN) }

    private fun ByteArrayOutputStream.removeCompletedLines(): List<String> {
        val readLines = toString(Charsets.UTF_8).lines()
        val readCompleteLines = readLines.dropLast(1)
        reset()
        write(readLines.last().toByteArray(Charsets.UTF_8))
        return readCompleteLines
    }

    public fun flush(): Unit {
        lock.withLock {
            val remainder = incomplete.toString(Charsets.UTF_8)
            incomplete.reset()
            if (remainder.isNotEmpty()) lineCompletedCallback(remainder.lines())
        }
    }

    override fun toString(): String = asString(::lock, ::incomplete)
}


/**
 * Filters this [IO] sequence by the specified type.
 *
 * By default [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) are removed.
 * Set [removeEscapeSequences] to `false` to keep escapes codes.
 */
public inline fun <reified T : IO> Sequence<IO>.merge(removeEscapeSequences: Boolean = true): String =
    filterIsInstance<T>().joinToString(LF) { if (removeEscapeSequences) it.unformatted else it.formatted }

/**
 * Filters this [IO] sequence using the given filter.
 *
 * By default [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) are removed.
 * Set [removeEscapeSequences] to `false` to keep escapes codes.
 */
public inline fun <reified T : IO> Iterable<T>.merge(removeEscapeSequences: Boolean = true, noinline filter: (T) -> Boolean = { true }): String =
    filter(filter).joinToString(LF) { if (removeEscapeSequences) it.unformatted else it.formatted }

/**
 * Convenience method to get the output of a process.
 *
 * - If the process was not started, it will be started.
 * - If the process is running, this method blocks until the process terminated.
 * - If the process already terminated, the recorded IO is returned.
 *
 * If nothing terribly goes wrong, all IO of type [IO.OUT] is returned.
 */
public fun Exec.output(): String = run {
    process({ sync }, Processors.noopProcessor())
    io.merge<IO.OUT>()
}

/**
 * Convenience method to get the output of a process, split the lines
 * and apply [transform] to each line. The lines [transform] maps to `null`
 * are filtered out.
 *
 * - If the process was not started, it will be started.
 * - If the process is running, this method blocks until the process terminated.
 * - If the process already terminated, the recorded IO is returned.
 *
 * If nothing terribly goes wrong, all IO of type [IO.OUT] is returned.
 */
public fun <T> Exec.output(transform: String.() -> T?): List<T> =
    output().lines(ignoreTrailingSeparator = true).mapNotNull { it.transform() }

/**
 * Convenience method to get the errors of a process.
 */
public fun Exec.errors(): String = run {
    process({ sync }, Processors.noopProcessor())
    io.merge<IO.ERR>()
}

/**
 * Convenience method to get the errors of a process.
 */
public fun <T> Exec.errors(transform: String.() -> T?): List<T> =
    errors().lines(ignoreTrailingSeparator = true).mapNotNull { it.transform() }
