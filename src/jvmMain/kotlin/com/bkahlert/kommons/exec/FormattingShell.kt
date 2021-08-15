package org.codehaus.plexus.util.cli.shell

import org.codehaus.plexus.util.cli.Commandline
import java.io.File

/**
 * A shell that delegates to the one returned by Plexus [Commandline]
 * and of what the sole purpose is to allow specifying the
 * argument [separator].
 */
internal class FormattingShell(private val separator: String) : Shell() {
    private val delegate = Commandline().shell.apply { setUnconditionalQuoting(false) }
    override fun clone(): Any = delegate.clone()
    override fun setUnconditionalQuoting(unconditionallyQuote: Boolean): Unit = delegate.setUnconditionalQuoting(unconditionallyQuote)
    override fun setShellCommand(shellCommand: String?): Unit = delegate.setShellCommand(shellCommand)
    override fun getShellCommand(): String = delegate.getShellCommand()
    override fun setShellArgs(shellArgs: Array<out String>?): Unit = delegate.setShellArgs(shellArgs)
    override fun getShellArgs(): Array<String> = delegate.getShellArgs()
    override fun getCommandLine(executable: String?, arguments: Array<out String>?): MutableList<String> = delegate.getCommandLine(executable, arguments)
    override fun quoteOneItem(inputString: String?, isExecutable: Boolean): String = delegate.quoteOneItem(inputString, isExecutable)
    public override fun getRawCommandLine(executable: String?, arguments: Array<out String>?): MutableList<String> {
        val commandLine = ArrayList<String>()
        val sb: StringBuilder = StringBuilder()

        if (executable != null) {
            val preamble: String? = getExecutionPreamble()
            if (preamble != null) {
                sb.append(preamble);
            }

            if (isQuotedExecutableEnabled()) {
                sb.append(quoteOneItem(getOriginalExecutable(), true));
            } else {
                sb.append(getExecutable());
            }
        }
        for (argument in arguments!!) {
            if (sb.length > 0) {
                sb.append(separator);
            }

            if (isQuotedArgumentsEnabled()) {
                sb.append(quoteOneItem(argument, false))
            } else {
                sb.append(argument)
            }
        }

        commandLine.add(sb.toString())

        return commandLine
    }

    override fun getQuotingTriggerChars(): CharArray = delegate.getQuotingTriggerChars()
    override fun getExecutionPreamble(): String = delegate.getExecutionPreamble()
    override fun getEscapeChars(includeSingleQuote: Boolean, includeDoubleQuote: Boolean): CharArray =
        delegate.getEscapeChars(includeSingleQuote, includeDoubleQuote)

    override fun isDoubleQuotedArgumentEscaped(): Boolean = delegate.isDoubleQuotedArgumentEscaped()
    override fun isSingleQuotedArgumentEscaped(): Boolean = delegate.isSingleQuotedArgumentEscaped()
    override fun isDoubleQuotedExecutableEscaped(): Boolean = delegate.isDoubleQuotedExecutableEscaped()
    override fun isSingleQuotedExecutableEscaped(): Boolean = delegate.isSingleQuotedExecutableEscaped()
    override fun setArgumentQuoteDelimiter(argQuoteDelimiter: Char): Unit = delegate.setArgumentQuoteDelimiter(argQuoteDelimiter)
    override fun getArgumentQuoteDelimiter(): Char = delegate.getArgumentQuoteDelimiter()
    override fun setExecutableQuoteDelimiter(exeQuoteDelimiter: Char): Unit = delegate.setExecutableQuoteDelimiter(exeQuoteDelimiter)
    override fun getExecutableQuoteDelimiter(): Char = delegate.getExecutableQuoteDelimiter()
    override fun setArgumentEscapePattern(argumentEscapePattern: String?): Unit = delegate.setArgumentEscapePattern(argumentEscapePattern)
    override fun getArgumentEscapePattern(): String = delegate.getArgumentEscapePattern()
    override fun getShellCommandLine(arguments: Array<out String>?): MutableList<String> = delegate.getShellCommandLine(arguments)
    override fun getShellArgsList(): MutableList<String> = delegate.getShellArgsList()
    override fun addShellArg(arg: String?): Unit = delegate.addShellArg(arg)
    override fun setQuotedArgumentsEnabled(quotedArgumentsEnabled: Boolean): Unit = delegate.setQuotedArgumentsEnabled(quotedArgumentsEnabled)
    override fun isQuotedArgumentsEnabled(): Boolean = delegate.isQuotedArgumentsEnabled()
    override fun setQuotedExecutableEnabled(quotedExecutableEnabled: Boolean): Unit = delegate.setQuotedExecutableEnabled(quotedExecutableEnabled)
    override fun isQuotedExecutableEnabled(): Boolean = delegate.isQuotedExecutableEnabled()
    override fun setExecutable(executable: String?): Unit = delegate.setExecutable(executable)
    override fun getExecutable(): String = delegate.getExecutable()
    override fun setWorkingDirectory(path: String?): Unit = delegate.setWorkingDirectory(path)
    override fun setWorkingDirectory(workingDir: File?): Unit = delegate.setWorkingDirectory(workingDir)
    override fun getWorkingDirectory(): File = delegate.getWorkingDirectory()
    override fun getWorkingDirectoryAsString(): String = delegate.getWorkingDirectoryAsString()
    override fun clearArguments(): Unit = delegate.clearArguments()
    override fun getOriginalExecutable(): String? = delegate.getOriginalExecutable()
    override fun getOriginalCommandLine(executable: String?, arguments: Array<out String>?): MutableList<String> =
        delegate.getOriginalCommandLine(executable, arguments)

    override fun setDoubleQuotedArgumentEscaped(doubleQuotedArgumentEscaped: Boolean): Unit =
        delegate.setDoubleQuotedArgumentEscaped(doubleQuotedArgumentEscaped)

    override fun setDoubleQuotedExecutableEscaped(doubleQuotedExecutableEscaped: Boolean): Unit =
        delegate.setDoubleQuotedExecutableEscaped(doubleQuotedExecutableEscaped)

    override fun setSingleQuotedArgumentEscaped(singleQuotedArgumentEscaped: Boolean): Unit =
        delegate.setSingleQuotedArgumentEscaped(singleQuotedArgumentEscaped)

    override fun setSingleQuotedExecutableEscaped(singleQuotedExecutableEscaped: Boolean): Unit =
        delegate.setSingleQuotedExecutableEscaped(singleQuotedExecutableEscaped)
}
