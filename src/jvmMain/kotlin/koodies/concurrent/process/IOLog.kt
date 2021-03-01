package koodies.concurrent.process

import koodies.concurrent.process.IO.Type
import koodies.exception.persistDump
import koodies.io.ByteArrayOutputStream
import koodies.text.INTERMEDIARY_LINE_PATTERN
import koodies.text.LineSeparators
import koodies.text.LineSeparators.lines
import koodies.text.Unicode.Emojis.heavyCheckMark
import koodies.text.joinLinesToString
import koodies.time.busyWait
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.Map.Entry
import kotlin.concurrent.withLock
import kotlin.time.seconds

/**
 * An I/O log can be used to log what a [ManagedProcess] received and produces as data.
 *
 * In order to log I/O only [add] must be called.
 */
class IOLog {

    private val lock = ReentrantLock()

    /**
     * Contains the currently logged I/O of the corresponding [ManagedProcess].
     *
     * **Important:** Only complete lines can be accessed as this is considered to be the only safe way
     * to have non-corrupted data (e.g. split characters).
     */
    val logged: List<IO> get() = lock.withLock { log.toList() }

    /**
     * Contains the currently logged I/O of the corresponding [ManagedProcess].
     *
     * **Important:** Only complete lines can be accessed as this is considered to be the only safe way
     * to have non-corrupted data (e.g. split characters).
     */
    fun logged(type: Type): IO = type typed logged.filter { it.type == type }.joinToString(LineSeparators.LF) { it.unformatted }

    /**
     * Contains the currently logged I/O of the corresponding [ManagedProcess].
     *
     * **Important:** Only complete lines can be accessed as this is considered to be the only safe way
     * to have non-corrupted data (e.g. split characters).
     */
    fun logged(): String = logged.joinToString(LineSeparators.LF) { it.unformatted }

    /**
     * Contains the currently logged I/O. See [logged] for more details.
     */
    private val log = mutableListOf<IO>()

    /**
     * Contains not yet fully logged I/O, that is, data not yet terminated by one of the [LineSeparators].
     */
    private val incompleteLines: MutableMap<Type, ByteArrayOutputStream> = mutableMapOf()

    /**
     * Adds [content] with the specified [IO.Type] to the [IOLog].
     *
     * [content] does not have to be *complete* in any way (like a complete line) but also be provided
     * in chunks of any size. The I/O will be correctly reconstructed and can be accessed using [logged].
     */
    fun add(type: Type, content: ByteArray) = lock.withLock {
        with(incompleteLines.getOrPut(type, { ByteArrayOutputStream() })) {
            write(content)
            while (true) {
                val justCompletedLines = incompleteLines.findCompletedLines()
                if (justCompletedLines != null) {
                    val completedLines = justCompletedLines.value.removeCompletedLines()
                    log.addAll(justCompletedLines.key typed completedLines)
                } else break
            }
        }
    }

    private fun Map<Type, ByteArrayOutputStream>.findCompletedLines(): Entry<Type, ByteArrayOutputStream>? =
        entries.firstOrNull { (_, builder) ->
            val toString = builder.toString(Charsets.UTF_8)
            toString.matches(LineSeparators.INTERMEDIARY_LINE_PATTERN)
        }

    private fun ByteArrayOutputStream.removeCompletedLines(): List<String> {
        val read = toString(Charsets.UTF_8).lines()
        return read.take(read.size - 1).also {
            reset()
            write(read.last().toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * Returns a dump of the logged I/O log.
     */
    fun dump(): String {
        2.seconds.busyWait()
        return logged.joinLinesToString { it.formatted }
    }

    /**
     * Dumps the logged I/O log in the specified [directory] using the name scheme `koodies.process.{PID}.{RANDOM}.log".
     */
    fun dump(directory: Path, pid: Int): Map<String, Path> = persistDump(directory.resolve("koodies.process.$pid.log")) { dump() }

    override fun toString(): String =
        "${this::class.simpleName}(${log.size} $heavyCheckMark; ${incompleteLines.filterValues { it.toByteArray().isNotEmpty() }.size} …)"
}

/**
 * Contains the currently logged I/O of this [ManagedProcess].
 *
 * **Important:** Only complete lines can be accessed as this is considered to be the only safe way
 * to have non-corrupted data (e.g. split characters).
 */
fun ManagedProcess.logged(type: Type): IO = ioLog.logged(type)

/**
 * Contains the currently logged I/O of this [ManagedProcess].
 *
 * **Important:** Only complete lines can be accessed as this is considered to be the only safe way
 * to have non-corrupted data (e.g. split characters).
 */
val ManagedProcess.logged: String get() = ioLog.logged()
