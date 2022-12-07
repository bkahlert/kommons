package com.bkahlert.kommons

import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.junit.jupiter.api.Test
import java.nio.file.attribute.FileTime

class JvmInstantTest {

    @Test fun `should return FileTime`() {
        val now = Clock.System.now()
        now.toFileTime() shouldBe FileTime.from(now.toJavaInstant())
    }
}
