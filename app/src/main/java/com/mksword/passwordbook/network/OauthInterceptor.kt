package com.mksword.passwordbook.network

import com.mksword.passwordbook.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class OauthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 1. 过滤掉不需要 Token 的请求（比如授权服务器自身的请求）
        if (originalRequest.url.host.contains("://mksword.com")) {
            return chain.proceed(originalRequest)
        }

        var validToken: String? = null
        val latch = CountDownLatch(1)

        // 2. 借助我们之前在 AuthManager 中封装的"超能力"方法，
        // 在子线程中安全、同步地获取最新的 Token（若快过期底层会自动静默刷新）
        AuthManager.getValidAccessToken { token, error ->
            validToken = token
            latch.countDown()
        }

        // 等待异步刷新完成（设置超时防死锁）
        latch.await(10, TimeUnit.SECONDS)

        // 3. 如果成功拿到 Token，自动塞进 HTTP 請求头的 Authorization 中
        return if (!validToken.isNullOrBlank()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $validToken")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            // 如果拿不到 Token（说明 Refresh Token 也过期了，需要彻底重新登录）
            chain.proceed(originalRequest)
        }
    }
}