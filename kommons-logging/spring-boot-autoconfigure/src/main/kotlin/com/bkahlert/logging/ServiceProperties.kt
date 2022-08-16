package com.bkahlert.logging

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("service")
public class ServiceProperties {
    /**
     * A message for the service.
     */
    public var message: String? = null
}
