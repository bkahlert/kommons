# Kommons Exec

## About

**Kommons Exec** is a Kotlin Library to execute command lines and shell scripts—locally or in a Docker Container.

## Installation / Setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-exec:2.1.0") { because("command line and shell script execution") }`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-exec</artifactId>
      <version>2.1.0</version>
  </dependency>
  ```

## Features

### ⌨️ *Execute* Command Lines *on* Host

```kotlin
CommandLine("printenv", "HOME")
    .exec() // .exec.logging() // .exec.processing { io -> … } 
```

### 📄 *Execute* Shell Scripts *on* Host

```kotlin
ShellScript { "printenv | grep HOME | perl -pe 's/.*?HOME=//'" }
    .exec() // .exec.logging() // .exec.processing { io -> … }
```

### 🐳 *Execute* Command Lines *in* Docker Container

```kotlin
CommandLine("printenv", "HOME").dockerized { "ubuntu" }
    .dockerized { "ubuntu" }
    .exec() // .exec.logging() // .exec.processing { io -> … }
```

*or even simpler*

```kotlin
with(tempDir()) { // working directory provided via receiver
    ubuntu("printenv", "HOME") // busybox
        .exec() // .exec.logging() // .exec.processing { io -> … }
}
```

### 🐳 *Execute* Shell Scripts *in* Docker Container

```kotlin
ShellScript { "printenv | grep HOME | perl -pe 's/.*?HOME=//'" }
    .dockerized { "ubuntu" }
    .exec() // .exec.logging() // .exec.processing { io -> … }
```

*or even simpler*

```kotlin
with(tempDir()) { // working directory provided via receiver
    ubuntu { "printenv | grep HOME | perl -pe 's/.*?HOME=//'" } // busybox
        .exec() // .exec.logging() // .exec.processing { io -> … }
}
```

### How can you run?

#### Degree of Interaction

##### ▶️ executing-only

```kotlin
CommandLine("…") // ShellScript { … }
    .exec()
```

##### 📝 logging

```kotlin
CommandLine("…") // ShellScript { … }
    .exec.logging()
```

- If things go wrong, it's also logged:
  ```text
  Process {PID} terminated with exit code {…}
  ➜ A dump has been written to:
  - {TempDir}/kommons/exec/dump.*.log
  - {TempDir}/kommons/exec/dump.*.ansi-removed.log
    ➜ The last 10 lines are:
    {…}
    3
    2
    1
    Boom!
  ```  

##### 🧠 processing

```kotlin
CommandLine("…") // ShellScript { … }
    .exec.processing { io -> doSomething(io) }`
```

- `io` is typed; simply use `io is IO.Output` to filter out errors and meta information

#### Synchronicity

##### 👯‍♀️ 👯‍♂️ synchronous

```kotlin
CommandLine("…") // ShellScript { … }
    .exec() // .exec.logging() // .exec.processing { io -> … }
```

##### 💃 🕺 asynchronous

```kotlin
CommandLine("…") // ShellScript { … }
    .exec.async() // .exec.async.logging() // .exec.async.processing { io -> … }
```

### Features

#### Automatically Captured I/O

Whatever variant you choose, life-cycle events, sent input, the process's output and errors are stored for you:

```kotlin
CommandLine(…).exec().io
CommandLine(…).exec().io.output
CommandLine(…).exec().io.error.ansiRemoved
```

#### Typed (Exit) State

- Access the state with `state`, which is either an instance of `Running`, `Exited`
  (with the sub states `Succeeded` and `Failed`) or `Excepted`.
- All states print nicely and provide a copy of all logged I/O, and state-dependent information such as the exit code.
- By default, processes are killed on VM shutdown, which can be configured.
- Life-cycle callbacks can be registered.

#### Ready to run Docker commands

```kotlin
with(tempDir()) {
    SvgFile.copyTo(resolve("kommons.svg"))

    // convert SVG to PNG using command line-style docker command
    docker("minidocks/librsvg", "-z", 5, "--output", "kommons.png", "kommons.svg")

    // convert PNG to ASCII art using shell script-style docker command
    docker("rafib/awesome-cli-binaries", logger = null) {
        """
           /opt/bin/chafa -c none --fill all  -w 9 kommons.png
        """
    }.io.output.ansiKept.resetLines().let { println(it) }
}
```

##### Output

<!-- @formatter:off -->
```text
&&&&&&&&&&&&&&&&&&&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶ 
&&&&&&&&&&&&&&&&&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶   
&&&&&&&&&&&&&&&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶     
&&&&&&&&&&&&&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶       
&&&&&&&&&&&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶         
&&&&&&&&&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶           
&&&&&&&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶             
&&&&&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶               
&&&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶                 
&&&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶                   
&&&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶                     
&▲┺u╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┶                       
┺╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲┺┘                        
╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲z╲╊╊}╷                      
╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲z╲╊╊╊╊╊╊}╷                    
╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲z╲╊╊╊╊╊╊╊╊╊╊}╷                  
╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲╲z╲╊╊╊╊╊╊╊╊╊╊╊╊╊╊}╷                
╲╲╲╲╲╲╲╲╲╲╲╲╲╲z╲╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊}╷              
╲╲╲╲╲╲╲╲╲╲╲╲z╲╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊}╷            
╲╲╲╲╲╲╲╲╲╲z╲╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊}╷          
╲╲╲╲╲╲╲╲z╲╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊}╷        
╲╲╲╲╲╲z╲╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊}╷      
╲╲╲╲z╲╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊}╷    
u╲z╲╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊}╷  
┺╲╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊╊┾⣠
```
<!-- @formatter:on -->

- All docker commands (`docker`, `ubuntu`, `busybox`, `curl`, `download`, `nginx`, …) use the path in the receiver to
    - set the working directory of both the host command and the docker container
    - map the host working directory to the container's working directory,
    - that is, all files of that directory are equally available in your container instance.
- Low-level docker commands: `start`, `run`, `stop`, `kill`, `remove`, `search`, `image`, `ps`
- Object-oriented design
    - [Docker](src/jvmMain/kotlin/com/bkahlert/kommons/docker/Docker.kt): `engineRunning`, `info`, `images`, `containers`, `search`, `exec`
    - [DockerImage](src/jvmMain/kotlin/com/bkahlert/kommons/docker/DockerImage.kt): `list`, `isPulled`, `pull`, `tagsOnDockerHub`
    - [DockerContainer](src/jvmMain/kotlin/com/bkahlert/kommons/docker/DockerContainer.kt): `start`, `stop`, `state`, `kill`, `remove`
- See [ExecutionIntegrationTest.kt](src/jvmTest/kotlin/com/bkahlert/kommons/ExecutionIntegrationTest.kt) and
  [Docker.kt](src/jvmMain/kotlin/com/bkahlert/kommons/docker/Docker.kt) for more examples.

## Contributing

Want to contribute? Awesome! The most basic way to show your support is to star the project, or to raise issues. You can also support this project by making
a [Paypal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it is much appreciated! :pray:

## License

MIT. See [LICENSE](LICENSE) for more details.
