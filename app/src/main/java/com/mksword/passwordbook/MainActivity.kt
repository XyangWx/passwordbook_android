package com.mksword.passwordbook

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.auth.AuthManager
import com.mksword.passwordbook.ui.theme.XyPasswordBookTheme
import net.openid.appauth.*

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

                // 启动时初始化
                LaunchedEffect(Unit) {
                    AuthManager.init(this@MainActivity) { success ->
                        isInitialized = success
                        if (success) {
                            isLoggedIn = AuthManager.authState.isAuthorized
                        } else {
                            Toast.makeText(this@MainActivity, "网络连接超时，请重试", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // 登录回调拦截
                val loginLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK && result.data != null) {
                        val response = AuthorizationResponse.fromIntent(result.data!!)
                        val exception = AuthorizationException.fromIntent(result.data)

                        AuthManager.authState.update(response, exception)
                        AuthManager.updateState(AuthManager.authState) // 存入加密盘

                        if (response != null) {
                            isProcessing = true
                            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
                                isProcessing = false
                                AuthManager.authState.update(tokenResponse, tokenException)
                                AuthManager.updateState(AuthManager.authState) // 存入加密盘

                                if (tokenResponse != null) {
                                    isLoggedIn = true
                                    Toast.makeText(this@MainActivity, "安全登录成功", Toast.LENGTH_SHORT).show()
                                    
                                    // 【演示：如何安全读取Token做业务】
                                    AuthManager.getValidAccessToken { token, err ->
                                        if (token != null) {
                                            // 用这个安全密令发送请求给你的密码本后端
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@MainActivity, "凭证换取失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                // 注销回调拦截
                val logoutLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    AuthManager.clearState() // 擦除加密盘
                    isLoggedIn = false
                    Toast.makeText(this@MainActivity, "会话已安全销毁", Toast.LENGTH_SHORT).show()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!isInitialized) {
                            CircularProgressIndicator()
                        } else if (isProcessing) {
                            CircularProgressIndicator()
                            Text("正在加密生成本地安全盘...")
                        } else {
                            if (isLoggedIn) {
                                MainAppContent(
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
                                        }
                                    }
                                )
                            } else {
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

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    Button(onClick = onLoginClick) { Text("登录") }
}

@Composable
fun MainAppContent(onLogoutClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("密码本", style = MaterialTheme.typography.headlineMedium)
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "用户信息")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("注销") },
                        onClick = {
                            expanded = false
                            onLogoutClick()
                        }
                    )
                }
            }
        }
    }
}