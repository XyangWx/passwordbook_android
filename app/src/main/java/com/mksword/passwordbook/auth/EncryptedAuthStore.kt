package com.mksword.passwordbook.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import net.openid.appauth.AuthState

class EncryptedAuthStore(context: Context) {
    private val tag = "EncryptedAuthStore"
    
    // 获取或创建系统级最高安全级别的本地硬件加密密钥 (AES256_GCM)
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    // 创建高安全性加密存储盘，替代传统的明文 SharedPreferences
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_auth_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthState(state: AuthState) {
        sharedPreferences.edit()
            .putString("auth_state_json", state.jsonSerializeString())
            .apply()
    }

    fun loadAuthState(): AuthState? {
        val json = sharedPreferences.getString("auth_state_json", null) ?: return null
        return try {
            AuthState.jsonDeserialize(json)
        } catch (e: Exception) {
            Log.e(tag, "解密或反序列化 AuthState 失败", e)
            null
        }
    }

    fun clear() {
        sharedPreferences.edit().remove("auth_state_json").apply()
    }
}