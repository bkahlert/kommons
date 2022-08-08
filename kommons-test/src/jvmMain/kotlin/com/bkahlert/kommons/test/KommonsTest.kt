package com.bkahlert.kommons.test

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

/** A couple of library features. */
public object KommonsTest {

    /** Working directory, that is, the directory in which this binary is located. */
    public val Work: Path = FileSystems.getDefault().getPath("").toAbsolutePath()

    /** Home directory of the currently logged-in user. */
    public val Home: Path = Paths.get(System.getProperty("user.home"))

    /** Directory in which temporary data can be stored. */
    public val Temp: Path = Paths.get(System.getProperty("java.io.tmpdir"))

    /** Directory of the currently running Java distribution. */
    public val JavaHome: Path = Paths.get(System.getProperty("java.home"))


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
