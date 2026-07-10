plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.jiyi.power"
    defaultConfig {
        applicationId = libs.versions.applicationId.get()
        minSdk = libs.versions.minSdkVersion.get().toInt()
        targetSdk = libs.versions.targetSdkVersion.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                argument("AROUTER_MODULE_NAME", project.name)
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
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

    kapt {
        arguments {
            arg("AROUTER_MODULE_NAME", project.name)
        }
    }

    packaging {
        resources.excludes.add("META-INF/proguard/coroutines.pro")
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.bundles.androidX)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.arouterApi)
    kapt(libs.arouterCompiler)
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidTestJunit)
    androidTestImplementation(libs.espressoCore)
    implementation(project(":module_base"))

    //MVVMLin

    //第三方
    implementation(libs.banner)
    implementation(libs.bravh)
    implementation(libs.refreshKernel)
    implementation(libs.refreshHeader)
    implementation(libs.netCache)
    implementation(libs.bundles.netWork)
    implementation(libs.coil)
}
