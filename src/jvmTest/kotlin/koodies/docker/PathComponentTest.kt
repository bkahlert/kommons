@file:Suppress("FINAL_UPPER_BOUND")

package koodies.docker

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message

@Execution(CONCURRENT)
class PathComponentTest {

    @Nested
    inner class OfFactory {

        @Test
        fun `should instantiate valid component`() {
            expectThat(PathComponent.of("repo-123")).component.isEqualTo("repo-123")
        }

        @Test
        fun `should throw on illegal component`() {
            expectCatching { PathComponent.of("repo/123") }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should provide pattern in exception`() {
            expectCatching { PathComponent.of("repo/123") }.isFailure().message.isNotNull().contains(PathComponent.REGEX.pattern)
        }
    }
}

val <T : PathComponent> Assertion.Builder<T>.component
    get() = get("component %s") { component }
