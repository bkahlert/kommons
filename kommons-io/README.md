# Kommons IO

## About

**Kommons IO** is a Kotlin Library for simpler IO handling on the JVM.

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-io:2.8.0") { because("print debugging") }`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-io</artifactId>
      <version>2.8.0</version>
  </dependency>
  ```

## Features

Create files with contents in one call using

- `createTextFile`
- `createBinaryFile`
- `createTempTextFile`
- `createTempBinaryFile`

Read files with

- `useInputStream`,
- `useBufferedInputStream`,
- `useReader`, and
- `useBufferedReader`

Write files with

- `useOutputStream`,
- `useBufferedOutputStream`,
- `useWriter`, and
- `useBufferedWriter`

Find the class directory, the source directory or the source file itself of a class.

### Example

```kotlin
Foo::class.findClassesDirectoryOrNull()  // /home/john/dev/project/build/classes/kotlin/jvm/test
Foo::class.findSourceDirectoryOrNull()   // /home/john/dev/project/src/jvmTest/kotlin
Foo::class.findSourceFileOrNull()        // /home/john/dev/project/src/jvmTest/kotlin/packages/source.kt
```

Access class path resources like any other NIO 2 path using the `classpath` URI scheme.

### Example

```kotlin
Paths.get("classpath:dir/to/resource").readText()
Paths.get("classpath:dir/to/resource").readBytes()
Paths.get("classpath:dir/to/resource").copyToDirectory(SystemLocations.Temp)
Paths.get("classpath:dir/to/resource").useBufferedReader { it.readLine() }
```

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../LICENSE) for more details.
