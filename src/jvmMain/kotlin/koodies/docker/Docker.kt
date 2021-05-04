package koodies.docker

import koodies.CallableProperty
import koodies.builder.Builder
import koodies.builder.Init
import koodies.builder.mapBuild
import koodies.docker.DockerExitStateHandler.Failure
import koodies.docker.DockerRunCommandLine.Companion
import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.Executable
import koodies.exec.parse
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.RenderingLogger
import koodies.map
import koodies.or
import koodies.provideDelegate
import koodies.regex.RegularExpressions
import koodies.shell.ShellScript
import koodies.shell.ShellScript.ScriptContext
import koodies.text.Semantics
import koodies.text.Semantics.formattedAs
import koodies.text.joinToKebabCase
import java.nio.file.Path

/**
 * Entrypoint to ease discovery of Docker related features.
 */
public object Docker {

    /**
     * System wide information regarding the Docker installation.
     */
    public object info {

        /**
         * Returns the information identified by the given [keys].
         *
         * The [keys] can either be an array of keys,
         * or a single string consisting of `.` separated keys.
         *
         * Each key is expected to be in `kebab-case`.
         *
         * Examples:
         * - `info["server.server-version"]`
         * - `info["server", "server-version"]`
         */
        public operator fun get(vararg keys: String): String? = BACKGROUND.get(*keys)

        /**
         * Returns the information identified by the given [keys].
         *
         * The [keys] can either be an array of keys,
         * or a single string consisting of `.` separated keys.
         *
         * Each key is expected to be in `kebab-case`.
         *
         * Examples:
         * - `info["server.server-version"]`
         * - `info["server", "server-version"]`
         */
        public operator fun RenderingLogger.get(vararg keys: String): String? =
            with(keys.flatMap { it.split(".") }.map { it.unify() }.toMutableList()) {
                DockerInfoCommandLine {}.exec.logging(this@get) {
                    noDetails("Querying info ${joinToString(Semantics.FieldDelimiters.UNIT) { it.formattedAs.input }}")
                }.parse.columns<String, Failure>(1) { (line) ->
                    if (isNotEmpty() && line.substringBefore(":").unify() == first()) {
                        removeAt(0)
                        if (isEmpty()) line.substringAfter(":").trim()
                        else null
                    } else {
                        null
                    }
                }.map { singleOrNull() } or { null }
            }

        private fun String.unify() = toLowerCase().split(RegularExpressions.SPACES).filterNot { it.isEmpty() }.joinToKebabCase()
    }

    /**
     * Entry point for [DockerImage] related features like pulling images.
     */
    public val images: DockerImage.Companion = DockerImage.Companion

    /**
     * Entry point for [DockerContainer] related features like starting containers.
     */
    public val containers: DockerContainer.Companion = DockerContainer.Companion

    /**
     * Whether the Docker engine itself is running.
     */
    public val engineRunning: Boolean get() = DockerInfoCommandLine {}.exec().successful == true

    /**
     * Returns a [DockerContainer] representing a Docker container of the same
     * (sanitized) name.
     */
    public operator fun invoke(name: String): DockerContainer = DockerContainer.from(name, randomSuffix = false)


//    /**
//     * Builds a [DockerSearchCommandLine] and executes it.
//     */
//    public val search: (Init<SearchContext> /* = koodies.docker.DockerSearchCommandLine.Companion.SearchContext.() -> kotlin.Unit */)
//    -> Exec by DockerSearchCommandLine.mapBuild { it.execute().output() }
//
//    /**
//     * Builds a [DockerSearchCommandLine] and executes it using `this` [RenderingLogger].
//     */
//    public val RenderingLogger?.search: Builder<Init<SearchContext>, Exec> by CallableProperty { thisRef: RenderingLogger?, _ ->
//        DockerSearchCommandLine.mapBuild { it.execute(processor = thisRef.toProcessor()) }
//    }


    /**
     * Builds a [DockerRunCommandLine] and executes it.
     */
    public val run: (Init<Companion.CommandContext> /* = koodies.docker.DockerRunCommandLine.Companion.DockerRunCommandContext.() -> kotlin.Unit */)
    -> Exec by DockerRunCommandLine.mapBuild { it.exec.logging() }

    /**
     * Builds a [DockerRunCommandLine] and executes it using `this` [RenderingLogger].
     */
    public val RenderingLogger?.run: Builder<Init<Companion.CommandContext>, Exec> by CallableProperty { thisRef: RenderingLogger?, _ ->
        DockerRunCommandLine.mapBuild { if (thisRef != null) it.exec.logging(thisRef) else it.exec.logging() }
    }
}

/**
 * Runs the given [command] and its [arguments] in
 * a [DockerContainer] with the [DockerImage] parsed from [image].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container.
 */
public fun Path.docker(image: String, command: Any? = null, vararg arguments: Any, logger: RenderingLogger? = BACKGROUND): DockerExec =
    dockerExecute(DockerImage { image }, logger) { CommandLine(command?.toString() ?: "", arguments.map { it.toString() }) }

/**
 * Runs the given [command] and its [arguments] in
 * a [DockerContainer] with the [DockerImage] built using [imageInit].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container.
 */
public fun Path.docker(imageInit: DockerImageInit, command: Any? = null, vararg arguments: Any, logger: RenderingLogger? = BACKGROUND): DockerExec =
    dockerExecute(DockerImage(imageInit), logger) { CommandLine(command?.toString() ?: "", arguments.map { it.toString() }) }

/**
 * Runs the given [command] and its [arguments] in
 * a [DockerContainer] with the given [image].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container.
 */
public fun Path.docker(image: DockerImage, command: Any? = null, vararg arguments: Any, logger: RenderingLogger? = BACKGROUND): DockerExec =
    dockerExecute(image, logger) { CommandLine(command?.toString() ?: "", arguments.map { it.toString() }) }

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [DockerContainer] with the [DockerImage] parsed from [image].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 */
public fun Path.docker(image: String, logger: RenderingLogger? = BACKGROUND, scriptInit: ScriptInitWithWorkingDirectory): DockerExec =
    dockerExecute(DockerImage { image }, logger) { workDir -> ShellScript { scriptInit(workDir) } }

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [DockerContainer] with the [DockerImage] built using [imageInit].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 */
public fun Path.docker(imageInit: DockerImageInit, logger: RenderingLogger? = BACKGROUND, scriptInit: ScriptInitWithWorkingDirectory): DockerExec =
    dockerExecute(DockerImage(imageInit), logger) { workDir -> ShellScript { scriptInit(workDir) } }

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [DockerContainer] with the given [image].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 */
public fun Path.docker(image: DockerImage, logger: RenderingLogger? = BACKGROUND, scriptInit: ScriptInitWithWorkingDirectory): DockerExec =
    dockerExecute(image, logger) { workDir -> ShellScript { scriptInit(workDir) } }

/**
 * Builds an [Executable] using the given [executableProvider] and runs it in
 * a [DockerContainer] with the given [image].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [executableProvider].
 */
private fun Path.dockerExecute(image: DockerImage, logger: RenderingLogger? = BACKGROUND, executableProvider: (ContainerPath) -> Executable<Exec>): DockerExec {
    val containerPath = "/work".asContainerPath()
    return executableProvider(containerPath).dockerized(image) {
        mounts { this@dockerExecute mountAt containerPath }
        workingDirectory { containerPath }
    }.run {
        if (logger != null) exec.logging(logger)
        else exec()
    }
}

/**
 * Type of the argument supported by [docker] and its variants (e.g. [ubuntu].
 */
public typealias ScriptInitWithWorkingDirectory = ScriptContext.(ContainerPath) -> CharSequence


/*
 * UBUNTU
 */

/**
 * Runs the given [command] and its [arguments] in
 * a [Ubuntu](https://hub.docker.com/_/ubuntu) based [DockerContainer].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container.
 */
public fun Path.ubuntu(command: Any? = null, vararg arguments: Any, logger: RenderingLogger? = BACKGROUND): DockerExec =
    docker(DockerImage { "ubuntu" }, command, *arguments, logger = logger)

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [Ubuntu](https://hub.docker.com/_/ubuntu) based [DockerContainer].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 */
public fun Path.ubuntu(logger: RenderingLogger? = BACKGROUND, scriptInit: ScriptInitWithWorkingDirectory): DockerExec =
    docker(DockerImage { "ubuntu" }, logger, scriptInit)


/*
 * BUSYBOX
 */

/**
 * Runs the given [command] and its [arguments] in
 * a [busybox](https://hub.docker.com/_/busybox) based [DockerContainer].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container.
 */
public fun Path.busybox(command: Any? = null, vararg arguments: Any, logger: RenderingLogger? = BACKGROUND): DockerExec =
    docker(DockerImage { "busybox" }, command, *arguments, logger = logger)

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [busybox](https://hub.docker.com/_/busybox) based [DockerContainer].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 */
public fun Path.busybox(logger: RenderingLogger? = BACKGROUND, scriptInit: ScriptInitWithWorkingDirectory): DockerExec =
    docker(DockerImage { "busybox" }, logger, scriptInit)
