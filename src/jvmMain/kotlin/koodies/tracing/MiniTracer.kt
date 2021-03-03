package koodies.tracing

import koodies.concurrent.process.IO
import koodies.logging.RenderingLogger
import koodies.logging.compactLogging
import koodies.text.Grapheme
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

@Suppress("NonAsciiCharacters")
public interface MiniTracer {
    public fun trace(input: String)
    public fun <R> microTrace(grapheme: Grapheme, block: MicroTracer.() -> R): R
    public fun <R> microTrace(f: String, block: MicroTracer.() -> R): R
    public fun <R> microTrace(f: KCallable<R>, block: MicroTracer.() -> R): R
    public fun <R> microTrace(f: KFunction0<R>, block: MicroTracer.() -> R): R
    public fun <R> microTrace1(f: KFunction1<*, R>, block: MicroTracer.() -> R): R
    public fun <R> microTrace2(f: KFunction2<*, *, R>, block: MicroTracer.() -> R): R
    public fun <R> microTrace3(f: KFunction3<*, *, *, R>, block: MicroTracer.() -> R): R
}

public fun MiniTracer?.trace(input: String) = this?.trace(input)
public fun <R> MiniTracer?.microTrace(f: String, block: MicroTracer?.() -> R) = this@microTrace?.microTrace(f, block)
public fun <R> MiniTracer?.microTrace(f: KCallable<R>, block: MicroTracer?.() -> R) = this@microTrace?.microTrace(f, block)
public fun <R> MiniTracer?.microTrace(f: KFunction0<R>, block: MicroTracer?.() -> R) = this@microTrace?.microTrace(f, block)
public fun <R> MiniTracer?.microTrace1(f: KFunction1<*, R>, block: MicroTracer?.() -> R) = this@microTrace1?.microTrace1(f, block)
public fun <R> MiniTracer?.microTrace2(f: KFunction2<*, *, R>, block: MicroTracer?.() -> R) = this@microTrace2?.microTrace2(f, block)
public fun <R> MiniTracer?.microTrace3(f: KFunction3<*, *, *, R>, block: MicroTracer?.() -> R) = this@microTrace3?.microTrace3(f, block)


public class RenderingLoggerBasedMiniTracer(private val renderingLogger: RenderingLogger) : MiniTracer {
    public override fun trace(input: String) = renderingLogger.logStatus { IO.Type.META typed input }
    public override fun <R> microTrace(grapheme: Grapheme, block: MicroTracer.() -> R): R = microTrace(grapheme, block)
    public override fun <R> microTrace(f: String, block: MicroTracer.() -> R): R = microTrace(Grapheme("𝙛"), block)
    public override fun <R> microTrace(f: KCallable<R>, block: MicroTracer.() -> R): R = microTrace(Grapheme("𝙛"), block)
    public override fun <R> microTrace(f: KFunction0<R>, block: MicroTracer.() -> R): R = microTrace(Grapheme("𝙛"), block)
    public override fun <R> microTrace1(f: KFunction1<*, R>, block: MicroTracer.() -> R): R = microTrace(Grapheme("𝙛"), block)
    public override fun <R> microTrace2(f: KFunction2<*, *, R>, block: MicroTracer.() -> R): R = microTrace(Grapheme("𝙛"), block)
    public override fun <R> microTrace3(f: KFunction3<*, *, *, R>, block: MicroTracer.() -> R): R = microTrace(Grapheme("𝙛"), block)
}

public inline fun <reified R> RenderingLogger?.subTrace(f: String, crossinline block: MiniTracer?.() -> R): R =
    compactLogging(f.format()) { RenderingLoggerBasedMiniTracer(this).run(block) }

public inline fun <reified R> RenderingLogger?.miniTrace(f: String, crossinline block: MiniTracer?.() -> R): R = subTrace(f.format(), block)
public inline fun <reified R> RenderingLogger?.miniTrace(f: KCallable<R>, crossinline block: MiniTracer?.() -> R): R = subTrace(f.format(), block)
public inline fun <reified R> RenderingLogger?.miniTrace(f: KFunction0<R>, crossinline block: MiniTracer?.() -> R): R = subTrace(f.format(), block)
public inline fun <reified R> RenderingLogger?.miniTrace1(f: KFunction1<*, R>, crossinline block: MiniTracer?.() -> R): R = subTrace(f.format(), block)
public inline fun <reified R> RenderingLogger?.miniTrace2(f: KFunction2<*, *, R>, crossinline block: MiniTracer?.() -> R): R = subTrace(f.format(), block)
public inline fun <reified R> RenderingLogger?.miniTrace3(f: KFunction3<*, *, *, R>, crossinline block: MiniTracer?.() -> R): R = subTrace(f.format(), block)
