pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

includeBuild("../MapGUI") {
    dependencySubstitution {
        substitute(module("io.github.flog99:mapgui-api")).using(project(":mapgui-api"))
        substitute(module("io.github.flog99:mapgui-layout")).using(project(":mapgui-layout"))
    }
}
rootProject.name = "merlin"
include(":merlin-api")
include(":merlin-common")
include(":merlin-paper")
include(":merlin-test")