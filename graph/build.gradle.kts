plugins {
    java
}

java.sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())

dependencies {
    implementation(libs.neo4j.driver)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    compileOnly(libs.jetbrains.annotations)
}
