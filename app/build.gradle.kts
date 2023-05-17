import Depends.Compose.implementCompose
import Depends.Hilt.implementHilt
import Depends.Core.implementCoreKtx
import Depends.Module.implementAllModules
import Depends.ViewModel.implementViewModel

plugins {
    id(Depends.Plugins.HILT)
}

android {
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.COMPOSE
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // =========== Modules ==============
    implementAllModules(Modules.app)

    // =========== Core ==============
    implementCoreKtx()

    // =========== Hilt ==============
    implementHilt()

    // =========== Compose ==============
    implementCompose()

    // =========== ViewModel ==============
    implementViewModel()
}