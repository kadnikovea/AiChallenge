plugins {
    id("java")
    id("application")
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("org.example.MainKt")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Clikt for CLI
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("com.github.ajalt.mordant:mordant:2.7.0")

    implementation("io.ktor:ktor-client-logging:2.3.12")
    implementation("ch.qos.logback:logback-classic:1.4.11") // для вывода в консоль


    // Ktor Client for HTTP
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    
    // Gson for JSON persistence
    implementation("com.google.code.gson:gson:2.10.1")

    // SQLite JDBC driver
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.shadowJar {
    archiveBaseName.set("llm-cli-agent")
    archiveClassifier.set("")
    manifest {
        attributes("Main-Class" to "org.example.MainKt")
    }
}

tasks.test {
    useJUnitPlatform()
}