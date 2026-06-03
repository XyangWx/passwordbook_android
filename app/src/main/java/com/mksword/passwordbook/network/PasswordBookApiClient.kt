package com.mksword.passwordbook.network

import android.content.Context
import com.mksword.passwordbook.BuildConfig
import com.mksword.passwordbook.entities.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.http.*


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

object PasswordBookApiClient {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // 【核心修复】：彻底删除了原本写在这里的另一个 by lazy 声明，从而完美消除 Conflicting 冲突
    private lateinit var apiService: PasswordBookService

    /**
     * 在全局初始化网络层（由 AuthViewModel 统一开机唤醒）
     */
    fun init(context: Context) {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            // 完美绑定包含上下文的拦截器
            .addInterceptor(OauthInterceptor(context.applicationContext))
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(BuildConfig.API_URI)
            .client(okHttpClient)
            .addConverterFactory(jsonConfig.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PasswordBookService::class.java)
    }

    private fun <T> handleNetworkResponse(response: retrofit2.Response<T>): T {
        when (response.code()) {
            401 -> throw IOException("用户未登录或访问令牌已过期，请重新登录 (401)")
            200, 201, 204 -> {
                if (response.body() == null && response.code() in listOf(201, 204)) {
                    @Suppress("UNCHECKED_CAST")
                    return Unit as T
                }
                return response.body() ?: throw IOException("服务器返回空响应体")
            }
            else -> throw IOException("服务器请求失败，状态码: ${response.code()}")
        }
    }

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