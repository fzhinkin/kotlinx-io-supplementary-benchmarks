plugins {
    kotlin("jvm")
    id("me.champeau.jmh")
}

dependencies {
    implementation(project(":shared"))
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}
