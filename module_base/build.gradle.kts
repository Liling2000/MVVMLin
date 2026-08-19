import java.util.regex.Pattern.compile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.aleyn.mvvm"
    defaultConfig {
        minSdk = libs.versions.minSdkVersion.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
    kotlin {
        jvmToolchain(11)
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.getByName("release"))
                groupId = "com.aleyn"
                artifactId = "MVVMLin"
                version = "2.0.1"
            }
        }
    }
}


dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    //androidx
    implementation(libs.appcompat)
    implementation(libs.viewModelKtx)
    implementation(libs.recyclerview)
    implementation(libs.bundles.bleSDK)
    implementation(project(":module_ble"))
    // Compose Support
    api(platform(libs.compose.bom))
    api(libs.bundles.compose)
    implementation(libs.constraintlayout)
    debugApi(libs.compose.ui.tooling)

    //network
    compileOnly(libs.bundles.netWork)
    //material-dialogs
    api(libs.dialogs)
    api(libs.dialogsCore)
    // utils 集合了大量常用的工具类
    api(libs.utilCode)
    api(libs.mmkv)
    api(libs.lottie)
    api(libs.xpopup)

    api("com.flyco.tablayout:FlycoTabLayout_Lib:2.1.2@aar")
    api("com.flyco.roundview:FlycoRoundView_Lib:1.1.4@aar")


}
