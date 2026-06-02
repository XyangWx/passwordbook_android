package com.mksword.passwordbook.auth

import android.content.Context
import android.net.Uri
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import org.json.JSONException

object AuthManager {
    // 授权服务器基本信息
    private const val AUTH_ENDPOINT = "https://mksword.com"
    private const val TOKEN_ENDPOINT = "https://mksword.com"
    
    const val CLIENT_ID = "password_book_app"
    // 注意：这里的 URL 需要与你授权服务后台配置的拼写严格一致
    const val REDIRECT_URI = "com.mksword.passwordbook://callboack" 
    const val LOGOUT_REDIRECT_URI = "com.mksword.passwordbook://logout-callback"

    val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse(AUTH_ENDPOINT),
        Uri.parse(TOKEN_ENDPOINT)
    )

    // 用于维护内存中的登录状态
    var authState: AuthState = AuthState()
        private set

    // 从本地加密存储(SharedPreferences)中恢复登录状态
    fun init(context: Context) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val stateJson = prefs.getString("state_json", null)
        if (stateJson != null) {
            try {
                authState = AuthState.jsonDeserialize(stateJson)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    // 状态更新后保存到本地
    fun saveState(context: Context) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("state_json", authState.jsonSerializeString()).apply()
    }

    // 清除登录状态
    fun clearState(context: Context) {
        authState = AuthState()
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("state_json").apply()
    }
}