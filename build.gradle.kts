plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.gradle.cmake) apply false
    alias(libs.plugins.maven.publish) apply false
}

tasks.register<Delete>("clean") {
    group = "build"
    delete(rootProject.layout.buildDirectory)
}
