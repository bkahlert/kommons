package com.bkahlert.kommons.exec

import com.bkahlert.kommons.headOrNull
import com.bkahlert.kommons.tail
import java.nio.file.Path

/** [CommandLine] described by this [ProcessBuilder] or `null` if none is specified yet. */
public val ProcessBuilder.commandLine: CommandLine?
    get() {
        val command = command()
        return if (command.isEmpty()) null
        else command.headOrNull?.let { CommandLine(it, command.tail) }
    }

/**
 * This process builder's environment;
 * used by built [Process] instances as their environment.
 */
public val ProcessBuilder.environment: MutableMap<String, String>
    get() = environment()

/**
 * This process builder's working directory;
 * used by built [Process] instances as their working directory.
 *
 * If no working directory is set, the working directory of the current
 * Java process is used (usually the directory named by the system property `user.dir`).
 */
public var ProcessBuilder.workingDirectory: Path?
    get() = directory()?.toPath()
    set(value) {
        directory(value?.toFile())
    }

/**
 * Starts a new [Process] using the attributes of this process builder,
 * the specified [workingDirectory] set,
 * the specified [environment] added, and
 * the specified [customize] applied.
 *
 * @see [java.lang.ProcessBuilder.start]
 */
public fun ProcessBuilder.start(
    workingDirectory: Path? = null,
    vararg environment: Pair<String, String>,
    customize: ProcessBuilder.() -> Unit = {},
): Process = this
    .also { it.environment.putAll(environment) }
    .also { it.workingDirectory = workingDirectory }
    .apply(customize)
    .let { Process(it) }
