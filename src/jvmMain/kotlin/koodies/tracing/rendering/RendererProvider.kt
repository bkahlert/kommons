package koodies.tracing.rendering

/**
 * A function that provides a [RendererProvider] based on
 * the given settings default provider.
 */
public typealias RendererProvider = Settings.((defaultProvider: Settings) -> Renderer) -> Renderer
