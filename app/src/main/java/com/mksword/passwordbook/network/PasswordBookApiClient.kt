package com.mksword.passwordbook.network

import com.mksword.passwordbook.BuildConfig
import com.mksword.passwordbook.auth.AuthManager
import com.mksword.passwordbook.entities.*
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 1. 使用 Retrofit 标准声明后端提供的所有业务路由 (等同于 Dio 的路由映射)
 */
interface PasswordBookService {
    @GET("api/password-book")
    suspend fun getPasswordBooks(): retrofit2.Response<PasswordBookListResponse>

    @POST("api/password-book")
    suspend fun createPasswordBook(
        @Body request: NewPasswordBookRequest
    ): retrofit2.Response<Unit>

    @GET("api/password-book/{id}/with-entries")
    suspend fun viewPasswordBook(
        @Path("id") id: String
    ): retrofit2.Response<ViewPasswordBookResponse>

    @DELETE("api/password-book/{id}")
    suspend fun deletePasswordBook(
        @Path("id") id: String
    ): retrofit2.Response<Unit>

    @POST("api/password-book/{passwordBookId}/entries")
    suspend fun createPasswordEntry(
        @Path("passwordBookId") passwordBookId: String,
        @Body request: CreatePasswordRequest
    ): retrofit2.Response<Unit>

    @POST("api/password-book/generate-random-password-from-weak-level")
    suspend fun generateRandomPassword(
        @Body request: GetRandomPasswordRequest
    ): retrofit2.Response<GetRandomPasswordResponse>

    @DELETE("api/password-book/{passwordBookId}/entries/{entryId}?queryKind=0")
    suspend fun deletePasswordEntry(
        @Path("passwordBookId") passwordBookId: String,
        @Path("entryId") entryId: String
    ): retrofit2.Response<Unit>

    @POST("api/password-book/{passwordBookId}/entries/{passwordEntryId}/restore")
    suspend fun restorePasswordEntry(
        @Path("passwordBookId") passwordBookId: String,
        @Path("passwordEntryId") passwordEntryId: String
    ): retrofit2.Response<Unit>
}

/**
 * 2. 核心网络控制中心：管理 OkHttp 拦截器并向上提供单例调用（完美平替 Dio 的 Client）
 */
object PasswordBookApiClient {

    // 配置通用的 kotlinx.serialization JSON 转换器规则
    private val jsonConfig = Json {
        ignoreUnknownKeys = true // 容错处理：当后端返回非预期新属性时不崩溃
        coerceInputValues = true // 容错处理：自动映射空安全默认值
    }

    private val apiService: PasswordBookService by lazy {

        // 🟢 核心平替：构建具有安全 Token 注入和 401 监听机制的 OkHttpClient 拦截器
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()

                // 借助 CountDownLatch 在子线程中安全同步获取 ValidAccessToken
                var validToken: String? = null
                val latch = CountDownLatch(1)
                AuthManager.getValidAccessToken { token, _ ->
                    validToken = token
                    latch.countDown()
                }
                latch.await(10, TimeUnit.SECONDS)

                // 🛠️ 完美平替 Dio 中注入的 ABP 跨域身份验证上下文请求头
                if (!validToken.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $validToken")
                }
                requestBuilder.header("X-Requested-With", "XMLHttpRequest")
                requestBuilder.header("Accept", "application/json")

                val response = chain.proceed(requestBuilder.build())

                // 🟢 401 全局网络状态判定与安全退出兜底
                if (response.code == 401) {
                    println("❌ [API 异常] 访问令牌已被服务器判定失效 (401)，可能由于 Session 被远端清理")
                    // 在此处可配合回调或发送事件总线通知 UI 彻底退回登录页
                }

                response
            })
            .build()

        // 绑定包含动态参数 BuildConfig.API_URI 的全套路由服务
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_URI)
            .client(okHttpClient)
            .addConverterFactory(jsonConfig.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PasswordBookService::class.java)
    }

    /**
     * 执行底层的网络状态码校验（高内聚函数，减少各 API 的冗余 try-catch）
     */
    private fun <T> handleNetworkResponse(response: retrofit2.Response<T>): T {
        when (response.code()) {
            401 -> throw IOException("用户未登录或访问令牌已过期，请重新登录 (401)")
            200, 201, 204 -> {
                // 如果是空请求体（如删除或创建），直接返回一个无意义结果
                if (response.body() == null && response.code() in listOf(201, 204)) {
                    @Suppress("UNCHECKED_CAST")
                    return Unit as T
                }
                return response.body() ?: throw IOException("服务器返回空响应体")
            }
            else -> throw IOException("服务器请求失败，状态码: ${response.code()}")
        }
    }

    // =========================================================================
    // 3. 对外业务接口函数调用（完全对齐您的 8 个 Flutter 核心 API 异步方法）
    // =========================================================================

    suspend fun getPasswordBooks(): List<PasswordBook> {
        return try {
            val response = apiService.getPasswordBooks()
            handleNetworkResponse(response).items
        } catch (e: Exception) {
            throw IOException("网络请求发生异常: ${e.message}")
        }
    }

    suspend fun createPasswordBook(request: NewPasswordBookRequest) {
        try {
            val response = apiService.createPasswordBook(request)
            handleNetworkResponse(response)
        } catch (e: Exception) {
            throw IOException("网络请求发生异常: ${e.message}")
        }
    }

    suspend fun viewPasswordBook(id: String): ViewPasswordBookResponse {
        return try {
            val response = apiService.viewPasswordBook(id)
            handleNetworkResponse(response)
        } catch (e: Exception) {
            throw IOException("网络请求发生异常: ${e.message}")
        }
    }

    suspend fun deletePasswordBook(id: String) {
        try {
            val response = apiService.deletePasswordBook(id)
            handleNetworkResponse(response)
        } catch (e: Exception) {
            throw IOException("网络请求发生异常: ${e.message}")
        }
    }

    suspend fun createPasswordEntry(passwordBookId: String, request: CreatePasswordRequest) {
        try {
            val response = apiService.createPasswordEntry(passwordBookId, request)
            handleNetworkResponse(response)
        } catch (e: Exception) {
            throw IOException("网络请求发生异常: ${e.message}")
        }
    }

    suspend fun generateRandomPassword(request: GetRandomPasswordRequest): GetRandomPasswordResponse {
        return try {
            val response = apiService.generateRandomPassword(request)
            handleNetworkResponse(response)
        } catch (e: Exception) {
            throw IOException("网络请求发生异常: ${e.message}")
        }
    }

    suspend fun deletePasswordEntry(passwordBookId: String, entryId: String) {
        try {
            val response = apiService.deletePasswordEntry(passwordBookId, entryId)
            handleNetworkResponse(response)
        } catch (e: Exception) {
            throw IOException("网络请求发生异常: ${e.message}")
        }
    }

    suspend fun restorePasswordEntry(passwordBookId: String, passwordEntryId: String) {
        try {
            val response = apiService.restorePasswordEntry(passwordBookId, passwordEntryId)
            handleNetworkResponse(response)
        } catch (e: Exception) {
            throw IOException("网络请求发生异常: ${e.message}")
        }
    }
}