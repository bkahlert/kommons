package com.bkahlert.kommons.logging.sample.helloworld

import com.bkahlert.kommons.logging.SLF4J
import com.bkahlert.kommons.logging.logback.StructuredArguments.v
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("hello-world")
class HelloWorldController(
    val props: HelloWorldConfigurationProperties,
) {

    private val logger by SLF4J

    @GetMapping("{name}", produces = ["text/plain"])
    fun getHello(@PathVariable name: String): String {
        logger.info("Service says hello to {}", v("name", name))
        return "${props.greeting}, $name"
    }

    @PostMapping(produces = ["text/plain"])
    fun postHello(@RequestBody name: String): String {
        logger.info("Service says hello to {}", v("name", name))
        return "${props.greeting}, $name"
    }
}
