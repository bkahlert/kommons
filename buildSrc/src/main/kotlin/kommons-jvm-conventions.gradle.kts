import com.bkahlert.kommons.gradle.jvmBytecodeTarget

plugins {
    id("kotlin-conventions")
    id("kommons-test-jdk-conventions")
}

kotlin {
    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
}

jvmBytecodeTarget(8)
