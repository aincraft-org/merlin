import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

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
    version = providers.gradleProperty("releaseVersion").orElse("1.0.0-SNAPSHOT").get()
}

subprojects {
    plugins.withId("java") {
        apply(plugin = "maven-publish")
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    artifactId = "wizardry-${project.name}"
                }
            }
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/aincraft-org/wizardry")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR") ?: ""
                        password = System.getenv("GITHUB_TOKEN") ?: ""
                    }
                }
            }
        }
    }
}
