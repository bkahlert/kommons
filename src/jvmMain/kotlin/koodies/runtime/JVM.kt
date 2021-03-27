package koodies.runtime

import koodies.runtime.JVM.currentStackTrace
import koodies.runtime.JVM.currentThread
import java.lang.reflect.Method

/**
 * Java Virtual Machine runtime information, such as the [currentThread]
 * or the [currentStackTrace].
 */
public object JVM {

    /**
     * Contains the context ClassLoader for the current [Thread].
     *
     * The context [ClassLoader] is provided by the creator of the [Thread] for use
     * by code running in this thread when loading classes and resources.
     */
    public val contextClassLoader: ClassLoader
        get() = currentThread.contextClassLoader

    /**
     * Contains the current [Thread].
     */
    public val currentThread: Thread
        get() = Thread.currentThread()

    /**
     * Contains the current stacktrace.
     */
    public val currentStackTrace: Array<StackTraceElement>
        get() = currentThread.stackTrace

    /**
     * Returns the current stacktrace with the given [transform] function
     * applied to each [StackTraceElement].
     */
    public fun <T> currentStackTrace(transform: StackTraceElement.() -> T): Sequence<T> =
        currentStackTrace.asSequence().map(transform)

    /**
     * The class containing the execution point represented by this stack trace element.
     */
    public val StackTraceElement.clazz: Class<*> get() = Class.forName(className)

    /**
     * The method containing the execution point represented by this stack trace element.
     *
     * If the execution point is contained in an instance or class initializer,
     * this method will be the appropriate *special method name*, `<init>` or
     * `<clinit>`, as per Section 3.9 of *The Java Virtual Machine Specification*.
     */
    public val StackTraceElement.method: Method get() = clazz.declaredMethods.single { it.name == methodName }
}
