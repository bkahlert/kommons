package koodies.concurrent.process

import koodies.text.LineSeparators.LF

public class ProcessExecutionException(
    public val pid: Long,
    public val commandLine: CommandLine,
    public val exitValue: Int,
    public val expectedExitValue: Int,
    public val additionalInformation: String? = null,
    cause: Throwable? = null,
) : Exception(
    StringBuilder("Process $pid terminated with exit code $exitValue. Expected $expectedExitValue.").apply {
        cause?.let { append(" Reason: $cause") }
        append(LF + commandLine.includedFiles.joinToString(LF) { IO.META typed it })
        additionalInformation?.let { append(LF + additionalInformation) }
    }.toString(),
    cause
) {
    public fun withCause(cause: Throwable?): ProcessExecutionException =
        ProcessExecutionException(
            pid,
            commandLine,
            exitValue,
            expectedExitValue,
            additionalInformation,
            cause
        )
}
