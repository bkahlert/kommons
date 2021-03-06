package koodies.tracing

import koodies.concurrent.process.IO.Type.META
import koodies.logging.RenderingLogger
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

public interface MacroTracer {
    public fun trace(input: String)
    public fun <R> miniTrace(f: String, block: MiniTracer.() -> R): R
    public fun <R> miniTrace(f: KCallable<R>, block: MiniTracer.() -> R): R
    public fun <R> miniTrace(f: KFunction0<R>, block: MiniTracer.() -> R): R
    public fun <R> miniTrace1(f: KFunction1<*, R>, block: MiniTracer.() -> R): R
    public fun <R> miniTrace2(f: KFunction2<*, *, R>, block: MiniTracer.() -> R): R
    public fun <R> miniTrace3(f: KFunction3<*, *, *, R>, block: MiniTracer.() -> R): R
}

public class RenderingLoggerBasedMacroTracer(private val logger: RenderingLogger) : MacroTracer {
    public override fun trace(input: String): Unit = logger.logStatus { META typed input }
    public override fun <R> miniTrace(f: String, block: MiniTracer.() -> R): R = miniTrace(f, block)
    public override fun <R> miniTrace(f: KCallable<R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
    public override fun <R> miniTrace(f: KFunction0<R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
    public override fun <R> miniTrace1(f: KFunction1<*, R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
    public override fun <R> miniTrace2(f: KFunction2<*, *, R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
    public override fun <R> miniTrace3(f: KFunction3<*, *, *, R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
}

public fun MacroTracer?.trace(input: String): Unit = this?.trace(input) ?: Unit
public fun <R> MacroTracer?.miniTrace(f: String, block: MiniTracer?.() -> R): R? = this@miniTrace?.miniTrace(f, block)
public fun <R> MacroTracer?.miniTrace(f: KCallable<R>, block: MiniTracer?.() -> R): R? = this@miniTrace?.miniTrace(f, block)
public fun <R> MacroTracer?.miniTrace(f: KFunction0<R>, block: MiniTracer?.() -> R): R? = this@miniTrace?.miniTrace(f, block)
public fun <R> MacroTracer?.miniTrace1(f: KFunction1<*, R>, block: MiniTracer?.() -> R): R? = this@miniTrace1?.miniTrace1(f, block)
public fun <R> MacroTracer?.miniTrace2(f: KFunction2<*, *, R>, block: MiniTracer?.() -> R): R? = this@miniTrace2?.miniTrace2(f, block)
public fun <R> MacroTracer?.miniTrace3(f: KFunction3<*, *, *, R>, block: MiniTracer?.() -> R): R? = this@miniTrace3?.miniTrace3(f, block)
