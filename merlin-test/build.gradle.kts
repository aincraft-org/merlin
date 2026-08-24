plugins {
    java
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":merlin-api"))
    implementation(project(":merlin-common"))
    implementation(project(":merlin-paper"))
    compileOnly(libs.mapgui.api)
    compileOnly(libs.mapgui.layout)
    compileOnly(libs.paper.api)
    compileOnly(libs.onnxruntime)
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
