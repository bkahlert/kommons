package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.test.CapturedOutput
import com.bkahlert.kommons.test.SystemIOExclusive
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Test
import strikt.api.expectThat

@SystemIOExclusive
class BackgroundPrinterTest {

    @Test
    fun `should prefix log messages with IO erase marker`(output: CapturedOutput) {
        BackgroundPrinter("This does not appear in the captured output.")
        BackgroundPrinter("But it shows on the actual console.".ansi.italic)
        println("This message is captured.")

        expectThat(output).toStringMatchesCurlyPattern("This message is captured.")
    }
}
