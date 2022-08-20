package com.bkahlert.kommons

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service

@Service
@EnableConfigurationProperties(ServiceProperties::class)
public class MyService(private val serviceProperties: ServiceProperties) {
    public fun message(): String? {
        return serviceProperties.message
    }
}
