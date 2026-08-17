plugins {
    base
    id("xyz.jpenilla.run-paper") version "2.3.1"
}
tasks {
    runServer {
        minecraftVersion("26.2")
        dependsOn(":paper:jar", gradle.includedBuild("MapGUI").task(":mapgui-plugin:shadowJar"))
        doFirst {
            delete(fileTree("run/plugins") {
                include("MapGUI-*.jar")
            })
        }
        pluginJars.from(
            project(":paper").tasks.named("jar"),
            layout.projectDirectory.file("../MapGUI/mapgui-plugin/build/libs/MapGUI-1.0.0-SNAPSHOT.jar")
        )
    }
}


allprojects {
    group = "dev.mintychochip"
    version = "1.0.0-SNAPSHOT"
}
