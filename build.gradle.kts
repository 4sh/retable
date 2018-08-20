object Versions {
    val junit = "5.2.0"
    val strikt = "0.11.2"
    val commonscsv = "1.5"
}

plugins {
    kotlin("jvm") version "1.2.60"
}

repositories {
    jcenter()
}

tasks.getByPath("test").doFirst({
    with(this as Test) {
        useJUnitPlatform()
    }
})

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("org.apache.commons:commons-csv:${Versions.commonscsv}")

    testCompile("io.strikt:strikt-core:${Versions.strikt}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
}

