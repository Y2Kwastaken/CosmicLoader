import java.lang.IllegalStateException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists
import kotlin.io.path.relativeTo

plugins {
    id("java-library")
}

val loaderVersion = project.properties["loader_version"] as String
val mixinVersion = project.properties["mixin_version"] as String
val mixinExtraVersion = project.properties["mixin_extras_version"] as String
val asmVersion = project.properties["asm_version"] as String

group = "sh.miles.cosmicloader"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal() // requires for cosmic reach
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://maven.fabricmc.net/")
}

dependencies {
    api("com.google.guava:guava:21.0")
    api("com.google.code.gson:gson:2.8.7")

    // Fabric dependencies
    api("net.fabricmc:fabric-loader:0.15.7")

    // Mixin dependencies
    api("org.ow2.asm:asm:$asmVersion")
    api("org.ow2.asm:asm-analysis:$asmVersion")
    api("org.ow2.asm:asm-commons:$asmVersion")
    api("org.ow2.asm:asm-tree:$asmVersion")
    api("org.ow2.asm:asm-util:$asmVersion")
    api("net.fabricmc:sponge-mixin:$mixinVersion") {
        exclude("launchwrapper")
        exclude("com.google.guava")
        exclude("com.google.code.gson")
    }
    api(annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtraVersion")!!)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.jar {
    manifest {
        attributes["Class-Path"] = configurations
            .compileClasspath
            .get()
            .joinToString(separator = " ") { file ->
                "libs/${file.name}"
            }
        attributes["Specification-Version"] = "8.0"
        attributes["Multi-Release"] = "true"
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Copy>("copyDependencies") {
    group = "build"
    from(configurations.runtimeClasspath)
    into("build/libs/libs")
}

tasks.register("liberate") {
    group = "build"
    dependsOn(tasks.jar)
    dependsOn(tasks.getByPath("copyDependencies"))
}
