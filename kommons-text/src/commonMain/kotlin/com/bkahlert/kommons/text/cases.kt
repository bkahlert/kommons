package com.bkahlert.kommons.text

/** Returns `true` if this character is uppercase. */
public fun kotlin.Char.isUpperCase(): Boolean = this == uppercaseChar() && this != lowercaseChar()

/** Returns `true` if this character is lowercase. */
public fun kotlin.Char.isLowerCase(): Boolean = this == lowercaseChar() && this != uppercaseChar()


/** Returns this character sequence with its first letter in uppercase. */
public fun CharSequence.capitalize(): CharSequence = if (isNotEmpty() && first().isLowerCase()) first().uppercaseChar() + substring(1) else this

/** Returns this string with its first letter in uppercase. */
public fun String.capitalize(): String = if (isNotEmpty() && first().isLowerCase()) first().uppercaseChar() + substring(1) else this


/** Returns this character sequence with its first letter in uppercase. */
public fun CharSequence.decapitalize(): CharSequence = if (isNotEmpty() && first().isUpperCase()) first().lowercaseChar() + substring(1) else this

/** Returns this string with its first letter in uppercase. */
public fun String.decapitalize(): String = if (isNotEmpty() && first().isUpperCase()) first().lowercaseChar() + substring(1) else this
