package com.bkahlert.kommons.logging.logback

import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory
import ch.qos.logback.core.status.ErrorStatus
import ch.qos.logback.core.status.InfoStatus
import ch.qos.logback.core.status.Status
import ch.qos.logback.core.status.WarnStatus
import com.bkahlert.kommons.logging.core.SLF4J

/** Logger for status messages related to [Logback]. */
public object StatusLogger {

    /** Logs the specified info [message] with `{}` replaced by the specified [args] and the specified [origin]. */
    public fun info(origin: Any, message: String, vararg args: Any?): Unit =
        addStatus(InfoStatus(SLF4J.format(message, *args.withoutException()), origin, args.exceptionOrNull()))

    /** Logs the specified info [message] with `{}` replaced by the specified [args] and the qualified class name of [T] as the origin. */
    public inline fun <reified T> info(message: String, vararg args: Any?): Unit =
        info(T::class.qualifiedName ?: T::class.toString(), message, *args)

    /** Logs the specified warn [message] with `{}` replaced by the specified [args] and the specified [origin]. */
    public fun warn(origin: Any, message: String, vararg args: Any?): Unit =
        addStatus(WarnStatus(SLF4J.format(message, *args.withoutException()), origin, args.exceptionOrNull()))

    /** Logs the specified warn [message] with `{}` replaced by the specified [args] and the qualified class name of [T] as the origin. */
    public inline fun <reified T> warn(message: String, vararg args: Any?): Unit =
        warn(T::class.qualifiedName ?: T::class.toString(), message, *args)

    /** Logs the specified error [message] with `{}` replaced by the specified [args] and the specified [origin]. */
    public fun error(origin: Any, message: String, vararg args: Any?): Unit =
        addStatus(ErrorStatus(SLF4J.format(message, *args.withoutException()), origin, args.exceptionOrNull()))

    /** Logs the specified error [message] with `{}` replaced by the specified [args] and the qualified class name of [T] as the origin. */
    public inline fun <reified T> error(message: String, vararg args: Any?): Unit =
        error(T::class.qualifiedName ?: T::class.toString(), message, *args)

    private fun addStatus(status: Status): Unit =
        StatusViaSLF4JLoggerFactory.addStatus(status)

    private fun Array<out Any?>.withoutException(): Array<out Any?> =
        if (lastOrNull() is Throwable) dropLast(1).toTypedArray() else this

    private fun Array<out Any?>.exceptionOrNull(): Throwable? =
        lastOrNull()?.let { it as? Throwable }
}
