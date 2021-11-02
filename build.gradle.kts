object Versions {
    val junit = "5.2.0"
    val strikt = "0.16.0"
    val commonscsv = "1.9.0"
    val poi = "3.17"
    val googleApiClient = "1.30.4"
    val googleOauthClientJetty = "1.30.6"
    val googleApiServicesSheets = "v4-rev581-1.25.0"
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
    compile("com.google.api-client:google-api-client:${Versions.googleApiClient}")
    compile("com.google.oauth-client:google-oauth-client-jetty:${Versions.googleOauthClientJetty}")
    compile("com.google.apis:google-api-services-sheets:${Versions.googleApiServicesSheets}")

    testCompile("io.strikt:strikt-core:${Versions.strikt}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.junit}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
}

