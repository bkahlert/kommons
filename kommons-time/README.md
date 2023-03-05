# Kommons Time

## About

**Kommons Time** is a Kotlin Multiplatform Library that extends
the [KotlinX multiplatform date/time library](https://github.com/Kotlin/kotlinx-datetime)

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-time:2.7.0")`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-time</artifactId>
      <version>2.7.0</version>
  </dependency>
  ```

## Features

### Time operations

```kotlin
Now + 2.seconds     // 2 seconds in the future
Today - 3.days      // 3 days in the past
Yesterday - 2.days  // 3 days in the past
Tomorrow + 1.days   // the day after tomorrow
Instant.parse("1910-06-22T13:00:00Z") + 5.minutes // 1910-06-22T12:05:00Z
LocalDate.parse("1910-06-22") - 2.days            // 1910-06-20
SystemLocations.Temp.createTempFile().age         // < 1ms

Now.toMomentString()               // "now"
(Now - 12.hours).toMomentString()  // "12h ago"
(Now + 3.days).toMomentString()    // "in 3d"
(Today - 1.days).toMomentString()  // "yesterday"
```

### Serializers

- Duration
    - DurationAsNanosecondsSerializer
    - DurationAsMicrosecondsSerializer
    - DurationAsMillisecondsSerializer
    - DurationAsSecondsSerializer
    - DurationAsMinutesSerializer
    - DurationAsHoursSerializer
    - DurationAsDaysSerializer
- Instant
    - InstantAsEpochMilliseconds
    - InstantAsEpochSeconds

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../LICENSE) for more details.
