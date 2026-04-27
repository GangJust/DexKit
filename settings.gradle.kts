pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://api.xposed.info/")
        maven(url = "https://jitpack.io")
    }
}

include(":dexkit")
include(":dexkit-dev")
include(":dexkit-android")
include(":demo")
include(":main")
include(":lint-rules")
