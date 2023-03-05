# Kommons BOM

## About

**Kommons BOM** is a platform library with the version of most Kommons modules.

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle**
  ```kotlin
  implementation(platform("com.bkahlert.kommons:kommons-bom:2.7.0"))
  
  // add the modules you like without having to specify their version
  implementation("com.bkahlert.kommons:kommons-uri")
  ```

* **Maven**
  ```xml
  <project>
  
    <dependencyManagement>
      <dependencies>
        <dependency>
          <groupId>com.bkahlert.kommons</groupId>
          <artifactId>kommons-bom</artifactId>
          <version>2.7.0</version>
          <type>pom</type>
          <scope>import</scope>
        </dependency>
      </dependencies>
    </dependencyManagement>
  
    <dependencies>
      <!-- add the modules you like without having to specify their version -->
      <dependency>
        <groupId>com.bkahlert.kommons</groupId>
        <artifactId>kommons-uri</artifactId>
          <version>2.7.0</version>
      </dependency>
    </dependencies>
  
  </project>
  ```

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../LICENSE) for more details.
