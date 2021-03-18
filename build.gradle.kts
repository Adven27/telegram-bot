import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.springframework.boot") version "2.5.0-SNAPSHOT"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.spring") version "1.4.30"
    kotlin("plugin.jpa") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "io.adven27"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_15

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["springCloudVersion"] = "2020.0.1-SNAPSHOT"

dependencies {
//    implementation(project(":scripting"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    //region scripting
    implementation(kotlin("compiler-embeddable"))
    implementation(kotlin("scripting-compiler-embeddable"))
    implementation(kotlin("script-util"))
    implementation(kotlin("script-runtime"))
    implementation("net.java.dev.jna:jna:4.2.2")
    //endregion
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.github.microutils:kotlin-logging:1.12.0")

    implementation("com.jayway.jsonpath:json-path:2.5.0")

    implementation("com.github.pengrad:java-telegram-bot-api:5.0.1")

    //region DB
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.testcontainers:postgresql:1.15.2")
    implementation("org.liquibase:liquibase-core")
    implementation("com.vladmihalcea:hibernate-types-52:2.10.3")
    //endregion

    //TODO clean up
    implementation("com.jcabi:jcabi-http:1.16")
    implementation("org.glassfish:javax.json:1.0.4")
    implementation("javax.json:javax.json-api:1.0")
    implementation("org.json:json:20160212")
    implementation("org.apache.httpcomponents:httpclient:4.5.2")

    implementation("com.rometools:rome:1.15.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.assertj:assertj-core:3.16.1")
    testImplementation("com.github.springtestdbunit:spring-test-dbunit:1.3.0")
    testImplementation("org.dbunit:dbunit:2.7.0")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "15"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

//https://discuss.kotlinlang.org/t/kotlin-compiler-embeddable-exception-on-kotlin-script-evaluation/6547/6
//https://discuss.kotlinlang.org/t/trying-to-use-kotlin-via-javax-script-jsr-223-in-a-webapp/8982
//https://github.com/spring-projects/spring-boot/issues/1828
tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("bots")
        isZip64 = true
        mergeServiceFiles()
        append("META-INF/spring.handlers")
        append("META-INF/spring.schemas")
        append("META-INF/spring.tooling")
        transform(PropertiesFileTransformer().apply {
            paths = listOf("META-INF/spring.factories")
            mergeStrategy = "append"
        })

        manifest { attributes(mapOf("Main-Class" to "io.adven27.telegram.bots.ApplicationKt")) }
    }
}

tasks {
    build { dependsOn(shadowJar) }
}