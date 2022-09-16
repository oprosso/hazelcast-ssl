group= "ru.oprosso"
version= "1.0.12-SNAPSHOT"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.bmuschko:gradle-nexus-plugin:2.3.1")
    }
}

plugins {
    id("java")
    id("maven-publish")
    id("signing")
}

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                packaging = "jar"
                name.set("Hazelcast-ssl")
                description.set("SSL plugin for Hazelcast IMDG community edition")
                url.set("https://github.com/oprosso/hazelcast-ssl")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("ashkart")
                        name.set("Maxim")
                        email.set("maxim@oprosso.ru")
                    }
                }

                scm {
                    connection.set("scm:https://github.com/oprosso/hazelcast-ssl.git")
                    developerConnection.set("scm:git@github.com:oprosso/hazelcast-ssl.git")
                    url.set("https://github.com/oprosso/hazelcast-ssl")
                }
            }
        }
    }
    repositories {
        maven {
            // change URLs to point to your repos, e.g. http://my.org/repo
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = project.findProperty("nexusUsername") as String?
                password = project.findProperty("nexusPassword") as String?
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}


java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly("com.hazelcast:hazelcast-all:4.2.2")
}

