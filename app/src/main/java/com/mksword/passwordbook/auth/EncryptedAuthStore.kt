package com.mksword.passwordbook.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.openid.appauth.AuthState

class EncryptedAuthStore(context: Context) {
    private val tag = "EncryptedAuthStore"
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs",
        masterKey,
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