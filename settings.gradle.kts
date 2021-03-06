pluginManagement {
    repositories {
        mavenCentral()
        google()
        maven { setUrl("https://plugins.gradle.org/m2/") }
        //maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
        //maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
        //jcenter()
    }
}

buildCache {
    remote(HttpBuildCache::class.java) {
        url = uri("http://svn.youtec.dk:5071/cache/")
        isPush = System.getenv("CI") == "true"
        credentials {
            username = "youtec"
            password = "QapcErxW2pSa3pR"
        }
    }
}

enableFeaturePreview("GRADLE_METADATA")

include(":androidApp", ":tv-library", ":appupdater", ":drapi", ":logic")

rootProject.buildFileName = "build.gradle.kts"