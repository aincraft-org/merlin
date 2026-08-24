import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    base
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
                    artifactId = project.name
                }
            }
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/aincraft-org/merlin")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR") ?: ""
                        password = System.getenv("GITHUB_TOKEN") ?: ""
                    }
                }
            }
        }
    }
}
