plugins {
    alias(libs.plugins.version.catalog.update)
}

versionCatalogUpdate {
    keep {
        keepUnusedVersions = true
    }
}

repositories {
    maven("https://www.jetbrains.com/intellij-repository/releases")
}
