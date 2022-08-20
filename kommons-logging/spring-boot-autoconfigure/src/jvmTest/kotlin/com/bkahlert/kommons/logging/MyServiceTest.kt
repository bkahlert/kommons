package com.bkahlert.kommons.logging

import com.bkahlert.kommons.MyService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest("service.message=Hello")
class MyServiceTest(
    val myService: MyService,
) {

    @Test
    fun contextLoads() {
        assertThat(myService.message()).isNotNull();
    }

    @SpringBootApplication
    open class TestConfiguration
}
