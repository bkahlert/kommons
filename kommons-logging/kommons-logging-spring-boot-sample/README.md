# Kommons Logging: Spring Boot Sample

## About

**Kommons Logging: Spring Boot Sample** is a Spring Boot application to demonstrate the usage
of [Kommons Logging: Spring Boot](../kommons-logging-spring-boot) using the [Kommons Logging: Spring Boot Starter](../kommons-logging-spring-boot-starter).

## Usage

To play around with this sample application, you need to download it first.

Open your shell and type:

```shell
git clone https://github.com/bkahlert/kommons.git
cd kommons
```

Once downloaded you can either run the application or run the tests:

```shell
# run application and check logfile in your browser
open http://localhost:8080/actuator/logfile
./gradlew :kommons-logging:kommons-logging-spring-boot-sample:bootRun

# run tests
./gradlew :kommons-logging:kommons-logging-spring-boot-sample:test
```

If you want to use this module as the starting point for a microservice of your own:

1. Make a copy of [kommons-logging/kommons-logging-spring-boot-sample](.)
2. Uncomment the commented lines in the dependency block of [build.gradle.kts](build.gradle.kts) and remove the project dependencies.

More information can be found on [Kommons Logging: Spring Boot](../kommons-logging-spring-boot)
and [spring.io/guides/gs/spring-boot](https://spring.io/guides/gs/spring-boot/).

## Features

- [SampleApplication.kt](src/main/kotlin/com/bkahlert/kommons/logging/sample/SampleApplication.kt) contains the application's entry point.
- [hello-world](src/main/kotlin/com/bkahlert/kommons/logging/sample/helloworld) contains a rest controller implementing a hello-world endpoint.
- [application.yaml](src/main/resources/application.yml) contains the application's configuration and be used to change the way logging takes place.
- [SampleSpringBootTest.kt](src/test/kotlin/com/bkahlert/kommons/logging/sample/SampleSpringBootTest.kt) contains a minimal Spring Boot test.
- [SampleUnitTest.kt](src/test/kotlin/com/bkahlert/kommons/logging/sample/SampleUnitTest.kt) contains a minimal unit test.

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../../LICENSE) for more details.
