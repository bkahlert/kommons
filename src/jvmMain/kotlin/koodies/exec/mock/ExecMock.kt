package koodies.exec.mock

import koodies.Koodies
import koodies.debug.asEmoji
import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.JavaExec
import koodies.exec.successfulOrNull
import koodies.text.Semantics.Symbols

/**
 * [Exec] mock to ease testing.
 */
public open class ExecMock(
    private val process: JavaProcessMock,
    private val name: String? = null,
) : Exec by JavaExec(process, Koodies.ExecTemp, CommandLine("echo", ExecMock::class.simpleName!!, name = name)) {

    override fun toString(): String {
        val delegateString =
            "${process.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successfulOrNull?.asEmoji ?: Symbols.Computation})"
        val string = "${ExecMock::class.simpleName ?: "object"}(process=$delegateString)".substringBeforeLast(")")
        return string.takeUnless { name != null } ?: string.substringBeforeLast(")") + ", name=$name)"
    }

    public companion object {
        public val RUNNING_EXEC: ExecMock get() = ExecMock(JavaProcessMock.RUNNING_PROCESS)
        public val SUCCEEDED_EXEC: ExecMock
            get() = ExecMock(JavaProcessMock.SUCCEEDING_PROCESS).apply {
                outputStream.readBytes()
                onExit.join()
            }
        public val FAILED_EXEC: ExecMock
            get() = ExecMock(JavaProcessMock.FAILING_PROCESS).apply {
                errorStream.readBytes()
                onExit.join()
            }
    }
}
