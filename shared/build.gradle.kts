import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val clientDir = rootProject.layout.projectDirectory.dir("client")
val webResourcesDir = layout.buildDirectory.dir("generated/webClient")
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val npm = if (isWindows) "npm.cmd" else "npm"
val skipWebBuild = providers.gradleProperty("pantry.skipWebBuild").map { it.toBoolean() }.orElse(false)

val npmInstall by tasks.registering(Exec::class) {
    group = "pantry"
    description = "Устанавливает npm-зависимости клиента, если их ещё нет"
    workingDir(clientDir)
    commandLine(npm, "ci")
    inputs.file(clientDir.file("package-lock.json"))
    outputs.dir(clientDir.dir("node_modules"))
    val skip = skipWebBuild
    onlyIf { !skip.get() }
}

val buildWebClient by tasks.registering(Exec::class) {
    group = "pantry"
    description = "Собирает React-клиент и кладёт его в ресурсы приложения"
    dependsOn(npmInstall)
    workingDir(clientDir)
    val outDir = webResourcesDir.map { it.dir("files/web") }
    commandLine(npm, "run", "build", "--", "--outDir", outDir.get().asFile.absolutePath, "--emptyOutDir")
    inputs.dir(clientDir.dir("src"))
    inputs.dir(clientDir.dir("public"))
    inputs.files(
        clientDir.file("index.html"),
        clientDir.file("package.json"),
        clientDir.file("package-lock.json"),
        clientDir.file("vite.config.ts"),
        clientDir.file("tsconfig.json"),
        clientDir.file("tsconfig.app.json"),
        clientDir.file("tsconfig.node.json"),
    )
    inputs.files(clientDir.asFileTree.matching { include(".env", ".env.*") })
    outputs.dir(webResourcesDir)
    val skip = skipWebBuild
    onlyIf { !skip.get() }
}

val backendUrl = providers.gradleProperty("pantry.backendUrl").orElse("")
val telegramDomain = providers.gradleProperty("pantry.telegramDomain").orElse("hack.crzto3.ru")
val buildConfigDir = layout.buildDirectory.dir("generated/pantryConfig/kotlin")

val generateBuildConfig by tasks.registering {
    group = "pantry"
    val url = backendUrl
    val domain = telegramDomain
    val outDir = buildConfigDir
    inputs.property("backendUrl", url)
    inputs.property("telegramDomain", domain)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("com/crzto3/pantry/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.crzto3.pantry
            |
            |internal object BuildConfig {
            |    const val BACKEND_URL: String = "${url.get().replace("\"", "\\\"")}"
            |    const val TELEGRAM_DOMAIN: String = "${domain.get().replace("\"", "\\\"")}"
            |}
            |""".trimMargin()
        )
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    android {
       namespace = "com.crzto3.pantry.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.jcefmaven)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        commonMain {
            kotlin.srcDir(generateBuildConfig.map { buildConfigDir.get() })
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.client.core)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    packageOfResClass = "com.crzto3.pantry.resources"
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = buildWebClient.map { webResourcesDir.get() },
    )
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
