package com.bkahlert.kommons.tracing

import com.bkahlert.kommons.docker.DockerContainer
import com.bkahlert.kommons.docker.DockerImage
import com.bkahlert.kommons.docker.DockerRunCommandLine
import com.bkahlert.kommons.docker.DockerRunCommandLine.Options
import com.bkahlert.kommons.exec.Processors
import com.bkahlert.kommons.exec.RendererProviders
import com.bkahlert.kommons.net.headers
import java.net.URI

/**
 * [Jaeger](https://www.jaegertracing.io/)
 */
@Suppress("SpellCheckingInspection")
public class Jaeger(hostname: String) {

    /**
     * Address of the UI.
     */
    public val uiEndpoint: URI = URI.create("http://$hostname:16686")

    /**
     * Address of the protobuf endpoint.
     */
    public val protobufEndpoint: URI = URI.create("http://$hostname:14250")

    /**
     * Whether Jaeger is running, that is, whether a valid
     * response is received when connecting to the [uiEndpoint].
     */
    public val isRunning: Boolean
        get() = kotlin.runCatching {
            uiEndpoint.headers()["status"]?.any { it.contains("200 OK") } ?: false
        }.onFailure { it.printStackTrace() }.getOrDefault(false)

    /**
     * Starts a Jaeger container locally unless Jaeger [isRunning] already.
     */
    public fun startLocally(): String {
        if (!isRunning) {
            DockerRunCommandLine(
                image = DockerImage("jaegertracing", listOf("all-in-one")),
                options = Options(
                    name = DockerContainer.from("jaeger"),
                    detached = true,
                    publish = listOf(
                        "5775:5775/udp",
                        "6831:6831/udp",
                        "6832:6832/udp",
                        "5778:5778",
                        "16686:${uiEndpoint.port}",
                        "14268:14268",
                        "14250:${protobufEndpoint.port}",
                        "9411:9411",
                    ),
                ),
            ).exec.processing(processor = Processors.processingProcessor(RendererProviders.errorsOnly()))
        }

        return protobufEndpoint.toString()
    }
}
