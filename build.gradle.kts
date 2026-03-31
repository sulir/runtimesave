plugins {
    alias(libs.plugins.version.catalog.update)
}

versionCatalogUpdate {
    keep {
        keepUnusedVersions = true
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://www.jetbrains.com/intellij-repository/releases")
}
