package com.mksword.passwordbook

import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mksword.passwordbook.entities.PasswordBook
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

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    Button(onClick = onLoginClick) { Text("登录") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(userName: String, isLoggingOut: Boolean = false, onLogoutClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val passwordBooks = remember {
        listOf(
            PasswordBook(
                id = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                name = "Social Life",
                description = "日常社交与生活",
                allowedType = 1,
                entryCount = 5
            ),
            PasswordBook(
                id = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                name = "Dev Credentials",
                description = "开发与服务器凭证",
                allowedType = 1,
                entryCount = 14
            ),
            PasswordBook(
                id = "a3d88e91-1b2f-4c3d-9e5f-6a7b8c9d0e1f",
                name = "Finance Accounts",
                description = "金融与银行账户",
                allowedType = 0,
                entryCount = 3
            )
        )
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF8BC34A))
                .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text("密码本", style = MaterialTheme.typography.titleLarge)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                    actions = {
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                            TextButton(
                                onClick = { if (!isLoggingOut) expanded = true },
                                enabled = !isLoggingOut,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "用户头像",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = userName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "下拉箭头",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                offset = DpOffset(x = 12.dp, y = 4.dp),
                                properties = androidx.compose.ui.window.PopupProperties(
                                    focusable = true,
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true
                                )
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                                contentDescription = "注销图标",
                                                tint = Color.Red,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isLoggingOut) "正在注销..." else "注销登录",
                                                color = Color.Red,
                                                fontWeight = FontWeight.W500
                                            )
                                        }
                                    },
                                    onClick = {
                                        expanded = false
                                        onLogoutClick()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(passwordBooks, key = { it.id }) { book ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    onClick = {}
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "密码本图标",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.description ?: "暂无描述",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (book.allowedType == 0) "Number Only" else "General",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "进入",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    },
    persistentFooterButtons = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("新建密码本", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
