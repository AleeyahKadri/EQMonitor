import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}

val dartDefines =
    mutableMapOf<String, String>()
if (project.hasProperty("dart-defines")) {
    project.property("dart-defines")
        .toString()
        .split(",")
        .forEach { entry ->
            val decoded =
                String(
                    Base64.getDecoder()
                        .decode(entry),
                    Charsets.UTF_8,
                )
            val pair = decoded.split("=", limit = 2)
            if (pair.size == 2) {
                dartDefines[pair.first()] = pair.last()
            }
        }
}

tasks.register<Copy>("copySources") {
    from("src/${dartDefines["flavor"]}/res")
    into("src/main/res")
}

tasks.configureEach {
    if (name != "copySources") {
        dependsOn("copySources")
    }
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile
        .inputStream()
        .use { keystoreProperties.load(it) }
}

android {
    namespace = "net.yumnumm.eqmonitor"
    buildToolsVersion = "34.0.0"
    compileSdk = 35

    compileOptions {
        // flutter_local_notificationsで利用
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }

    defaultConfig {
        applicationId = "net.yumnumm.eqmonitor"
        dartDefines["appIdSuffix"]?.let {
            applicationIdSuffix = it
        }
        minSdk = 26
        targetSdk = 35
        multiDexEnabled = true
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        resValue(
            "string",
            "app_name",
            dartDefines["appName"].toString(),
        )
    }

    signingConfigs {
        create("release") {
            keyAlias =
                keystoreProperties["keyAlias"] as String?
            keyPassword =
                keystoreProperties["keyPassword"] as String?
            storeFile =
                (keystoreProperties["storeFile"] as String?)
                    ?.let { file(it) }
            storePassword =
                keystoreProperties["storePassword"] as String?
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            multiDexEnabled = true
            resValue("string", "app_name", "EQMonitor")
        }
        getByName("debug") {
            versionNameSuffix = ".d"
            resValue(
                "string",
                "app_name",
                "EQMonitor (Debug)",
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

flutter {
    source = "../.."
}

dependencies {
    implementation(
        "com.google.firebase:firebase-crashlytics:19.4.0",
    )
    coreLibraryDesugaring(
        "com.android.tools:desugar_jdk_libs:2.1.4",
    )
    implementation("androidx.core:core-splashscreen:1.0.1")
}
