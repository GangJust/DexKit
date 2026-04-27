import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Lint-Registry-V2"] = "org.luckypray.dexkit.lint.DexKitIssueRegistry"
    }
}

dependencies {
    compileOnly(libs.kotlin.stdlib) {
        version {
            strictly(libs.versions.kotlin.stdlib.get())
        }
    }
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)
    testImplementation(libs.lint)
    testImplementation(libs.lint.tests)
}
