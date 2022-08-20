package com.bkahlert.kommons

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("service")
public class ServiceProperties {
    /**
     * A message for the service.
     */
    public var message: String? = null
}
