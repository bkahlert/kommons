package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Parser
import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.takeIfNotEmpty
import com.bkahlert.kommons.text.LineSeparators.removeTrailingLineSeparator
import org.codehaus.plexus.util.cli.Commandline
import org.codehaus.plexus.util.cli.shell.FormattingShell
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.reflect.KClass

/** A command line that can be executed. */
public data class CommandLine(

    /** Command to be executed. */
    public val command: String,

    /** Arguments to be passed to [command]. */
    public val arguments: List<String> = emptyList(),
) : List<String> by listOf(command, *arguments.toTypedArray()), Executable {

    public constructor(
        command: CharSequence,
        vararg arguments: CharSequence,
    ) : this(command.toString(), arguments.map { it.toString() })

    /**
     * Instantiates a command line that invokes the `main` method
     * of the specified [kClass] using the specified [arguments].
     *
     * @param arguments passed to the `main` method
     * @param javaBinary binary location of the Java distribution to use to invoke the `main` method
     * @param classPath class path to use to invoke the `main` method
     *
     * @throws IllegalArgumentException if [kClass] has no qualified name
     */
    public constructor(
        kClass: KClass<*>,
        vararg arguments: String,
        javaBinary: Path = SystemLocations.JavaHome / "bin" / "java",
        classPath: String? = System.getProperty("java.class.path"),
    ) : this(javaBinary.pathString, buildList {
        if (classPath != null) {
            add("-cp")
            add(classPath)
        }
        add(requireNotNull(kClass.qualifiedName) { "missing qualified name" })
        addAll(arguments)
    })

    /**
     * Returns a command line containing the command and arguments of the original command line and
     * then the given [argument].
     */
    public operator fun plus(argument: CharSequence): CommandLine =
        CommandLine(command, arguments + argument.toString())

    override val exec: Executor<Process.ExitState>
        get() = SyncExecutor(this)

    /**
     * Returns this command line formatted in a shell-compatible way.
     *
     * **Example: `pretty=false`**
     * ```sh
     * 'echo' 'Hello World!'
     * ```
     *
     * **Example: `pretty=true`**
     * ```sh
     * 'echo' \
     * 'Hello World!'
     * ```
     */
    public fun toString(pretty: Boolean): String = FormattingShell(if (pretty) " \\\n" else " ").run {
        getRawCommandLine(originalExecutable, toTypedArray()).last()
    }

    /**
     * Returns this command line formatted in a shell-compatible way.
     *
     * **Example**
     * ```sh
     * 'echo' 'Hello World!'
     * ```
     */
    override fun toString(): String = toString(pretty = false)

    public companion object : Parser<CommandLine> by (Parser.parser { text ->
        val plexusCommandLine = Commandline(text.toString().replace("\\\n", "").removeTrailingLineSeparator())
        val rawCommandline = plexusCommandLine.rawCommandline.takeIfNotEmpty()
        rawCommandline?.let { CommandLine(it.first(), it.drop(1)) }
    })
}
