# Kommons Core

## About

**Kommons Core** is a Kotlin Multiplatform Library that offers:

1. [runtime](#runtime) information on the running [program](#program) and its [platform](#platform)
2. [system locations](#system-locations)
2. [string operations](#string-operations)
3. facilitated [time handling](#time-operations) using `Now`, `Yesterday`, and `Tomorrow`
3. [byte operations](#byte-operations)
4. [factories](#factories) to easily implement `of`/`ofOrNull`, `from`/`fromOrNull`, and `parse`/`parseOrNull`

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-core:2.7.0")

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-core</artifactId>
      <version>2.7.0</version>
  </dependency>
  ```

## Features

### Runtime

#### Program

Reflects the running program and provides:

- `Program.isDebugging`: Returns whether the program is running in debug mode.
- `Program.onExit`: Allows registering callbacks that are invoked when the program exits.

#### Platform

Reflects the platform the program runs on (e.g. `Platform.JVM`)
and provides:

- `Platform.ansiSupport`: Returns to what extent ANSI escape codes are supported.

### System Locations

```kotlin
SystemLocations.Work        // /home/john/dev/project
SystemLocations.Home        // /home/john
SystemLocations.Temp        // /tmp
SystemLocations.JavaHome    // /usr/lib/jvm/java-8-openjdk-amd64
```

### String Operations

#### Any?.asEmoji()

Renders any object as an emoji.

##### Examples

```kotlin
null.asEmoji()       //  "❔"
true.asEmoji()       //  "✅"
false.asEmoji()      //  "❌"
Now.asEmoji()        //  "🕝"
Now.asEmoji(Floor)   //  "🕑"
"other".asEmoji()    //  "🔣"
```

#### Any?.quoted

Escapes and wraps the string returned by `toString()` double quotes.

```kotlin
"string".quoted              // "string"
"""{ bar: "baz" }""".quoted  // "{ bar: \"baz\" }"

"""
line 1
"line 2"
""".quoted                   // "line1\n\"line2\""
```

#### String.ansiRemoved

Removes any ANSI escape sequences from a string.

```kotlin
"\u001B[1mbold \u001B[34mand blue\u001B[0m".ansiRemoved
// "bold and blue"

"\u001B[34m↗\u001B(B\u001B[m \u001B]8;;https://example.com\u001B\\link\u001B]8;;\u001B\\".ansiRemoved
// "↗ link"
```

#### randomString

Returns a random string.

```kotlin
randomString()
// returns "Ax-212kss0-xTzy5" (16 characters by default) 
```

### Collections and Ranges

Require or check emptiness of collections and arrays using `requireNotEmpty`
and `checkNotEmpty`.

Iterate any type of closed ranges using `asIterable`.

#### Examples

```kotlin
(-4.2..42.0)
    .asIterable { it + 9 }
    .map { it.toInt() } // [-4, 4, 13, 22, 31, 40]
```

### Time Operations

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

### Byte Operations

#### toHexadecimalString(), toOctalString(), toBinaryString()

The extension functions

- `toHexadecimalString()`
- `toOctalString()`
- `toBinaryString()`

... are provided for:

- `Byte`
- `ByteArray`
- `Int`
- `Long`
- `UByte`
- `UByteArray`
- `UInt`
- `ULong`

##### Examples

```kotlin
val byteArray = byteArrayOf(0x00, 0x7f, -0x80, -0x01)
val largeByteArrayOf = byteArrayOf(-0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01)
val veryLargeByteArray = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

byteArray.map { it.toHexadecimalString() } // "00", "7f", "80", "ff"
byteArray.toHexadecimalString()            // "007f80ff"
largeByteArrayOf.toHexadecimalString()     // "ffffffffffffffffffffffffffffffff"
veryLargeByteArray.toHexadecimalString()   // "0100000000000000000000000000000000"

byteArray.map { it.toOctalString() } // "000", "177", "200", "377"
byteArray.toOctalString()            // "000177200377"
largeByteArrayOf.toOctalString()     // "377377377377377377377377377377377377377377377377"
veryLargeByteArray.toOctalString()   // "001000000000000000000000000000000000000000000000000"

byteArray.map { it.toBinaryString() } // "00000000", "01111111", "10000000", "11111111"
byteArray.toBinaryString()            // "00000000011111111000000011111111"
largeByteArrayOf.toBinaryString()     //         "111111111111111111111111111...111111"
veryLargeByteArray.toBinaryString()   // "00000001000000000000000000000000000...000000"
```

Further conversions:

- `Int.toByteArray()`
- `Long.toByteArray()`
- `UInt.toUByteArray()`
- `ULong.toUByteArray()`

#### Checksums

Compute `MD5`, `SHA-1`, and `SHA-256` checksums for arbitrary files.

##### Examples

```kotlin
val file = SystemLocations.Home / ".gitconfig"
file.md5Checksum()
file.sha1Checksum()
file.sha256Checksum()
```

### Factories

The [Factory interface](src/commonMain/kotlin/com/bkahlert/kommons/factories.kt) provides
the factory builders `creator`, `converter`, and `parser` to easily implement
the factory methods `of`/`ofOrNull`, `from`/`fromOrNull`, and `parse`/`parseOrNull`
as shown in the following example:

```kotlin
data class Version(val major: Int, val minor: Int, val patch: Int) {
    companion object : Parser<Version> by (parser {                 // The `parsing` method supports the following outcomes:
        it.split('.').let { (major, minor, patch) ->                // - return a `Version` instance in case of success 
            Version(major.toInt(), minor.toInt(), patch.toInt())    // - return `null` a generic ParsingException is thrown.
        }                                                           // - If you throw an exception it will be wrapped in a ParsingException.
    })
}

Version.parseOrNull("1.2.3")   // returns Version(1, 2, 3)
Version.parse("invalid")       // throws ParsingException: "Failed to parse "invalid" into an instance of Version"
```

### Miscellaneous

#### Scaling

```kotlin
0.5.scale(+0.5)               // = +0.75 (0.5 scaled 50% closer to +1.0) 
0.5.scale(-0.5)               // = -0.25 (0.5 scaled 50% closer to -1.0) 

4.0.scale(+0.5, -10.0..+10.0)  // = +7.0 (4.0 scaled 50% closer to +10.0) 
4.0.scale(-0.5, -10.0..+10.0)  // = -4.0 (4.0 scaled 50% closer to -10.0) 
```

#### Either

Generic either type that can be used as a replacement for `Result`,
i.e. in cases where the alternative value doesn't necessarily mean failure.

Available methods are:

- `getLeftOrThrow` / `getRightOrThrow`
- `getLeftOrElse` / `getRightOrElse`
- `getLeftOrDefault` / `getRightOrDefault`
- `fold` / `mapLeft` / `mapRight`
- `onLeft` / `onLeft`
- `toResult` / `Result.toEither`

```kotlin
val foo: Either<Foo, Bar> = Left(Foo)
foo.getLeftOrThrow()   // returns Foo
foo.getRighttOrThrow() // throws an exception
```

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../LICENSE) for more details.
