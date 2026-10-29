pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "accessai-android-sdk"

include(":accessai-core")

// The Android modules need an Android SDK. GitHub Actions and JitPack provide one; on a machine
// without it, build and test the pure-Kotlin core with -PcoreOnly=true.
if (providers.gradleProperty("coreOnly").orNull != "true") {
    include(":accessai-android")
    include(":accessai-compose")
    include(":sample")
}
