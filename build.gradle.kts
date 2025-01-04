plugins {
    java
    idea
}

allprojects {
    group = "com.github.sulir.rutimesave"
    version = "snapshot"

    ext {
        set("neo4jVersion", "5.27.0")
        set("asmVersion", "9.7.1")
    }
}

subprojects {
    apply(plugin = "java")
    java.sourceCompatibility = JavaVersion.VERSION_17

    apply(plugin = "idea")
    idea {
        module {
            isDownloadSources = true
            isDownloadJavadoc = true
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(rootProject.file("LICENSE.txt")) {
            into("META-INF/")
        }
    }
}