package koodies.builder

/**
 * Builder to build groups of elements.
 *
 * @sample elementsDemo
 * @sample unitDemo
 */
class ElementGroupsBuilder<T>(private val elementGroups: MutableList<MutableList<T>>) {
    operator fun T.unaryPlus(): MutableList<T> = mutableListOf(this).also { elementGroups.add(it) }
    operator fun MutableList<T>.plus(element: T): MutableList<T> = also { it.add(element) }
}

private fun elementsDemo(init: ElementGroupsBuilder<String>.() -> Unit) {
    elementsDemo {
        +"1.a"
        +"2.a" + "2.b" + "2.c"
        +"3.a" + "3.b"
    }
}

private fun unitDemo(init: ElementGroupsBuilder<Unit>.() -> Unit) {
    unitDemo {
        +Unit + Unit + Unit
        +Unit
        +Unit + Unit
    }
}

