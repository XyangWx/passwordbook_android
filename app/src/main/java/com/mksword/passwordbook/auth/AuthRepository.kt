package com.mksword.passwordbook.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService

class AuthRepository(context: Context) {
    private val authStore = EncryptedAuthStore(context)
    private val authService = AuthorizationService(context)
    
    // 暴露出响应式的登录状态给 Activity/Fragment 监听
    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> get() = _isLoggedIn

    private var currentAuthState: AuthState = authStore.loadAuthState() ?: AuthState()

    init {
        _isLoggedIn.value = currentAuthState.isAuthorized
    }

    // 更新并自动持久化状态
    fun updateState(newState: AuthState) {
        currentAuthState = newState
        authStore.saveAuthState(newState)
        _isLoggedIn.postValue(newState.isAuthorized)
    }

    // 核心：像 Flutter 插件一样，自动检查并静默刷新 Token
    fun getValidAccessToken(onResult: (String?, Exception?) -> Unit) {
        // performActionWithFreshTokens 会自动判断 Token 是否快过期，
        // 如果快过期，它会在后台自动调用 /token 端点，使用 Refresh Token 换取新 Token 并自动更新状态
        currentAuthState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
            if (ex != null) {
                onResult(null, ex)
            } else {
                // 如果在执行过程中发生了自动刷新，重新持久化
                authStore.saveAuthState(currentAuthState)
                onResult(accessToken, null)
            }
        }
    }

    fun logout() {
        authStore.clear()
        currentAuthState = AuthState()
        _isLoggedIn.value = false
    }
}