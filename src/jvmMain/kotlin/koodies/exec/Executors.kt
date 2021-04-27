package koodies.exec

import koodies.exec.Executors.runCommandLineAsJavaProcess
import koodies.exec.Executors.runScriptAsJavaProcess
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.io.path.randomPath
import koodies.jvm.deleteOldTempFilesOnExit
import koodies.shell.HereDoc
import koodies.shell.ShellScript
import koodies.text.LineSeparators
import org.codehaus.plexus.util.cli.Commandline
import org.codehaus.plexus.util.cli.shell.Shell
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.time.days
import java.lang.Process as JavaProcess

private const val shellScriptPrefix: String = "koodies.process."
private const val shellScriptExtension: String = ".sh"

private object Executors {

    init {
        deleteOldTempFilesOnExit(shellScriptPrefix, shellScriptExtension, 3.days, keepAtMost = 100)
    }

    fun requireValidWorkingDirectory(workingDirectory: Path): File =
        workingDirectory.toAbsolutePath().run {
            require(exists()) { "Working directory $this does not exist." }
            require(isDirectory()) { "Working directory $this is no directory." }
            toFile()
        }

    fun Path.runScriptAsJavaProcess(environment: Map<String, String>, workingDirectory: Path): JavaProcess {
        require(isScriptFile) { "$this must be a script file." }
        val scriptFile: String = asString()

        val directory: File = requireValidWorkingDirectory(workingDirectory)

        val shell = Commandline().shell
        val shellCommandLine = shell.getShellCommandLine(arrayOf(scriptFile))

        return ProcessBuilder(shellCommandLine).let { pb ->
            pb.environment().putAll(environment)
            pb.directory(directory)
            pb.start()
        }
    }

    fun CommandLine.runCommandLineAsJavaProcess(): JavaProcess {

        require(redirects.isEmpty()) {
            "Redirects are only supported for shell scripts.${LineSeparators.LF}" +
                "Convert your command line first to a script file and execute that one."
        }
        val hereDocDelimiters = HereDoc.findAllDelimiters(CommandLine.asShellCommand(commandLineParts))
        require(hereDocDelimiters.isEmpty()) {
            "The command line contained here documents ($hereDocDelimiters) which " +
                "will not be escaped and are not what you intended to do."
        }

        val directory: File = requireValidWorkingDirectory(workingDirectory)

        val shell = Commandline().shell
        val shellCommandLine = shell.getShellCommandLine(commandLineParts)

        return ProcessBuilder(shellCommandLine).let { pb ->
            pb.environment().putAll(environment)
            pb.directory(directory)
            pb.start()
        }
    }
}

// TODO implement toggle to run commandLine always as script to provide
// an easy way to see what was executed
/**
 * # THE function to execute command lines
 *
 * Checks if `this` command line points to a script file and if yes,
 * executes it using a [Shell].
 *
 * Otherwise the command line is taken as is and executes using the VM's
 * [ProcessBuilder].
 */
public fun CommandLine.toJavaProcess(): JavaProcess =
    asScriptFileOrNull()
        ?.runScriptAsJavaProcess(environment, workingDirectory)
        ?: runCommandLineAsJavaProcess()

/**
 * If this [CommandLine] is pointing to a [ShellScript] file,
 * this function will return its [Path] and `null` otherwise.
 */
public fun CommandLine.asScriptFileOrNull(): Path? =
    kotlin.runCatching { command.asPath() }.getOrNull()?.takeIf { it.isScriptFile }

/**
 * Returns a random path to a shell script file in `this` directory.
 *
 * The file does not exist, yet.
 */
internal fun Path.scriptPath(): Path = randomPath(base = shellScriptPrefix, extension = shellScriptExtension)

/**
 * Whether this path points to a script file. No matter if it exists or not.
 */
internal val Path.isScriptFile: Boolean
    get() = name.startsWith(shellScriptPrefix) && name.endsWith(shellScriptExtension)
