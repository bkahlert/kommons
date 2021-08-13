package koodies.exec

import koodies.Either
import koodies.Either.Left
import koodies.Either.Right
import koodies.docker.Docker
import koodies.exception.dump
import koodies.exec.IO.Meta
import koodies.exec.IO.Meta.Dump
import koodies.exec.IO.Output
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.exec.Process.State
import koodies.exec.Process.State.Exited.Failed
import koodies.exec.Process.State.Exited.Succeeded
import koodies.io.path.Locations
import koodies.shell.ShellScript
import koodies.text.LineSeparators.DEFAULT
import koodies.text.Semantics.formattedAs
import koodies.time.Now
import koodies.tracing.KoodiesTelemetry
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * # Exec: Feature-Rich [Process] Execution
 *
 * ## Background
 * The [Java's Development Kit](https://docs.oracle.com/en/java/javase/index.html) allows developers to execute native OS processes
 * using the [Runtime.exec] since its first release. As of Java 1.5/5.0 the newly introduced [ProcessBuilder] rendered
 * process handling easier; as did subsequent feature additions like [ProcessHandle].
 *
 * [Apache Commons Exec](https://commons.apache.org/proper/commons-exec/) was somewhat a breakthrough as it took away one of the biggest
 * challenges programmers face with processes—reading their standard output and error. Furthermore Apache Commons Exec supports parameter
 * substitution and helps coping with concurrency (e.g.
 * [DefaultExecutor](https://commons.apache.org/proper/commons-exec/apidocs/org/apache/commons/exec/DefaultExecutor.html),
 * [ExecuteWatchdog](https://commons.apache.org/proper/commons-exec/apidocs/org/apache/commons/exec/ExecuteWatchdog.html)).
 * Yet, a couple of everyday tasks are still not easily achievable.
 *
 * [ZT Process Executor](https://github.com/zeroturnaround/zt-exec) is a process executor that makes a lot of things better. It has meta logging, which greatly
 * helps at debugging and provides "one-liners" (that are rather "one-line-likes" because of line length and exception handling boilerplate):
 * ```java
 *      String output;
 *      boolean success = false;
 *      try {
 *          output = new ProcessExecutor().command("java", "-version")
 *                        .readOutput(true).exitValues(3)
 *                        .execute().outputUTF8();
 *          success = true;
 *      } catch (InvalidExitValueException e) {
 *          System.out.println("Process terminated with " + e.getExitValue());
 *          output = e.getResult().outputUTF8();
 *      }
 * ```
 *
 * Unfortunately when it comes to script, no simple solutions seem to exist at all. Also libraries taking advantage of Kotlin features are not known.
 *
 * **This is where *Koodies Exec* jumps in.**
 * The following snippet provides the same functionality as the code from:
 * ```kotlin
 *      val exec = CommandLine("java", "-version").exec()
 *           .apply { if(state is Failed) println(exitCode) }
 *      val (output, success) = exec.io.out to exec.successful
 * ```
 *
 * Or:
 * ```kotlin
 *      // 🕝 asynchronously
 *      CommandLine(…).exec.async()
 *
 *      // 📝 logging
 *      CommandLine(…).exec.logging()
 *
 *      // 🧠 processing / interactively
 *      CommandLine(…).exec.processing { io -> … }
 *
 *      // 📄 run shell scripts with same API (exec, exec.logging, exec.processing)
 *      ShellScript {
 *        "curl -s https://api.github.com/repos/jetbrains/kotlin/releases/latest | jq -r .tag_name | perl -pe 's/v//'"
 *      }.exec()
 *
 *      // 🐳 dockerized, e.g. if a command line tool is missing
 *      ShellScript {
 *        "curl -s https://api.github.com/repos/jetbrains/kotlin/releases/latest | jq -r .tag_name | perl -pe 's/v//'"
 *      }.dockerized { "dwdraju" / "alpine-curl-jq" }.exec()
 * ```
 *
 * ## Features
 *
 * ### Simplified API
 * [koodies.exec.Process] interface for
 * 1) easier mocking and
 * 2) and a simplified API
 *
 * ### Tracing
 * If [OpenTelemetry](https://opentelemetry.io/) is detected or explicitly set using [koodies.tracing.KoodiesTelemetry.register]
 * each execution of a process creates a new span with IO recorded as events. No further configuration is necessary.
 *
 * ### I/O Handling
 * The input and output of a wrapped process is typed with sub-classes of the sealed [IO] class:
 * - [IO.Meta] (process life-cycle messages)
 * - [IO.Input] (all data sent to the process)
 * - [IO.Output] (standard output) and
 * - [IO.Error] (standard error)
 *
 * I/O can be accessed:
 * - always using the [io] property and
 * - as part of [state] (i.e. [ExitState] which is accompanied with a full copy of all recorded [IO])
 *
 * ### State Handling
 * Wrapped processes have a [state].
 *
 * States are modelled with sealed classes:
 * - [State.Running]
 * - [State.Exited]
 *      - [Succeeded]
 *      - [Failed]
 * - [State.Excepted]
 *
 * By default all non-`0` exit codes are considered failed.
 * Failed and fatal exit states contain a [dump] of the form
 * (and in case of [Executor.logging] is also automatically printed):
 * ```
 *      Process {PID} terminated with exit code {exit code}
 *      ➜ A dump has been written to:
 *      - {TempDir}/koodies/exec/dump.{}.log
 *      - {TempDir}/koodies/exec/dump.{}.ansi-removed.log
 *      ➜ The last 10 lines are:
 *      {…}
 *      3
 *      2
 *      1
 *      Boom!
 * ```
 *
 * Additionally, in the rare case of an actual exception, it is contained in [State.Excepted]
 * and also included in the just described dump.
 *
 * That design already covers a lot of use cases. Even if other exit codes don't represent
 * an erroneous state, nothing must be done, as the caller is not forced to handle exceptions with
 * verbose try-catch constructs.
 *
 * For fine-grained control, such as domain-specific exit states,
 * the exit state computation can be delegated to an [ExitStateHandler].
 *
 * Optionally an [ExecTerminationCallback] can be set to be notified the moment the wrapped process terminates
 * (with the eventually occurred problem is passed as an argument).
 *
 * Last but not least, processes still running when the VM shuts down are attempted
 * to be stopped. This behaviour can be deactivated for each created process.
 *
 * ## Executables: Command Line and Shell Script
 *
 * - [CommandLine] represents a **single** command and optional arguments
 * - [ShellScript] represents what most users would expect to be executable until confronted with reality
 *
 * Command lines and shell scripts can be executed using the same simple API
 * with no known limitations (provided, a shell is installed at all):
 * - [synchronously][ProcessingMode.Synchronicity.Sync]
 * - [asynchronously][ProcessingMode.Synchronicity.Async]
 * - [logging][KoodiesTelemetry]
 * - [interactively][ProcessingMode.Interactivity]
 * - [dockerized][Docker]
 */
public interface Exec : Process {

    public companion object {

        /**
         * Returns an [ExitStateHandler] that interprets `this` [Exec]
         * once terminated as a [Succeeded] if it exits with code 0
         * and as a [Failed] otherwise.
         */
        public fun fallbackExitStateHandler(): ExitStateHandler = ExitStateHandler { pid, exitCode, io ->
            if (exitCode == 0) {
                Succeeded(start, Now.instant, pid, io)
            } else {
                val relevantFiles = commandLine.includedFiles
                val dump = createDump("Process ${pid.formattedAs.input} terminated with exit code ${exitCode.formattedAs.input}")
                Failed(start, Now.instant, pid, exitCode, io, relevantFiles.map { it.toUri() }, dump)
            }
        }

        /**
         * Dumps the [IO] of `this` [Exec] individualized with the given [errorMessage]
         * to the process's [workingDirectory] and returns the same dump as a string.
         *
         * The given error messages are concatenated with a line break.
         */
        public fun Exec.createDump(vararg errorMessage: String): String {
            metaStream.emit(Meta typed errorMessage.joinToString(DEFAULT))
            return (workingDirectory ?: Locations.Temp).dump(null) { io.ansiKept }.also { dump -> metaStream.emit(Dump(dump)) }
        }
    }

    /**
     * Contains the so far logged I/O of this exec.
     */
    public val io: IOSequence<IO>

    /**
     * The working directory of this exec.
     */
    public val workingDirectory: Path?

    /**
     * The command its arguments executed.
     */
    public val commandLine: CommandLine

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
 * A callback that is invoked the moment an [Exec] terminates.
 */
public typealias ExecTerminationCallback = (Throwable?) -> Unit

/**
 * In compliance to [Process.outputStream] this type of stream
 * consists of [Meta] about an [Exec].
 */
public class MetaStream(vararg listeners: (Meta) -> Unit) {
    private val lock = ReentrantLock()
    private val history: MutableList<Meta> = mutableListOf()
    private val listeners: MutableList<(Meta) -> Unit> = mutableListOf(*listeners)

    /**
     * Subscribes the given [listener] to this meta stream, that is,
     * already emitted and future messages are sent to the [listener].
     */
    public fun subscribe(listener: (Meta) -> Unit): Unit = lock.withLock {
        history.forEach { listener(it) }
        listeners.add(listener)
    }

    /**
     * Exits the given [message] to all subscribed [listeners].
     */
    public fun emit(message: Meta): Unit = lock.withLock {
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
@JvmInline
public value class ColumnParser(
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
     * Otherwise the [State.Exited.Failed] [Exec.exitCode] is returned.
     */
    public inline fun <T : Any, reified E : ExitState> columns(num: Int, crossinline lineParser: (List<String>) -> T?): Either<List<T>, E> =
        when (val exitState = exec.waitFor()) {

            is E -> Right(exitState)

            is Succeeded -> {
                Left(exitState.io.asSequence()
                    .filterIsInstance<Output>()
                    .map { it.ansiRemoved }
                    .map { it.split("\t") }
                    .filter { it.size == num }
                    .map { it.map { field -> if (field == "<none>") "" else field } }
                    .mapNotNull { columns ->
                        kotlin.runCatching { lineParser(columns) }.recover {
                            throw IllegalStateException("Error parsing $columns", it)
                        }.getOrThrow()
                    }.toList())
            }

            else -> error("Unmapped ${E::class.simpleName} ${exitState::class.simpleName}: $exitState")
        }
}
