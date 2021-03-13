package koodies.builder

import koodies.asString
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder to build lists of type [E].
 *
 * The most convenient way to actually build a list is using [buildList].
 *
 * @sample elementsDemo
 * @sample unitDemo
 */
public class NestedListsBuilder<E> : BuilderTemplate<NestedListsBuilder<E>.NestedListsContext, List<List<E>>>() {

    public inner class NestedListsContext(override val captures: CapturesMap) : CapturingContext() {
        public val list: SkippableCapturingBuilderInterface<ListBuildingContext<E>.() -> Unit, List<E>?> by ListBuilder<E>()
        public operator fun E.unaryPlus(): MutableList<E> = mutableListOf(this).also { list by it }
        public operator fun MutableList<E>.plus(element: E): MutableList<E> = also { it.add(element) }
    }

    override fun BuildContext.build(): List<List<E>> = ::NestedListsContext { ::list.evalAll() }

    override fun toString(): String = asString()

    public companion object {

        @OptIn(ExperimentalTypeInference::class)
        public operator fun <E> invoke(@BuilderInference init: Init<NestedListsBuilder<E>.NestedListsContext>): List<List<E>> =
            NestedListsBuilder<E>().invoke(init)
    }
}

/**
 * Builds a list of type [E] as specified by [init].
 */
@OptIn(ExperimentalTypeInference::class)
public fun <E> buildNestedLists(@BuilderInference init: Init<NestedListsBuilder<E>.NestedListsContext>): List<List<E>> {
    contract { callsInPlace(init, EXACTLY_ONCE) }
    return NestedListsBuilder(init)
}

private fun elementsDemo(@Suppress("UNUSED_PARAMETER") init: Init<NestedListsBuilder<String>.NestedListsContext>) {
    elementsDemo {
        +"1.a"
        +"2.a" + "2.b" + "2.c"
        list { +"3.a" + "3.b" }
    }
}

private fun unitDemo(@Suppress("UNUSED_PARAMETER") init: Init<NestedListsBuilder<Unit>.NestedListsContext>) {
    unitDemo {
        +Unit + Unit + Unit
        +Unit
        +Unit + Unit
    }
}
