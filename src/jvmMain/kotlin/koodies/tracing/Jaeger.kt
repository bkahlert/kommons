package koodies.tracing

import koodies.docker.DockerContainer
import koodies.docker.DockerImage
import koodies.docker.DockerRunCommandLine
import koodies.docker.DockerRunCommandLine.Options
import koodies.exec.Processors.processingProcessor
import koodies.exec.RendererProviders.errorsOnly
import koodies.net.headers
import koodies.text.Semantics.formattedAs
import koodies.tracing.Jaeger.startLocally
import java.net.URI

/**
 * [Jaeger](https://www.jaegertracing.io/) [DockerImage] that one can
 * simply [startLocally].
 */
@Suppress("SpellCheckingInspection")
public object Jaeger : DockerImage("jaegertracing", listOf("all-in-one")) {

    /**
     * Address where the UI can be found.
     */
    public val uiEndpoint: URI = URI.create("http://localhost:16686")

    /**
     * Address where the protobuf endpoint can be found.
     */
    public val protobufEndpoint: URI = URI.create("http://localhost:14250")

    /**
     * Whether Jaeger is running, that is, whether a valid
     * response was received when connecting to the [uiEndpoint].
     */
    public val isRunning: Boolean
        get() = kotlin.runCatching {
            uiEndpoint.headers()["status"]?.any { it.contains("200 OK") } ?: false
        }.onFailure { it.printStackTrace() }.getOrDefault(false)

    /**
     * Starts a Jaeger container locally unless it [isRunning] already.
     */
    public fun startLocally(): String {
        if (isRunning) return protobufEndpoint.toString()
        check(protobufEndpoint.host == "localhost") { "Can only locally but ${protobufEndpoint.formattedAs.input} was specified." }

        DockerRunCommandLine(
            image = this@Jaeger,
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
        ).exec.processing(processor = processingProcessor(errorsOnly()))

        return protobufEndpoint.toString()
    }
}
