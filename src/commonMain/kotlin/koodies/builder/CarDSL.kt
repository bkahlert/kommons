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
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.unit.kilo
import koodies.unit.milli
import kotlin.time.Duration
import kotlin.time.hours

public class CarDSL {

    public data class Car(val name: String, val color: String, val traits: Set<Trait>, val engine: Engine, val wheels: Int)

    public object CarBuilder : BuilderTemplate<CarContext, Car>() {

        public class CarContext(
            override val captures: CapturesMap,
        ) : CapturingContext() {
            public var name: String? by setter<String>()
            public val color: (h: Int, s: Int, v: Int) -> Unit by External::color
            public val traits: SkippableCapturingBuilderInterface<ListBuildingContext<Trait>.() -> Unit, Set<Trait>> by enumSetBuilder<Trait>()
            public val engine: SkippableCapturingBuilderInterface<EngineContext.() -> Unit, Engine?> by Engine
            public val wheels: SkippableCapturingBuilderInterface<() -> Int, Int> by builder<Int>() default 4
        }

        override fun BuildContext.build(): Car = ::CarContext {
            Car(::name.eval(), ::color.evalOrDefault("#111111"), ::traits.eval(), ::engine.eval(), ::wheels.eval())
        }
    }

    public fun car(init: Init<CarContext>): Car = CarBuilder(init)

    public fun printSamples() {

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
            engine using exclusiveCar.engine
        }

        println(exclusiveCar)
        println(defaultCarWithCopiedMotor)
    }


    public inline class EnginePower(public val watts: BigDecimal) {
        public companion object : Returning<EnginePowerContext, EnginePower>(EnginePowerContext) {
            public object EnginePowerContext {
                public val Int.kW: EnginePower get() = kilo.W
                public val BigDecimal.W: EnginePower get() = EnginePower(this)
            }
        }
    }

    public inline class Distance(public val meter: BigDecimal) {
        public companion object : Returning<DistanceContext, Distance>(DistanceContext) {
            public object DistanceContext {
                public val Int.mm: Distance get() = milli.m
                public val BigDecimal.m: Distance get() = Distance(this)
            }
        }
    }

    public data class Speed(val distance: Distance, val time: Duration) {
        public companion object : Returning<SpeedContext, Speed>(SpeedContext) {
            public object SpeedContext {
                public val Int.km: Distance get() = kilo.m
                public val hour: Duration = 1.hours
                public infix fun Distance.per(time: Duration): Speed = Speed(this, time)
                public val BigDecimal.m: Distance get() = Distance(this)
            }
        }
    }

    public data class Engine(val power: EnginePower, val maxSpeed: Speed) {
        public companion object EngineBuilder : BuilderTemplate<EngineContext, Engine>() {

            public class EngineContext(
                override val captures: CapturesMap,
            ) : CapturingContext() {
                public val power: SkippableCapturingBuilderInterface<EnginePowerContext.() -> EnginePower, EnginePower?> by EnginePower
                public val maxSpeed: SkippableCapturingBuilderInterface<SpeedContext.() -> Speed, Speed?> by Speed
            }

            override fun BuildContext.build(): Engine = ::EngineContext {
                Engine(::power.evalOrDefault { EnginePower { 130.kW } }, ::maxSpeed.evalOrDefault { Speed { 228.km per hour } })
            }
        }
    }

    public enum class Trait { Exclusive, PreOwned, TaxExempt }

    public object External {
        public fun color(h: Int, s: Int, v: Int): String = "hsv($h, $s, $v)"
    }

}
