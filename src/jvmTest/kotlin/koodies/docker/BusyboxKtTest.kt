package koodies.docker

import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.process
import koodies.test.UniqueId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse

@Execution(CONCURRENT)
class BusyboxKtTest {

    @DockerRequiring @Test
    fun `should start busybox`(uniqueId: UniqueId) {
        val processed = mutableListOf<IO>()
        val dockerProcess: ManagedProcess = Docker.busybox(uniqueId.simple, "echo busybox").execute().process { io ->
            processed.add(io)
        }

        expect {
            that(dockerProcess.waitFor()).isEqualTo(0)
            that(dockerProcess.alive).isFalse()
            that(processed).containsExactly(IO.Type.OUT typed "busybox")
        }
    }
}
