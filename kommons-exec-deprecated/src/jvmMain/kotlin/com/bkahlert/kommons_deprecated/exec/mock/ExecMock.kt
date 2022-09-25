package com.bkahlert.kommons_deprecated.exec.mock

import com.bkahlert.kommons_deprecated.Kommons
import com.bkahlert.kommons.asEmoji
import com.bkahlert.kommons_deprecated.exec.CommandLine
import com.bkahlert.kommons_deprecated.exec.Exec
import com.bkahlert.kommons_deprecated.exec.JavaExec
import com.bkahlert.kommons_deprecated.exec.successfulOrNull
import com.bkahlert.kommons_deprecated.text.Semantics.Symbols

/**
 * [Exec] mock to ease testing.
 */
public open class ExecMock(
    private val process: JavaProcessMock,
    private val name: String? = null,
) : Exec by JavaExec(process, com.bkahlert.kommons_deprecated.Kommons.ExecTemp, CommandLine("echo", ExecMock::class.simpleName!!, name = name)) {

    override fun toString(): String {
        val delegateString =
            "${process.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successfulOrNull?.asEmoji() ?: Symbols.Computation})"
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
