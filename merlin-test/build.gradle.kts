plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

dependencies {
    implementation(project(":merlin-api"))
    implementation(project(":merlin-common"))
    implementation(project(":merlin-paper"))
    compileOnly("io.github.flog99:mapgui-api:1.0.0")
    compileOnly("io.github.flog99:mapgui-layout:1.0.0")
    compileOnly("io.papermc.paper:paper-api:26.2.build.84-stable")
    compileOnly("com.microsoft.onnxruntime:onnxruntime:1.29.0")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

tasks {
    runServer {
        runDirectory.set(rootProject.layout.projectDirectory.dir("run"))
        minecraftVersion("26.2")
        dependsOn(
            project.tasks.jar,
            project(":merlin-paper").tasks.named("jar"),
            gradle.includedBuild("MapGUI").task(":mapgui-plugin:shadowJar")
        )
        pluginJars.from(
            project.tasks.jar,
            project(":merlin-paper").tasks.named("jar"),
            rootProject.layout.projectDirectory.file("../MapGUI/mapgui-plugin/build/libs/MapGUI-1.0.0-SNAPSHOT.jar")
        )
        doFirst {
            delete(fileTree(rootProject.layout.projectDirectory.dir("run/plugins")) {
                include("MapGUI-*.jar")
            })
        }
    }
}
