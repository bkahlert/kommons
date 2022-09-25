package com.bkahlert.kommons_deprecated.tracing.rendering

import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons_deprecated.test.CapturedOutput
import com.bkahlert.kommons_deprecated.test.SystemIOExclusive
import com.bkahlert.kommons_deprecated.text.ANSI.Text.Companion.ansi
import org.junit.jupiter.api.Test

@SystemIOExclusive
class BackgroundPrinterTest {

    @Test
    fun `should prefix log messages with IO erase marker`(output: CapturedOutput) {
        BackgroundPrinter("This does not appear in the captured output.")
        BackgroundPrinter("But it shows on the actual console.".ansi.italic)
        println("This message is captured.")

        output.toString() shouldMatchGlob "This message is captured."
    }
}
