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
    testImplementation("org.mockito:mockito-core:5.+")
    implementation("io.javalin:javalin:7.1.0")
    testImplementation("io.javalin:javalin-testtools:7.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.test {
    useJUnitPlatform()
}