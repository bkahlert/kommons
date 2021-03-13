package koodies.builder

import koodies.test.SystemIoExclusive
import koodies.test.output.CapturedOutput
import koodies.test.output.OutputCaptureExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains

@Execution(CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
class CarDSLTest {

    @Test
    @SystemIoExclusive
    fun `should print cars`(output: CapturedOutput) {
        CarDSL().printSamples()
        val w1 = "⌀ 40.64cm"
        val w2 = "⌀ 35.56cm"
        expectThat(output.outLines).contains(
            "Car(name=Koodies Car, color=hsv(198, 82, 89), traits=[Exclusive, TaxExempt], engine=244.0km/h, 145.0kW, wheels=[$w1, $w1, $w1, $w1])",
            "Car(name=Default Car, color=#111111, traits=[], engine=244.0km/h, 145.0kW, wheels=[$w2, $w2, $w2, $w2])",
        )
    }
}
