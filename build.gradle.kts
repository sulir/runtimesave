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

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(rootProject.file("LICENSE.txt")) {
            into("META-INF/")
        }
    }
}