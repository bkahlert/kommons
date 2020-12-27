package koodies.tracing

import koodies.concurrent.process.IO.Type.META
import koodies.logging.RenderingLogger
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

interface MacroTracer {
    fun trace(input: String)
    fun <R> miniTrace(f: String, block: MiniTracer.() -> R): R
    fun <R> miniTrace(f: KCallable<R>, block: MiniTracer.() -> R): R
    fun <R> miniTrace(f: KFunction0<R>, block: MiniTracer.() -> R): R
    fun <R> miniTrace1(f: KFunction1<*, R>, block: MiniTracer.() -> R): R
    fun <R> miniTrace2(f: KFunction2<*, *, R>, block: MiniTracer.() -> R): R
    fun <R> miniTrace3(f: KFunction3<*, *, *, R>, block: MiniTracer.() -> R): R
}

class RenderingLoggerBasedMacroTracer(private val logger: RenderingLogger) : MacroTracer {
    override fun trace(input: String) = logger.logStatus { META typed input }
    override fun <R> miniTrace(f: String, block: MiniTracer.() -> R): R = miniTrace(f, block)
    override fun <R> miniTrace(f: KCallable<R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
    override fun <R> miniTrace(f: KFunction0<R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
    override fun <R> miniTrace1(f: KFunction1<*, R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
    override fun <R> miniTrace2(f: KFunction2<*, *, R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
    override fun <R> miniTrace3(f: KFunction3<*, *, *, R>, block: MiniTracer.() -> R): R = miniTrace(f.format(), block)
}


fun MacroTracer?.trace(input: String) = this?.trace(input)
fun <R> MacroTracer?.miniTrace(f: String, block: MiniTracer?.() -> R) = this@miniTrace?.miniTrace(f, block)
fun <R> MacroTracer?.miniTrace(f: KCallable<R>, block: MiniTracer?.() -> R) = this@miniTrace?.miniTrace(f, block)
fun <R> MacroTracer?.miniTrace(f: KFunction0<R>, block: MiniTracer?.() -> R) = this@miniTrace?.miniTrace(f, block)
fun <R> MacroTracer?.miniTrace1(f: KFunction1<*, R>, block: MiniTracer?.() -> R) = this@miniTrace1?.miniTrace1(f, block)
fun <R> MacroTracer?.miniTrace2(f: KFunction2<*, *, R>, block: MiniTracer?.() -> R) = this@miniTrace2?.miniTrace2(f, block)
fun <R> MacroTracer?.miniTrace3(f: KFunction3<*, *, *, R>, block: MiniTracer?.() -> R) = this@miniTrace3?.miniTrace3(f, block)
