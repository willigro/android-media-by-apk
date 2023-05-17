import Depends.Compose.implementCompose
import Depends.Core.implementCoreKtx
import Depends.Module.implementModules

android {
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.COMPOSE
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // =========== Core ==============
    implementCoreKtx()

    // =========== Compose ==============
    implementCompose()
}