package koodies.tracing

import koodies.docker.DockerImage
import koodies.docker.DockerRunCommandLine
import koodies.exec.RendererProviders
import koodies.net.headers
import koodies.text.Semantics.formattedAs
import koodies.tracing.Jaeger.startLocally
import koodies.tracing.Tracer.NOOP
import java.net.URI

/**
 * [Jaeger](https://www.jaegertracing.io/) [DockerImage] that you can
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
                    +"16686:${uiEndpoint.port}"
                    +"14268:14268"
                    +"14250:${protobufEndpoint.port}"
                    +"9411:9411"
                }
            }
        }.exec.logging(renderer = RendererProviders.errorsOnly(), tracer = NOOP)

        return protobufEndpoint.toString()
    }
}
