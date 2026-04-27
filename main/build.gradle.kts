import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "org.luckypray"
version = "1.0-SNAPSHOT"

val libraryDir = layout.buildDirectory.dir("library")
val libraryJvmArg = provider {
    "-Djava.library.path=${libraryDir.get().asFile}"
}

application {
    applicationDefaultJvmArgs += libraryJvmArg.map(::listOf).get()
    mainClass.set("MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":dexkit-dev"))
}

val cmakeBuild by tasks.registering(Copy::class) {
    group = "build"
    dependsOn(":dexkit-dev:cmakeBuild")
    from(project(":dexkit-dev").layout.buildDirectory.dir("cmake"))
    into(libraryDir)
    include("**/*.so", "**/*.dll", "**/*.dylib")
    eachFile {
        path = name
    }
    includeEmptyDirs = false
}

tasks.named("jar") {
    dependsOn(cmakeBuild)
}

tasks.named("run") {
    dependsOn("jar")
}
