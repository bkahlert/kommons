package koodies.tracing

import koodies.docker.DockerImage
import koodies.docker.DockerRunCommandLine
import koodies.net.headers
import koodies.text.Semantics.formattedAs
import java.net.URI

object Jaeger : DockerImage("jaegertracing", listOf("all-in-one")) {

    val restEndpoint = URI.create("http://localhost:16686")
    val protobufEndpoint = URI.create("http://localhost:14250")
    val isRunning: Boolean
        get() = kotlin.runCatching {
            restEndpoint.headers()["status"]?.any { it.contains("200 OK") } ?: false
        }.onFailure { it.printStackTrace() }.getOrDefault(false)

    fun startLocally(): String {
        if (isRunning) return protobufEndpoint.toString()
        check(protobufEndpoint.host == "localhost") { "Can only locally but ${protobufEndpoint.formattedAs.input} was specified." }

        DockerRunCommandLine {
            image by this@Jaeger
            options {
                name { "jaeger" }
                detached { on }
                publish {
                    +"5775:5775/udp"
                    +"6831:6831/udp"
                    +"6832:6832/udp"
                    +"5778:5778"
                    +"16686:${restEndpoint.port}"
                    +"14268:14268"
                    +"14250:${protobufEndpoint.port}"
                    +"9411:9411"
                }
            }
        }.exec.logging()

        return protobufEndpoint.toString()
    }
}
