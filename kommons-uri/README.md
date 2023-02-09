# Kommons URI

## About

**Kommons URI** is a Kotlin Multiplatform Library that offers:

1. parsing and serializing [Uniform Resource Identifiers (RFC3986)](https://www.rfc-editor.org/rfc/rfc3986)
2. support for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
3. support for [data URIs (RFC2397)](https://www.rfc-editor.org/rfc/rfc2397) with an
   appropriate [URL stream handler provider](https://docs.oracle.com/javase/9/docs/api/java/net/spi/URLStreamHandlerProvider.html)
   registered on the JVM

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-uri:2.7.0")

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-uri</artifactId>
      <version>2.7.0</version>
  </dependency>
  ```

## Features

### Multiplatform

```kotlin
// Parser
val uri = Uri.parse("https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz")
val dataUri = Uri.parse("data:text/plain;charset=UTF-8;base64,ICEwQEF6e3x9fg")

// Extensions
val endpoint = uri / "path-segment"
// https://username:password@example.com:8080/poo/par/path-segment?qoo=qar&qaz#foo=far&faz
```

### JVM

```kotlin
// Parser
val uri = URI("https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz")
val dataUrl = URL("data:text/plain;charset=UTF-8;base64,ICEwQEF6e3x9fg")

// Extensions
val endpoint = uri / "path-segment" // same extensions on URI and URL
val decoded = dataUrl.openConnection().getInputStream().readBytes().decodeToString() // " !0@Az{|}~"
val decodedWithKotlin = dataUrl.readText() // " !0@Az{|}~" 
```

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../LICENSE) for more details.
