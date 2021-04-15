package koodies.debug

import koodies.jvm.currentStackTrace

private val markerClassName = XRay::class.qualifiedName
private val skipMethods = listOf("xray", "trace")

public actual val XRay<*>.callSource: String
    get() = this::class.qualifiedName.let { skipTo ->
        currentStackTrace
            .dropWhile { it.className != skipTo }
            .drop(1)
            .dropWhile { frame -> skipMethods.any { frame.methodName.endsWith(it, ignoreCase = true) } }
            .firstOrNull()?.let { "${it.fileName}:${it.lineNumber}" } ?: "❓"
    }
