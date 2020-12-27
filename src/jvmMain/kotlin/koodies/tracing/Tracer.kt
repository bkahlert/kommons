package koodies.tracing

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

interface Tracer {
    fun trace(input: String)
    fun <R> macroTrace(f: String, block: MacroTracer.() -> R): R
    fun <R> macroTrace(f: KCallable<R>, block: MacroTracer.() -> R): R
    fun <R> macroTrace(f: KFunction0<R>, block: MacroTracer.() -> R): R
    fun <R> macroTrace1(f: KFunction1<*, R>, block: MacroTracer.() -> R): R
    fun <R> macroTrace2(f: KFunction2<*, *, R>, block: MacroTracer.() -> R): R
    fun <R> macroTrace3(f: KFunction3<*, *, *, R>, block: MacroTracer.() -> R): R
}

fun Tracer?.trace(input: String) = this?.trace(input)
fun <R> Tracer?.macroTrace(f: String, block: MacroTracer?.() -> R) = this@macroTrace?.macroTrace(f, block)
fun <R> Tracer?.macroTrace(f: KCallable<R>, block: MacroTracer?.() -> R) = this@macroTrace?.macroTrace(f, block)
fun <R> Tracer?.macroTrace(f: KFunction0<R>, block: MacroTracer?.() -> R) = this@macroTrace?.macroTrace(f, block)
fun <R> Tracer?.macroTrace1(f: KFunction1<*, R>, block: MacroTracer?.() -> R) = this@macroTrace1?.macroTrace1(f, block)
fun <R> Tracer?.macroTrace2(f: KFunction2<*, *, R>, block: MacroTracer?.() -> R) = this@macroTrace2?.macroTrace2(f, block)
fun <R> Tracer?.macroTrace3(f: KFunction3<*, *, *, R>, block: MacroTracer?.() -> R) = this@macroTrace3?.macroTrace3(f, block)

fun <R> KCallable<R>.format(): String = name + " -- TODO"
