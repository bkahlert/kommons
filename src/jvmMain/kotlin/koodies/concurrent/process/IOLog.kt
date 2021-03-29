package koodies.concurrent.process

import koodies.asString
import koodies.concurrent.process.IO.META
import koodies.exception.persistDump
import koodies.io.ByteArrayOutputStream
import koodies.text.INTERMEDIARY_LINE_PATTERN
import koodies.text.LineSeparators
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.Semantics.OK
import koodies.text.joinLinesToString
import koodies.text.truncate
import koodies.time.busyWait
import koodies.unit.Size
import koodies.unit.bytes
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.seconds

/**
 * An I/O log can be used to log what a [ManagedProcess] received and produces as data.
 *
 * In order to log I/O only [add] must be called.
 */
public class IOLog {

    private val lock = ReentrantLock()

    /**
     * Contains the currently logged I/O of the corresponding [ManagedProcess].
     *
     * ***Note:** Only complete lines can be accessed to avoid
     * non-corrupted data (e.g. split characters).*
     */
    public val logged: List<IO> get() = lock.withLock { log.toList() }

    /**
     * Contains the currently logged I/O of the corresponding [ManagedProcess].
     *
     * ***Note:** Only complete lines can be accessed to avoid
     * non-corrupted data (e.g. split characters).*
     */
    public inline fun <reified T : IO> logged(removeEscapeSequences: Boolean = true): String =
        logged.filterIsInstance<T>().joinToString(LF) { if (removeEscapeSequences) it.unformatted else it.formatted }

    /**
     * Contains the currently logged I/O. See [logged] for more details.
     */
    private val log = mutableListOf<IO>()

    /**
     * Adds the specified [META] [IO] to this log.
     */
    public operator fun plus(meta: META) {
        lock.withLock { log.add(meta) }
    }

    /**
     * Assembles [IO.IN] chunks and adds successfully reconstructed ones
     * to this log.
     */
    public val input: IOAssembler = IOAssembler { lines ->
        lock.withLock {
            lines.forEach { log.add(IO.IN typed it) }
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
     * Returns a dump of the logged I/O log.
     */
    public fun dump(): String {
        2.seconds.busyWait()
        return logged.joinLinesToString { it.formatted }
    }

    /**
     * Dumps the logged I/O log in the specified [directory] using the name scheme `koodies.process.{PID}.{RANDOM}.log".
     */
    public fun dump(directory: Path, pid: Int): Map<String, Path> = persistDump(directory.resolve("koodies.process.$pid.log")) { dump() }

    override fun toString(): String = asString {
        OK to logged.map { it.truncate() }.joinToString()
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

    override fun toString(): String = asString(::lock, ::incomplete)
}

/**
 * Contains the currently logged I/O of this [ManagedProcess].
 *
 * **Important:** Only complete lines can be accessed as this is considered to be the only safe way
 * to have non-corrupted data (e.g. split characters).
 */
public inline fun <reified T : IO> ManagedProcess.logged(): String = ioLog.logged<T>()

/**
 * Contains the currently logged I/O of this [ManagedProcess].
 *
 * **Important:** Only complete lines can be accessed as this is considered to be the only safe way
 * to have non-corrupted data (e.g. split characters).
 */
public val ManagedProcess.logged: String get() = ioLog.logged<IO>()

/**
 * Returns (and possibly blocks until finished) the output of `this` [ManagedProcess].
 *
 * This method is idempotent.
 */
public fun ManagedProcess.output(): String = run {
    process({ sync }, Processors.noopProcessor())
    ioLog.logged.filterIsInstance<IO.OUT>().joinToString(LF) { it.unformatted }
}
