import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Locale

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    signing
}

val kDocRepoName = "LuckyPray/DexKit-Doc"
val libVersion = "2.2.0"
val enableInternalMetricsApi = false

android {
    namespace = "org.luckypray.dexkit"
    compileSdk = 34
    ndkVersion = "26.1.10909125"

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=none",
                    "-DDEXKIT_ENABLE_INTERNAL_METRICS=${if (enableInternalMetricsApi) "ON" else "OFF"}",
                    "-DDEXKIT_ENABLE_INTERNAL_METRICS_API=${if (enableInternalMetricsApi) "ON" else "OFF"}",
                )
                val flags = listOf(
                    "-funwind-tables",
                    "-fasynchronous-unwind-tables",
                    "-Qunused-arguments",
                    "-fno-rtti",
                    "-fno-exceptions",
                    "-fvisibility=hidden",
                    "-fvisibility-inlines-hidden",
                    "-Wno-unused-value",
                    "-Wno-unused-variable",
                    "-Wno-unused-command-line-argument",
                )
                cppFlags += listOf("-std=c++20") + flags
                cFlags += listOf("-std=c18") + flags
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            externalNativeBuild {
                cmake {
                    val releaseFlags = listOf(
                        "-ffunction-sections",
                        "-fdata-sections",
                        "-Wl,--gc-sections",
                        "-Wl,--exclude-libs,ALL",
                        "-Wl,--strip-all",
                        "-Wl,-z,max-page-size=16384",
                        "-flto=full",
                    )
                    val configFlags = listOf(
                        "-Oz",
                        "-DNDEBUG",
                    ).joinToString(" ")
                    cppFlags += releaseFlags
                    cFlags += releaseFlags
                    arguments += listOf(
                        "-DCMAKE_BUILD_TYPE=Release",
                        "-DCMAKE_CXX_FLAGS_RELEASE=$configFlags",
                        "-DCMAKE_C_FLAGS_RELEASE=$configFlags",
                    )
                }
            }
            consumerProguardFiles("proguard-rules.pro")
        }

        debug {
            externalNativeBuild {
                cmake {
                    arguments += listOf(
                        "-DCMAKE_CXX_FLAGS_DEBUG=-Og",
                        "-DCMAKE_C_FLAGS_DEBUG=-Og",
                    )
                }
            }
        }
    }

    sourceSets {
        named("main") {
            manifest.srcFile("AndroidManifest.xml")
            val mainSourceDir = project(":dexkit").file("src/main/java").invariantSeparatorsPath
            java.directories.add(mainSourceDir)
            kotlin.directories.add(mainSourceDir)
            if (enableInternalMetricsApi) {
                val internalMetricsDir = project(":dexkit").file("src/internalMetrics/java").invariantSeparatorsPath
                java.directories.add(internalMetricsDir)
                kotlin.directories.add(internalMetricsDir)
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }

    buildFeatures {
        prefab = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.flatbuffers.java)
    implementation(libs.ndk.cxx)
    implementation(libs.kotlin.stdlib) {
        version {
            require(libs.versions.kotlin.stdlib.get())
        }
    }
    lintPublish(project(":lint-rules"))
}

mavenPublishing {
    coordinates("org.luckypray", "dexkit", libVersion)
    pom {
        name.set("dexkit")
        description.set("A high-performance runtime parsing library for dex implemented in C++.")
        url.set("https://github.com/LuckyPray/DexKit")
        licenses {
            license {
                name.set("GNU Lesser General Public License v3.0")
                url.set("https://www.gnu.org/licenses/lgpl-3.0.html")
                distribution.set("repo")
            }
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                name.set("luckypray")
                url.set("https://luckypray.org/DexKit")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/LuckyPray/DexKit.git")
            developerConnection.set("scm:git:ssh://github.com/LuckyPray/DexKit.git")
            url.set("https://github.com/LuckyPray/DexKit.git")
        }
    }
    publishToMavenCentral(true)
    signAllPublications()
}

extensions.configure<DokkaExtension>("dokka") {
    dokkaSourceSets.configureEach {
        enableAndroidDocumentationLink.set(false)
        enableKotlinStdLibDocumentationLink.set(false)
        enableJdkDocumentationLink.set(false)
    }
}

tasks.register("updateKDoc") {
    doLast {
        val dokkaHtmlDir = layout.buildDirectory.dir("dokka/html").get().asFile
        if (dokkaHtmlDir.exists()) {
            println(dokkaHtmlDir.absolutePath)

            val deployRepo = "https://username:${System.getenv("ACCESS_TOKEN")}@github.com/$kDocRepoName.git"
            val userName = System.getenv("GITHUB_ACTOR")
            val userEmail = "$userName@users.noreply.github.com"
            val gitCommand = buildString {
                append("git init -b master ")
                append("&& git config user.name $userName ")
                append("&& git config user.email $userEmail ")
                append("&& git add . ")
                append("&& git commit -m 'Auto deploy KDoc' ")
                append("&& git push -f $deployRepo master:main")
            }

            val execOutput = providers.exec {
                workingDir = dokkaHtmlDir
                if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) {
                    commandLine("cmd", "/c", gitCommand)
                } else {
                    commandLine("sh", "-c", gitCommand)
                }
            }
            execOutput.result.get().assertNormalExitValue()
            println(gitCommand)
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    val dexkitAndroidSourceDirs = mutableListOf(
        project(":dexkit").file("src/main/java"),
    )
    if (enableInternalMetricsApi) {
        dexkitAndroidSourceDirs += project(":dexkit").file("src/internalMetrics/java")
    }

    archiveBaseName.set("dexkit-android")
    archiveClassifier.set("sources")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        files(dexkitAndroidSourceDirs).asFileTree.matching {
            include("**/*.java", "**/*.kt")
        }
    )
}
