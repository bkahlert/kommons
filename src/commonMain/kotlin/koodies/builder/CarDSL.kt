@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING", "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING")

package koodies.builder

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import koodies.builder.CarDSL.CarBuilder.CarContext
import koodies.builder.CarDSL.Distance.Companion.DistanceContext
import koodies.builder.CarDSL.Distance.Companion.DistanceContext.inch
import koodies.builder.CarDSL.Engine.EngineBuilder.EngineContext
import koodies.builder.CarDSL.EnginePower.Companion.EnginePowerContext
import koodies.builder.CarDSL.Speed.Companion.SpeedContext
import koodies.builder.CarDSL.Trait.Exclusive
import koodies.builder.CarDSL.Trait.TaxExempt
import koodies.builder.CarDSL.Wheel.WheelBuilder.WheelContext
import koodies.builder.StatelessBuilder.Returning
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.unit.centi
import koodies.unit.kilo
import koodies.unit.milli
import kotlin.time.Duration
import kotlin.time.hours

class CarDSL {

    data class Car(val name: String, val color: String, val traits: Set<Trait>, val engine: Engine, val wheels: List<Wheel>)

    object CarBuilder : BuilderTemplate<CarContext, Car>() {

        class CarContext(
            override val captures: CapturesMap,
        ) : CapturingContext() {
            var name by setter<String>() // name = "…"
            val color by External::color // color(…, …, …)
            val traits by enumSetBuilder<Trait>() // traits { … }
            val engine by Engine // engine { power { … }; maxSpeed { … } }
            val wheel by Wheel // wheel { … }
            val fourWheeler by Wheel delegate { builtWheel -> // same signature as wheel but …
                repeat(4) { wheel using builtWheel } // … will call wheel builder 4 times with the same builtWheel instance
            }
        }

        override fun BuildContext.build(): Car = ::CarContext {
            Car(
                ::name.eval(),
                ::color.evalOrDefault("#111111"),
                ::traits.eval(),
                ::engine.eval(),
                ::wheel.evalAll<Wheel>().takeUnless { it.isEmpty() } ?: List(3) { Wheel() },
            )
        }
    }

    fun car(init: Init<CarContext>): Car = CarBuilder(init)

    fun printSamples() {

        val exclusiveCar = car {
            name = "Koodies Car"
            color(198, 82, 89)
            engine {
                power { 145.kW }
                maxSpeed { 244.km per hour }
            }
            fourWheeler { diameter { 16.inch } }
            traits { +Exclusive + TaxExempt }
        }

        val defaultCarWithCopiedMotor = car {
            name = "Default Car"
            engine using exclusiveCar.engine
        }

        println(exclusiveCar)
        println(defaultCarWithCopiedMotor)
    }


    inline class EnginePower(val watts: BigDecimal) {
        companion object : Returning<EnginePowerContext, EnginePower>(EnginePowerContext) {
            object EnginePowerContext {
                val Int.kW: EnginePower get() = kilo.W
                val BigDecimal.W: EnginePower get() = EnginePower(this)
            }
        }

        override fun toString(): String = "${watts.doubleValue() / 1000.0}kW"
    }

    inline class Distance(val meter: BigDecimal) {
        companion object : Returning<DistanceContext, Distance>(DistanceContext) {
            object DistanceContext {
                val Int.mm: Distance get() = milli.m
                val Int.inch: Distance get() = (toDouble() * 2.54).centi.m
                val BigDecimal.m: Distance get() = Distance(this)
            }
        }

        override fun toString(): String = "${meter}m"
    }

    data class Speed(val distance: Distance, val time: Duration) {
        companion object : Returning<SpeedContext, Speed>(SpeedContext) {
            object SpeedContext {
                val Int.km: Distance get() = kilo.m
                val hour: Duration = 1.hours
                infix fun Distance.per(time: Duration): Speed = Speed(this, time)
                val BigDecimal.m: Distance get() = Distance(this)
            }
        }

        override fun toString(): String = "${(distance.meter.doubleValue() / 1000.0) / time.inHours}km/h"
    }

    data class Engine(val power: EnginePower, val maxSpeed: Speed) {
        companion object EngineBuilder : BuilderTemplate<EngineContext, Engine>() {

            class EngineContext(
                override val captures: CapturesMap,
            ) : CapturingContext() {
                val power by EnginePower
                val maxSpeed by Speed
            }

            override fun BuildContext.build(): Engine = ::EngineContext {
                Engine(::power.evalOrDefault { EnginePower { 130.kW } }, ::maxSpeed.evalOrDefault { Speed { 228.km per hour } })
            }
        }

        override fun toString(): String = "$maxSpeed, $power"
    }

    data class Wheel(val diameter: Distance = 14.inch) {
        companion object WheelBuilder : BuilderTemplate<WheelContext, Wheel>() {

            class WheelContext(
                override val captures: CapturesMap,
            ) : CapturingContext() {
                val diameter by Distance
            }

            override fun BuildContext.build(): Wheel = ::WheelContext {
                ::diameter.evalOrNull<Distance>()?.let { Wheel(it) } ?: Wheel()
            }
        }

        override fun toString(): String = "⌀ ${diameter.meter.doubleValue() * 100}cm"
    }

    enum class Trait { Exclusive, PreOwned, TaxExempt }

    object External {
        fun color(h: Int, s: Int, v: Int): String = "hsv($h, $s, $v)"
    }

    inline fun Int.times(function: () -> Unit) {
        repeat(this) { function() }
    }
}
