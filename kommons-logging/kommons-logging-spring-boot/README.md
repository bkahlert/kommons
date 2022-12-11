# Kommons Logging: Spring Boot

## About

**Kommons Logging: Spring Boot** is the Spring Boot integration of [Kommons Logging: Logback](../kommons-logging-logback).

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-logging-spring-boot:2.4.1")`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-logging-spring-boot</artifactId>
      <version>2.4.1</version>
  </dependency>
  ```

## Features

You can configure logging to the console and to a file using application properties.

The following settings make your application

- log to the console the way [Spring Boot](https://spring.io/projects/spring-boot/) does, and
- log to a file using the JSON format:

```properties
# application.properties
logging.preset.console=spring
logging.preset.file=json
```

```yaml
# application.yaml
logging:
  preset:
    console: spring
    file: json
```

Spring Boot's logging configuration options like `file.path` and `logback.rollingpolicy.max-file-size` are still supported.


## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../../LICENSE) for more details.
