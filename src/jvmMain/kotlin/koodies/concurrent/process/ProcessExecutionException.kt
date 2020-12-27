package koodies.concurrent.process

import koodies.text.LineSeparators

class ProcessExecutionException(
    val pid: Long,
    val commandLine: CommandLine,
    val exitValue: Int,
    val expectedExitValue: Int,
    val additionalInformation: String? = null,
    cause: Throwable? = null,
) : Exception(
    StringBuilder("Process $pid terminated with exit code $exitValue. Expected $expectedExitValue.").apply {
        cause?.let { append(" Reason: $cause") }
        append(LineSeparators.LF + commandLine.formattedIncludesFiles)
        additionalInformation?.let { append(LineSeparators.LF + additionalInformation) }
    }.toString(),
    cause
) {
    fun withCause(cause: Throwable?) =
        ProcessExecutionException(
            pid,
            commandLine,
            exitValue,
            expectedExitValue,
            additionalInformation,
            cause
        )
}
