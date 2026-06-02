import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 【新增】安全读取编译参数/系统变量/本地文件的辅助函数
fun getAuthProperty(propertyName: String, envName: String, defaultValue: String): String {
    // 1. 优先从命令行参数读取 (例如: -PAUTH_ENDPOINT=...)
    if (project.hasProperty(propertyName)) {
        return project.property(propertyName).toString()
    }
    // 2. 其次从系统环境变量读取 (CI/CD 注入)
    val envValue = System.getenv(envName)
    if (!envValue.isNullOrEmpty()) {
        return envValue
    }
    // 3. 再次从本地 local.properties 读取 (本地日常开发)
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val properties = Properties()
        properties.load(localPropertiesFile.inputStream())
        val localValue = properties.getProperty(propertyName)
        if (!localValue.isNullOrEmpty()) {
            return localValue
        }
    }
    return defaultValue
}

android {
    namespace = "com.mksword.passwordbook"
    // 注意：Android 16 (API 36) 相关的 compileSdk 结构
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mksword.passwordbook"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["appAuthRedirectScheme"] = "com.mksword.passwordbook"
    }

    // 【新增】开启 BuildConfig 自动生成功能，否则代码中无法引用 BuildConfig 类
    buildFeatures {
        compose = true
        buildConfig = true 
    }

    buildTypes {
        release {
            isMinifyEnabled = false // 生产环境建议后续开启并配置混淆
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 【新增】Release 编译时动态注入参数
            val authEp = getAuthProperty("AUTH_ENDPOINT", "PROD_AUTH_ENDPOINT", "")
            val tokenEp = getAuthProperty("TOKEN_ENDPOINT", "PROD_TOKEN_ENDPOINT", "")
            val clientId = getAuthProperty("CLIENT_ID", "PROD_CLIENT_ID", "")

            buildConfigField("String", "AUTH_ENDPOINT", "\"$authEp\"")
            buildConfigField("String", "TOKEN_ENDPOINT", "\"$tokenEp\"")
            buildConfigField("String", "CLIENT_ID", "\"$clientId\"")
        }
        
        // 【新增】显式声明 debug 闭包，用于配置开发环境的参数
        getByName("debug") {
            val authEp = getAuthProperty("AUTH_ENDPOINT_DEBUG", "DEV_AUTH_ENDPOINT", "https://mksword.com")
            val tokenEp = getAuthProperty("TOKEN_ENDPOINT_DEBUG", "DEV_TOKEN_ENDPOINT", "https://mksword.com")
            val clientId = getAuthProperty("CLIENT_ID_DEBUG", "DEV_CLIENT_ID", "password_book_app_dev")

            buildConfigField("String", "AUTH_ENDPOINT", "\"$authEp\"")
            buildConfigField("String", "TOKEN_ENDPOINT", "\"$tokenEp\"")
            buildConfigField("String", "CLIENT_ID", "\"$clientId\"")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    // OIDC 协议核心库
    implementation("net.openid:appauth:0.11.1")

    // Android 官方加密存储库
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // 协程与生命周期库
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}