package com.bkahlert.kommons.docker

import com.bkahlert.kommons.Exceptions
import com.bkahlert.kommons.docker.Docker.BusyBox
import com.bkahlert.kommons.docker.Docker.CurlJq
import com.bkahlert.kommons.docker.Docker.Ubuntu
import com.bkahlert.kommons.docker.Docker.info.get
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed
import com.bkahlert.kommons.docker.DockerRunCommandLine.Options
import com.bkahlert.kommons.docker.DockerSearchCommandLine.DockerSearchResult
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.Exec
import com.bkahlert.kommons.exec.Executable
import com.bkahlert.kommons.exec.ProcessingMode
import com.bkahlert.kommons.exec.RendererProviders
import com.bkahlert.kommons.exec.RendererProviders.noDetails
import com.bkahlert.kommons.exec.parse
import com.bkahlert.kommons.exec.successful
import com.bkahlert.kommons.io.path.deleteRecursively
import com.bkahlert.kommons.io.path.listDirectoryEntriesRecursively
import com.bkahlert.kommons.io.path.moveTo
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.randomDirectory
import com.bkahlert.kommons.io.path.uriString
import com.bkahlert.kommons.map
import com.bkahlert.kommons.or
import com.bkahlert.kommons.regex.RegularExpressions
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.shell.ShellScript.ScriptContext
import com.bkahlert.kommons.text.joinToKebabCase
import com.bkahlert.kommons.tracing.Key
import com.bkahlert.kommons.tracing.rendering.RendererProvider
import com.bkahlert.kommons.tracing.runSpanning
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import java.util.Locale

/**
 * Entrypoint to ease discovery of Docker related features.
 */
public object Docker {

    /**
     * System wide information regarding the Docker installation.
     *
     * Usage:
     * - `info["server.server-version"]`
     * - `info["server", "server-version"]`
     *
     * @see get
     */
    @Suppress("ClassName") // used like an array
    public object info {

        /**
         * Returns the information identified by the given [keys].
         *
         * The [keys] can either be an array of keys,
         * or a single string consisting of `.` separated keys.
         *
         * Each key has to be in `kebab-case`.
         *
         * Examples:
         * - `info["server.server-version"]`
         * - `info["server", "server-version"]`
         */
        public operator fun get(vararg keys: String): String? =
            with(keys.flatMap { it.split(".") }.map { it.unify() }.toMutableList()) {
                DockerInfoCommandLine(query = this)
                    .exec.logging(renderer = noDetails())
                    .parse.columns<String, Failed>(1) { (line) ->
                        if (isNotEmpty() && line.substringBefore(":").unify() == first()) {
                            removeAt(0)
                            if (isEmpty()) line.substringAfter(":").trim()
                            else null
                        } else {
                            null
                        }
                    }.map { singleOrNull() } or { null }
            }

        private fun String.unify() = lowercase(Locale.getDefault()).split(RegularExpressions.SPACES).filterNot { it.isEmpty() }.joinToKebabCase()
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
    public val engineRunning: Boolean get() = DockerInfoCommandLine().exec().successful

    /**
     * Returns a [DockerContainer] representing a Docker container of the same
     * (sanitized) name.
     */
    public operator fun invoke(name: String): DockerContainer = DockerContainer.from(name, randomSuffix = false)

    /**
     * Searches for at most [limit] Docker images matching [term],
     * having at least the given number of [stars] and being [automated] and/or [official].
     */
    public fun search(
        term: String,
        stars: Int? = null,
        automated: Boolean? = null,
        official: Boolean? = null,
        limit: Int = 100,
        renderer: RendererProvider? = RendererProviders.errorsOnly(),
    ): List<DockerSearchResult> = DockerSearchCommandLine.search(term, stars, automated, official, limit, renderer)

    /**
     * Executes the given [command] and its [arguments] in
     * a [DockerContainer] with the given [image].
     *
     * The given [workingDirectory] is mapped to `/work`,
     * which is configured as the working directory
     * inside of the container.
     *
     * If [inputStream] is set, the contents will be piped to the standard input
     * of the created process.
     */
    public fun exec(
        image: DockerImage,
        workingDirectory: Path,
        command: Any? = null,
        vararg arguments: Any,
        name: CharSequence? = null,
        renderer: RendererProvider? = RendererProviders.errorsOnly(),
        inputStream: InputStream? = null,
    ): DockerExec = execute(image, workingDirectory, renderer, inputStream) {
        CommandLine(command?.toString() ?: "", arguments.map { it.toString() }, name = name)
    }

    /**
     * Builds a shell script using the given [scriptInit] and executes it in
     * a [DockerContainer] with the given [image].
     *
     * The given [workingDirectory] is mapped to `/work`,
     * which is configured as the working directory
     * inside of the container and also passed to [scriptInit] as the only argument.
     *
     * If [inputStream] is set, the contents will be piped to the standard input
     * of the created process.
     */
    public fun exec(
        image: DockerImage,
        workingDirectory: Path,
        name: CharSequence? = null,
        renderer: RendererProvider? = RendererProviders.errorsOnly(),
        inputStream: InputStream? = null,
        scriptInit: ScriptInitWithWorkingDirectory,
    ): DockerExec = execute(image, workingDirectory, renderer, inputStream) { workDir ->
        ShellScript(name) { scriptInit(workDir) }
    }

    /**
     * Builds an [Executable] using the given [executableProvider] and executes it in
     * a [DockerContainer] with the given [image].
     *
     * The given [workingDirectory] is mapped to `/work`,
     * which is configured as the working directory
     * inside of the container and also passed to [executableProvider].
     *
     * If [inputStream] is set, the contents will be piped to the standard input
     * of the created process.
     */
    private fun execute(
        image: DockerImage,
        workingDirectory: Path,
        renderer: RendererProvider? = RendererProviders.errorsOnly(),
        inputStream: InputStream? = null,
        executableProvider: (ContainerPath) -> Executable<Exec>,
    ): DockerExec {
        val containerPath = "/work".asContainerPath()
        return executableProvider(containerPath).dockerized(image, Options(
            mounts = MountOptions { workingDirectory mountAt containerPath },
            workingDirectory = containerPath,
        )).run {
            val mode = ProcessingMode(async = false, inputStream)
            if (renderer != null) exec.mode(mode).logging(renderer = renderer)
            else exec.mode(mode).invoke()
        }
    }


    /**
     * **Official Image**
     *
     * [Ubuntu](https://hub.docker.com/_/ubuntu) is a Debian-based Linux operating system based on free software.
     */
    public object Ubuntu : DockerImage("ubuntu")

    /**
     * **Official Image**
     *
     * [Busybox base image](https://hub.docker.com/_/busybox)
     */
    public object BusyBox : DockerImage("busybox")

    /**
     * **Official Image**
     *
     * [Hello World!](https://hub.docker.com/_/hello-world)
     */
    public object HelloWorld : DockerImage("hello-world")

    /**
     * **Official Image**
     *
     * [nginx](https://hub.docker.com/_/nginx)
     */
    public object Nginx : DockerImage("nginx")

    /**
     * Alpine Docker Image with `curl`, `jq`, `bash`.
     *
     * [dwdraju/alpine-curl-jq](https://hub.docker.com/r/dwdraju/alpine-curl-jq)
     */
    @Suppress("SpellCheckingInspection")
    public object CurlJq : DockerImage("dwdraju", listOf("alpine-curl-jq"), digest = "sha256:5f6561fff50ab16cba4a9da5c72a2278082bcfdca0f72a9769d7e78bdc5eb954")

    /**
     * librsvg is a SVG rendering library.
     * The Linux command-line program rsvg uses the library to turn SVG files into raster images.
     *
     * [minidocks/librsvg](https://hub.docker.com/r/minidocks/librsvg)
     */
    @Suppress("SpellCheckingInspection")
    public object LibRSvg : DockerImage("minidocks", listOf("librsvg"))

    /**
     * Popular Linux x86_64 CLI app binaries.
     * - `tmux` 3.1c (+ncurses 6.2 +libevent 2.1.12)
     * - `bandwidth` 0.20.0
     * - `bat` 0.17.1
     * - `chafa` 1.4.1
     * - `dua` 2.10.7
     * - `duf` 0.4.0
     * - `dyff` 1.1.0
     * - `fd` 8.2.1
     * - `fzf` 0.24.4
     * - `glow` 1.1.0
     * - `heksa` 1.13.0
     * - `hexyl` 0.8.0
     * - `httpiego` 0.6.0
     * - `hyperfine` 1.11.0
     * - `jq` 1.6
     * - `lf` r18
     * - `mkcert` 1.4.1
     * - `ncdu` 1.15.1
     * - `reg` 0.16.1
     * - `ripgrep` 12.1.1
     * - `starship` 0.46.2
     * - `stern` 1.13.1
     * - `yank` 1.2.0
     * - `yj` 5.0.0
     * - `zoxide` 0.5.0
     *
     * [rafib/awesome-cli-binaries](https://hub.docker.com/r/rafib/awesome-cli-binaries)
     */
    @Suppress("SpellCheckingInspection")
    public object AwesomeCliBinaries : DockerImage("rafib", listOf("awesome-cli-binaries"))
}


/**
 * Type of the argument supported by [docker] and its variants (e.g. [ubuntu].
 */
public typealias ScriptInitWithWorkingDirectory = ScriptContext.(ContainerPath) -> CharSequence

/**
 * Runs the given [command] and its [arguments] in
 * a [DockerContainer] with the [DockerImage] parsed from [image].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container.
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.docker(
    image: String,
    command: Any? = null,
    vararg arguments: Any,
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
): DockerExec =
    Docker.exec(DockerImage { image }, this, command, *arguments, name = name, renderer = renderer, inputStream = inputStream)

/**
 * Runs the given [command] and its [arguments] in
 * a [DockerContainer] with the [DockerImage] built using [imageInit].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container.
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.docker(
    imageInit: DockerImageInit,
    command: Any? = null,
    vararg arguments: Any,
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
): DockerExec =
    Docker.exec(DockerImage(imageInit), this, command, *arguments, name = name, renderer = renderer, inputStream = inputStream)

/**
 * Runs the given [command] and its [arguments] in
 * a [DockerContainer] with the given [image].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container.
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.docker(
    image: DockerImage,
    command: Any? = null,
    vararg arguments: Any,
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
): DockerExec =
    Docker.exec(image, this, command, *arguments, name = name, renderer = renderer, inputStream = inputStream)

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [DockerContainer] with the [DockerImage] parsed from [image].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.docker(
    image: String,
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
    scriptInit: ScriptInitWithWorkingDirectory,
): DockerExec =
    Docker.exec(DockerImage { image }, this, name, renderer, inputStream, scriptInit)

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [DockerContainer] with the [DockerImage] built using [imageInit].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.docker(
    imageInit: DockerImageInit,
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
    scriptInit: ScriptInitWithWorkingDirectory,
): DockerExec =
    Docker.exec(DockerImage(imageInit), this, name, renderer, inputStream, scriptInit)

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [DockerContainer] with the given [image].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.docker(
    image: DockerImage,
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
    scriptInit: ScriptInitWithWorkingDirectory,
): DockerExec =
    Docker.exec(image, this, name, renderer, inputStream, scriptInit)


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
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.ubuntu(
    command: Any? = null,
    vararg arguments: Any,
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
): DockerExec =
    docker(Ubuntu, command, *arguments, name = name, renderer = renderer, inputStream = inputStream)

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [Ubuntu](https://hub.docker.com/_/ubuntu) based [DockerContainer].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.ubuntu(
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
    scriptInit: ScriptInitWithWorkingDirectory,
): DockerExec =
    docker(Ubuntu, name, renderer, inputStream, scriptInit)


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
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.busybox(
    command: Any? = null,
    vararg arguments: Any,
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
): DockerExec =
    docker(BusyBox, command, *arguments, name = name, renderer = renderer, inputStream = inputStream)

/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [busybox](https://hub.docker.com/_/busybox) based [DockerContainer].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 *
 * If [inputStream] is set, the contents will be piped to the standard input
 * of the created process.
 */
public fun Path.busybox(
    name: CharSequence? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    inputStream: InputStream? = null,
    scriptInit: ScriptInitWithWorkingDirectory,
): DockerExec =
    docker(BusyBox, name, renderer, inputStream, scriptInit)


/**
 * Builds a shell script using the given [scriptInit] and runs it in
 * a [alpine-curl-jq](https://hub.docker.com/dwdraju/alpine-curl-jq) based [DockerContainer].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container and also passed to [scriptInit] as the only argument.
 */
public fun Path.curlJq(
    name: CharSequence = "curl | jq",
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
    scriptInit: ScriptInitWithWorkingDirectory,
): DockerExec =
    docker(CurlJq, name, renderer, null, scriptInit)

/**
 * Runs a [curl](https://curl.se/docs/manpage.html) with the given [arguments] in
 * a [alpine-curl-jq](https://hub.docker.com/dwdraju/alpine-curl-jq) based [DockerContainer].
 *
 * `this` [Path] is used as the working directory on this host and
 * is mapped to `/work`, which is configured as the working directory
 * inside of the container.
 */
public fun Path.curl(
    vararg arguments: Any,
    name: CharSequence = "curl",
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
): DockerExec =
    docker(CurlJq, "curl", *arguments, name = name, renderer = renderer)

/**
 * Downloads the given [uri] to [fileName] (automatically determined if not specified) in `this` directory using
 * a [alpine-curl-jq](https://hub.docker.com/dwdraju/alpine-curl-jq) based [DockerContainer].
 */
public fun Path.download(
    uri: String,
    fileName: String? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
): Path = runSpanning("Downloading $uri", Key.stringKey("uri") to uri) {
    if (fileName != null) {
        resolve(fileName).also {
            log("Downloading to ${it.uriString}")
            curl("--location", uri, "-o", fileName, renderer = renderer)
        }
    } else {
        val downloadDir = randomDirectory()
        downloadDir.run {
            log("Using temporary directory $uriString")
            curl("--location", "--remote-name", "--remote-header-name", "--compressed", uri, renderer = renderer)
            val downloaded = listDirectoryEntriesRecursively().apply { log("Downloaded ${map { it.uriString }}") }
            when (downloaded.size) {
                0 -> throw FileNotFoundException("Failed to download $uri")
                1 -> downloaded.first().let {
                    val target = parent.resolve(it.cleanFileName()).apply { log("Moving download to $uriString") }
                    it.moveTo(target)
                }
                else -> throw Exceptions.ISE("More than one file found:", *downloaded.map { it.uriString }.toTypedArray())
            }
        }.also {
            log("Deleting ${downloadDir.uriString}")
            downloadDir.deleteRecursively()
        }
    }
}

/**
 * Downloads the given [uri] to [fileName] (automatically determined if not specified) in `this` directory using
 * a [alpine-curl-jq](https://hub.docker.com/dwdraju/alpine-curl-jq) based [DockerContainer].
 */
public fun Path.download(
    uri: URI,
    fileName: String? = null,
    renderer: RendererProvider? = RendererProviders.errorsOnly(),
): Path = download(uri.toString(), fileName, renderer)

private fun Path.cleanFileName(): String = listOf("?", "#").fold(fileName.pathString) { acc, symbol -> acc.substringBefore(symbol) }
