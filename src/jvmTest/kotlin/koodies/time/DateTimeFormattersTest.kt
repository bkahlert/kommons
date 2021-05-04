@file:Suppress("ClassName")

package koodies.time

import koodies.time.DateTimeFormatters.ISO8601_INSTANT
import koodies.time.DateTimeFormatters.ISO8601_LOCAL_DATE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate

class DateTimeFormattersTest {

    val instant: Instant = Instant.parse("2021-11-09T00:08:02.123456789Z")
    val localDate: LocalDate = LocalDate.parse("2021-11-09")

    @Test
    fun `should convert instant to local date roundtrip`() {
        expectThat(localDate.toInstant().toLocalDate()).isEqualTo(localDate)
    }

    @Nested
    inner class ByDefault {
        @Nested
        inner class ISO_INSTANT {
            @Test
            fun `should format instant`() {
                val formatted = ISO8601_INSTANT.format(instant)
                expectThat(formatted).isEqualTo("2021-11-09T00:08:02.123456789Z")
            }

            @Test
            fun `should parse instant`() {
                val parsed: Instant = ISO8601_INSTANT.parseAny("2021-11-09T00:08:02Z")
                expectThat(parsed).isEqualTo(Instant.ofEpochMilli(1636416482000))
            }
        }

        @Nested
        inner class ISO_LOCAL_DATE {
            @Test
            fun `should format local date`() {
                val formatted = ISO8601_LOCAL_DATE.format(localDate)
                expectThat(formatted).isEqualTo("2021-11-09")
            }


            @Test
            fun `should parse local date`() {
                val parsed: LocalDate = ISO8601_LOCAL_DATE.parseAny("2021-11-09")
                expectThat(parsed).isEqualTo(localDate)
            }
        }
    }

    @Nested
    inner class ForPath {
        @Nested
        inner class ISO_INSTANT {
            @Test
            fun `should format instant`() {
                val formatted = ISO8601_INSTANT.format<Path>(instant)
                expectThat(formatted).isEqualTo("2021-11-09T00-08-02")
            }

            @Test
            fun `should parse instant`() {
                val parsed: Instant = ISO8601_INSTANT.parse("2021-11-09T00-08-02", Path::class)
                expectThat(parsed).isEqualTo(Instant.ofEpochMilli(1636416482000))
            }
        }

        @Nested
        inner class ISO_LOCAL_DATE {
            @Test
            fun `should format local date`() {
                val formatted = ISO8601_LOCAL_DATE.format<Path>(localDate)
                expectThat(formatted).isEqualTo("2021-11-09")
            }


            @Test
            fun `should parse local date`() {
                val parsed = ISO8601_LOCAL_DATE.parse<LocalDate, Path>("2021-11-09")
                expectThat(parsed).isEqualTo(localDate)
            }
        }
    }
}
