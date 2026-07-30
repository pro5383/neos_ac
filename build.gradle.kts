plugins {
    java
}

group = "net.neos"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://repo.codemc.io/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.5.0")
    compileOnly("org.jetbrains:annotations:24.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val version = project.version
    inputs.property("version", version)
    filesMatching(listOf("plugin.yml", "config.yml", "messages.yml")) {
        expand("version" to version)
    }
}

tasks.jar {
    archiveBaseName.set("NeosAC")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
