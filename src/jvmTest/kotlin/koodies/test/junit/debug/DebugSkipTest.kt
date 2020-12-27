package koodies.test.junit.debug


import koodies.test.junit.JUnit.runTests
import koodies.test.output.AdHocOutputCapture.Companion.capture
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.PostDiscoveryFilter
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.util.logging.LogManager

@Isolated
@Execution(CONCURRENT)
class DebugSkipTest {

    private var skipTestsRun = false

    @Suppress("unused")
    @BeforeAll
    fun setup() {
        LogManager.getLogManager().reset()
        HiddenSkipTests.hidden = false
    }

    @Suppress("unused")
    @AfterAll
    fun tearDown() {
        HiddenSkipTests.hidden = true
        LogManager.getLogManager().readConfiguration()
        expectThat(skipTestsRun).isTrue()
    }

    @Debug(includeInReport = false)
    @Test
    fun `should skip non-annotated tests on present @Debug`() {
        val selectClass = selectClass(HiddenSkipTests()::class.java)

        val (listener, capturedOutput) = capture(redirect = true) { runTests(selectClass) }.also { skipTestsRun = true }

        expectThat(listener)
            .get { summary }
            .compose("correct test execution") {
                get { testsFoundCount }.isEqualTo(3)
                get { testsSkippedCount }.isEqualTo(2)
                get { testsSucceededCount }.isEqualTo(1)
                get { testsFailedCount }.isEqualTo(0)
            } then { if (allPassed) pass() else fail() }
        expectThat("$capturedOutput")
            .contains("You only see the results of the")
            .contains("1 @Debug annotated test")
            .contains("Don't forget to remove")
    }


    @TestMethodOrder(OrderAnnotation::class)
    @Execution(CONCURRENT)
    class HiddenSkipTests : PostDiscoveryFilter {
        companion object {
            var hidden = true
        }

        override fun apply(testDescriptor: TestDescriptor): FilterResult {
            val isSelf = testDescriptor.uniqueId.segments.any { it.value == this::class.java.name }
            return if (isSelf && hidden) FilterResult.excluded("run separately to not have erroneously tests reported as disabled")
            else FilterResult.included("/")
        }

        private var siblingTestRun = false
        var siblingContainerRun = false

        @Order(1)
        @Test
        fun `should not run due to sibling @Debug`() {
            siblingTestRun = true
            fail { "This test should have been disabled due to @Debug on sibling test." }
        }


        @Nested
        inner class Container {
            @Order(2)
            @Test
            fun `should not run due to sibling @Debug`() {
                siblingContainerRun = true
                fail { "This test should have been disabled due to @Debug on sibling test." }
            }
        }

        @Order(3)
        @Debug
        @Test
        fun `should run (and check if non-@Debug) did not run`() {
            expectThat(siblingTestRun).isFalse()
            expectThat(siblingContainerRun).isFalse()
        }

    }
}
