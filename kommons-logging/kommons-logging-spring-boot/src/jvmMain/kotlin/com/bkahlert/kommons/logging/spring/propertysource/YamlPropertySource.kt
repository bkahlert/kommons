package com.bkahlert.kommons.logging.spring.propertysource

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.CompositePropertySource
import org.springframework.core.io.DefaultResourceLoader

/**
 * Property source that loads a YAML file, create one property source for each document ands merges them.
 */
public class YamlPropertySource(name: String, loggingConfigResource: String) : CompositePropertySource(name) {
    init {
        val resource = DefaultResourceLoader().getResource(loggingConfigResource)
        YamlPropertySourceLoader().load(name, resource).forEach { addPropertySource(it) }
    }
}
