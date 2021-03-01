package koodies.builder

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import koodies.builder.CarDSL.CarBuilder.CarContext
import koodies.builder.CarDSL.Distance.Companion.DistanceContext
import koodies.builder.CarDSL.Engine.EngineBuilder.EngineContext
import koodies.builder.CarDSL.EnginePower.Companion.EnginePowerContext
import koodies.builder.CarDSL.Speed.Companion.SpeedContext
import koodies.builder.CarDSL.Trait.Exclusive
import koodies.builder.CarDSL.Trait.TaxExempt
import koodies.builder.StatelessBuilder.Returning
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.unit.kilo
import koodies.unit.milli
import kotlin.time.Duration
import kotlin.time.hours

class CarDSL {

    data class Car(val name: String, val color: String, val traits: Set<Trait>, val engine: Engine, val wheels: Int)

    object CarBuilder : BuilderTemplate<CarContext, Car>() {

        class CarContext(
            override val captures: CapturesMap,
        ) : CapturingContext() {
            var name by setter<String>()
            val color by External::color
            val traits by enumSetBuilder<Trait>()
            val engine by Engine
            val wheels by builder<Int>() default 4
        }

        override fun BuildContext.build() = ::CarContext {
            Car(::name.eval(), ::color.evalOrDefault("#111111"), ::traits.eval(), ::engine.eval(), ::wheels.eval())
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
            wheels { 4 }
            traits { +Exclusive + TaxExempt }
        }

        val defaultCarWithCopiedMotor = car {
            name = "Default Car"
            engine instead exclusiveCar.engine
        }

        println(exclusiveCar)
        println(defaultCarWithCopiedMotor)
    }


    inline class EnginePower(val watts: BigDecimal) {
        companion object : Returning<EnginePowerContext, EnginePower>(EnginePowerContext) {
            object EnginePowerContext {
                val Int.kW get() = kilo.W
                val BigDecimal.W: EnginePower get() = EnginePower(this)
            }
        }
    }

    inline class Distance(val meter: BigDecimal) {
        companion object : Returning<DistanceContext, Distance>(DistanceContext) {
            object DistanceContext {
                val Int.mm get() = milli.m
                val BigDecimal.m: Distance get() = Distance(this)
            }
        }
    }

    data class Speed(val distance: Distance, val time: Duration) {
        companion object : Returning<SpeedContext, Speed>(SpeedContext) {
            object SpeedContext {
                val Int.km get() = kilo.m
                val hour = 1.hours
                infix fun Distance.per(time: Duration) = Speed(Distance(this), time)
                val BigDecimal.m: Distance get() = Distance(this)
            }
        }
    }

    data class Engine(val power: EnginePower, val maxSpeed: Speed) {
        companion object EngineBuilder : BuilderTemplate<EngineContext, Engine>() {

            class EngineContext(
                override val captures: CapturesMap,
            ) : CapturingContext() {
                val power by EnginePower
                val maxSpeed by Speed
            }

            override fun BuildContext.build() = ::EngineContext {
                Engine(::power.evalOrDefault { EnginePower { 130.kW } }, ::maxSpeed.evalOrDefault { Speed { 228.km per hour } })
            }
        }
    }

    enum class Trait { Exclusive, PreOwned, TaxExempt }

    object External {
        fun color(h: Int, s: Int, v: Int): String = "hsv($h, $s, $v)"
    }

}
