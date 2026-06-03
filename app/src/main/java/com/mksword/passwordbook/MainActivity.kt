package com.mksword.passwordbook

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.auth.AuthManager
import com.mksword.passwordbook.ui.theme.XyPasswordBookTheme
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.ResponseTypeValues

class MainActivity : ComponentActivity() {

    // AppAuth 的核心服务类，负责底层的跳转和交互
    private lateinit var authService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authService = AuthorizationService(this)

        setContent {
            XyPasswordBookTheme {
                // 1. 定义 OIDC 相关的 Compose 响应式状态
                var isInitialized by remember { mutableStateOf(false) } // 发现文档是否拉取成功
                var isLoggedIn by remember { mutableStateOf(false) }    // 用户是否处于登录状态
                var isProcessing by remember { mutableStateOf(false) }  // 换取 Token 期间的加载动画

                // 2. 异步初始化 AuthManager（拉取发现文档并加载本地历史凭证）
                LaunchedEffect(Unit) {
                    AuthManager.init(this@MainActivity) { success ->
                        isInitialized = success
                        if (success) {
                            // 检查本地恢复的状态中，是否已经拥有合法的授权
                            isLoggedIn = AuthManager.authState.isAuthorized
                        } else {
                            Toast.makeText(this@MainActivity, "初始化 OIDC 失败，请检查网络", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // 3. 注册登录跳转的回调接收器（平替传统 Activity 的 onActivityResult）
                val loginLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK && result.data != null) {
                        val response = AuthorizationResponse.fromIntent(result.data!!)
                        val exception = AuthorizationException.fromIntent(result.data)

                        // 更新内存状态
                        AuthManager.authState.update(response, exception)
                        AuthManager.saveState(this@MainActivity)

                        if (response != null) {
                            // 拿到 Auth Code，立刻去换取真实的 Access Token
                            isProcessing = true
                            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
                                isProcessing = false
                                AuthManager.authState.update(tokenResponse, tokenException)
                                AuthManager.saveState(this@MainActivity)

                                if (tokenResponse != null) {
                                    isLoggedIn = true
                                    Toast.makeText(this@MainActivity, "登录成功！", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "Token 换取失败: ${tokenException?.errorDescription}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "登录被取消或失败: ${exception?.errorDescription}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // 4. 注册注销（EndSession）跳转的回调接收器
                val logoutLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    // 用户在浏览器完成注销动作并跳回 App 后触发
                    AuthManager.clearState(this@MainActivity)
                    isLoggedIn = false
                    Toast.makeText(this@MainActivity, "已安全退出登录", Toast.LENGTH_SHORT).show()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!isInitialized) {
                            // 正在联网拉取 https://mksword.com 的发现文档
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("正在连接认证服务器...")
                        } else if (isProcessing) {
                            // 正在拿 Code 换 Token
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("正在安全加密登录...")
                        } else {
                            // 核心业务：根据登录状态渲染不同的 Compose 界面
                            if (isLoggedIn) {
                                MainAppContent(
                                    onLogoutClick = {
                                        val config = AuthManager.serviceConfig
                                        // 检查发现文档中是否含有标准的注销端点 (end_session_endpoint)
                                        if (config?.endSessionEndpoint != null) {
                                            val logoutRequest = EndSessionRequest.Builder(config)
                                                .setIdTokenHint(AuthManager.authState.idToken) // 传入 idToken 告知服务器注销谁
                                                .setPostLogoutRedirectUri(android.net.Uri.parse(AuthManager.LOGOUT_REDIRECT_URI))
                                                .build()
                                            val logoutIntent = authService.getEndSessionRequestIntent(logoutRequest)
                                            logoutLauncher.launch(logoutIntent)
                                        } else {
                                            // 服务器无端点时，支持本地强制清理
                                            AuthManager.clearState(this@MainActivity)
                                            isLoggedIn = false
                                        }
                                    }
                                )
                            } else {
                                LoginScreen(
                                    onLoginClick = {
                                        val config = AuthManager.serviceConfig
                                        if (config != null) {
                                            // 构造符合 PKCE 安全标准的 Authorization Code 请求
                                            val authRequest = AuthorizationRequest.Builder(
                                                config,
                                                AuthManager.CLIENT_ID,
                                                ResponseTypeValues.CODE,
                                                android.net.Uri.parse(AuthManager.REDIRECT_URI)
                                            ).setScopes("openid", "profile", "email").build()

                                            val loginIntent = authService.getAuthorizationRequestIntent(authRequest)
                                            loginLauncher.launch(loginIntent)
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
        authService.dispose() // 释放 AppAuth 资源，防止内存泄露
    }
}

/**
 * 未登录时的 Compose 界面
 */
@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    Text(text = "密码本 App", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onLoginClick) {
        Text("使用 mksword 账号登录")
    }
}

/**
 * 登录成功后的业务主界面
 */
@Composable
fun MainAppContent(onLogoutClick: () -> Unit) {
    // 成功获取到 Token，后续可通过 AuthManager.authState.accessToken 请求业务接口
    Text(text = "欢迎回来，您已成功登录！")
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onLogoutClick) {
        Text("退出登录")
    }
}