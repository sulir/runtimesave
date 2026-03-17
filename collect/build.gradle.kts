plugins {
    base
}

val buildPath = layout.projectDirectory.dir("build")

tasks.register<Exec>("configureNativeAgent") {
    val cmakeCache = buildPath.file("CMakeCache.txt").asFile
    onlyIf { !cmakeCache.exists() }
    commandLine("cmake", "-S", layout.projectDirectory, "-B", buildPath)
}

tasks.register<Exec>("buildNativeAgent") {
    dependsOn("configureNativeAgent")
    commandLine("cmake", "--build", buildPath)
}

tasks.register<Exec>("cleanNativeAgent") {
    val buildDir = buildPath.asFile
    onlyIf { buildDir.exists() }
    commandLine("cmake", "--build", buildPath, "--target", "clean")
    finalizedBy("deleteNativeBuild")
}

tasks.register<Delete>("deleteNativeBuild") {
    delete(buildPath)
}

tasks.assemble {
    dependsOn("buildNativeAgent")
}

tasks.clean {
    dependsOn("cleanNativeAgent")
}
