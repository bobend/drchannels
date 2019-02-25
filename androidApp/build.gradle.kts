import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-kapt")
    id("kotlin-android-extensions")
    id("com.github.triplet.play")
    id("org.sonarqube") version "2.6.2"
    id("dk.youtec.appupdater")
    id("com.github.plnice.canidropjetifier") version "0.4" // ./gradlew -Pandroid.enableJetifier=false canIDropJetifier
}

android {
    compileSdkVersion(compileSdk)
    buildToolsVersion(buildToolsVersion)

    flavorDimensions("app")

    if (project.hasProperty("devBuild")) {
        aaptOptions.cruncherEnabled = false
    }

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(targetSdk)

        applicationId = "dk.youtec.drchannels"
        versionCode = versionCodeTimestamp
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        vectorDrawables.useSupportLibrary = true

        resConfigs("en", "da")

        signingConfigs {
            create("release")
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = true
                isShrinkResources = true
                isDebuggable = false
                proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

                signingConfig = signingConfigs["release"]
            }
        }

        productFlavors {
            create("mobile") {
                applicationIdSuffix = ".mobile"
            }
            create("tv") {
                applicationIdSuffix = ".tv"
            }
        }

        lintOptions {
            isCheckReleaseBuilds = false
            isCheckDependencies = true
            isAbortOnError = false
        }
    }

    compileOptions {
        setSourceCompatibility("1.8")
        setTargetCompatibility("1.8")
    }

    packagingOptions {
        exclude("META-INF/ktor-http.kotlin_module")
        exclude("META-INF/kotlinx-io.kotlin_module")
        exclude("META-INF/atomicfu.kotlin_module")
        exclude("META-INF/ktor-utils.kotlin_module")
        exclude("META-INF/kotlinx-coroutines-io.kotlin_module")
        exclude("META-INF/ktor-client-core.kotlin_module")
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

kapt {
    useBuildCache = true
}

dependencies {
    implementation(project(":drapi"))
    implementation(project(":tv-library"))
    implementation(project(":appupdater"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    implementation("com.google.android.exoplayer:exoplayer-core:$exoPlayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-hls:$exoPlayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-ui:$exoPlayerVersion")

    implementation("org.jetbrains.anko:anko-sdk21:$ankoVersion")
    implementation("com.squareup.picasso:picasso:$picassoVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.google.android.material:material:$androidxVersion")
    implementation("androidx.legacy:legacy-support-v4:$androidxVersion")
    implementation("androidx.recyclerview:recyclerview:$androidxVersion")
    implementation("androidx.tvprovider:tvprovider:$androidxVersion")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-alpha3")
    implementation("androidx.core:core-ktx:1.0.1")
    implementation("org.koin:koin-android:2.0.0-beta-1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$archComponentVersion")
    implementation("androidx.lifecycle:lifecycle-extensions:$archComponentVersion")
    kapt("androidx.lifecycle:lifecycle-compiler:$archComponentVersion")
    implementation("android.arch.work:work-runtime-ktx:1.0.0-rc02")

    androidTestImplementation("androidx.test:core:1.1.0")
    androidTestImplementation("androidx.test:runner:1.1.1")
    androidTestImplementation("androidx.test:rules:1.1.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.0")
    androidTestUtil("androidx.test:orchestrator:1.1.1")
}

play {
    jsonFile = project.file("youtec.json")

    setTrack("alpha") // 'production', 'beta' or 'alpha'
}

val releasePropertiesFile: File = rootProject.file("release.properties")
if (releasePropertiesFile.exists()) {
    val props = Properties().apply { load(releasePropertiesFile.inputStream()) }

    with(android.signingConfigs["release"]) {
        storeFile = rootProject.file(System.getProperty("user.home") + props.getProperty("keyStore"))
        storePassword = props.getProperty("keyStorePassword")
        keyAlias = props.getProperty("keyAlias")
        keyPassword = props.getProperty("keyAliasPassword")
    }
}
