package koodies.exec

import koodies.asString
import koodies.exception.persistDump
import koodies.exec.IO.Error
import koodies.exec.IO.Input
import koodies.exec.IO.Output
import koodies.io.ByteArrayOutputStream
import koodies.text.INTERMEDIARY_LINE_PATTERN
import koodies.text.LineSeparators
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
     * Contains the currently logged I/O.
     */
    private val log = mutableListOf<IO>()

    /**
     * Adds the specified [IO.Meta] to this log.
     */
    public operator fun plus(meta: IO.Meta) {
        lock.withLock { log.add(meta) }
    }

    /**
     * Assembles [IO.Input] chunks and adds successfully reconstructed ones
     * to this log.
     */
    public val input: IOAssembler = IOAssembler { lines ->
        lock.withLock {
            lines.forEach { log.add(Input typed it) }
        }
    }

    /**
     * Assembles [IO.Output] chunks and adds successfully reconstructed ones
     * to this log.
     */
    public val output: IOAssembler = IOAssembler { lines ->
        lock.withLock {
            lines.forEach { log.add(Output typed it) }
        }
    }

    /**
     * Assembles [IO.Error] chunks and adds successfully reconstructed ones
     * to this log.
     */
    public val error: IOAssembler = IOAssembler { lines ->
        lock.withLock {
            lines.forEach { log.add(Error typed it) }
        }
    }

    /**
     * For each type of [IO] all so far saved incomplete strings are treated
     * as if they were complete, that is, appended to the list of completed strings.
     */
    public fun flush() {
        input.flush()
        output.flush()
        error.flush()
    }

    /**
     * Returns a dump of the logged I/O log.
     */
    public fun dump(): String = merge<IO>(removeAnsi = false)

    /**
     * Dumps the logged I/O log in the specified [directory] using the name scheme `koodies.exec.{PID}.{RANDOM}.log".
     */
    public fun dump(directory: Path, pid: Int): Map<String, Path> = persistDump(directory.resolve("koodies.exec.$pid.log")) { dump() }

    override fun toString(): String = asString {
        Symbols.OK to joinToString { it.truncate() }
        "OUT" to output.incompleteBytes
        "ERR" to error.incompleteBytes
    }
}

/**
 * Utility that assembles [IO] by continuously accumulating bytes
 * and that calls the specified [lineCompletedCallback] every time
 * a string terminated by a [LineSeparators] was re-constructed.
 *
 * Bytes are provided using [plus].
 */
public class IOAssembler(private val lineCompletedCallback: (List<String>) -> Unit) {

    private val lock = ReentrantLock()

    /**
     * Contains not yet fully assembled [IO], that is, not yet terminated
     * by one of the [LineSeparators].
     */
    private val incomplete: ByteArrayOutputStream = ByteArrayOutputStream()

    /**
     * Amount of [IO] bytes not yet fully assembled, that is, not yet terminated
     * by one of the [LineSeparators].
     */
    public val incompleteBytes: Size get() = incomplete.size().bytes

    /**
     * Takes the given [bytes] and attempts to re-construct complete text lines
     * based on already stored bytes.
     */
    public operator fun plus(bytes: ByteArray) {
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

    /**
     * Clears currently [incomplete] [IO] by invoking [lineCompletedCallback]
     * (and thus pretending the provided string was originally terminated
     * by one of the [LineSeparators]).
     */
    public fun flush() {
        lock.withLock {
            val remainder = incomplete.toString(Charsets.UTF_8)
            incomplete.reset()
            if (remainder.isNotEmpty()) lineCompletedCallback(remainder.lines())
        }
    }

    override fun toString(): String = asString(::lock, ::incomplete)
}
