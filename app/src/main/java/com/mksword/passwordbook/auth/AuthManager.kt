package com.mksword.passwordbook.auth

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.mksword.passwordbook.BuildConfig
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration

object AuthManager {
    private const val TAG = "AuthManager"

    // 编译期动态参数
    const val CLIENT_ID: String = BuildConfig.CLIENT_ID
    private const val AUTH_ISSUER: String = BuildConfig.AUTH_ISSUER

    // 精确的 Scheme 回调拼写
    const val REDIRECT_URI = "com.mksword.passwordbook://callback"
    const val LOGOUT_REDIRECT_URI = "com.mksword.passwordbook://logout-callback"

    private lateinit var authStore: EncryptedAuthStore

    var serviceConfig: AuthorizationServiceConfiguration? = null
        private set

    var authState: AuthState = AuthState()
        private set

    /**
     * 全局初始化
     */
    fun init(context: Context, onReady: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        authStore = EncryptedAuthStore(appContext)

        // 1. 自动从安全密盘中恢复上一次的登录令牌
        authState = authStore.loadAuthState() ?: AuthState()

        if (AUTH_ISSUER.isBlank()) {
            Log.e(TAG, "编译参数 AUTH_ISSUER 未传入")
            onReady(false)
            return
        }

        // 2. 异步联网拉取发现文档
        AuthorizationServiceConfiguration.fetchFromIssuer(
            AUTH_ISSUER.toUri()
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
     * 【核心修复】：由于需要实例化临时的 AuthorizationService，这里增加 context 传参，
     * 利用方法局部生命周期，确保用完后自动释放，彻底杜绝单项静态引用持有的内存泄漏。
     */
    fun getValidAccessToken(context: Context, onResult: (String?, Exception?) -> Unit) {
        if (!::authStore.isInitialized) {
            onResult(null, IllegalStateException("AuthManager 尚未初始化完毕"))
            return
        }

        if (!authState.isAuthorized) {
            onResult(null, IllegalStateException("用户未登录"))
            return
        }

        // 🟢 动态创建局部 Service，绑定当前上下文
        val temporaryService = AuthorizationService(context.applicationContext)

        // 自动计算过期时间并走刷新逻辑
        authState.performActionWithFreshTokens(temporaryService) { accessToken, _, ex ->
            // 🟢 核心加固：Token 置换或检查完成后，立刻物理释放局部 Context 链接，防止任何隐式持有
            temporaryService.dispose()

            if (ex != null) {
                onResult(null, ex)
            } else {
                authStore.saveAuthState(authState)
                onResult(accessToken, null)
            }
        }
    }
}