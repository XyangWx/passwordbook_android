package com.mksword.passwordbook.network

import com.mksword.passwordbook.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class OauthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        val host = originalRequest.url.host

        // 1. 【完美修复】：修正宿主判定。只要是发往授权服务器的域名，或者是动态获取的 Issuer 域名，直接放行，绝不带令牌。
        // 同时根据您之前的配置，将原先错误的 "://mksword.com" 修正为干净的关键字匹配
        if (host.contains("mksword.com")) {
            // 如果请求的本身就是授权服务（如 fetchFromIssuer 或换取 Token 的接口），直接放行
            if (originalRequest.url.encodedPath.contains("connect") ||
                originalRequest.url.encodedPath.contains("oauth2")) {
                return chain.proceed(originalRequest)
            }
        }

        var validToken: String? = null
        val latch = CountDownLatch(1)

        // 2. 借助 AuthManager 在子线程中安全同步获取最新可用的 Token
        AuthManager.getValidAccessToken { token, _ ->
            validToken = token
            latch.countDown()
        }

        // 等待异步刷新（10秒超时防御，防止网络极差时整个 App 彻底卡死在拦截器里）
        latch.await(10, TimeUnit.SECONDS)

        // 3. 【完美修复】：打满令牌并对齐 Flutter 的 ABP 专属跨域认证头上下文，彻底修复潜在的跨域拒绝隐患
        if (!validToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $validToken")
        }
        requestBuilder.header("X-Requested-With", "XMLHttpRequest")
        requestBuilder.header("Accept", "application/json")

        val response = chain.proceed(requestBuilder.build())

        // 4. 【全自动感知】：如果在这里抓到了 401 报错，说明远端 Session 已经被彻底清空
        if (response.code == 401) {
            println("❌ [API 异常] 访问令牌已被服务器判定失效 (401)，可能由于 Session 被远端清理")
        }

        return response
    }
}