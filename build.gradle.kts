plugins {
    application
    kotlin("jvm") version "2.0.0"
}

application {
    mainClass = "io.github.nomisrev.AppKt"
}

group = "io.github.nomisrev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
}

dependencies {
    implementation("io.arrow-kt:arrow-autoclose:2.0.0-alpha.1")
    implementation("io.arrow-kt:arrow-core:2.0.0-alpha.1")
    implementation("io.arrow-kt:arrow-fx-coroutines:2.0.0-alpha.1")
    implementation("io.arrow-kt:arrow-fx-stm:2.0.0-alpha.1")
    implementation("io.arrow-kt:arrow-resilience:2.0.0-alpha.1")

    // Ktor Netty Server
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("ch.qos.logback:logback-classic:1.4.12")

    // PostgreSQL, Hikari, Container & Exposed ORM
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.testcontainers:postgresql:1.19.8")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")

}

tasks.test {
    useJUnitPlatform()
}
