package com.mksword.passwordbook.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import net.openid.appauth.AuthState

class EncryptedAuthStore(context: Context) {
    
    // 创建或者获取硬件级别的加密密钥
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    // 初始化安全存储
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_auth_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // 保存状态（转为密文 JSON 存储）
    fun saveAuthState(state: AuthState) {
        sharedPreferences.edit()
            .putString("auth_state_json", state.jsonSerializeString())
            .apply()
    }

    // 读取状态（自动解密）
    fun loadAuthState(): AuthState? {
        val json = sharedPreferences.getString("auth_state_json", null) ?: return null
        return try {
            AuthState.jsonDeserialize(json)
        } catch (e: Exception) {
            null
        }
    }

    // 清空存储（注销时使用）
    fun clear() {
        sharedPreferences.edit().remove("auth_state_json").apply()
    }
}