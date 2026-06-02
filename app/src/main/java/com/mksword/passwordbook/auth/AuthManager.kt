package com.mksword.passwordbook.auth

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mksword.passwordbook.BuildConfig
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration

object AuthManager {
    private const val TAG = "AuthManager"

    // 编译期动态参数
    val CLIENT_ID: String = BuildConfig.CLIENT_ID
    private val AUTH_ISSUER: String = BuildConfig.AUTH_ISSUER

    // 精确的 Scheme 回调拼写纠正
    const val REDIRECT_URI = "com.mksword.passwordbook://callback"
    const val LOGOUT_REDIRECT_URI = "com.mksword.passwordbook://logout-callback"

    private lateinit var authStore: EncryptedAuthStore
    private lateinit var authService: AuthorizationService

    var serviceConfig: AuthorizationServiceConfiguration? = null
        private set

    var authState: AuthState = AuthState()
        private set

    /**
     * 全局初始化
     */
    fun init(context: Context, onReady: (Boolean) -> Unit) {
        authStore = EncryptedAuthStore(context.applicationContext)
        authService = AuthorizationService(context.applicationContext)

        // 1. 自动从安全密盘中恢复上一次的登录令牌
        authState = authStore.loadAuthState() ?: AuthState()

        if (AUTH_ISSUER.isBlank()) {
            Log.e(TAG, "编译参数 AUTH_ISSUER 未传入")
            onReady(false)
            return
        }

        // 2. 异步联网拉取发现文档
        AuthorizationServiceConfiguration.fetchFromIssuer(
            Uri.parse(AUTH_ISSUER)
        ) { config, exception ->
            if (exception != null) {
                Log.e(TAG, "端点发现失败", exception)
                onReady(false)
            } else if (config != null) {
                serviceConfig = config
                onReady(true)
            }
        }
    }

    /**
     * 更新状态并自动触发加密保存
     */
    fun updateState(updatedState: AuthState) {
        authState = updatedState
        authStore.saveAuthState(updatedState)
    }

    /**
     * 清除本地密盘并注销
     */
    fun clearState() {
        authState = AuthState()
        authStore.clear()
    }

    /**
     * 【超能力核心】业务网络请求时获取安全有效的 Token
     * 如果过期，它会在后台静默刷新，永远不需要你在各个页面重复写刷新逻辑
     */
    fun getValidAccessToken(onResult: (String?, Exception?) -> Unit) {
        // 如果未登录，直接返回
        if (!authState.isAuthorized) {
            onResult(null, IllegalStateException("用户未登录"))
            return
        }
        
        // 自动计算过期时间，若过期则底层悄悄用 Refresh Token 换新，随后回调返回
        authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
            if (ex != null) {
                onResult(null, ex)
            } else {
                // 确保刷新后的最新 Token 再次被加密保存入盘
                authStore.saveAuthState(authState)
                onResult(accessToken, null)
            }
        }
    }
}