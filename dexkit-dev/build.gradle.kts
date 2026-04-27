import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.cmake)
}

val dexkitSourceProject = project(":dexkit")
val isLinux = System.getProperty("os.name").lowercase().contains("linux")

base {
    archivesName.set("dexkit-dev")
}

cmake {
    targets {
        register("main") {
            cmakeLists.set(dexkitSourceProject.file("src/main/cpp/CMakeLists.txt"))
            targetMachines.add(machines.host)
            generator.set(generators.ninja)

            val configFlags = listOf(
                "-O3",
                "-DNDEBUG",
            ).joinToString(" ")

            cmakeArgs.add("-DCMAKE_BUILD_TYPE=Release")
            cmakeArgs.add("-DCMAKE_CXX_FLAGS_RELEASE=$configFlags")
            cmakeArgs.add("-DCMAKE_C_FLAGS_RELEASE=$configFlags")
            cmakeArgs.add("-DDEXKIT_ENABLE_INTERNAL_METRICS=ON")
            cmakeArgs.add("-DDEXKIT_ENABLE_INTERNAL_METRICS_API=ON")
            if (project.hasProperty("linuxMuslBuild") && isLinux) {
                cmakeArgs.add("-DUSE_MUSL_LIBC=ON")
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
    named("main") {
        java.setSrcDirs(
            listOf(
                "${dexkitSourceProject.projectDir}/src/main/java",
                "${dexkitSourceProject.projectDir}/src/internalMetrics/java",
            )
        )
    }
    named("test") {
        java.setSrcDirs(
            listOf(
                "${dexkitSourceProject.projectDir}/src/test/java",
                "src/test/java",
            )
        )
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.named<Test>("test") {
    systemProperty("java.library.path", layout.buildDirectory.dir("library").get().asFile.absolutePath)
    systemProperty("apk.path", layout.projectDirectory.dir("apk").asFile.absolutePath)
}

dependencies {
    implementation(libs.flatbuffers.java)
    testImplementation(libs.junit4)
}

val copyLibrary by tasks.registering(Copy::class) {
    dependsOn("cmakeBuild")
    from(layout.buildDirectory.dir("cmake"))
    into(layout.buildDirectory.dir("library"))
    include("**/*.so", "**/*.dll", "**/*.dylib")
    eachFile {
        path = name
    }
    includeEmptyDirs = false
}

tasks.named("jar") {
    dependsOn(copyLibrary)
}

val copyReleaseDemo by tasks.registering(Copy::class) {
    dependsOn(":demo:assembleRelease")
    from(project(":demo").layout.buildDirectory.dir("outputs/apk/release"))
    into(layout.projectDirectory.dir("apk"))
    include("**/*.apk")
    eachFile {
        path = name
    }
    rename { fileName ->
        fileName.replace(Regex(".*\\.apk"), "demo.apk")
    }
    includeEmptyDirs = false
}

tasks.named("testClasses") {
    dependsOn(copyReleaseDemo, copyLibrary)
}
