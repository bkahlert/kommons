package com.bkahlert.kommons

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.reflect.KClass

/**
 * Isolated process execution.
 */
public object IsolatedProcess {

    /**
     * The binary location of the running Java distribution.
     */
    private val JavaBinary: Path by lazy { SystemLocations.JavaHome / "bin" / "java" }

    /**
     * The class path used to locate classes and other resources.
     */
    private val ClassPath: String by lazy { System.getProperty("java.class.path") }

    /**
     * Returns a [ProcessBuilder] to executes the `main` method of the specified [mainClass]
     * with the specified [args] in a separate [Process].
     */
    public fun builder(mainClass: KClass<*>, vararg args: String): ProcessBuilder =
        ProcessBuilder(JavaBinary.pathString, "-cp", ClassPath, mainClass.qualifiedName, *args)

    /**
     * Synchronously executes the `main` method of the specified [mainClass]
     * with the specified [args] in a separate [Process] and returns its exit code.
     *
     * Use [customize] to make any adaptions to the [ProcessBuilder] right before execution.
     */
    public fun exec(mainClass: KClass<*>, vararg args: String, customize: (ProcessBuilder) -> Unit = {}): Int {
        require(kotlin.runCatching { mainClass.java.getMethod("main", Array<String>::class.java) }.isSuccess) { "missing main method" }
        return builder(mainClass, *args)
            .inheritIO()
            .apply(customize)
            .start()
            .waitFor()
    }
}
