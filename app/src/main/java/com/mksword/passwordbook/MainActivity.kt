package com.mksword.passwordbook

import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.auth.AuthManager
import com.mksword.passwordbook.ui.theme.XyPasswordBookTheme
import net.openid.appauth.*
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var authService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authService = AuthorizationService(this)

        setContent {
            XyPasswordBookTheme {
                var isInitialized by remember { mutableStateOf(false) }
                var isLoggedIn by remember { mutableStateOf(false) }
                var isProcessing by remember { mutableStateOf(false) }
                var userName by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    AuthManager.init(this@MainActivity) { success ->
                        isInitialized = success
                        if (success) {
                            isLoggedIn = AuthManager.authState.isAuthorized
                            if (isLoggedIn) {
                                userName = parseUserNameFromToken(AuthManager.authState.accessToken ?: "")
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "网络连接超时，请重试", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                val loginLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK && result.data != null) {
                        val response = AuthorizationResponse.fromIntent(result.data!!)
                        val exception = AuthorizationException.fromIntent(result.data)

                        AuthManager.authState.update(response, exception)
                        AuthManager.updateState(AuthManager.authState)

                        if (response != null) {
                            isProcessing = true
                            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
                                isProcessing = false
                                AuthManager.authState.update(tokenResponse, tokenException)
                                AuthManager.updateState(AuthManager.authState)

                                if (tokenResponse != null) {
                                    isLoggedIn = true
                                    userName = parseUserNameFromToken(tokenResponse.accessToken ?: "")
                                    Toast.makeText(this@MainActivity, "安全登录成功", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "凭证换取失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                val logoutLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    AuthManager.clearState()
                    isLoggedIn = false
                    userName = ""
                    Toast.makeText(this@MainActivity, "会话已安全销毁", Toast.LENGTH_SHORT).show()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isInitialized) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (isProcessing) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("正在加密生成本地安全盘...")
                        }
                    } else {
                        if (isLoggedIn) {
                            MainAppContent(
                                userName = userName,
                                isLoggingOut = false,
                                onLogoutClick = {
                                    val config = AuthManager.serviceConfig
                                    if (config?.endSessionEndpoint != null) {
                                        val logoutRequest = EndSessionRequest.Builder(config)
                                            .setIdTokenHint(AuthManager.authState.idToken)
                                            .setPostLogoutRedirectUri(android.net.Uri.parse(AuthManager.LOGOUT_REDIRECT_URI))
                                            .build()
                                        logoutLauncher.launch(authService.getEndSessionRequestIntent(logoutRequest))
                                    } else {
                                        AuthManager.clearState()
                                        isLoggedIn = false
                                        userName = ""
                                    }
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                LoginScreen(
                                    onLoginClick = {
                                        AuthManager.serviceConfig?.let { config ->
                                            val authRequest = AuthorizationRequest.Builder(
                                                config,
                                                AuthManager.CLIENT_ID,
                                                ResponseTypeValues.CODE,
                                                android.net.Uri.parse(AuthManager.REDIRECT_URI)
                                            ).setScopes("openid", "profile", "email").build()
                                            loginLauncher.launch(authService.getAuthorizationRequestIntent(authRequest))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
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
            if (finalName.isNotEmpty()) finalName else "User"
        }
    } catch (e: Exception) {
        "User"
    }
}
