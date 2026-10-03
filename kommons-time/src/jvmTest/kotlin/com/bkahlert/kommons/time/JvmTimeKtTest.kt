package com.bkahlert.kommons.time

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.nio.file.attribute.FileTime
import java.util.TimeZone
import kotlin.time.Clock
import kotlin.time.toJavaInstant

class JvmInstantTest {

    @Test fun `should return FileTime`() {
        val now = Clock.System.now()
        now.toFileTime() shouldBe FileTime.from(now.toJavaInstant())
    }
}

@Isolated
class JvmTimeKtTest {

    @Test fun `to_moment_string of a far date is that date whatever offset it had`() {
        // Kiritimati moved from UTC-10 to UTC+14 in 1995: a 24-hour offset difference to any date before.
        withDefaultTimeZone("Pacific/Kiritimati") {
            val date = LocalDate(1994, 6, 15)

            val result = date.toMomentString()

            result shouldBe date.toLocalDateString()
        }
    }
}

private fun withDefaultTimeZone(id: String, block: () -> Unit) {
    val previous = TimeZone.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone(id))
    try {
        block()
    } finally {
        TimeZone.setDefault(previous)
    }
}
