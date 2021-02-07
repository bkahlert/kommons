package koodies.concurrent

import koodies.builder.ArrayBuilder
import org.junit.jupiter.api.Test

class DockerStopCommandLineTest {


    @Test
    fun name2() {
        val xx = ArrayBuilder.buildArray<String> {
            +"a"
            +"bn"
        }
        println(xx)
    }

    @Test
    fun name() {
        val dockerStopCommandLine = DockerStopCommandLineBuilder.build {
            optionsX { 20 }
            containers { +"test1" + "test2" }
        }
        println(dockerStopCommandLine)
    }
}
