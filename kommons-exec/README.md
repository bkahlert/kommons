# Kommons Exec

## About

**Kommons Exec** is a Kotlin Library to execute command lines and shell scripts.

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-exec:2.2.0") { because("print debugging") }`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-exec</artifactId>
      <version>2.2.0</version>
  </dependency>
  ```

## Features

### Run command lines

```kotlin
// run a command line
CommandLine("echo", "output").exec()

// run a command line and read its output
CommandLine("echo", "output").exec().readLinesOrThrow()

// run a command line and log its output
CommandLine("echo", "output").exec.logging()
```

### Run shell scripts

```kotlin
// run a shell script
ShellScript("echo output").exec()

// run a shell script and read its output
ShellScript("echo output").exec().readLinesOrThrow()

// run a shell script and log its output
ShellScript("echo output").exec.logging()
```

### Error handling

```kotlin
// run a command line or shell script ...
val exitState = ShellScript(
    """
        echo output
        echo error 1>&2
        exit 42
    """.trimIndent()
).exec()

// ... and handle errors using states
when (exitState) {
    is Succeeded -> logger.info("Process took ${exitState.runtime}")      // "Process took 12 ms"
    is Failed -> logger.error("Process ${exitState.process.pid} failed")  // "Process 4323 failed"
}

// ... or handle errors using exceptions
exitState.readLinesOrThrow()  // throws IOException "Process 4323 terminated after 12 ms with exit code 42"
```

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../LICENSE) for more details.
