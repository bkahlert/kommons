package com.bkahlert.kommons.time

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.nio.file.attribute.FileTime
import java.util.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class JvmInstantTest {

    @Test fun `should return FileTime`() {
        val now = Clock.System.now()
        now.toFileTime() shouldBe FileTime.from(now.toJavaInstant())
    }
}

@Isolated
class JvmTimeKtTest {

    @Test fun `toMomentString of a far date is that date whatever offset it had`() {
        // Kiritimati moved from UTC-10 to UTC+14 in 1995: a 24-hour offset difference to any date from 1979 to 1994.
        withDefaultTimeZone("Pacific/Kiritimati") {
            val date = LocalDate(1994, 6, 15)

            val result = date.toMomentString()

            result shouldBe date.toLocalDateString()
        }
    }

    @Test fun `toLocalDateString of an instant follows the default time zone`() {
        val instant = Instant.parse("1994-06-15T20:00:00Z")

        val inUtc = withDefaultTimeZone("UTC") { instant.toLocalDateString() }
        val fourteenHoursEast = withDefaultTimeZone("Etc/GMT-14") { instant.toLocalDateString() }

        inUtc shouldBe LocalDate(1994, 6, 15).toLocalDateString()
        fourteenHoursEast shouldBe LocalDate(1994, 6, 16).toLocalDateString()
    }
}

private fun <T> withDefaultTimeZone(id: String, block: () -> T): T {
    val previous = TimeZone.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone(id))
    try {
        return block()
    } finally {
        TimeZone.setDefault(previous)
    }
}
