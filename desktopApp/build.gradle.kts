import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

val jcefJvmArgs = listOf(
    "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
    "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
    "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
    "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED",
    "--enable-native-access=ALL-UNNAMED",
)

compose.desktop {
    application {
        mainClass = "com.crzto3.pantry.MainKt"
        jvmArgs += jcefJvmArgs

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Pantry"
            packageVersion = "1.0.0"
            includeAllModules = true

            macOS {
                bundleID = "com.crzto3.pantry"
            }
        }
    }
}
