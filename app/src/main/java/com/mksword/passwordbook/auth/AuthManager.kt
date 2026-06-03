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

    // 精确的 Scheme 回调拼写
    const val REDIRECT_URI = "com.mksword.passwordbook://callback"
    const val LOGOUT_REDIRECT_URI = "com.mksword.passwordbook://logout-callback"

    // 可靠的延迟初始化保障，或保持 lateinit 但在 init 入口立刻无条件实例化
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
        // 【核心修复】：将这两个硬件级服务的实例化提到最前面！
        // 这样可以确保无论网络拉取是成功还是失败，后续调用 updateState 或 clearState 绝不会报 lateinit 未初始化闪退
        val appContext = context.applicationContext
        authStore = EncryptedAuthStore(appContext)
        authService = AuthorizationService(appContext)

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
                onReady(false) // 此时虽然网络失败，但本地 authStore 已经安全就绪
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
        // 加固保护：确保即便在极端情况下也具备防御力
        if (::authStore.isInitialized) {
            authStore.saveAuthState(updatedState)
        }
    }

    /**
     * 清除本地密盘并注销
     */
    fun clearState() {
        authState = AuthState()
        if (::authStore.isInitialized) {
            authStore.clear()
        }
    }

    /**
     * 业务网络请求时获取安全有效的 Token（带静默自动刷新机制）
     */
    fun getValidAccessToken(onResult: (String?, Exception?) -> Unit) {
        if (!::authService.isInitialized || !::authStore.isInitialized) {
            onResult(null, IllegalStateException("AuthManager 尚未初始化完毕"))
            return
        }

        if (!authState.isAuthorized) {
            onResult(null, IllegalStateException("用户未登录"))
            return
        }

        // 自动计算过期时间并走刷新逻辑
        authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
            if (ex != null) {
                onResult(null, ex)
            } else {
                authStore.saveAuthState(authState)
                onResult(accessToken, null)
            }
        }
    }
}