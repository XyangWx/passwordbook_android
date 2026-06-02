package com.mksword.passwordbook.auth

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mksword.passwordbook.BuildConfig
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import org.json.JSONException

object AuthManager {
    private const val TAG = "AuthManager"
    private const val PREFS_NAME = "secure_auth_prefs"
    private const val KEY_AUTH_STATE = "auth_state_json"

    // 动态从 BuildConfig 读取打包时传入的参数
    val CLIENT_ID: String = BuildConfig.CLIENT_ID
    private val AUTH_ISSUER: String = BuildConfig.AUTH_ISSUER

    // 回调地址，必须与服务器后台配置严格一致
    const val REDIRECT_URI = "com.mksword.passwordbook://callback"
    const val LOGOUT_REDIRECT_URI = "com.mksword.passwordbook://logout-callback"

    // 内存中的 OIDC 终结点配置，由 fetchFromIssuer 成功后赋值
    var serviceConfig: AuthorizationServiceConfiguration? = null
        private set

    // 内存中维护的当前登录状态（包含 Token、过期时间等）
    var authState: AuthState = AuthState()
        private set

    /**
     * 在 App 启动时（例如 Application 或 MainActivity）调用此方法初始化
     * @param context 上下文
     * @param onReady 初始化完成后的回调（Success: true 代表端点发现成功，可以使用登录功能）
     */
    fun init(context: Context, onReady: (Boolean) -> Unit) {
        // 1. 先从本地存储中恢复上一次的登录状态
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stateJson = prefs.getString(KEY_AUTH_STATE, null)
        if (stateJson != null) {
            try {
                authState = AuthState.jsonDeserialize(stateJson)
            } catch (e: JSONException) {
                Log.e(TAG, "恢复本地 AuthState 失败", e)
            }
        }

        // 2. 异步拉取 OIDC 发现文档，获取最新的授权、Token 和注销端点
        if (AUTH_ISSUER.isBlank()) {
            Log.e(TAG, "初始化失败：AUTH_ISSUER 为空，请检查编译参数是否正确传入。")
            onReady(false)
            return
        }

        AuthorizationServiceConfiguration.fetchFromIssuer(
            Uri.parse(AUTH_ISSUER)
        ) { config, exception ->
            if (exception != null) {
                Log.e(TAG, "从 Issuer 自动获取配置失败: ${exception.errorDescription}", exception)
                onReady(false)
            } else if (config != null) {
                serviceConfig = config
                Log.d(TAG, "OIDC 端点自动发现成功！")
                onReady(true)
            }
        }
    }

    /**
     * 当授权成功、Token 刷新或状态改变时，调用此方法更新内存并持久化到本地
     */
    fun updateState(updatedState: AuthState, context: Context) {
        authState = updatedState
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUTH_STATE, authState.jsonSerializeString()).apply()
    }

    /**
     * 单独保存当前内存中 authState 的快捷方法
     */
    fun saveState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUTH_STATE, authState.jsonSerializeString()).apply()
    }

    /**
     * 退出登录：清空内存状态，并擦除本地文件中的凭证
     */
    fun clearState(context: Context) {
        authState = AuthState()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_AUTH_STATE).apply()
    }
}