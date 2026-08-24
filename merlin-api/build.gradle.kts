plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

dependencies {
    implementation(libs.jackson.databind)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("ci.workflow", rootProject.file(".github/workflows/ci.yml").absolutePath)
    systemProperty("project.root", rootProject.projectDir.absolutePath)
    systemProperty(
        "ci.pom",
        layout.buildDirectory.file("publications/maven/pom-default.xml").get().asFile.absolutePath,
    )
}
