package koodies.exec

import koodies.exec.IO.Error
import koodies.exec.IO.Meta
import koodies.text.truncate
import koodies.tracing.rendering.BlockRenderer
import koodies.tracing.rendering.CompactRenderer
import koodies.tracing.rendering.OneLineRenderer
import koodies.tracing.rendering.Renderable
import koodies.tracing.rendering.Renderer
import koodies.tracing.rendering.RendererFactory
import koodies.tracing.rendering.RendererProvider
import koodies.tracing.rendering.Settings
import koodies.tracing.rendering.Styles.None

public object RendererProviders {

    public val NOOP: RendererProvider = object : (Settings, RendererFactory) -> Renderer {
        override fun invoke(settings: Settings, default: RendererFactory): Renderer {
            return Renderer.Companion.NOOP
        }

        override fun toString(): String {
            return "NOOP"
        }
    }

    public fun block(customize: Settings.() -> Settings = { this }): RendererProvider = {
        BlockRenderer(customize())
    }

    public fun oneLine(customize: Settings.() -> Settings = { this }): RendererProvider = {
        OneLineRenderer(customize())
    }

    public fun compact(customize: Settings.() -> Settings = { this }): RendererProvider = {
        CompactRenderer(customize())
    }

    /**
     * Formats the output in a compact fashion with each message generically shortened using the following rules:
     * - remove meta messages
     * - messages containing a colon (e.g. `first: second`) are reduced to the part after the colon (e.g. `second`)
     * - if the reduced message is still longer than the given [maxMessageLength], the message is truncated
     *
     * Example output: `Pulling busybox ❱ latest ❱ library/busybox ❱ sha256:ce2…af390a2ac ❱ busybox:latest ❱ latest ✔`
     */
    public fun summary(
        maxMessageLength: Int = 20,
        customize: Settings.() -> Settings = { this },
    ): RendererProvider = {
        val customized = customize()
        OneLineRenderer(customized.copy(
            contentFormatter = {
                customized.contentFormatter(
                    if (it is Meta) ""
                    else it.toString().run {
                        substringAfter(":").trim().run {
                            takeIf { length < maxMessageLength } ?: split(Regex("\\s+")).last().truncate(maxMessageLength)
                        }
                    })
            }
        ))
    }

    /**
     * Formats the output by hiding all details, that is, only the name and an eventual occurring exception is displayed.
     *
     * Example output: `Listing images ✔`
     */
    public fun noDetails(
        customize: Settings.() -> Settings = { this },
    ): RendererProvider = {
        OneLineRenderer(customize().copy(
            nameFormatter = { it },
            contentFormatter = { null },
            style = None,
        ))
    }

    /**
     * Filters all IO but errors.
     *
     * Example output: `ϟ Process 64207 terminated with exit code 255.`
     */
    public fun errorsOnly(
        maxNameLength: Int = 0,
        customize: Settings.() -> Settings = { this },
    ): RendererProvider = {
        val customized = customize()
        CompactRenderer(customize().copy(
            nameFormatter = {
                if (maxNameLength > 0) {
                    val originallyFormattedName = customized.nameFormatter(it)
                    originallyFormattedName?.let(Renderable::of)?.render(maxNameLength, 1)
                } else {
                    null
                }
            },
            contentFormatter = {
                if (it is Error) customized.contentFormatter(it)
                else null
            },
            decorationFormatter = { "" },
            returnValueTransform = {
                it.takeUnless { it.successful }?.let(customized.returnValueTransform)
            },
            style = None,
        ))
    }
}
