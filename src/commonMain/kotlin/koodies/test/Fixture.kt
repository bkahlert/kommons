package koodies.test

import koodies.asString
import koodies.builder.Builder
import koodies.builder.build

/**
 * A well known piece of data especially suited for tests scenarios.
 *
 * Each fixture has a [name] (e.g. `username` or `sample.html`) and
 * provides access to its [data] in a binary form and [text] form.
 *
 * @see TextFixture
 * @see BinaryFixture
 */
public interface Fixture {
    public val name: String
    public val data: ByteArray
    public val text: String get() = data.decodeToString()
}

/**
 * Default implementation of a text-based [Fixture].
 *
 * The [data] field contains the text encoded using `UTF-8`.
 */
public open class TextFixture(override val name: String, data: String) : Fixture {
    override val data: ByteArray = data.encodeToByteArray()
}

/**
 * Default implementation of a binary-based [Fixture].
 */
public open class BinaryFixture(override val name: String, override val data: ByteArray) : Fixture {
    public companion object {
        public fun unsigned(name: String, vararg data: UByte): BinaryFixture = BinaryFixture(name, data.toByteArray())
    }
}


public open class BuilderFixture<T : Function<*>, R>(public val builder: Builder<T, R>, public val init: T, public val result: R) : Fixture {
    override val name: String = "builder-fixture_${builder::class.simpleName}"
    override val data: ByteArray by lazy {
        asString {
            listOf(
                ::builder.name to builder.toString(),
                ::init.name to init.toString(),
                ::result.name to result.toString(),
            )
        }.encodeToByteArray()
    }

    init {
        val actual = builder(init)
        require(actual == result) { "Building $init with $this did return\n$actual\nbut the following was expected:\n$result" }
    }

    public companion object {
        public inline infix fun <reified T : Function<*>, reified R> Builder<T, R>.fixture(initToResult: Pair<T, R>): BuilderFixture<T, R> {
            val init = initToResult.first
            val expect = initToResult.second
            val actual = build(initToResult.first)
            require(actual == expect) { "Building $init with $this did return\n$actual\nbut the following was expected:\n$expect" }
            return BuilderFixture(this, initToResult.first, initToResult.second)
        }
    }
}
