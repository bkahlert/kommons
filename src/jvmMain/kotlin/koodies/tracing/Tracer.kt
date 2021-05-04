package koodies.tracing

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
 
public interface Tracer {
    public fun trace(input: String)
    public fun <R> macroTrace(f: String, block: MacroTracer.() -> R): R
    public fun <R> macroTrace(f: KCallable<R>, block: MacroTracer.() -> R): R
    public fun <R> macroTrace(f: KFunction0<R>, block: MacroTracer.() -> R): R
    public fun <R> macroTrace1(f: KFunction1<*, R>, block: MacroTracer.() -> R): R
    public fun <R> macroTrace2(f: KFunction2<*, *, R>, block: MacroTracer.() -> R): R
    public fun <R> macroTrace3(f: KFunction3<*, *, *, R>, block: MacroTracer.() -> R): R
}

public fun Tracer?.trace(input: String): Unit = this?.trace(input) ?: Unit
public fun <R> Tracer?.macroTrace(f: String, block: MacroTracer?.() -> R): R? = this@macroTrace?.macroTrace(f, block)
public fun <R> Tracer?.macroTrace(f: KCallable<R>, block: MacroTracer?.() -> R): R? = this@macroTrace?.macroTrace(f, block)
public fun <R> Tracer?.macroTrace(f: KFunction0<R>, block: MacroTracer?.() -> R): R? = this@macroTrace?.macroTrace(f, block)
public fun <R> Tracer?.macroTrace1(f: KFunction1<*, R>, block: MacroTracer?.() -> R): R? = this@macroTrace1?.macroTrace1(f, block)
public fun <R> Tracer?.macroTrace2(f: KFunction2<*, *, R>, block: MacroTracer?.() -> R): R? = this@macroTrace2?.macroTrace2(f, block)
public fun <R> Tracer?.macroTrace3(f: KFunction3<*, *, *, R>, block: MacroTracer?.() -> R): R? = this@macroTrace3?.macroTrace3(f, block)

public fun <R> KCallable<R>.format(): String = name + " -- TODO"
