plugins {
    java
}

dependencies {
    implementation(project(":merlin-api"))
    implementation(project(":merlin-common"))
    compileOnly("io.github.flog99:mapgui-api:1.0.0")
    compileOnly("io.github.flog99:mapgui-layout:1.0.0")
    compileOnly("io.papermc.paper:paper-api:26.2.build.84-stable")
    compileOnly("com.microsoft.onnxruntime:onnxruntime:1.29.0")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("io.github.flog99:mapgui-api:1.0.0")
    testImplementation("io.papermc.paper:paper-api:26.2.build.84-stable")
    testImplementation("com.microsoft.onnxruntime:onnxruntime:1.29.0")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
