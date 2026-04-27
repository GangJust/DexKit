import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.cmake)
}

val isLinux = System.getProperty("os.name").lowercase().contains("linux")
val enableInternalMetricsApi = providers.gradleProperty("enableInternalMetricsApi")
    .map(String::toBoolean)
    .orElse(false)
    .get()

cmake {
    targets {
        register("main") {
            cmakeLists.set(file("src/main/cpp/CMakeLists.txt"))
            targetMachines.add(machines.host)
            generator.set(generators.ninja)

            val configFlags = listOf(
                "-O3",
                "-DNDEBUG",
            ).joinToString(" ")

            cmakeArgs.add("-DCMAKE_BUILD_TYPE=Release")
            cmakeArgs.add("-DCMAKE_CXX_FLAGS_RELEASE=$configFlags")
            cmakeArgs.add("-DCMAKE_C_FLAGS_RELEASE=$configFlags")
            cmakeArgs.add("-DDEXKIT_ENABLE_INTERNAL_METRICS=${if (enableInternalMetricsApi) "ON" else "OFF"}")
            cmakeArgs.add("-DDEXKIT_ENABLE_INTERNAL_METRICS_API=${if (enableInternalMetricsApi) "ON" else "OFF"}")
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
        java {
            if (enableInternalMetricsApi) {
                srcDir("src/internalMetrics/java")
            }
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.named("main").map { it.java.srcDirs })
    include("**/*.java", "**/*.kt")
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
