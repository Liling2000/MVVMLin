pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { setUrl("https://artifactory.appodeal.com/appodeal-public/") }
        maven { setUrl("https://repo.spring.io/libs-milestone/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://artifactory.appodeal.com/appodeal-public/") }
        maven { setUrl("https://repo.spring.io/libs-milestone/") }
        maven {
            setUrl("https://jcenter.bintray.com")
            metadataSources {
                mavenPom()
                artifact()
            }
        }
        maven {
            setUrl("https://jitpack.io")
        }
    }
}
rootProject.name = "MVVMLin"
include(":app", ":module_base", ":module_ble")
