import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.5.0-SNAPSHOT"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.4.30"
	kotlin("plugin.spring") version "1.4.30"
	kotlin("plugin.jpa") version "1.4.30"
	kotlin("plugin.serialization") version "1.4.30"
}

group = "io.adven27"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_15

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["springCloudVersion"] = "2020.0.1-SNAPSHOT"

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation(kotlin("reflect"))
	//region kotlin script
	implementation(kotlin("compiler-embeddable"))
	implementation(kotlin("scripting-compiler-embeddable"))
	implementation(kotlin("script-util"))
	implementation("net.java.dev.jna:jna:4.2.2")
	//endregion
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
	implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
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

tasks.withType<Jar> {
	manifest {
		attributes["Main-Class"] = "io.adven27.telegram.bots.ApplicationKt"
	}
}