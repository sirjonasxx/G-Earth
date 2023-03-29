import org.gradle.internal.os.OperatingSystem

plugins {
    java
    `java-library`
}

description = "G-Wasm"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}