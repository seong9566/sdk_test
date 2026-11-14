import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Kakao JavaScript 키는 커밋되지 않는 local.properties에 둔다: KAKAO_JS_KEY=xxxx
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.example.sdk_test"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.sdk_test"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "kakao_js_key", localProps.getProperty("KAKAO_JS_KEY", ""))
        // 위치 공유 중계 서버. 기본값은 에뮬레이터에서 본 호스트 PC(10.0.2.2)의 로컬 서버(relay/server.js).
        resValue("string", "relay_url", localProps.getProperty("RELAY_URL", "ws://10.0.2.2:8080"))
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        resValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}