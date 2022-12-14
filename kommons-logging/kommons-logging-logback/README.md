# Kommons Logging: Logback

## About

**Kommons Logging: Logback** is a Kotlin Library for configuring [Logback](https://logback.qos.ch/) with
nothing but system properties, and provides support for JSON.

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-logging-logback:2.5.0")`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-logging-logback</artifactId>
      <version>2.5.0</version>
  </dependency>
  ```

## Features

### System property based configuration

You can configure logging to the console and to a file using system properties.

The following example invokes `java` with the system properties

- `CONSOLE_LOG_PRESET` set to `spring`, and
- `FILE_LOG_PRESET` set to `json`:

```shell
java -DCONSOLE_LOG_PRESET=spring -DFILE_LOG_PRESET=json
```

Those settings make your application

- log to the console the way [Spring Boot](https://spring.io/projects/spring-boot/) does, and
- log to a file using the JSON format.

You can choose from the following presets:

#### Spring Preset: `spring`

Configures the log to use the default Spring Boot logging settings:

```log
2022-08-29 00:08:52.917  INFO   --- [ool-1-worker-18] TestLogger                               : message
```

This is the default preset for **console logging**.

#### Minimal Preset: `minimal`

Configures the log to only include the time, log level, logger, and message:

```log
08:52.917  INFO TestLogger                     : message
```

#### JSON Preset: `json`

Configures the log to use the JSON format:

```json
{
  "@timestamp": "2022-08-29T00:08:52.917+02:00",
  "level": "INFO",
  "thread_name": "ForkJoinPool-1-worker-18",
  "logger_name": "TestLogger",
  "message": "message",
  "key": "value",
  "foo": "bar"
}
```

#### Off Preset: `off`

The `off` preset turns off logging.

This is the default preset for **file logging**.  
To enable file logging, `FILE_LOGGING_PRESET` needs to be set to:

- `spring`,
- `minimal`, or
- `json`.

You can set the following system properties to customize file logging:

- `LOG_FILE`: file used for logging, default: `$LOG_PATH/kommons.log`
- `LOG_PATH`: directory used for logging, default: `$TMP`
- `LOGBACK_ROLLINGPOLICY_*`: check [rolling-policy.xml](src/jvmMain/resources/com/bkahlert/kommons/logging/logback/includes/rolling-policy.xml) for available
  properties and default values

### Convenient structured arguments

Given you wanted to log the successful creation of these objects:

```kotlin
data class FooBar(val foo: String, val bar: String = "baz")

val createdObjects = listOf(
    FooBar("1"),
    FooBar("2"),
)
```

You could do it the old-fashioned way:

```kotlin
logger.info("Successfully created {}", createdObjects)
```

```json
{
  "@timestamp": "2022-12-14T14:41:42.777+01:00",
  "level": "INFO",
  "message": "Successfully created [FooBar(foo=1, bar=baz), FooBar(foo=2, bar=baz)]"
}
```

Or, you could log your arguments structured, making your log files
far more valuable:

```kotlin
logger.info("Successfully created {}", StructuredArguments.objects("objects", createdObjects))
```

```json
{
  "@timestamp": "2022-12-14T14:41:42.778+01:00",
  "level": "INFO",
  "message": "Successfully created objects=[FooBar(foo=1, bar=baz), FooBar(foo=2, bar=baz)]",
  "objects": [
    {
      "foo": "1",
      "bar": "baz"
    },
    {
      "foo": "2",
      "bar": "baz"
    }
  ]
}
```

You can even auto-derive the JSON key from the type of your objects, or
customize their serialization:

```kotlin
logger.info("Successfully created {}", StructuredArguments.objects(createdObjects) { "foo-${it.foo}" })
```

```json
{
  "@timestamp": "2022-12-14T14:51:57.583+01:00",
  "level": "INFO",
  "message": "Successfully created foo-bars=[foo-1, foo-2]",
  "foo-bars": [
    "foo-1",
    "foo-2"
  ]
}
```

Explore the [StructuredArguments](src/jvmMain/kotlin/com/bkahlert/kommons/logging/logback/StructuredArguments.kt) for more options.


## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../../LICENSE) for more details.
