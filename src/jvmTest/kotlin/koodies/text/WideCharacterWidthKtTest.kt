package koodies.text

import koodies.debug.trace
import koodies.docker.busybox
import koodies.io.Locations
import koodies.logging.InMemoryLogger
import koodies.text.Wcwidth.of
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class WideCharacterWidthKtTest {

    @Disabled
    @Test
    fun testWcwidth() {
        expectThat(of('한'.code)).isEqualTo(2)
        expectThat(of('글'.code)).isEqualTo(2)
        expectThat(of('A'.code)).isEqualTo(1)
        expectThat(of('\u0000'.code)).isEqualTo(0)
        expectThat(of('\t'.code)).isEqualTo(-1)
        expectThat(of('\u0301'.code)).isEqualTo(0)
        expectThat(of('\u09C0'.code)).isEqualTo(1)

        expectThat(of('⮕'.code)).isEqualTo(2)
    }

    @Disabled
    @Test
    fun InMemoryLogger.name() {
        Locations.InternalTemp.path.busybox("wcswidth", "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O FIRST BOOT", logger = this).io.trace
    }
}
