package com.mksword.passwordbook

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mksword.passwordbook.entities.NewPasswordBookRequest
import com.mksword.passwordbook.entities.PasswordBook
import com.mksword.passwordbook.network.PasswordBookApiClient
import kotlinx.coroutines.launch

@Composable
fun MainAppContent(
    userName: String,
    passwordBooks: List<PasswordBook>,
    isListLoading: Boolean,
    isLoggingOut: Boolean = false,
    onLogoutClick: () -> Unit,
    onRefreshList: () -> Unit // 🟢 新增：供网络成功后回调父级刷新列表的方法
) {
    // 1. 业务逻辑控制状态
    var isCreatingNewBook by remember { mutableStateOf(false) }
    var currentNewBookRequest by remember { mutableStateOf<NewPasswordBookRequest?>(null) }
    var isFormValid by remember { mutableStateOf(false) }

    // 🟢 新增：网络提交中的 Loading 状态，防止重复点击
    var isSubmitting by remember { mutableStateOf(false) }

    // 2. 引入协程与全局提示组件
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            PasswordBookHeader(
                userName = userName,
                isLoggingOut = isLoggingOut,
                onLogoutClick = onLogoutClick
            )
        },
        // 🟢 将提示挂载在 Scaffold 专用的宿主槽位上
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!isCreatingNewBook) {
                PasswordBookFooter(
                    onNewBookClick = { isCreatingNewBook = true }
                )
            } else {
                NewBookFooter(
                    onConfirmClick = {
                        // 校验通过且不在提交中，才触发网络请求
                        if (isFormValid && currentNewBookRequest != null && !isSubmitting) {
                            scope.launch {
                                try {
                                    isSubmitting = true

                                    // 🟢 调用 ApiClient 发起网络请求
                                    PasswordBookApiClient.createPasswordBook(currentNewBookRequest!!)

                                    // 请求成功：刷新外部列表、重置并关闭白板
                                    onRefreshList()
                                    isCreatingNewBook = false
                                    currentNewBookRequest = null
                                } catch (e: Exception) {
                                    // 请求失败：停在原处，通过 Snackbar 弹出后端或网络抛出的具体异常
                                    snackbarHostState.showSnackbar(
                                        message = e.message ?: "创建密码本失败，请重试"
                                    )
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    },
                    onCancelClick = {
                        if (!isSubmitting) { // 正在提交时禁止取消
                            isCreatingNewBook = false
                            currentNewBookRequest = null
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (!isCreatingNewBook) {
            PasswordBookBody(
                modifier = Modifier.padding(innerPadding),
                passwordBooks = passwordBooks,
                isListLoading = isListLoading,
                onBookClick = { _ -> /* 触发进入二级明细逻辑 */ }
            )
        } else {
            CreatePasswordBookBody(
                modifier = Modifier.padding(innerPadding),
                onFormChange = { request, isValid ->
                    // 提交中时不接收表单的实时变更
                    if (!isSubmitting) {
                        currentNewBookRequest = request
                        isFormValid = isValid
                    }
                }
            )
        }
    }
}