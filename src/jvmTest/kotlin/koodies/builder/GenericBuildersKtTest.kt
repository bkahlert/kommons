//package koodies.builder
//
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.parallel.Execution
//import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
//import strikt.api.expectThat
//import strikt.assertions.contains
//
//@Execution(CONCURRENT)
//class GenericBuildersKtTest {
//
//    private val producer: () -> String = { "string" }
//
//    @Test
//    fun `should build using producer`() {
//        val target = mutableListOf<Int>()
//        producer.buildTo(target) { length }
//        expectThat(target).contains("string".length)
//    }
//
//
//    private class InvocableZeroArgBuilder() : (InvocableZeroArgBuilder) -> String by { it.instance } {
//        private lateinit var instance: String
//
//        fun settings(setting: String) {
//            instance = setting
//        }
//    }
//
//    @Test
//    fun `should build using invocable zero-arg builder`() {
//        val target = mutableListOf<Int>()
//        val init: InvocableZeroArgBuilder.() -> Unit = { settings("some setting") }
//        init.buildTo(target) { length }
//        expectThat(target).contains("some setting".length)
//    }
//
//
//    private class AccessedBuilder() {
//        companion object : BuilderAccessor<AccessedBuilder, String> {
//            override fun invoke(): AccessedBuilder = AccessedBuilder()
//            override fun AccessedBuilder.invoke(): String = instance
//        }
//
//        private lateinit var instance: String
//
//        fun settings(setting: String) {
//            instance = setting
//        }
//    }
//
//    @Test
//    fun `should build using builder accessor`() {
//        val target = mutableListOf<Int>()
//        val init: AccessedBuilder.() -> Unit = { settings("some setting") }
//        AccessedBuilder.buildTo(init, target) { length }
//        expectThat(target).contains("some setting".length)
//    }
//}
