package koodies.debug

import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.test.TextFile
import koodies.text.asCodePointSequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class DebugPropertyKtTest {

    @Test
    fun `should render String`() {
        expectThat(TextFile.data.decodeToString().debug.removeEscapeSequences())
            .isEqualTo("❬a⏎␤𝕓⏎␍⏎␊☰⏎␊👋⏎␊⫻11❭")
    }

    @Test
    fun `should render Byte`() {
        expectThat(byteArrayOf(0x01).first().debug.removeEscapeSequences())
            .isEqualTo("❬0x01␁❭")
    }

    @Test
    fun `should render ByteArray`() {
        expectThat(byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE).debug.removeEscapeSequences())
            .isEqualTo("【0x80\u0080,0xFFÿ,0x00␀,0x01␁,0x7F␡】")
    }

    @Test
    fun `should render any Array`() {
        val stringArray = TextFile.data.decodeToString().asCodePointSequence().map { it.string }.toList().toTypedArray()
        expectThat(stringArray.debug.removeEscapeSequences())
            .isEqualTo("【a⫻1,⏎␤⫻1,𝕓⫻2,⏎␍⫻1,⏎␊⫻1,☰⫻1,⏎␊⫻1,👋⫻2,⏎␊⫻1】")
    }
}
