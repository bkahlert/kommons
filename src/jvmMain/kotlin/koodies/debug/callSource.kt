package koodies.debug

import koodies.runtime.JVM

private val markerClassName = XRay::class.qualifiedName
private val skipMethods = listOf("xray", "trace")

public actual val XRay<*>.callSource: String
    get() = this::class.qualifiedName.let { skipTo ->
        JVM.currentStackTrace
            .dropWhile { it.className != skipTo }
            .drop(1)
            .dropWhile { frame -> skipMethods.any { frame.methodName.endsWith(it, ignoreCase = true) } }
            .firstOrNull()?.let { "${it.fileName}:${it.lineNumber}" } ?: "❓"
    }
