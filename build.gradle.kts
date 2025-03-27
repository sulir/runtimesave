plugins {
    java
}

allprojects {
    group = "com.github.sulir.rutimesave"
    version = "snapshot"
}

subprojects {
    apply(plugin = "java")
    java.sourceCompatibility = JavaVersion.VERSION_17

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:_")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:_")
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
