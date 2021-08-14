package koodies.time

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateTimeFormatterTest {
    
    val localDate = LocalDate.parse("2010-09-20")

    private class Foo
    private class Bar

    val formatter = DateTimeFormatter(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        Bar::class to DateTimeFormatter.ofPattern("yyyy=MM=dd"),
    )

    @Nested
    inner class Formatting {
        @Test
        fun `should format using fallback on missing type`() {
            expectThat(formatter.format(localDate)).isEqualTo("2010-09-20")
        }

        @Test
        fun `should format using fallback on no match`() {
            expectThat(formatter.format<Foo>(localDate)).isEqualTo("2010-09-20")
        }

        @Test
        fun `should format using responsible formatter on match`() {
            expectThat(formatter.format<Bar>(localDate)).isEqualTo("2010=09=20")
        }
    }

    @Nested
    inner class Parsing {
        @Test
        fun `should parse using fallback on missing type`() {
            expectThat(formatter.parse<LocalDate, Any>("2010-09-20")).isEqualTo(localDate)
        }

        @Test
        fun `should parse using fallback on no match`() {
            expectThat(formatter.parse<LocalDate, Foo>("2010-09-20")).isEqualTo(localDate)
        }

        @Test
        fun `should parse using responsible formatter on match`() {
            expectThat(formatter.parse<LocalDate, Bar>("2010=09=20")).isEqualTo(localDate)
        }
    }
}
