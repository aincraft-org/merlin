plugins {
    java
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":merlin-api"))
    implementation(project(":merlin-common"))
    implementation(project(":merlin-paper"))
    compileOnly(libs.mapgui.api)
    compileOnly(libs.paper.api)
    compileOnly(libs.onnxruntime)
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

val mapGuiPluginJar = layout.buildDirectory.file("mapgui/MapGUI-${libs.versions.mapgui.get()}.jar")
val fetchMapGuiPlugin = tasks.register("fetchMapGuiPlugin") {
    val dest = mapGuiPluginJar
    val version = libs.versions.mapgui.get()
    outputs.file(dest)
    doLast {
        val file = dest.get().asFile
        if (file.exists() && file.length() > 0L) return@doLast
        file.parentFile.mkdirs()
        uri("https://github.com/FloG99/MapGUI/releases/download/v$version/MapGUI-$version.jar")
            .toURL()
            .openStream()
            .use { input -> file.outputStream().use { input.copyTo(it) } }
    }
}

tasks {
    runServer {
        runDirectory.set(rootProject.layout.projectDirectory.dir("run"))
        minecraftVersion("26.2")
        dependsOn(
            project.tasks.jar,
            project(":merlin-paper").tasks.named("jar"),
            fetchMapGuiPlugin
        )
        pluginJars.from(
            project.tasks.jar,
            project(":merlin-paper").tasks.named("jar"),
            mapGuiPluginJar
        )
        doFirst {
            delete(fileTree(rootProject.layout.projectDirectory.dir("run/plugins")) {
                include("MapGUI-*.jar")
            })
        }
    }
}
