package com.bkahlert.kommons.debug

import com.bkahlert.kommons.Platform
import com.bkahlert.kommons.Platform.Browser
import com.bkahlert.kommons.Platform.JVM
import com.bkahlert.kommons.Platform.NodeJS
import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.collections.Map.Entry
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.test.Test
import kotlin.test.fail

class RenderTypeTest {

    @Test fun render_primitive_types() = testAll {
        when (Platform.Current) {
            Browser, NodeJS -> {
                PrimitiveTypes.allValues.map { it.renderType() } shouldContainExactly listOf(
                    "String",
                    "Boolean", "Char", "Double", "Double",
                    "UByte", "UShort", "UInt", "ULong",
                    "Int", "Int", "Int", "Long",

                    "BooleanArray", "CharArray", "FloatArray", "DoubleArray",
                    "UByteArray", "UShortArray", "UIntArray", "ULongArray",
                    "ByteArray", "ShortArray", "IntArray", "LongArray",
                    "Array",
                )
                PrimitiveTypes.allValues.map { it.renderType(simplified = false) } shouldContainExactly listOf(
                    "String",
                    "Boolean", "Char", "Number", "Number",
                    "UByte", "UShort", "UInt", "ULong",
                    "Number", "Number", "Number", "Long",

                    "Array", "Uint16Array", "Float32Array", "Float64Array",
                    "UByteArray", "UShortArray", "UIntArray", "ULongArray",
                    "Int8Array", "Int16Array", "Int32Array", "Array",
                    "Array",
                )
            }

            JVM -> {
                PrimitiveTypes.allValues.map { it.renderType() } shouldContainExactly listOf(
                    "String",
                    "Boolean", "Char", "Float", "Double",
                    "UByte", "UShort", "UInt", "ULong",
                    "Byte", "Short", "Int", "Long",

                    "BooleanArray", "CharArray", "FloatArray", "DoubleArray",
                    "UByteArray", "UShortArray", "UIntArray", "ULongArray",
                    "ByteArray", "ShortArray", "IntArray", "LongArray",
                    "Array",
                )
                PrimitiveTypes.allValues.map { it.renderType(simplified = false) } shouldContainExactly listOf(
                    "kotlin.String",
                    "kotlin.Boolean", "kotlin.Char", "kotlin.Float", "kotlin.Double",
                    "kotlin.UByte", "kotlin.UShort", "kotlin.UInt", "kotlin.ULong",
                    "kotlin.Byte", "kotlin.Short", "kotlin.Int", "kotlin.Long",

                    "kotlin.BooleanArray", "kotlin.CharArray", "kotlin.FloatArray", "kotlin.DoubleArray",
                    "kotlin.UByteArray", "kotlin.UShortArray", "kotlin.UIntArray", "kotlin.ULongArray",
                    "kotlin.ByteArray", "kotlin.ShortArray", "kotlin.IntArray", "kotlin.LongArray",
                    "kotlin.Array",
                )
            }

            else -> fail("untested platform")
        }
    }

    @Test fun render_collections() = testAll {
        when (Platform.Current) {
            Browser, NodeJS -> {
                CollectionTypes.allValues.map { it.renderType() } shouldContainExactly listOf(
                    "Iterable", "EmptySet", "List", "List"
                )
                CollectionTypes.allValues.map { it.renderType(simplified = false) } shouldContainExactly listOf(
                    "<object>", "EmptySet", "ArrayList", "ArrayList"
                )
            }

            JVM -> {
                CollectionTypes.allValues.map { it.renderType() } shouldContainExactly listOf(
                    "Iterable", "Set", "List", "List"
                )
                CollectionTypes.allValues.map { it.renderType(simplified = false) } shouldContainExactly listOf(
                    "<object>", "kotlin.collections.EmptySet", "java.util.Arrays.ArrayList", "java.util.ArrayList",
                )
            }

            else -> fail("untested platform")
        }
    }

    @Test fun render_classes() = testAll {
        when (Platform.Current) {
            Browser, NodeJS -> {
                ClassTypes.allValues.map { it.renderType() } shouldContainExactly listOf(
                    "Singleton",
                    "<object>",
                    "ListImplementingSingleton",
                    "<object>",
                    "MapImplementingSingleton",
                    "<object>",
                    "OrdinaryClass",
                    "NestedClass",
                    "InnerNestedClass",
                    "NestedObject",
                    "Pair",
                    "Triple",
                    "KClass"
                )
                ClassTypes.allValues.map { it.renderType(simplified = false) } shouldContainExactly listOf(
                    "Singleton",
                    "<object>",
                    "ListImplementingSingleton",
                    "<object>",
                    "MapImplementingSingleton",
                    "<object>",
                    "OrdinaryClass",
                    "NestedClass",
                    "InnerNestedClass",
                    "NestedObject",
                    "Pair",
                    "Triple",
                    "KClass"
                )
            }

            JVM -> {
                ClassTypes.allValues.map { it.renderType() } shouldContainExactly listOf(
                    "Singleton",
                    "<object>",
                    "ListImplementingSingleton",
                    "<object>",
                    "MapImplementingSingleton",
                    "<object>",
                    "OrdinaryClass",
                    "OrdinaryClass.NestedClass",
                    "NestedClass.InnerNestedClass",
                    "SealedClass.NestedObject",
                    "Pair",
                    "Triple",
                    "KClass"
                )
                ClassTypes.allValues.map { it.renderType(simplified = false) } shouldContainExactly listOf(
                    "com.bkahlert.kommons.debug.Singleton",
                    "<object>",
                    "com.bkahlert.kommons.debug.ListImplementingSingleton",
                    "<object>",
                    "com.bkahlert.kommons.debug.MapImplementingSingleton",
                    "<object>",
                    "com.bkahlert.kommons.debug.OrdinaryClass",
                    "com.bkahlert.kommons.debug.OrdinaryClass.NestedClass",
                    "com.bkahlert.kommons.debug.OrdinaryClass.NestedClass.InnerNestedClass",
                    "com.bkahlert.kommons.debug.SealedClass.NestedObject",
                    "kotlin.Pair",
                    "kotlin.Triple",
                    "kotlin.reflect.jvm.internal.KClassImpl"
                )
            }

            else -> fail("untested platform")
        }
    }

    @Test fun test_functions() = testAll {
        when (Platform.Current) {
            Browser, NodeJS -> {
                FunctionTypes.allValues.filterIsInstance<Function<*>>()
                    .map { it.renderFunctionType() }.shouldForAll { it shouldBe "Function" }
                ({}).renderFunctionType() shouldBe "Function"
                FunctionTypes.allValues.filterIsInstance<Function<*>>()
                    .map { it.renderFunctionType(simplified = false) }.shouldForAll { it shouldBe "Function" }
                ({}).renderFunctionType(simplified = false) shouldBe "Function"
            }

            JVM -> {
                FunctionTypes.allValues.filterIsInstance<Function<*>>()
                    .map { it.renderFunctionType() } shouldContainExactly listOf(
                    "work0() -> Unit",
                    "provide0() -> Int",
                    "consume0(String) -> Unit",
                    "process0(String) -> Int",
                    "Receiver.work1() -> Unit",
                    "Receiver.provide1() -> Int",
                    "Receiver.consume1(String) -> Unit",
                    "Receiver.process1(String) -> Int",
                    "Owner.work1() -> Unit",
                    "Owner.provide1() -> Int",
                    "Owner.consume1(String) -> Unit",
                    "Owner.process1(String) -> Int"
                )
                ({}).renderFunctionType() shouldBe "() -> Unit"
                FunctionTypes.allValues.filterIsInstance<Function<*>>()
                    .map { it.renderFunctionType(simplified = false) } shouldContainExactly listOf(
                    "work0() -> kotlin.Unit",
                    "provide0() -> kotlin.Int",
                    "consume0(kotlin.String) -> kotlin.Unit",
                    "process0(kotlin.String) -> kotlin.Int",
                    "com.bkahlert.kommons.debug.Receiver.work1() -> kotlin.Unit",
                    "com.bkahlert.kommons.debug.Receiver.provide1() -> kotlin.Int",
                    "com.bkahlert.kommons.debug.Receiver.consume1(kotlin.String) -> kotlin.Unit",
                    "com.bkahlert.kommons.debug.Receiver.process1(kotlin.String) -> kotlin.Int",
                    "com.bkahlert.kommons.debug.Owner.work1() -> kotlin.Unit",
                    "com.bkahlert.kommons.debug.Owner.provide1() -> kotlin.Int",
                    "com.bkahlert.kommons.debug.Owner.consume1(kotlin.String) -> kotlin.Unit",
                    "com.bkahlert.kommons.debug.Owner.process1(kotlin.String) -> kotlin.Int"
                )
                ({}).renderFunctionType(simplified = false) shouldBe "() -> kotlin.Unit"
            }

            else -> fail("untested platform")
        }
    }
}

internal abstract class TypeMap : AbstractMap<KClass<*>, Any>() {
    /** Contains all values, that is, also different values with same type. */
    internal abstract val allValues: Collection<Any>
    override val entries: Set<Entry<KClass<out Any>, Any>> by lazy { allValues.associateBy { it::class }.entries }
}

@Suppress("MemberVisibilityCanBePrivate")
internal object PrimitiveTypes : TypeMap() {
    const val string: String = "string"

    const val boolean: Boolean = true
    const val char: kotlin.Char = '*'
    const val float: Float = 42.1f
    const val double: Double = 42.12

    val uByte: UByte = 39u
    val uShort: UShort = 40u
    val uInt: UInt = 41u
    val uLong: ULong = 42u

    const val byte: Byte = 39
    const val short: Short = 40
    const val int: Int = 41
    const val long: Long = 42


    val booleanArray: BooleanArray = booleanArrayOf(true, false, false)
    val charArray: CharArray = charArrayOf('a', 'r', 'r', 'a', 'y')
    val floatArray: FloatArray = floatArrayOf('a'.code.toFloat(), 'r'.code.toFloat(), 'r'.code.toFloat(), 'a'.code.toFloat(), 'y'.code.toFloat())
    val doubleArray: DoubleArray = doubleArrayOf('a'.code.toDouble(), 'r'.code.toDouble(), 'r'.code.toDouble(), 'a'.code.toDouble(), 'y'.code.toDouble())

    val uByteArray: UByteArray = ubyteArrayOf('a'.code.toUByte(), 'r'.code.toUByte(), 'r'.code.toUByte(), 'a'.code.toUByte(), 'y'.code.toUByte())
    val uShortArray: UShortArray = ushortArrayOf('a'.code.toUShort(), 'r'.code.toUShort(), 'r'.code.toUShort(), 'a'.code.toUShort(), 'y'.code.toUShort())
    val uIntArray: UIntArray = uintArrayOf('a'.code.toUInt(), 'r'.code.toUInt(), 'r'.code.toUInt(), 'a'.code.toUInt(), 'y'.code.toUInt())
    val uLongArray: ULongArray = ulongArrayOf('a'.code.toULong(), 'r'.code.toULong(), 'r'.code.toULong(), 'a'.code.toULong(), 'y'.code.toULong())

    val byteArray: ByteArray = byteArrayOf('a'.code.toByte(), 'r'.code.toByte(), 'r'.code.toByte(), 'a'.code.toByte(), 'y'.code.toByte())
    val shortArray: ShortArray = shortArrayOf('a'.code.toShort(), 'r'.code.toShort(), 'r'.code.toShort(), 'a'.code.toShort(), 'y'.code.toShort())
    val intArray: IntArray = intArrayOf('a'.code, 'r'.code, 'r'.code, 'a'.code, 'y'.code)
    val longArray: LongArray = longArrayOf('a'.code.toLong(), 'r'.code.toLong(), 'r'.code.toLong(), 'a'.code.toLong(), 'y'.code.toLong())

    val array: Array<Char> = arrayOf('a', 'r', 'r', 'a', 'y')

    override val allValues: Collection<Any> = listOf(
        string,
        boolean, char, float, double,
        uByte, uShort, uInt, uLong,
        byte, short, int, long,

        booleanArray, charArray, floatArray, doubleArray,
        uByteArray, uShortArray, uIntArray, uLongArray,
        byteArray, shortArray, intArray, longArray,
        array,
    )
}

@Suppress("MemberVisibilityCanBePrivate")
internal object CollectionTypes : TypeMap() {
    val iterable: Iterable<Any?> = object : Iterable<Any?> {
        override fun iterator(): Iterator<Any?> = PrimitiveTypes.values.iterator()
    }

    val set: Set<Any?> = emptySet()
    val list: List<Any?> = listOf("foo", null)
    val mutableList: MutableList<Any?> = mutableListOf(PrimitiveTypes.values)
    override val allValues: Collection<Any> = listOf(iterable, set, list, mutableList)
}

@Suppress("MemberVisibilityCanBePrivate")
internal object ClassTypes : TypeMap() {
    val singleton: Singleton = Singleton
    val anonymousSingleton: Any = AnonymousSingleton
    val listImplementingSingleton: ListImplementingSingleton = ListImplementingSingleton
    val listImplementingAnonymousSingleton: Any = ListImplementingAnonymousSingleton
    val mapImplementingSingleton: MapImplementingSingleton = MapImplementingSingleton
    val mapImplementingAnonymousSingleton: Any = MapImplementingAnonymousSingleton

    val `class`: OrdinaryClass = OrdinaryClass()
    val nestedClass: OrdinaryClass.NestedClass = OrdinaryClass.NestedClass()
    val innerNestedClass: OrdinaryClass.NestedClass.InnerNestedClass = OrdinaryClass.NestedClass().InnerNestedClass()
    val sealedClass: SealedClass = SealedClass.NestedObject
    val pair: Pair<Any?, Any?> = PrimitiveTypes.byte to PrimitiveTypes.short
    val triple: Triple<Any?, Any?, Any?> = Triple(PrimitiveTypes.byte, PrimitiveTypes.short, PrimitiveTypes.int)
    val map: Map<Any, Any?> = mapOf("foo" to "bar", "baz" to null)
    val reflect: KClass<*> = OrdinaryClass::class
    override val allValues: Collection<Any> = listOf(
        singleton,
        anonymousSingleton,
        listImplementingSingleton,
        listImplementingAnonymousSingleton,
        mapImplementingSingleton,
        mapImplementingAnonymousSingleton,
        `class`,
        nestedClass,
        innerNestedClass,
        sealedClass,
        pair,
        triple,
        reflect,
    )
}

internal fun work0() {}
internal fun provide0(): Int {
    return Random.nextInt()
}

internal fun consume0(value: String) {
    @Suppress("UNUSED_EXPRESSION") value
}

internal fun process0(value: String): Int {
    return value.length
}

internal class Receiver

internal fun Receiver.work1() {
    @Suppress("UNUSED_EXPRESSION") this
}

internal fun Receiver.provide1(): Int {
    @Suppress("UNUSED_EXPRESSION") this
    return Random.nextInt()
}

internal fun Receiver.consume1(value: String) {
    @Suppress("UNUSED_EXPRESSION") this
    @Suppress("UNUSED_EXPRESSION") value
}

internal fun Receiver.process1(value: String): Int {
    @Suppress("UNUSED_EXPRESSION") this
    return value.length
}

internal class Owner {
    internal fun work1() {}
    internal fun provide1(): Int {
        return Random.nextInt()
    }

    internal fun consume1(value: String) {
        @Suppress("UNUSED_EXPRESSION") value
    }

    internal fun process1(value: String): Int {
        return value.length
    }

    internal fun Receiver.work2() {
        @Suppress("UNUSED_EXPRESSION") this
    }

    internal fun Receiver.provide2(): Int {
        @Suppress("UNUSED_EXPRESSION") this
        return Random.nextInt()
    }

    internal fun Receiver.consume2(value: String) {
        @Suppress("UNUSED_EXPRESSION") this
        @Suppress("UNUSED_EXPRESSION") value
    }

    internal fun Receiver.process2(value: String): Int {
        @Suppress("UNUSED_EXPRESSION") this
        return value.length
    }
}

@Suppress("MemberVisibilityCanBePrivate")
internal object FunctionTypes : TypeMap() {
    val worker: KFunction0<Unit> = ::work0
    val provider: KFunction0<Any> = ::provide0
    val consumer: KFunction1<String, Unit> = ::consume0
    val processor: KFunction1<String, Int> = ::process0
    val extensionWorker: KFunction1<Receiver, Unit> = Receiver::work1
    val extensionProvider: KFunction1<Receiver, Int> = Receiver::provide1
    val extensionConsumer: KFunction2<Receiver, String, Unit> = Receiver::consume1
    val extensionProcessor: KFunction2<Receiver, String, Int> = Receiver::process1
    val memberWorker: KFunction1<Owner, Unit> = Owner::work1
    val memberProvider: KFunction1<Owner, Int> = Owner::provide1
    val memberConsumer: KFunction2<Owner, String, Unit> = Owner::consume1
    val memberProcessor: KFunction2<Owner, String, Int> = Owner::process1

    //    val memberExtensionWorker: KFunction1<Owner, Unit> = Owner::Receiver::work2
//    val memberExtensionProvider: KFunction1<Owner, Int> = Owner::Receiver::::provide2
//    val memberExtensionConsumer: KFunction2<Owner, String, Unit> = Owner::Receiver::::consume2
//    val memberExtensionProcessor: KFunction2<Owner, String, Int> = Owner::Receiver::::process2
    override val allValues: Collection<Any> = listOf(
        worker,
        provider,
        consumer,
        processor,
        extensionWorker,
        extensionProvider,
        extensionConsumer,
        extensionProcessor,
        memberWorker,
        memberProvider,
        memberConsumer,
        memberProcessor,
    )
}
