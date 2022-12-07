package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Timestamp
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.event.Level.DEBUG
import org.slf4j.event.Level.ERROR
import org.slf4j.event.Level.INFO
import org.slf4j.event.Level.TRACE
import org.slf4j.event.Level.WARN
import org.slf4j.event.LoggingEvent
import java.util.Collections
import kotlin.reflect.KClass

data class RecordedLoggingEvent(
    private val level: Level,
    private val marker: Marker?,
    private val loggerName: String,
    private val message: String?,
    private val threadName: String,
    private val argumentArray: List<Any?>,
    private val timeStamp: Long,
    private val throwable: Throwable?,
) : LoggingEvent {
    override fun getLevel(): Level = level
    override fun getMarker(): Marker? = marker
    override fun getLoggerName(): String = loggerName
    override fun getMessage(): String? = message
    override fun getThreadName(): String = threadName
    override fun getArgumentArray(): Array<Any?> = argumentArray.toTypedArray()
    override fun getTimeStamp(): Long = timeStamp
    override fun getThrowable(): Throwable? = throwable
}

class RecordingLogger(
    private val name: String?,
) : Logger {
    constructor(kClass: KClass<*>? = null) : this(kClass?.simpleName)

    val events: List<LoggingEvent> get() = _events
    private val _events: MutableList<LoggingEvent> = Collections.synchronizedList(mutableListOf())
    private fun log(
        level: Level,
        message: String?,
        vararg argumentArray: Any?,
        marker: Marker? = null,
        threadName: String = Thread.currentThread().name,
        timeStamp: Long = Timestamp,
        throwable: Throwable? = null,
    ): Unit {
        _events.add(RecordedLoggingEvent(level, marker, getName(), message, threadName, argumentArray.asList(), timeStamp, throwable))
    }

    override fun getName(): String = name ?: "<unnamed>"

    override fun isTraceEnabled(): Boolean = true
    override fun isTraceEnabled(marker: Marker?): Boolean = true
    override fun trace(msg: String?) = log(TRACE, msg)
    override fun trace(format: String?, arg: Any?) = log(TRACE, format, arg)
    override fun trace(format: String?, arg1: Any?, arg2: Any?) = log(TRACE, format, arg1, arg2)
    override fun trace(format: String?, vararg arguments: Any?) = log(TRACE, format, *arguments)
    override fun trace(msg: String?, t: Throwable?) = log(TRACE, msg, throwable = t)
    override fun trace(marker: Marker?, msg: String?) = log(TRACE, msg, marker = marker)
    override fun trace(marker: Marker?, format: String?, arg: Any?) = log(TRACE, format, arg, marker = marker)
    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) = log(TRACE, format, arg1, arg2, marker = marker)
    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) = log(TRACE, format, *argArray, marker = marker)
    override fun trace(marker: Marker?, msg: String?, t: Throwable?) = log(TRACE, msg, marker = marker, throwable = t)

    override fun isDebugEnabled(): Boolean = true
    override fun isDebugEnabled(marker: Marker?): Boolean = true
    override fun debug(msg: String?) = log(DEBUG, msg)
    override fun debug(format: String?, arg: Any?) = log(DEBUG, format, arg)
    override fun debug(format: String?, arg1: Any?, arg2: Any?) = log(DEBUG, format, arg1, arg2)
    override fun debug(format: String?, vararg arguments: Any?) = log(DEBUG, format, *arguments)
    override fun debug(msg: String?, t: Throwable?) = log(DEBUG, msg, throwable = t)
    override fun debug(marker: Marker?, msg: String?) = log(DEBUG, msg, marker = marker)
    override fun debug(marker: Marker?, format: String?, arg: Any?) = log(DEBUG, format, arg, marker = marker)
    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) = log(DEBUG, format, arg1, arg2, marker = marker)
    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) = log(DEBUG, format, *arguments, marker = marker)
    override fun debug(marker: Marker?, msg: String?, t: Throwable?) = log(DEBUG, msg, marker = marker, throwable = t)

    override fun isInfoEnabled(): Boolean = true
    override fun isInfoEnabled(marker: Marker?): Boolean = true
    override fun info(msg: String?) = log(INFO, msg)
    override fun info(format: String?, arg: Any?) = log(INFO, format, arg)
    override fun info(format: String?, arg1: Any?, arg2: Any?) = log(INFO, format, arg1, arg2)
    override fun info(format: String?, vararg arguments: Any?) = log(INFO, format, *arguments)
    override fun info(msg: String?, t: Throwable?) = log(INFO, msg, throwable = t)
    override fun info(marker: Marker?, msg: String?) = log(INFO, msg, marker = marker)
    override fun info(marker: Marker?, format: String?, arg: Any?) = log(INFO, format, arg, marker = marker)
    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) = log(INFO, format, arg1, arg2, marker = marker)
    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) = log(INFO, format, *arguments, marker = marker)
    override fun info(marker: Marker?, msg: String?, t: Throwable?) = log(INFO, msg, marker = marker, throwable = t)

    override fun isWarnEnabled(): Boolean = true
    override fun isWarnEnabled(marker: Marker?): Boolean = true
    override fun warn(msg: String?) = log(WARN, msg)
    override fun warn(format: String?, arg: Any?) = log(WARN, format, arg)
    override fun warn(format: String?, arg1: Any?, arg2: Any?) = log(WARN, format, arg1, arg2)
    override fun warn(format: String?, vararg arguments: Any?) = log(WARN, format, *arguments)
    override fun warn(msg: String?, t: Throwable?) = log(WARN, msg, throwable = t)
    override fun warn(marker: Marker?, msg: String?) = log(WARN, msg, marker = marker)
    override fun warn(marker: Marker?, format: String?, arg: Any?) = log(WARN, format, arg, marker = marker)
    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) = log(WARN, format, arg1, arg2, marker = marker)
    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) = log(WARN, format, *arguments, marker = marker)
    override fun warn(marker: Marker?, msg: String?, t: Throwable?) = log(WARN, msg, marker = marker, throwable = t)

    override fun isErrorEnabled(): Boolean = true
    override fun isErrorEnabled(marker: Marker?): Boolean = true
    override fun error(msg: String?) = log(ERROR, msg)
    override fun error(format: String?, arg: Any?) = log(ERROR, format, arg)
    override fun error(format: String?, arg1: Any?, arg2: Any?) = log(ERROR, format, arg1, arg2)
    override fun error(format: String?, vararg arguments: Any?) = log(ERROR, format, *arguments)
    override fun error(msg: String?, t: Throwable?) = log(ERROR, msg, throwable = t)
    override fun error(marker: Marker?, msg: String?) = log(ERROR, msg, marker = marker)
    override fun error(marker: Marker?, format: String?, arg: Any?) = log(ERROR, format, arg, marker = marker)
    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) = log(ERROR, format, arg1, arg2, marker = marker)
    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) = log(ERROR, format, *arguments, marker = marker)
    override fun error(marker: Marker?, msg: String?, t: Throwable?) = log(ERROR, msg, marker = marker, throwable = t)
}
