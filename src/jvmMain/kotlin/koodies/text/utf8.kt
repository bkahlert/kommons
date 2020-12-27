package koodies.text

val CharSequence.utf8: ByteArray get() = toByteArray(Charsets.UTF_8)
