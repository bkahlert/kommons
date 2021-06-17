package koodies.tracing

import koodies.tracing.Span.State.Started
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isNotNull

class TestTelemetryTest {

    @Test
    fun Span.`should be started`() {
        expectThat(state).isA<Started>()
    }

    @Test
    fun Span.`should have trace ID`() {
        expectThat(traceId).isNotNull() and {
            isValid()
        }
    }

    @Test
    fun Span.`should have span ID`() {
        expectThat(spanId).isNotNull() and {
            isValid()
        }
    }
}
