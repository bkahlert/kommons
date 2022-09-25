package org.codehaus.plexus.util.cli.shell

import org.codehaus.plexus.util.cli.Commandline
import java.io.File

/**
 * A shell that delegates to the specified [delegate]
 * and of what the sole purpose is to support specifying the
 * argument [separator].
 */
internal class FormattingShell(
    private val separator: String,
    private val delegate: Shell = Commandline().shell.apply { setUnconditionalQuoting(false) },
) : Shell() {

    override fun clone(): Any = delegate.clone()

    override fun setUnconditionalQuoting(unconditionallyQuote: Boolean): Unit =
        delegate.setUnconditionalQuoting(unconditionallyQuote)

    override fun getShellCommand(): String? = delegate.shellCommand

    override fun setShellCommand(shellCommand: String?) {
        delegate.shellCommand = shellCommand
    }

    override fun getShellArgs(): Array<String> = delegate.shellArgs
    override fun setShellArgs(shellArgs: Array<out String>?) {
        delegate.shellArgs = shellArgs
    }

    override fun getCommandLine(executable: String?, arguments: Array<out String>): MutableList<String> =
        delegate.getCommandLine(executable, arguments)

    override fun quoteOneItem(inputString: String?, isExecutable: Boolean): String =
        delegate.quoteOneItem(inputString, isExecutable)

    public override fun getRawCommandLine(executable: String?, arguments: Array<out String>): MutableList<String> {
        val commandLine = ArrayList<String>()
        val sb: StringBuilder = StringBuilder()

        if (executable != null) {
            val preamble: String? = executionPreamble
            if (preamble != null) {
                sb.append(preamble)
            }

            if (isQuotedExecutableEnabled) {
                sb.append(quoteOneItem(originalExecutable, true))
            } else {
                sb.append(getExecutable())
            }
        }
        for (argument in arguments) {
            if (sb.isNotEmpty()) {
                sb.append(separator)
            }

            if (isQuotedArgumentsEnabled) {
                sb.append(quoteOneItem(argument, false))
            } else {
                sb.append(argument)
            }
        }

        commandLine.add(sb.toString())

        return commandLine
    }

    override fun getQuotingTriggerChars(): CharArray = delegate.quotingTriggerChars
    override fun getExecutionPreamble(): String? = delegate.executionPreamble

    override fun getEscapeChars(includeSingleQuote: Boolean, includeDoubleQuote: Boolean): CharArray =
        delegate.getEscapeChars(includeSingleQuote, includeDoubleQuote)

    override fun isDoubleQuotedArgumentEscaped(): Boolean = delegate.isDoubleQuotedArgumentEscaped
    override fun isSingleQuotedArgumentEscaped(): Boolean = delegate.isSingleQuotedArgumentEscaped
    override fun isDoubleQuotedExecutableEscaped(): Boolean = delegate.isDoubleQuotedExecutableEscaped
    override fun isSingleQuotedExecutableEscaped(): Boolean = delegate.isSingleQuotedExecutableEscaped
    override fun setArgumentQuoteDelimiter(argQuoteDelimiter: Char) {
        delegate.argumentQuoteDelimiter = argQuoteDelimiter
    }

    override fun getArgumentQuoteDelimiter(): Char = delegate.argumentQuoteDelimiter
    override fun setExecutableQuoteDelimiter(exeQuoteDelimiter: Char) {
        delegate.executableQuoteDelimiter = exeQuoteDelimiter
    }

    override fun getExecutableQuoteDelimiter(): Char = delegate.executableQuoteDelimiter
    override fun setArgumentEscapePattern(argumentEscapePattern: String?) {
        delegate.argumentEscapePattern = argumentEscapePattern
    }

    override fun getArgumentEscapePattern(): String = delegate.argumentEscapePattern
    override fun getShellCommandLine(arguments: Array<out String>?): MutableList<String> = delegate.getShellCommandLine(arguments)
    override fun getShellArgsList(): MutableList<String> = delegate.shellArgsList
    override fun addShellArg(arg: String?): Unit = delegate.addShellArg(arg)
    override fun setQuotedArgumentsEnabled(quotedArgumentsEnabled: Boolean) {
        delegate.isQuotedArgumentsEnabled = quotedArgumentsEnabled
    }

    override fun isQuotedArgumentsEnabled(): Boolean = delegate.isQuotedArgumentsEnabled
    override fun setQuotedExecutableEnabled(quotedExecutableEnabled: Boolean) {
        delegate.isQuotedExecutableEnabled = quotedExecutableEnabled
    }

    override fun isQuotedExecutableEnabled(): Boolean = delegate.isQuotedExecutableEnabled
    override fun setExecutable(executable: String?) {
        delegate.executable = executable
    }

    override fun getExecutable(): String = delegate.executable
    override fun setWorkingDirectory(path: String?): Unit = delegate.setWorkingDirectory(path)
    override fun setWorkingDirectory(workingDir: File?) {
        delegate.workingDirectory = workingDir
    }

    override fun getWorkingDirectory(): File = delegate.workingDirectory
    override fun getWorkingDirectoryAsString(): String = delegate.workingDirectoryAsString
    override fun clearArguments(): Unit = delegate.clearArguments()
    override fun getOriginalExecutable(): String? = delegate.originalExecutable
    override fun getOriginalCommandLine(executable: String?, arguments: Array<out String>?): MutableList<String> =
        delegate.getOriginalCommandLine(executable, arguments)

    override fun setDoubleQuotedArgumentEscaped(doubleQuotedArgumentEscaped: Boolean) {
        delegate.isDoubleQuotedArgumentEscaped = doubleQuotedArgumentEscaped
    }

    override fun setDoubleQuotedExecutableEscaped(doubleQuotedExecutableEscaped: Boolean) {
        delegate.isDoubleQuotedExecutableEscaped = doubleQuotedExecutableEscaped
    }

    override fun setSingleQuotedArgumentEscaped(singleQuotedArgumentEscaped: Boolean) {
        delegate.isSingleQuotedArgumentEscaped = singleQuotedArgumentEscaped
    }

    override fun setSingleQuotedExecutableEscaped(singleQuotedExecutableEscaped: Boolean) {
        delegate.isSingleQuotedExecutableEscaped = singleQuotedExecutableEscaped
    }
}
