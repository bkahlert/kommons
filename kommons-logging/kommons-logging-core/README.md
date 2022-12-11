# Kommons Logging: Core

## About

**Kommons Logging: Core** is a Kotlin Multiplatform Library with convenience features
for [Kotlin Logging](https://github.com/MicroUtils/kotlin-logging)
and [SLF4J](https://www.slf4j.org/).

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-logging-core:2.4.1")`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-logging-core</artifactId>
      <version>2.4.1</version>
  </dependency>
  ```

## Features

### Get Logger

No matter how you prefer to get and where to store your logger,
you always get the right one:

```kotlin
@file:JvmName("FooKt")

val logger by KotlinLogging          // logger name: "FooKt"

class Foo {
    val logger by KotlinLogging      // logger name: "Foo"

    companion object {
        val logger by SLF4J          // logger name: "Foo"
    }
}
```

The example demonstrates how and where loggers can be retrieved from any of the supported logger factories.

The logger factories `KotlinLogging` and `SLF4J` can be used interchangeably.

The name of the logger is always the name of the enclosing class or
the file class if there is no regular enclosing class.

*Due to limited reflection support in Kotlin/JS,
file-level loggers have the name `<global>` and
companion-level loggers have the name `Companion`.*

### Loggers of Proxied Classes

Users of frameworks that proxy classes benefit from sanitized logger names:

```kotlin
@SpringBootApplication
class FooApplication {
    val logger by SLF4J              // logger name: "FooApplication"
}
```

The example shows a Spring Boot application and a logger
that uses the name you would expect: `FooApplication`

If you ask yourself, how this could be any different, test a Spring application of your choice.
Chances are that log messages produced by the above logger will have a name of the form `FooApplication$$EnhancerBySpringCGLIB$$a5932cf` which complicates log
analyses.

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../../LICENSE) for more details.
