package koodies.concurrent

import koodies.concurrent.process.IO
import koodies.exec.Process.ExitState.Failure
import koodies.exec.completesSuccessfully
import koodies.exec.containsDump
import koodies.exec.hasState
import koodies.exec.io
import koodies.concurrent.process.output
import koodies.debug.CapturedOutput
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.test.HtmlFile
import koodies.test.SystemIoRead
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
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue
import kotlin.time.measureTime
import kotlin.time.seconds

@Execution(CONCURRENT)
class ScriptsKtTest {

    @Nested
    inner class ScriptsFn {

        @Test
        fun `should run immediately and synchronously`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val passed = measureTime {
                expectThat(script { !"sleep 1" }).completesSuccessfully()
            }
            expectThat(passed).isGreaterThan(1.seconds)
        }

        @Test
        fun `should run in temp by default`() {
            expectThat(script { !"pwd" }.output()).isEqualTo(Locations.Temp.asString())
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

        @SystemIoRead
        @Test
        fun `should not print to console`(uniqueId: UniqueId, capturedOutput: CapturedOutput) = withTempDir(uniqueId) {
            script { !"echo 'test'" }
            expectThat(capturedOutput).isEmpty()
        }

        @Test
        fun `should have failed state`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(script { !"exit -1" }).hasState<Failure> { io<IO>().containsDump() }
        }
    }

    @Nested
    inner class ScriptOutputContains {

        @Test
        fun `should assert present string`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(scriptOutputContains("echo 'this is a test'", "Test", caseSensitive = false)).isTrue()
        }

        @Test
        fun `should assert missing string`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(scriptOutputContains("echo 'this is a test'", "Missing", caseSensitive = false)).isFalse()
        }

        @Test
        fun `should assert present string case-sensitive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(scriptOutputContains("echo 'this is a test'", "test", caseSensitive = true)).isTrue()
        }

        @Test
        fun `should assert missing string case-sensitive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(scriptOutputContains("echo 'this is a test'", "Test", caseSensitive = true)).isFalse()
        }
    }
}
