package koodies.builder

/*
 * Build methods for builders with no receiver object, a.k.a producers.
 */

/**
 * Builds an instance of [T] by invoking `this` initializer.
 *
 * @return the build instance
 */
inline fun <reified T> (() -> T).build(): T =
    invoke()

inline fun <reified T, reified X1> ((X1) -> T).build(x1: X1): T =
    invoke(x1)

/**
 * Builds an instance of [T] and adds it to [target] by invoking `this` initializer.
 *
 * @return the build instance
 */
inline fun <reified T> (() -> T).buildTo(target: MutableCollection<in T>): T =
    build().also { target.add(it) }

inline fun <reified T, reified X1> ((X1) -> T).buildTo(x1: X1, target: MutableCollection<in T>): T =
    build(x1).also { target.add(it) }

/**
 * Builds an instance of [T] by invoking `this` initializer and [transform]s it to [U].
 *
 * @return the transformed instance
 */
inline fun <reified T, reified U> (() -> T).build(transform: T.() -> U): U =
    build().run(transform)

inline fun <reified T, reified U, reified X1> ((X1) -> T).build(x1: X1, transform: T.() -> U): U =
    build(x1).run(transform)

/**
 * Builds an instance of [T] by invoking `this` initializer,
 * [transform]s and adds it to [target].
 *
 * @return the transformed instance
 */
inline fun <reified T, reified U> (() -> T).buildTo(target: MutableCollection<in U>, transform: T.() -> U): U =
    build(transform).also { target.add(it) }

inline fun <reified T, reified U, reified X1> ((X1) -> T).buildTo(x1: X1, target: MutableCollection<in U>, transform: T.() -> U): U =
    build(x1, transform).also { target.add(it) }

/**
 * Builds an instance of [T] by invoking `this` initializer,
 * [transform]s it to multiple instances of [U] and adds all to [target].
 *
 * @return the transformed instances
 */
inline fun <reified T, reified U> (() -> T).buildMultipleTo(target: MutableCollection<in U>, transform: T.() -> List<U>): List<U> =
    build(transform).also { target.addAll(it) }

inline fun <reified T, reified U, reified X1> ((X1) -> T).buildMultipleTo(x1: X1, target: MutableCollection<in U>, transform: T.() -> List<U>): List<U> =
    build(x1, transform).also { target.addAll(it) }


/*
 * Build methods for zero-arg builders implementing `(B) -> T` to retrieve the result.
 */

/**
 * Builds an instance of [T] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`.
 *
 * @return the build instance
 */
inline fun <reified B : (B) -> T, reified T> (B.() -> Unit).build(): T {
    val zeroArgConstructors = B::class.java.declaredConstructors.filter { it.parameterCount == 0 }
    val builder: B = zeroArgConstructors.singleOrNull()?.newInstance() as? B
        ?: throw IllegalArgumentException("${B::class.simpleName} has no zero-arg constructor and cannot be used to create an instance of ${T::class.simpleName}.")
    return builder.apply(this).let { it.invoke(it) }
}

inline fun <reified B : (B) -> T, reified T, reified X1> (B.(X1) -> Unit).build(x1: X1): T {
    val zeroArgConstructors = B::class.java.declaredConstructors.filter { it.parameterCount == 0 }
    val builder: B = zeroArgConstructors.singleOrNull()?.newInstance() as? B
        ?: throw IllegalArgumentException("${B::class.simpleName} has no zero-arg constructor and cannot be used to create an instance of ${T::class.simpleName}.")
    this(builder, x1)
    return builder(builder)
}

/**
 * Builds an instance of [T] and adds it to [target] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`.
 *
 * @return the build instance
 */
inline fun <reified B : (B) -> T, reified T> (B.() -> Unit).buildTo(target: MutableCollection<in T>): T =
    build().also { target.add(it) }

inline fun <reified B : (B) -> T, reified T, reified X1> (B.(X1) -> Unit).buildTo(x1: X1, target: MutableCollection<in T>): T =
    build(x1).also { target.add(it) }

/**
 * Builds an instance of [T] and [transform]s it to [U] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`
 * 4) applying [transform].
 *
 * @return the transformed instance
 */
inline fun <reified B : (B) -> T, reified T, reified U> (B.() -> Unit).build(transform: T.() -> U): U =
    build().run(transform)

inline fun <reified B : (B) -> T, reified T, reified U, reified X1> (B.(X1) -> Unit).build(x1: X1, transform: T.() -> U): U =
    build(x1).run(transform)

/**
 * Builds an instance of [T] and adds the to [U] [transform]ed instance to [target] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`
 * 4) applying [transform].
 *
 * @return the transformed instance
 */
inline fun <reified B : (B) -> T, reified T, reified U> (B.() -> Unit).buildTo(target: MutableCollection<in U>, transform: T.() -> U): U =
    build(transform).also { target.add(it) }

inline fun <reified B : (B) -> T, reified T, reified U, reified X1> (B.(X1) -> Unit).buildTo(x1: X1, target: MutableCollection<in U>, transform: T.() -> U): U =
    build(x1, transform).also { target.add(it) }

/**
 * Builds an instance of [T] and adds the to list [U] [transform]ed instance to [target] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`
 * 4) applying [transform].
 *
 * @return the transformed instances
 */
inline fun <reified B : (B) -> T, reified T, reified U> (B.() -> Unit).buildMultipleTo(target: MutableCollection<in U>, transform: T.() -> List<U>): List<U> =
    build(transform).also { target.addAll(it) }

inline fun <reified B : (B) -> T, reified T, reified U, reified X1> (B.(X1) -> Unit).buildMultipleTo(
    x1: X1,
    target: MutableCollection<in U>,
    transform: T.() -> List<U>,
): List<U> =
    build(x1, transform).also { target.addAll(it) }


interface BuilderAccessor<B, T> {
    operator fun invoke(): B
    operator fun B.invoke(): T
}

inline fun <reified B, reified T> BuilderAccessor<B, T>.build(init: B.() -> Unit): T {
    val builder = this.invoke()
    return builder.apply(init).invoke()
}

inline fun <reified B, reified T> BuilderAccessor<B, T>.buildTo(init: B.() -> Unit, target: MutableCollection<in T>): T =
    build(init).also { target.add(it) }

inline fun <reified B, reified T, reified U> BuilderAccessor<B, T>.buildTo(init: B.() -> Unit, transform: T.() -> U): U =
    build(init).run(transform)

inline fun <reified B, reified T, reified U> BuilderAccessor<B, T>.buildTo(init: B.() -> Unit, target: MutableCollection<in U>, transform: T.() -> U): U =
    build(init).run(transform).also { target.add(it) }

inline fun <reified B, reified T, reified U> BuilderAccessor<B, T>.buildMultipleTo(
    init: B.() -> Unit,
    target: MutableCollection<in U>,
    transform: T.() -> List<U>,
): List<U> =
    build(init).run(transform).also { target.addAll(it) }


//inline fun <reified A, reified B, reified T> build2(accessor: A, init: A.() -> B.() -> Unit): T where
//    A : () -> B,
//    A : (B) -> T {
//    val b: B = accessor.invoke()
//    val apply: B = b.apply(init)
//    return accessor(b)
//}
