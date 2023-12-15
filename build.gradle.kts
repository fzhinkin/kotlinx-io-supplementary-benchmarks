plugins {
    kotlin("jvm") version "1.9.21"
    id("me.champeau.jmh") version "0.7.2"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}
