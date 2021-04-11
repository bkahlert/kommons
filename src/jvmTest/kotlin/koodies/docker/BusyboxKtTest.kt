package koodies.docker

import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process.ExitState
import koodies.test.UniqueId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isFalse

@Execution(CONCURRENT)
class BusyboxKtTest { // TODO generalize to docker run command

    @DockerRequiring(requiredImages = ["busybox"]) @Test
    fun `should start busybox`(uniqueId: UniqueId) {
        val processed = mutableListOf<IO>()
        val dockerProcess: ManagedProcess = Docker.busybox(uniqueId.simplified, "echo busybox") { io ->
            processed.add(io)
        }

        expect {
            that(dockerProcess.waitFor()).isA<ExitState.Success>()
            that(dockerProcess.alive).isFalse()
            that(processed.filterIsInstance<IO.OUT>()).containsExactly(IO.OUT typed "busybox")
        }
    }
}
