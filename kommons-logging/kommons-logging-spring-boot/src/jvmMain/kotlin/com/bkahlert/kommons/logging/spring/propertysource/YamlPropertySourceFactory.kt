package com.bkahlert.kommons.logging.spring.propertysource

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory
import org.springframework.lang.Nullable

public class YamlPropertySourceFactory : PropertySourceFactory {

    override fun createPropertySource(@Nullable name: String?, resource: EncodedResource): PropertySource<*> {
        return LOADER.load(name, resource.resource).stream().findFirst()
            .orElseThrow { NoSuchElementException("Could not load $resource") }
    }

    public companion object {
        private val LOADER = YamlPropertySourceLoader()
    }
}
