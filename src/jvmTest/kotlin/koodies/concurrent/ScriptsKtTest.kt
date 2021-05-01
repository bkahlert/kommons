package koodies.concurrent

import koodies.concurrent.process.output
import koodies.debug.CapturedOutput
import koodies.exec.Process.ExitState.Failure
import koodies.exec.containsDump
import koodies.exec.hasState
import koodies.exec.io
import koodies.exec.output
import koodies.exec.succeeds
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.test.HtmlFile
import koodies.test.SystemIORead
import koodies.test.UniqueId
import koodies.test.copyToDirectory
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import kotlin.time.measureTime
import kotlin.time.seconds

@Execution(CONCURRENT)
class ScriptsKtTest {

    @Nested
    inner class ScriptsFn {

        @Test
        fun `should run immediately and synchronously`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val passed = measureTime {
                expectThat(script { !"sleep 1" }).succeeds()
            }
            expectThat(passed).isGreaterThan(1.seconds)
        }

        @Test
        fun `should run in receiver path if present`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val receiverPath = this
            require(receiverPath != Locations.Temp) { "test must not run in temp" }
            expectThat(script { !"pwd" }.output()).isEqualTo(receiverPath.asString())
        }

        @Test
        fun `should provide IO`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(script { !"echo 'test'" }).output.isEqualTo("test")
        }

        @Test
        fun `should provide multi-line OUT`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val name = HtmlFile.copyToDirectory(this)
            val script = script { !"cat $name" }
            expectThat(script.output()).isEqualTo(HtmlFile.text)
        }

        @SystemIORead
        @Test
        fun `should not print to console`(uniqueId: UniqueId, capturedOutput: CapturedOutput) = withTempDir(uniqueId) {
            script { !"echo 'test'" }
            expectThat(capturedOutput).isEmpty()
        }

        @Test
        fun `should have failed state`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val subject = script { !"exit -1" }
            expectThat(subject).hasState<Failure> { io().containsDump() }
        }
    }
}
