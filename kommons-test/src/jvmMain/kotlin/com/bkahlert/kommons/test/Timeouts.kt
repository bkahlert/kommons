package com.bkahlert.kommons.test

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * Specifies a timeout of one minute.
 * @see Timeout
 */
@Timeout(1, unit = MINUTES)
@Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
public annotation class OneMinuteTimeout

/**
 * Specifies a timeout of two minutes.
 * @see Timeout
 */
@Timeout(2, unit = MINUTES)
@Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
public annotation class TwoMinutesTimeout

/**
 * Specifies a timeout of five minutes.
 * @see Timeout
 */
@Timeout(5, unit = MINUTES)
@Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
public annotation class FiveMinutesTimeout

/**
 * Specifies a timeout of ten minutes.
 * @see Timeout
 */
@Timeout(10, unit = MINUTES)
@Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
public annotation class TenMinutesTimeout

/**
 * Specifies a timeout of 15 minutes.
 * @see Timeout
 */
@Timeout(15, unit = MINUTES)
@Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
public annotation class FifteenMinutesTimeout

/**
 * Specifies a timeout of 30 minutes.
 * @see Timeout
 */
@Timeout(30, unit = MINUTES)
@Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
public annotation class ThirtyMinutesTimeout

/**
 * Specifies a timeout of two minutes and adds the tag `slow`.
 * @see Timeout
 */
@TwoMinutesTimeout
@Tag("slow")
@Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
public annotation class Slow
