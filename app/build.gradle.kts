import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val apkName = project.findProperty("APK_OUTPUT_NAME")?.toString() ?: "${project.name}_${buildType.name}"
            output.outputFileName = "$apkName.apk"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val authIssuer = getAuthProperty("AUTH_ISSUER", "PROD_AUTH_ISSUER", "")
            val clientId = getAuthProperty("CLIENT_ID", "PROD_CLIENT_ID", "")
            val apiUri = getAuthProperty("API_URI", "PROD_API_URI", "")

            buildConfigField("String", "AUTH_ISSUER", "\"$authIssuer\"")
            buildConfigField("String", "CLIENT_ID", "\"$clientId\"")
            buildConfigField("String", "API_URI", "\"$apiUri\"")
        }
        getByName("debug") {
            val authIssuer = getAuthProperty("AUTH_ISSUER_DEBUG", "DEV_AUTH_ISSUER", "https://mksword.com")
            val clientId = getAuthProperty("CLIENT_ID_DEBUG", "DEV_CLIENT_ID", "password_book_app")
            val apiUri = getAuthProperty("API_URI_DEBUG", "DEV_API_URI", "https://api-test.mksword.com")

            buildConfigField("String", "AUTH_ISSUER", "\"$authIssuer\"")
            buildConfigField("String", "CLIENT_ID", "\"$clientId\"")
            buildConfigField("String", "API_URI", "\"$apiUri\"")
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

    // OkHttp 网络库
    implementation(libs.okhttp.core)

    // Kotlin 序列化
    implementation(libs.kotlinx.serialization.json)

    // Material Icons
    implementation("androidx.compose.material:material-icons-extended")

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
    
    // 【新增】：引入 Retrofit 网络架构核心及其 kotlinx 官方专用强类型 JSON 适配工厂
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // 【必须追加】：引入 Activity 级的高阶 ViewModel 委托扩展支持
    implementation("androidx.activity:activity-ktx:1.9.0")
}