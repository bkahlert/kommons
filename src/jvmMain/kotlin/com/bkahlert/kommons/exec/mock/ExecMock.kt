package com.bkahlert.kommons.exec.mock

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.debug.asEmoji
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.Exec
import com.bkahlert.kommons.exec.JavaExec
import com.bkahlert.kommons.exec.successfulOrNull
import com.bkahlert.kommons.text.Semantics.Symbols

/**
 * [Exec] mock to ease testing.
 */
public open class ExecMock(
    private val process: JavaProcessMock,
    private val name: String? = null,
) : Exec by JavaExec(process, Kommons.execTemp, CommandLine("echo", ExecMock::class.simpleName!!, name = name)) {

    override fun toString(): String {
        val delegateString =
            "${process.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successfulOrNull?.asEmoji ?: Symbols.Computation})"
        val string = "${ExecMock::class.simpleName ?: "object"}(process=$delegateString)".substringBeforeLast(")")
        return string.takeUnless { name != null } ?: (string.substringBeforeLast(")") + ", name=$name)")
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
