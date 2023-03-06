import java.util.Base64

object Versions {
    val junit = "5.2.0"
    val strikt = "0.34.0"
    val commonscsv = "1.9.0"
    val poi = "5.2.3"
}

plugins {
    kotlin("jvm") version "1.6.21"
    id("maven")
    id("maven-publish")
    signing
    id("com.palantir.git-version") version "0.12.3"
}

apply<com.palantir.gradle.gitversion.GitVersionPlugin>()

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val lastTag: String = versionDetails().lastTag

version = if (lastTag != "") lastTag else "SNAPSHOT"
group = "io.github.4sh.retable"

repositories {
    mavenCentral()
}

tasks.getByPath("test").doFirst {
    with(this as Test) {
        useJUnitPlatform()
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("ExposedJar") {
            artifactId = "${rootProject.name}-${project.name}"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("Retable")
                description.set("Kotlin library to work with tabular data files")
                url.set("https://github.com/4sh/retable")

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("4SH")
                        name.set("4SH")
                        organization.set("4SH")
                        organizationUrl.set("https://www.4sh.fr")
                    }
                }

                scm {
                    url.set("https://github.com/4sh/retable")
                    connection.set("scm:git:git://github.com/4sh/retable.git")
                    developerConnection.set("scm:git:git@github.com:4sh/retable.git")
                }
            }
        }
    }
}

signing {
    val secretKey = Base64.getDecoder()
        .decode(System.getenv("SIGNING_KEY"))
        .toString(Charsets.UTF_8)
    val passphrase = System.getenv("SIGNING_KEY_PASSPHRASE")
    @Suppress("UnstableApiUsage")
    useInMemoryPgpKeys(secretKey, passphrase)
    sign(publishing.publications["ExposedJar"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.apache.commons:commons-csv:${Versions.commonscsv}")
    implementation("org.apache.poi:poi:${Versions.poi}")
    implementation("org.apache.poi:poi-ooxml:${Versions.poi}")

    testImplementation("io.strikt:strikt-core:${Versions.strikt}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.junit}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
}
