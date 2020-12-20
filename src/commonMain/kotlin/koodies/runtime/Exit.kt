package koodies.runtime

import kotlin.reflect.KClass

typealias OnExitHandler = () -> Unit

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
expect fun <T : OnExitHandler> onExit(handler: T): T

/**
 * Throws this throwable if is not any of the specified [classes].
 * Otherwise is returned.
 */
internal fun <T : Throwable> T.throwUnlessOfType(vararg classes: KClass<out Throwable>): T {
    if (classes.none { it.isInstance(this) }) throw this
    return this
}
