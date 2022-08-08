package com.bkahlert.kommons

import com.bkahlert.kommons.Converter.Converter1
import com.bkahlert.kommons.Converter.Converter2
import com.bkahlert.kommons.Converter.Converter3
import com.bkahlert.kommons.Creator.Creator1
import com.bkahlert.kommons.Creator.Creator2
import com.bkahlert.kommons.Creator.Creator3
import com.bkahlert.kommons.Parser.Companion.parser
import com.bkahlert.kommons.text.quoted
import kotlin.reflect.KClass

/**
 * Factory that can create a [T] object.
 * @see Creator1
 * @see Creator2
 * @see Creator3
 */
public sealed interface Creator<T> {

    /**
     * Factory that can create a [T] object of a [P1] object.
     *
     * *Intended to be implemented by delegation by a companion object
     * using [creator] to offer
     * a [of] factory method and
     * a [ofOrNull] factory method.*
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/datetime/overview/naming.html">Method Naming Convention</a>
     */
    public interface Creator1<P1, T> : Creator<T> {
        /** Returns an object of type [T] created of the [P1] object, or `null` otherwise. */
        public fun ofOrNull(obj: P1): T? = kotlin.runCatching { of(obj) }.getOrNull()

        /** Returns an object of type [T] created of the [P1] object, or throws a [CreationException] otherwise. */
        public fun of(obj: P1): T
    }

    /**
     * Factory that can create a [T] object of a [P1] and a [P2] object.
     *
     * *Intended to be implemented by delegation by a companion object
     * using [creator] to offer
     * a [of] factory method and
     * a [ofOrNull] factory method.*
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/datetime/overview/naming.html">Method Naming Convention</a>
     */
    public interface Creator2<P1, P2, T> : Creator<T> {
        /** Returns an object of type [T] created of the [P1] and [P2] object, or `null` otherwise. */
        public fun ofOrNull(obj1: P1, obj2: P2): T? = kotlin.runCatching { of(obj1, obj2) }.getOrNull()

        /** Returns an object of type [T] created of the [P1] and [P2] object, or throws a [CreationException] otherwise. */
        public fun of(obj1: P1, obj2: P2): T
    }

    /**
     * Factory that can create a [T] object of a [P1], a [P2] and a [P3] object.
     *
     * *Intended to be implemented by delegation by a companion object
     * using [creator] to offer
     * a [of] factory method and
     * a [ofOrNull] factory method.*
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/datetime/overview/naming.html">Method Naming Convention</a>
     */
    public interface Creator3<P1, P2, P3, T> : Creator<T> {
        /** Returns an object of type [T] created of the [P1], [P2], and [P3] object, or `null` otherwise. */
        public fun ofOrNull(obj1: P1, obj2: P2, obj3: P3): T? = kotlin.runCatching { of(obj1, obj2, obj3) }.getOrNull()

        /** Returns an object of type [T] created of the [P1], [P2], and [P3] object, or throws a [CreationException] otherwise. */
        public fun of(obj1: P1, obj2: P2, obj3: P3): T
    }

    public companion object {
        /** Returns a [Creator1] that can create a [T] object of a [P1] object using the specified [createOrNull]. */
        public inline fun <P1, reified T : Any> creator(crossinline createOrNull: (P1) -> T?): Creator1<P1, T> = object : Creator1<P1, T> {
            override fun of(obj: P1): T = kotlin.runCatching {
                createOrNull(obj) ?: throw CreationException(T::class, obj)
            }.getOrElse {
                if (it is CreationException) throw it
                throw throw CreationException(T::class, it, obj)
            }
        }

        /** Returns a [Creator2] that can create a [T] object of a [P1] and a [P2] object using the specified [createOrNull]. */
        public inline fun <P1, P2, reified T : Any> creator(crossinline createOrNull: (P1, P2) -> T?): Creator2<P1, P2, T> = object : Creator2<P1, P2, T> {
            override fun of(obj1: P1, obj2: P2): T = kotlin.runCatching {
                createOrNull(obj1, obj2) ?: throw CreationException(T::class, obj1, obj2)
            }.getOrElse {
                if (it is CreationException) throw it
                throw throw CreationException(T::class, it, obj1, obj2)
            }
        }

        /** Returns a [Creator3] that can create a [T] object of a [P1], a [P2], and a [P3] object using the specified [createOrNull]. */
        public inline fun <P1, P2, P3, reified T : Any> creator(crossinline createOrNull: (P1, P2, P3) -> T?): Creator3<P1, P2, P3, T> =
            object : Creator3<P1, P2, P3, T> {
                override fun of(obj1: P1, obj2: P2, obj3: P3): T = kotlin.runCatching {
                    createOrNull(obj1, obj2, obj3) ?: throw CreationException(T::class, obj1, obj2, obj3)
                }.getOrElse {
                    if (it is CreationException) throw it
                    throw throw CreationException(T::class, it, obj1, obj2, obj3)
                }
            }
    }

    /** Exception thrown by a [Creator] when creation fails. */
    public class CreationException(
        message: String,
        /** Optional cause of this exception. */
        cause: Throwable? = null,
    ) : IllegalArgumentException(message, cause) {
        /** Creates a new creator exception for the specified [objects], the specified [type], and the optional [cause]. */
        public constructor(
            type: KClass<*>,
            cause: Throwable? = null,
            vararg objects: Any?,
        ) : this("Failed to create an instance of ${type.simpleName ?: type.toString()} of ${objects.joinToString { it.quoted }}", cause)

        /** Creates a new creator exception for the specified [objects] and the specified [type]. */
        public constructor(
            type: KClass<*>,
            vararg objects: Any?,
        ) : this(type, null, *objects)
    }
}

/**
 * Factory that can convert objects to a [T] object.
 * @see Converter1
 * @see Converter2
 * @see Converter3
 */
public sealed interface Converter<T> {
    /**
     * Factory that can convert a [P1] object to a [T] object.
     *
     * *Intended to be implemented by delegation by a companion object
     * using [converter] to offer
     * a [from] factory method and
     * a [fromOrNull] factory method.*
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/datetime/overview/naming.html">Method Naming Convention</a>
     */
    public interface Converter1<P1, T> : Converter<T> {
        /** Returns an object of type [T] representing the converted [P1] object, or `null` otherwise. */
        public fun fromOrNull(obj: P1): T? = kotlin.runCatching { from(obj) }.getOrNull()

        /** Returns an object of type [T] representing the converted [P1] object, or throws a [ConversionException] otherwise. */
        public fun from(obj: P1): T
    }

    /**
     * Factory that can convert a [P1] and a [P2] object to a [T] object.
     *
     * *Intended to be implemented by delegation by a companion object
     * using [converter] to offer
     * a [from] factory method and
     * a [fromOrNull] factory method.*
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/datetime/overview/naming.html">Method Naming Convention</a>
     */
    public interface Converter2<P1, P2, T> : Converter<T> {
        /** Returns an object of type [T] representing the converted [P1] and [P2] object, or `null` otherwise. */
        public fun fromOrNull(obj1: P1, obj2: P2): T? = kotlin.runCatching { from(obj1, obj2) }.getOrNull()

        /** Returns an object of type [T] representing the converted [P1] and [P2] object, or throws a [ConversionException] otherwise. */
        public fun from(obj1: P1, obj2: P2): T
    }

    /**
     * Factory that can convert a [P1], a [P2], and a [P3] object to a [T] object.
     *
     * *Intended to be implemented by delegation by a companion object
     * using [converter] to offer
     * a [from] factory method and
     * a [fromOrNull] factory method.*
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/datetime/overview/naming.html">Method Naming Convention</a>
     */
    public interface Converter3<P1, P2, P3, T> : Converter<T> {
        /** Returns an object of type [T] representing the converted [P1], [P2], and [P3] object, or `null` otherwise. */
        public fun fromOrNull(obj1: P1, obj2: P2, obj3: P3): T? = kotlin.runCatching { from(obj1, obj2, obj3) }.getOrNull()

        /** Returns an object of type [T] representing the converted [P1], [P2], and [P3] object, or throws a [ConversionException] otherwise. */
        public fun from(obj1: P1, obj2: P2, obj3: P3): T
    }

    public companion object {
        /** Returns a [Converter1] that can convert a [P1] object to a [T] object using the specified [convertOrNull]. */
        public inline fun <P1, reified T : Any> converter(crossinline convertOrNull: (P1) -> T?): Converter1<P1, T> = object : Converter1<P1, T> {
            override fun from(obj: P1): T = kotlin.runCatching {
                convertOrNull(obj) ?: throw ConversionException(T::class, obj)
            }.getOrElse {
                if (it is ConversionException) throw it
                throw throw ConversionException(T::class, it, obj)
            }
        }

        /** Returns a [Converter2] that can convert a [P1] and [P2] object to a [T] object using the specified [convertOrNull]. */
        public inline fun <P1, P2, reified T : Any> converter(crossinline convertOrNull: (P1, P2) -> T?): Converter2<P1, P2, T> =
            object : Converter2<P1, P2, T> {
                override fun from(obj1: P1, obj2: P2): T = kotlin.runCatching {
                    convertOrNull(obj1, obj2) ?: throw ConversionException(T::class, obj1, obj2)
                }.getOrElse {
                    if (it is ConversionException) throw it
                    throw throw ConversionException(T::class, it, obj1, obj2)
                }
            }

        /** Returns a [Converter3] that can convert a [P1], a [P2], and a [P3] object to a [T] object using the specified [convertOrNull]. */
        public inline fun <P1, P2, P3, reified T : Any> converter(crossinline convertOrNull: (P1, P2, P3) -> T?): Converter3<P1, P2, P3, T> =
            object : Converter3<P1, P2, P3, T> {
                override fun from(obj1: P1, obj2: P2, obj3: P3): T = kotlin.runCatching {
                    convertOrNull(obj1, obj2, obj3) ?: throw ConversionException(T::class, obj1, obj2, obj3)
                }.getOrElse {
                    if (it is ConversionException) throw it
                    throw throw ConversionException(T::class, it, obj1, obj2, obj3)
                }
            }
    }

    /** Exception thrown by a [Converter] when conversion fails. */
    public class ConversionException(
        message: String,
        /** Optional cause of this exception. */
        cause: Throwable? = null,
    ) : IllegalArgumentException(message, cause) {
        /** Creates a new converter exception for the specified [objects], the specified [type], and the optional [cause]. */
        public constructor(
            type: KClass<*>,
            cause: Throwable? = null,
            vararg objects: Any?,
        ) : this("Failed to convert ${objects.joinToString { it.quoted }} to an instance of ${type.simpleName ?: type.toString()}", cause)

        /** Creates a new converter exception for the specified [objects] and the specified [type]. */
        public constructor(
            type: KClass<*>,
            vararg objects: Any?,
        ) : this(type, null, *objects)
    }
}


/**
 * Parser than can parse a string into a [T] object.
 *
 * *Intended to be implemented by delegation by a companion object
 * using [parser] to offer
 * a [parse] factory method and
 * a [parseOrNull] factory method.*
 *
 * @see <a href="https://docs.oracle.com/javase/tutorial/datetime/overview/naming.html">Method Naming Convention</a>
 */
public interface Parser<T : Any> {
    /** Returns an object of type [T] representing the parsed [string], or `null` otherwise. */
    public fun parseOrNull(string: CharSequence): T? = kotlin.runCatching { parse(string) }.getOrNull()

    /** Returns an object of type [T] representing the parsed [string], or throws a [ParsingException] otherwise. */
    public fun parse(string: CharSequence): T

    public companion object {
        /** Returns a [Parser] that can parse a string into a [T] object using the specified [parseOrNull]. */
        public inline fun <reified T : Any> parser(crossinline parseOrNull: (CharSequence) -> T?): Parser<T> = object : Parser<T> {
            override fun parse(string: CharSequence): T = kotlin.runCatching {
                parseOrNull(string) ?: throw ParsingException(string, T::class)
            }.getOrElse {
                if (it is ParsingException) throw it
                throw throw ParsingException(string, T::class, it)
            }
        }
    }

    /** Exception thrown by a [Parser] when parsing fails. */
    public class ParsingException(
        message: String,
        /** Optional cause of this exception. */
        cause: Throwable? = null,
    ) : IllegalArgumentException(message, cause) {
        /** Creates a new parse exception for the specified [string], the specified [type], and the optional [cause]. */
        public constructor(
            string: CharSequence,
            type: KClass<*>,
            cause: Throwable? = null,
        ) : this("Failed to parse ${string.quoted} into an instance of ${type.simpleName ?: type.toString()}", cause)
    }
}
