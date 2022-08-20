# Kommons Logging: Logback

## About

**Kommons Logging: Logback** is a Kotlin Library for configuring [Logback](https://logback.qos.ch/) with nothing but system properties, and provides support for
JSON.

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-logging-logback:2.0.0")`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-logging-logback</artifactId>
      <version>2.0.0</version>
  </dependency>
  ```

## Features

### Logging Presets

Logging can be configured based on presets. The following exist:

#### spring

Configures the log to use the default Spring Boot logging settings:

```log
2022-08-29 00:08:52.917  INFO   --- [ool-1-worker-18] TestLogger                               : message
```

#### minimal

Configures the log to only include the time, log level, logger, and message:

```log
08:52.917  INFO TestLogger                     : message
```

#### json

Configures the log to log using the JSON format:

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

#### off

Configures the log to not log at all.

#### default

Explicitly configures the log to use default logging settings,
see [Console Logging](#console-logging) and [File Logging](#file-logging).

### Console Logging

Logging to the console is enabled by default and uses the [spring preset](#spring)—though you **don't need Spring** to use it.

To use a different preset, start your application with the system property `CONSOLE_LOG_PRESET` set to an [available preset](#logging-presets),
e.g. [minimal](#minimal).

### File Logging

Logging to a file is turned off by default.

To use a file logging, start your application with the system property `FILE_LOG_PRESET` set to an [available preset](#logging-presets), e.g. [json](#json).

You can set the following system properties to customize file logging:

- `LOG_FILE`: file used for logging, default: `$LOG_PATH/kommons.log`
- `LOG_PATH`: directory used for logging, default: `$TMP`
- `LOGBACK_ROLLINGPOLICY_*`: check [rolling-policy.xml](src/jvmMain/resources/com/bkahlert/kommons/logging/logback/includes/rolling-policy.xml) for available
  properties and default values

## Contributing

Want to contribute? Awesome! The most basic way to show your support is to star the project, or to raise issues. You can also support this project by making
a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it is much appreciated! :pray:

## License

MIT. See [LICENSE](LICENSE) for more details.
