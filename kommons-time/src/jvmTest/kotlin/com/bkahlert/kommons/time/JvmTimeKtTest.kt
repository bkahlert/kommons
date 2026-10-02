package com.bkahlert.kommons.time

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.attribute.FileTime
import kotlin.time.Clock
import kotlin.time.toJavaInstant

class JvmInstantTest {

    @Test fun `should return FileTime`() {
        val now = Clock.System.now()
        now.toFileTime() shouldBe FileTime.from(now.toJavaInstant())
    }
}
