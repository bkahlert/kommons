package com.bkahlert.kommons.tracing.rendering

/**
 * A function that provides a [RendererProvider] based on
 * the given settings default provider.
 */
public typealias RendererProvider = Settings.(default: RendererFactory) -> Renderer
