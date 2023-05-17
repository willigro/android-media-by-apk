import com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        mavenLocal()
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath(Depends.Gradle.getGradlePlugin())
        classpath(Depends.Gradle.getKotlinPlugin())
        classpath(Depends.Plugins.CLASS_PATH_HILT)
    }
}

plugins {
    id("com.google.dagger.hilt.android") version Versions.HILT_PLUGIN apply false
}

allprojects {
    repositories {
        mavenLocal()
        google()
        jcenter()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    androidCompile()

}

fun Project.androidCompile() {
    if (Modules.modules.map {
            val a = it.split(":")
            val b = a[a.size - 1]
            b
        }.contains(name).not()) return

    val isApplication = if (":$name" == Modules.app) {
        println("Building: application - $name")
        apply(plugin = "com.android.application")
        true
    } else {
        println("Building: library - $name")
        apply(plugin = "com.android.library")
        false
    }

    apply(plugin = "kotlin-android")
    apply(plugin = "kotlin-kapt")

    configure<BaseExtension> {
        compileSdkVersion(Versions.COMPILE_SKD_VERSION)
        buildToolsVersion(Versions.BUILD_TOOLS_VERSION)

        defaultConfig {
            if (isApplication)
                applicationId = Versions.APPLICATION_ID

            minSdk = Versions.MIN_SDK_VERSION
            targetSdk = Versions.TARGET_SDK_VERION
            versionCode = Versions.VERSION_CODE
            versionName = Versions.VERSION_NAME

            testInstrumentationRunner = Depends.Test.ANDROID_JUNIT_RUNNER
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = true
//                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
            getByName("debug") {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
        }

        sourceSets {
            getByName("androidTest").java.srcDirs("src/androidTest/java")
        }

        packagingOptions {
            resources.excludes.add("META-INF/DEPENDENCIES")
            resources.excludes.add("META-INF/LICENSE")
            resources.excludes.add("META-INF/LICENSE.txt")
            resources.excludes.add("META-INF/license.txt")
            resources.excludes.add("META-INF/NOTICE")
            resources.excludes.add("META-INF/NOTICE.txt")
            resources.excludes.add("META-INF/notice.txt")
            resources.excludes.add("META-INF/ASL2.0")
            resources.excludes.add("META-INF/*")
            resources.excludes.add("META-INF/*.kotlin_module")
            resources.excludes.add("/META-INF/AL2.0")
            resources.excludes.add("/META-INF/LGPL2.1")
            resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}