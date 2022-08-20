package com.bkahlert.kommons.logging.logback

import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory
import ch.qos.logback.core.status.ErrorStatus
import ch.qos.logback.core.status.InfoStatus
import ch.qos.logback.core.status.Status
import ch.qos.logback.core.status.WarnStatus
import com.bkahlert.kommons.logging.slf4j.SLF4J

public object StatusLogger {

    public fun info(origin: Any, message: String, vararg args: Any?): Unit =
        addStatus(InfoStatus(SLF4J.format(message, *args.withoutException()), origin, args.exceptionOrNull()))

    public fun warn(origin: Any, message: String, vararg args: Any?): Unit =
        addStatus(WarnStatus(SLF4J.format(message, *args.withoutException()), origin, args.exceptionOrNull()))

    public fun error(origin: Any, message: String, vararg args: Any?): Unit =
        addStatus(ErrorStatus(SLF4J.format(message, *args.withoutException()), origin, args.exceptionOrNull()))

    private fun addStatus(status: Status): Unit =
        StatusViaSLF4JLoggerFactory.addStatus(status)

    private fun Array<out Any?>.withoutException(): Array<out Any?> =
        if (lastOrNull() is Throwable) dropLast(1).toTypedArray() else this

    private fun Array<out Any?>.exceptionOrNull(): Throwable? =
        lastOrNull()?.let { it as? Throwable }
}
