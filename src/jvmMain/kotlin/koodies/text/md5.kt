package koodies.text


import java.security.MessageDigest

public val CharSequence.md5: String
    get() = MessageDigest.getInstance("MD5").digest(toString().toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
