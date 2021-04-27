package koodies.docker

import koodies.concurrent.process.IO
import koodies.docker.TestImages.BusyBox
import koodies.exec.Exec
import koodies.exec.Process.ExitState
import koodies.exec.alive
import koodies.test.UniqueId
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isFalse

@Disabled
@Execution(CONCURRENT)
class BusyboxKtTest { // TODO generalize to docker run command

    @DockerRequiring([BusyBox::class]) @Test
    fun `should start busybox`(uniqueId: UniqueId) {
        val processed = mutableListOf<IO>()
        val dockerExec: Exec = Docker.busybox(uniqueId.simplified, "echo busybox") { io ->
            processed.add(io)
        }

        expect {
            that(dockerExec.waitFor()).isA<ExitState.Success>()
            that(dockerExec.alive).isFalse()
            that(processed.filterIsInstance<IO.OUT>()).containsExactly(IO.OUT typed "busybox")
        }
    }
}
