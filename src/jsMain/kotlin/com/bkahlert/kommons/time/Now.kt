package com.bkahlert.kommons.time

import kotlin.js.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The current date and time.
 */
public inline val Now: Date get() = Date()

/**
 * Adds the [other] date to this date.
 */
public inline operator fun Date.plus(other: Date): Duration = (getTime().toLong() + other.getTime().toLong()).milliseconds

/**
 * Subtracts the [other] date from this date.
 */
public inline operator fun Date.minus(other: Date): Duration = (getTime().toLong() - other.getTime().toLong()).milliseconds

/**
 * Adds the [other] duration to this date.
 */
public inline operator fun Date.plus(other: Duration): Date = Date(getTime().toLong() + other.inWholeMilliseconds)

/**
 * Subtracts the [other] duration from this date.
 */
public inline operator fun Date.minus(other: Duration): Date = Date(getTime().toLong() - other.inWholeMilliseconds)
