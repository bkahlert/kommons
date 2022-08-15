package com.bkahlert.kommons.test

/** A couple of library features. */
public object KommonsTest {

    /** Locates the stack trace element representing a call to this library. */
    public fun locateCall(): StackTraceElement =
        locateCall(StackTrace.get())

    /** Locates the stack trace element representing a call to this library. */
    internal fun locateCall(stackTrace: StackTrace): StackTraceElement =
        stackTrace.findOrNull {
            val insideTestPackage = it.className.substringBeforeLast(".").startsWith(enclosingPackage)
            val topLevelClassName = it.className.substringBefore("\$")
            val isTest = topLevelClassName != enclosingClassName && topLevelClassName.endsWith("Test")
            insideTestPackage && !isTest
        } ?: stackTrace.first()

    /** Locates the stack trace element representing a call to this library. */
    internal fun locateCall(exception: Throwable): StackTraceElement =
        locateCall(StackTrace(exception.stackTrace.toList()))

    private val enclosingClassName = this::class.qualifiedName
        ?.let { if (this::class.isCompanion) it.substringBeforeLast(".") else it }
        ?: error("unknown name")
    private val enclosingPackage = enclosingClassName.substringBeforeLast(".")
}
