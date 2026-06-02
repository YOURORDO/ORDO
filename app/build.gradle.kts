import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

android {
    namespace = "com.example.ordo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ordo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        //  заглушка
        val serverUrl = localProperties.getProperty("ordo.server.url") ?: "wss://your-private-relay-server.com/connect"

        // 2. Встраиваем адрес в XML-ресурсы приложения под именем default_server_url
        resValue("string", "default_server_url", serverUrl)

        // Добавляем поддержку нативных библиотек для разных процессоров
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }
    }

    buildTypes {
        release {

            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures { compose = true }
}

dependencies {
    //для шрифта
    implementation("com.github.jeziellago:compose-markdown:0.5.0")
    // Для генерации QR-кодов
    implementation("com.google.zxing:core:3.5.3")
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compiler)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.animation.core)

    // Для камеры и сканирования
    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.0")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Защита ключей (Keystore)
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    // Сеть и крипто
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Room (База данных) - используем KSP
    val room_version = "2.6.1"
    // Современное шифрование базы данных (SQLCipher) с поддержкой 16 KB (Android 15+)
    implementation("net.zetetic:sqlcipher-android:4.6.1@aar")
    implementation("androidx.sqlite:sqlite:2.4.0")
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Стандартные либы
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
}

configurations.all {
    resolutionStrategy {
        force("org.bouncycastle:bcprov-jdk18on:1.77")
    }
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")


    exclude(group = "com.google.guava", module = "listenablefuture")
}