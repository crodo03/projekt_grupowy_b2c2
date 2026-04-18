plugins {
    id("java")
}

group = "com.ep07"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Source: https://mvnrepository.com/artifact/org.web3j/core
    implementation("org.web3j:core:5.0.2")
}

tasks.test {
    useJUnitPlatform()
}