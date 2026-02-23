plugins {
    alias(libs.plugins.version.catalog.update)
}

versionCatalogUpdate {
    keep {
        keepUnusedVersions = true
    }
}
