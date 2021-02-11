package koodies.collections

inline infix fun <A, B, C> Pair<A, B>.to(third: C) =
    Triple(first, second, third)
