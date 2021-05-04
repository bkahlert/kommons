package koodies.io.file

import koodies.io.useClassPath
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.io.path.readText

class ReadTextKtTest {

    @TestFactory
    fun `should read complete string`() {
        @Suppress("SpellCheckingInspection", "LongLine")
        val expected = """
                console=serial0,115200 console=tty1 root=PARTUUID=907af7d0-02 rootfstype=ext4 elevator=deadline fsck.repair=yes rootwait quiet init=/usr/lib/raspi-config/init_resize.sh

            """.trimIndent()
        expectThat(useClassPath("cmdline.txt") { readText() }).isEqualTo(expected)
    }
}
