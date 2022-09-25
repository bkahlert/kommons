package com.bkahlert.kommons_deprecated.io

public open class InMemoryImage(
    override val name: String,
    override val data: ByteArray,
) : InMemoryFile {

    public constructor(name: String, data: String) : this(name, data.encodeToByteArray())

    public val baseName: String get() = name.substringBeforeLast(".")
    public val extension: String get() = name.removePrefix(baseName).removePrefix(".")

    init {
        require(extension in EXTENSIONS) { error("$extension is none of the supported extensions: $EXTENSIONS") }
    }

    public val isVector: Boolean get() = extension == "svg"
    public val isBitmap: Boolean get() = !isVector

    public companion object {
        public val EXTENSIONS: List<String> = listOf("jpg", "jpeg", "png", "svg")
    }
}
