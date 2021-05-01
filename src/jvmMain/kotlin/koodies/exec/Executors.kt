package koodies.exec

import koodies.exec.Executors.runCommandLineAsJavaProcess
import koodies.exec.Executors.runScriptAsJavaProcess
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.io.path.deleteRecursively
import koodies.io.path.randomPath
import koodies.jvm.deleteOldTempFilesOnExit
import koodies.shell.HereDoc
import koodies.shell.ShellScript
import org.codehaus.plexus.util.cli.Commandline
import org.codehaus.plexus.util.cli.shell.Shell
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.time.days
import java.lang.Process as JavaProcess

private const val shellScriptPrefix: String = "koodies.process."
private const val shellScriptExtension: String = ".sh"

private object Executors {

    init {
        deleteOldTempFilesOnExit(shellScriptPrefix, shellScriptExtension, 3.days, keepAtMost = 100)
    }

    fun requireValidWorkingDirectory(workingDirectory: Path?): File? =
        workingDirectory?.toAbsolutePath()?.run {
            require(exists()) { "Working directory $this does not exist." }
            require(isDirectory()) { "Working directory $this is no directory." }
            toFile()
        }

    fun Path.runScriptAsJavaProcess(
        redirectErrorStream: Boolean,
        environment: Map<String, String>,
        workingDirectory: Path?,
    ): JavaProcess {
        require(isScriptFile) { "$this must be a script file." }
        val script: String = (readText() + "pwd\n").also { deleteRecursively() }

        val directory: File? = requireValidWorkingDirectory(workingDirectory)

        val shell = Commandline().shell
        val shellCommandLine: Array<String> = arrayOf(shell.shellCommand, *shell.shellArgsList.toTypedArray(), script)
        return ProcessBuilder(*shellCommandLine).let { pb ->
            pb.redirectErrorStream(redirectErrorStream)
            pb.environment().putAll(environment)
            directory?.also { pb.directory(it) }
            pb.start()
        }
    }

    fun Path.runScriptAsJavaProcess2(
        redirectErrorStream: Boolean,
        environment: Map<String, String>,
        workingDirectory: Path?,
    ): JavaProcess {
        require(isScriptFile) { "$this must be a script file." }
        val scriptFile: String = asString()

        val directory: File? = requireValidWorkingDirectory(workingDirectory)

        val shell = Commandline().shell
        val shellCommandLine = shell.getShellCommandLine(arrayOf(scriptFile))

        return ProcessBuilder(shellCommandLine).let { pb ->
            pb.redirectErrorStream(redirectErrorStream)
            pb.environment().putAll(environment)
            directory?.also { pb.directory(it) }
            pb.start()
        }
    }

    fun CommandLine.runCommandLineAsJavaProcess(
        redirectErrorStream: Boolean,
        environment: Map<String, String>,
        workingDirectory: Path?,
    ): JavaProcess {

        val hereDocDelimiters = HereDoc.findAllDelimiters(CommandLine.asShellCommand(commandLineParts))
        require(hereDocDelimiters.isEmpty()) {
            "The command line contained here documents ($hereDocDelimiters) which " +
                "will not be escaped and are not what you intended to do."
        }

        val directory: File? = requireValidWorkingDirectory(workingDirectory)

        val shell = Commandline().shell
        val shellCommandLine = shell.getShellCommandLine(commandLineParts)

        return ProcessBuilder(shellCommandLine).let { pb ->
            pb.redirectErrorStream(redirectErrorStream)
            pb.environment().putAll(environment)
            directory?.also { pb.directory(it) }
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
 *
 * @param redirectErrorStream whether standard error is redirected to standard output during execution
 * @param environment the environment to be exposed to the [Exec] during execution
 * @param workingDirectory the working directory to be used during execution
 */
public fun CommandLine.toJavaProcess(
    redirectErrorStream: Boolean,
    environment: Map<String, String>,
    workingDirectory: Path?,
): JavaProcess = asScriptFileOrNull()
    ?.runScriptAsJavaProcess(redirectErrorStream, environment, workingDirectory)
    ?: runCommandLineAsJavaProcess(redirectErrorStream || redirects.isNotEmpty(), environment + this.environment, workingDirectory ?: this.workingDirectory)

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
