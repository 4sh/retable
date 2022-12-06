object Versions {
    val junit = "5.2.0"
    val strikt = "0.16.0"
    val commonscsv = "1.9.0"
    val poi = "5.2.3"
}


plugins {
    kotlin("jvm") version "1.6.21"
    id("maven")
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
    compile(kotlin("reflect"))
    compile("org.apache.commons:commons-csv:${Versions.commonscsv}")
    compile("org.apache.poi:poi:${Versions.poi}")
    compile("org.apache.poi:poi-ooxml:${Versions.poi}")

    testCompile("io.strikt:strikt-core:${Versions.strikt}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.junit}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
}

