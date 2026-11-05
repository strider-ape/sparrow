import java.net.URI
import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.agp.application)
    alias(libs.plugins.kotlin.android)
}

// Poweramp's public resources, used by aapt2 as an include so the skin can reference
// com.maxmpz.audioplayer:style/... and attrs. Downloaded from the official SDK repo at a
// pinned commit and checked by SHA-256, so the SDK itself never lives in this repository.
val powerampResourcesUrl =
    "https://raw.githubusercontent.com/maxmpz/powerampapi/38a42f3275c577c973dc7991bd5bbd62ec6d68dd/Poweramp-build-1035-resources.zip"
val powerampResourcesSha256 = "8b9ba0d774d4f644dedd822d862be0fb38ad5868aec93605ec145449afb37c7c"

fun sha256(file: File): String =
    MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

fun powerampResources(): File {
    val localProps = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { localProps.load(it) }
    // A full Poweramp APK can be used instead of the bundled resources: poweramp.apk=/path/to/Poweramp.apk
    localProps.getProperty("poweramp.apk")?.let { return file(it) }

    val zip = rootProject.layout.projectDirectory.file(".poweramp/Poweramp-build-1035-resources.zip").asFile
    if (!zip.exists() || sha256(zip) != powerampResourcesSha256) {
        logger.lifecycle("Downloading Poweramp resources from the official SDK repository")
        zip.parentFile.mkdirs()
        URI(powerampResourcesUrl).toURL().openStream().use { input -> zip.outputStream().use { input.copyTo(it) } }
        check(sha256(zip) == powerampResourcesSha256) { "Poweramp resources checksum mismatch: $zip" }
    }
    return zip
}

// Release signing comes from keystore.properties (never committed). Debug builds use the same key
// when it is present, because Poweramp only verifies skins signed with the allowlisted certificate.
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

val stableIds = file("stable-ids.txt")

android {
    namespace = "io.github.strider_ape.sparrow"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.strider_ape.sparrow"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    if (keystoreProps.isNotEmpty()) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
        buildTypes {
            getByName("release") { signingConfig = signingConfigs.getByName("release") }
            getByName("debug") { signingConfig = signingConfigs.getByName("release") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // Poweramp attributes look unknown to lint.
        disable += setOf("ResAuto", "MissingTranslation")
    }

    androidResources {
        // Package id 0x80 keeps skin resource ids apart from Poweramp's own 0x7f ids.
        additionalParameters += listOf(
            "--package-id", "0x80",
            "-I", powerampResources().path,
        )
        // Stable ids keep users' saved skin options across clean builds.
        if (stableIds.exists()) additionalParameters += listOf("--stable-ids", stableIds.path)
    }
}

// AGP passes its own --emit-ids to aapt2, so copy what it emits back into the tracked stable-ids.txt.
tasks.matching { it.name == "processDebugResources" || it.name == "processReleaseResources" }.configureEach {
    doLast {
        val variant = name.removePrefix("process").removeSuffix("Resources").replaceFirstChar { it.lowercase() }
        val emitted = layout.buildDirectory
            .file("intermediates/stable_resource_ids_file/$variant/$name/stableIds.txt").get().asFile
        // Sorted, so the tracked file only changes when ids actually change
        if (emitted.exists()) stableIds.writeText(emitted.readLines().filter { it.isNotBlank() }.sorted().joinToString("\n", postfix = "\n"))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
