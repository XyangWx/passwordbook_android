package com.mksword.passwordbook

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mksword.passwordbook.auth.AuthManager
import com.mksword.passwordbook.entities.PasswordBook
import com.mksword.passwordbook.network.PasswordBookApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject


class AuthViewModel : ViewModel() {

    // UI 驱动状态收拢
    var isInitialized by mutableStateOf(false)
        private set
    var isLoggedIn by mutableStateOf(false)
        private set

    // 【核心修复 1】：彻底去掉后面的 private set，允许外部直接赋值
    var isProcessing by mutableStateOf(false)

    var userName by mutableStateOf("")
        private set
    var passwordBooks by mutableStateOf<List<PasswordBook>>(emptyList())
        private set
    var isListLoading by mutableStateOf(true)
        private set

    /**
     * App 启动初始化
     */
    fun initialize(context: Context, onFail: () -> Unit) {
        val appContext = context.applicationContext

        // 【核心修复】：请把原先写在这里的这行代码【完全删掉】！
        // PasswordBookApiClient.init(appContext) ➔ 已成功上移到 MainActivity.kt

        AuthManager.init(appContext) { success ->
            isInitialized = success
            if (success) {
                isLoggedIn = AuthManager.authState.isAuthorized
                if (isLoggedIn) {
                    userName = parseUserNameFromToken(AuthManager.authState.accessToken ?: "")
                    fetchPasswordBooks() // 此时由于全局 ApiClient 已经就绪，这里将绝对安全出数
                }
            } else {
                onFail()
            }
        }
    }

    /**
     * 登录成功凭证置换
     */
    fun onLoginSuccess(token: String) {
        isLoggedIn = true
        userName = parseUserNameFromToken(token)
        fetchPasswordBooks()
    }

    /**
     * 注销并清理本地状态
     */
    fun onLogoutSuccess() {
        AuthManager.clearState()
        isLoggedIn = false
        userName = ""
        passwordBooks = emptyList()
    }

    /**
     * 异步联网拉取真实密码本列表
     */
    fun fetchPasswordBooks() {
        viewModelScope.launch {
            isListLoading = true
            try {
                passwordBooks = PasswordBookApiClient.getPasswordBooks()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isListLoading = false
            }
        }
    }

    private fun parseUserNameFromToken(token: String): String {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return ""
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            val familyName = json.optString("family_name", "")
            val givenName = json.optString("given_name", "").trim()
            if (givenName.isNotEmpty()) {
                familyName + givenName
            } else {
                val name = json.optString("name", "").trim()
                val surname = json.optString("surname", "")
                val finalName = (surname + name).trim()
                finalName.ifEmpty { "User" }
            }
        } catch (_: Exception) {
            "User"
        }
    }
}