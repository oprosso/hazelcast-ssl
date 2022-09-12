group= "ru.oprosso"
version= "1.0.5"

plugins {
    id("java")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly("com.hazelcast:hazelcast-all:4.2.2")
}

