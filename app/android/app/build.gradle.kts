import java.util.Properties
import java.io.FileInputStream
import java.util.Base64

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}

val dartDefines = mutableMapOf<String, String>()
if (project.hasProperty("dart-defines")) {
    val dartDefinesProperty = project.property("dart-defines") as String
    dartDefinesProperty
        .split(",")
        .forEach { entry ->
            val decodedEntry = String(Base64.getDecoder().decode(entry), Charsets.UTF_8)
            val pair = decodedEntry.split("=")
            // valueがemptyの時にlastがkeyになるので、lengthが2でなければ空のmapにする
            if (pair.size == 2) {
                dartDefines[pair[0]] = pair[1]
            }
        }
}

val copySources by tasks.registering(Copy::class) {
    from("src/${dartDefines["flavor"]}/res")
    into("src/main/res")
}

tasks.whenTaskAdded {
    dependsOn(copySources)
}


val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
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
        getByName("main").java.srcDirs("src/main/kotlin")
    }

    defaultConfig {
        applicationId = "net.yumnumm.eqmonitor"
        dartDefines["appIdSuffix"]?.let {
            applicationIdSuffix = it
        }
        minSdkVersion(26)
        targetSdkVersion(35)
        multiDexEnabled = true
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        resValue("string", "app_name", dartDefines["appName"] ?: "EQMonitor")
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = keystoreProperties["storeFile"]?.let { file(it as String) }
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            multiDexEnabled = true
            resValue("string", "app_name", "EQMonitor")
        }
        debug {
            versionNameSuffix = ".d"
            resValue("string", "app_name", "EQMonitor (Debug)")
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
    implementation("com.google.firebase:firebase-crashlytics:19.4.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation("androidx.core:core-splashscreen:1.0.1")
}
