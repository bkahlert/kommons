@file:Suppress("MayBeConstant")

package com.bkahlert.kommons.debug

internal object NativeObject {
    val property: String = "Function-property"
}

internal actual fun nativeObject(): Any = NativeObject
