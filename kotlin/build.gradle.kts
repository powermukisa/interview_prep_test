import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("com.github.ben-manes.versions") version "0.51.0"
    idea
}

group = "com.rillet.coding-challenge"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_21


repositories {
    mavenCentral()
}

idea {
    module {
        isDownloadJavadoc = false
        isDownloadSources = true
    }
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.javamoney:moneta:1.4.4")
    implementation("org.zalando:jackson-datatype-money:1.3.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation(platform("io.strikt:strikt-bom:0.35.1"))
    testImplementation("io.strikt:strikt-jackson")
    testImplementation("io.strikt:strikt-mockk")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JVM_21)
    }
}
