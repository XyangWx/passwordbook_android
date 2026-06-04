package com.mksword.passwordbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.entities.PasswordEntry
import com.mksword.passwordbook.network.PasswordBookApiClient
import kotlinx.coroutines.launch

@Composable
fun ViewPasswordBookDetailBody(
    passwordBookId: String,
    refreshKey: Int = 0,
    modifier: Modifier = Modifier,
    onDeleteError: (String) -> Unit = {},
    onRestoreError: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var responseData by remember(refreshKey) { mutableStateOf<com.mksword.passwordbook.entities.ViewPasswordBookResponse?>(null) }
    var isLoading by remember(refreshKey) { mutableStateOf(true) }
    var errorMessage by remember(refreshKey) { mutableStateOf<String?>(null) }
    // 用于 delete/restore 成功后触发刷新的计数器
    var actionRefreshKey by remember { mutableIntStateOf(0) }

    fun refresh() {
        actionRefreshKey++
    }

    LaunchedEffect(passwordBookId, refreshKey, actionRefreshKey) {
        isLoading = true
        errorMessage = null
        try {
            responseData = PasswordBookApiClient.viewPasswordBook(passwordBookId)
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载密码明细失败"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            responseData != null -> {
                val entries = responseData!!.passwordEntries

                if (entries.isEmpty()) {
                    Text(
                        text = "该密码本下暂无密码项\n请点击下方按钮新增",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        items(entries) { entry ->
                            PasswordEntryItem(
                                entry = entry,
                                onDeleteClick = { e ->
                                    scope.launch {
                                        try {
                                            PasswordBookApiClient.deletePasswordEntry(passwordBookId, e.id)
                                            refresh()
                                        } catch (ex: Exception) {
                                            onDeleteError(ex.message ?: "删除失败")
                                        }
                                    }
                                },
                                onRestoreClick = { e ->
                                    scope.launch {
                                        try {
                                            PasswordBookApiClient.restorePasswordEntry(passwordBookId, e.id)
                                            refresh()
                                        } catch (ex: Exception) {
                                            onRestoreError(ex.message ?: "恢复失败")
                                        }
                                    }
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}