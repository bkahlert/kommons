package koodies.collections

inline infix fun <reified A, reified B, reified C> Pair<A, B>.to(third: C) =
    Triple(first, second, third)
