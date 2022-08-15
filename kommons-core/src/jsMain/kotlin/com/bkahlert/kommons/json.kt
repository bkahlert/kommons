package com.bkahlert.kommons

import kotlin.js.Json

/** Returns a simple JavaScript object, as [Json], using the specified [properties]. */
public fun json(properties: Map<String, Any?>): Json =
    kotlin.js.json(*properties.entries.map { (key, value) -> key to value }.toTypedArray())
