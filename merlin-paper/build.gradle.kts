plugins {
    java
}

dependencies {
    implementation(project(":merlin-api"))
    implementation(project(":merlin-common"))
    compileOnly(libs.mapgui.api)
    compileOnly(libs.mapgui.layout)
    compileOnly(libs.paper.api)
    compileOnly(libs.onnxruntime)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.mapgui.api)
    testImplementation(libs.paper.api)
    testImplementation(libs.onnxruntime)
    testImplementation(libs.mockito.core)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.jar)
}

tasks.jar {
    from(project(":merlin-api").sourceSets.main.get().output)
    from(project(":merlin-common").sourceSets.main.get().output)
}
