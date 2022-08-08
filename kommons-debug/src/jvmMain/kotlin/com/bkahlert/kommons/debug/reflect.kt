package com.bkahlert.kommons.debug

import java.lang.reflect.AccessibleObject

/** Alias for [AccessibleObject.isAccessible] */
public var AccessibleObject.accessible: Boolean
    get() {
        @Suppress("DEPRECATION")
        return isAccessible
    }
    set(value) {
        @Suppress("DEPRECATION")
        isAccessible = value
    }
