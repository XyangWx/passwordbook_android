package com.mksword.passwordbook

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.auth.AuthManager
import com.mksword.passwordbook.ui.theme.XyPasswordBookTheme
import net.openid.appauth.NoClientAuthentication
import net.openid.appauth.*
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private lateinit var authService: AuthorizationService
    // ➔ 利用 Android 官方扩展库一键安全委托注入声明好的 ViewModel
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.mksword.passwordbook.network.PasswordBookApiClient.init(this.applicationContext)

        enableEdgeToEdge() // 开启沉浸式

        authService = AuthorizationService(this)

        setContent {
            XyPasswordBookTheme {

                // ➔ 在这里单向初始化调用，不污染 Compose 生命周期
                LaunchedEffect(Unit) {
                    viewModel.initialize(this@MainActivity) {
                        Toast.makeText(this@MainActivity, "网络连接超时，请重试", Toast.LENGTH_LONG).show()
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
                            viewModel.isProcessing = true

                            // 恢复为原生的无参请求创建，绝对不传手动 Map
                            val tokenExchangeRequest = response.createTokenExchangeRequest()

                            // 【核心修复】：将之前错误的 NoAuthentication 替换为官方标准的 NoClientAuthentication.INSTANCE
                            // 这会强行告知授权服务器：这是一个运行在安卓手机上的公共应用，不携带任何硬编码的 client_secret
                            val clientAuthentication = NoClientAuthentication.INSTANCE

                            // 执行 Token 置换请求
                            authService.performTokenRequest(
                                tokenExchangeRequest,
                                clientAuthentication
                            ) { tokenResponse, tokenException ->
                                viewModel.isProcessing = false
                                AuthManager.authState.update(tokenResponse, tokenException)
                                AuthManager.updateState(AuthManager.authState)

                                if (tokenResponse != null) {
                                    viewModel.onLoginSuccess(tokenResponse.accessToken ?: "")
                                    Toast.makeText(this@MainActivity, "安全登录成功", Toast.LENGTH_SHORT).show()
                                } else {
                                    val errorDetail = tokenException?.errorDescription
                                        ?: tokenException?.localizedMessage
                                        ?: "服务器拒绝授信"
                                    Toast.makeText(this@MainActivity, "凭证换取失败: $errorDetail", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

                val logoutLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.onLogoutSuccess()
                    Toast.makeText(this@MainActivity, "会话已安全销毁", Toast.LENGTH_SHORT).show()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!viewModel.isInitialized) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else if (viewModel.isProcessing) {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("正在加密生成本地安全盘...")
                        }
                    } else {
                        if (viewModel.isLoggedIn) {
                            // ➔ 清爽分发给解耦后的独立文件
                            MainAppContent(
                                userName = viewModel.userName,
                                passwordBooks = viewModel.passwordBooks,
                                isListLoading = viewModel.isListLoading,
                                // 🟢 完美对接：调用 ViewModel 中真实的 fetchPasswordBooks() 方法
                                onRefreshList = {
                                    viewModel.fetchPasswordBooks()
                                },
                                onLogoutClick = {
                                    val config = AuthManager.serviceConfig
                                    if (config?.endSessionEndpoint != null) {
                                        val logoutRequest = EndSessionRequest.Builder(config)
                                            .setIdTokenHint(AuthManager.authState.idToken)
                                            .setPostLogoutRedirectUri(AuthManager.LOGOUT_REDIRECT_URI.toUri())
                                            .build()
                                        logoutLauncher.launch(authService.getEndSessionRequestIntent(logoutRequest))
                                    } else {
                                        viewModel.onLogoutSuccess()
                                    }
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LoginScreen(
                                    onLoginClick = {
                                        AuthManager.serviceConfig?.let { config ->
                                            val authRequest = AuthorizationRequest.Builder(
                                                config, AuthManager.CLIENT_ID, ResponseTypeValues.CODE,
                                                AuthManager.REDIRECT_URI.toUri()
                                            ).setScopes("openid", "profile", "email", "offline_access", "XYPortal").build()
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