package koodies.transform

import koodies.math.BigInteger
import koodies.math.bigIntegerOf
import koodies.math.byteArrayOfBinaryString
import koodies.math.byteArrayOfDecimalString
import koodies.math.byteArrayOfHexadecimalString
import koodies.math.toBinaryString
import koodies.math.toByteArray
import koodies.math.toDecimalString
import koodies.math.toHexadecimalString
import koodies.math.toString
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

public open class Conversion<T>(private val acc: () -> T) {
    public fun <R> map(transform: (T) -> R): Conversion<R> = Conversion { transform(acc()) }
    public val ok: T get() = acc()
    public fun <R> ok(transform: T.() -> R): R = acc().transform()
}

public abstract class AbstractConversionContext<T : Any>(protected val conversion: Conversion<T>) {
    public infix fun <R> map(transform: (T) -> R): (T) -> R = transform
    public infix fun <R, S : AbstractConversionContext<R>> ((T) -> R).then(stage: (Conversion<R>) -> S): S = conversion.map(this).let(stage)
}

public class StringConversionContext(conversion: Conversion<String>) : AbstractConversionContext<String>(conversion) {

    public val asBinaryString: ByteArrayConversionContext get() = map(::byteArrayOfBinaryString) then ::ByteArrayConversionContext
    public val asDecimalString: ByteArrayConversionContext get() = map(::byteArrayOfDecimalString) then ::ByteArrayConversionContext
    public val asHexadecimalString: ByteArrayConversionContext get() = map(::byteArrayOfHexadecimalString) then ::ByteArrayConversionContext

    public companion object : ReadOnlyProperty<CharSequence, StringConversionContext> {
        override operator fun getValue(thisRef: CharSequence, property: KProperty<*>): StringConversionContext =
            StringConversionContext(Conversion { thisRef.toString() })
    }
}

public class ByteArrayConversionContext(conversion: Conversion<ByteArray>) : AbstractConversionContext<ByteArray>(conversion) {
    public fun toBinaryString(padding: Boolean = true): String = conversion.map { it.toBinaryString(padding) }.ok
    public fun toDecimalString(): String = conversion.map { it.toDecimalString() }.ok
    public fun toHexadecimalString(padding: Boolean = true): String = conversion.map { it.toHexadecimalString(padding) }.ok
    public fun toByteArray(): ByteArray = conversion.ok
    public fun toUByteArray(): UByteArray = conversion.map { it.toUByteArray() }.ok
    public fun toBigInteger(): BigInteger = conversion.map { bigIntegerOf(toUByteArray()) }.ok

    public companion object {
        public operator fun getValue(thisRef: ByteArray, property: KProperty<*>): ByteArrayConversionContext =
            ByteArrayConversionContext(Conversion { thisRef })

        public operator fun getValue(thisRef: UByteArray, property: KProperty<*>): ByteArrayConversionContext =
            ByteArrayConversionContext(Conversion { thisRef.toByteArray() })
    }
}

public class BigIntegerConversionContext(conversion: Conversion<BigInteger>) : AbstractConversionContext<BigInteger>(conversion) {

    public val asSigned: ByteArrayConversionContext get() = map { it.toByteArray() } then ::ByteArrayConversionContext
    public val asUnsigned: ByteArrayConversionContext get() = map { byteArrayOfHexadecimalString(it.toString(16)) } then ::ByteArrayConversionContext

    public companion object {
        public operator fun getValue(thisRef: BigInteger, property: KProperty<*>): BigIntegerConversionContext =
            BigIntegerConversionContext(Conversion { thisRef })
    }
}

public val CharSequence.convert: StringConversionContext by StringConversionContext
public val ByteArray.convert: ByteArrayConversionContext by ByteArrayConversionContext
public val UByteArray.convert: ByteArrayConversionContext by ByteArrayConversionContext
public val BigInteger.convert: BigIntegerConversionContext by BigIntegerConversionContext
