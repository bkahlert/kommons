package koodies.test

import koodies.asString
import koodies.builder.Builder
import koodies.builder.build


public open class BuilderFixture<T : Function<*>, R>(
    public val builder: Builder<T, R>,
    public val init: T,
    public val result: R,
) : Fixture<Pair<T, R>> {
    override val name: String = "builder-fixture_${builder::class.simpleName}"
    override val contents: Pair<T, R> = init to result

    init {
        val actual = builder(init)
        require(actual == result) { "Building $init with $this did return\n$actual\nbut the following was expected:\n$result" }
    }

    override fun toString(): String = asString {
        ::builder.name to builder.toString()
        ::init.name to init.toString()
        ::result.name to result.toString()
    }

    public companion object {
        public inline infix fun <reified T : Function<*>, reified R> Builder<T, R>.fixture(initToResult: Pair<T, R>): BuilderFixture<T, R> {
            val init = initToResult.first
            val expected = initToResult.second
            val actual = build(initToResult.first)
            require(actual == expected) { "Building $init with $this did return\n$actual\nbut the following was expected:\n$expected" }
            return BuilderFixture(this, initToResult.first, initToResult.second)
        }
    }
}
