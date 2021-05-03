package koodies.debug

import koodies.test.TextFile
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.Symbols
import koodies.text.asCodePointSequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


class DebugPropertyKtTest {

    @Test
    fun `should render String`() {
        expectThat(TextFile.data.decodeToString().debug.ansiRemoved)
            .isEqualTo("❬a⏎␤𝕓⏎␍⏎␊☰⏎␊👋⏎␊⦀11❭")
    }

    @Test
    fun `should render Byte`() {
        expectThat(byteArrayOf(0x01).first().debug.ansiRemoved)
            .isEqualTo("❬0x01␁❭")
    }

    @Test
    fun `should render ByteArray`() {
        expectThat(byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE).debug.ansiRemoved)
            .isEqualTo("【0x80\u0080,0xFFÿ,0x00${Symbols.Null.ansiRemoved},0x01␁,0x7F␡】")
    }

    @Test
    fun `should render any Array`() {
        val stringArray = TextFile.data.decodeToString().asCodePointSequence().map { it.string }.toList().toTypedArray()
        expectThat(stringArray.debug.ansiRemoved)
            .isEqualTo("【a⦀1,⏎␤⦀1,𝕓⦀2,⏎␍⦀1,⏎␊⦀1,☰⦀1,⏎␊⦀1,👋⦀2,⏎␊⦀1】")
    }
}
