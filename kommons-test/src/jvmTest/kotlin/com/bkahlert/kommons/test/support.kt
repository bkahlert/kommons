package com.bkahlert.kommons.test

import java.util.stream.Stream
import kotlin.streams.asSequence

/**
 * Returns a List containing all elements.
 *
 * On some platforms [Stream.toList] will not work.
 */
internal fun <T> Stream<T>.asList() = asSequence().toList()
