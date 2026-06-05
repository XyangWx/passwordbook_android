package com.mksword.passwordbook.network

import android.content.Context
import com.mksword.passwordbook.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


// 【核心修复】：在构造函数中要求传入 context，并自动将其转换为全生命周期安全的 applicationContext
class OauthInterceptor(context: Context) : Interceptor {

    private val appContext = context.applicationContext

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        val host = originalRequest.url.host

        // 1. 宿主域名过滤拦截
        if (host.contains("mksword.com")) {
            if (originalRequest.url.encodedPath.contains("connect") ||
                originalRequest.url.encodedPath.contains("oauth2")) {
                return chain.proceed(originalRequest)
            }
        }

        var validToken: String? = null
        val latch = CountDownLatch(1)

        // 2. 【核心修复】：传入经由构造函数注入的真正的 appContext，满足接口约束并彻底根除内存泄漏！
        AuthManager.getValidAccessToken(appContext) { token, _ ->
            validToken = token
            latch.countDown()
        }

        // 等待异步刷新完成（设置超时防死锁）
        latch.await(10, TimeUnit.SECONDS)

        // 3. 补齐 ABP 跨域身份验证上下文请求头
        if (!validToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $validToken")
        }
        requestBuilder.header("X-Requested-With", "XMLHttpRequest")
        requestBuilder.header("Accept", "application/json")

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            println("❌ [API 异常] 访问令牌已被服务器判定失效 (401)")
        }

        return response
    }
}