package com.mksword.passwordbook

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mksword.passwordbook.entities.NewPasswordBookRequest
import com.mksword.passwordbook.network.PasswordBookApiClient
import kotlinx.coroutines.launch

@Composable
fun MainAppFooterDispatcher(
    isCreatingNewBook: Boolean,
    isFormValid: Boolean,
    isSubmitting: Boolean,
    currentNewBookRequest: NewPasswordBookRequest?,
    currentViewBookId: String?,
    snackbarHostState: SnackbarHostState,
    onRefreshList: () -> Unit,
    onCloseCreateMode: () -> Unit,
    onOpenCreateMode: () -> Unit,
    onCloseDetailMode: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // 1. 声明二级"新增密码项"对话框的显示隐藏状态
    var showAddEntryDialog by remember { mutableStateOf(false) }

    // 2. 当状态为 true 时，挂载并渲染对话框
    if (showAddEntryDialog && currentViewBookId != null) {
        AddPasswordEntryDialog(
            passwordBookId = currentViewBookId,
            snackbarHostState = snackbarHostState,
            onDismissRequest = { showAddEntryDialog = false },
            onConfirm = { createPasswordRequest ->
                scope.launch {
                    try {
                        // 异步调用 ApiClient 的接口创建二级密码项
                        PasswordBookApiClient.createPasswordEntry(
                            passwordBookId = currentViewBookId,
                            request = createPasswordRequest
                        )
                        showAddEntryDialog = false // 创建成功后关闭对话框
                        onRefreshList()           // 回调刷新当前列表数据
                    } catch (e: Exception) {
                        // 发生异常时展示 Snackbar 提示
                        snackbarHostState.showSnackbar(e.message ?: "添加密码项失败")
                    }
                }
            }
        )
    }

    when {
        isCreatingNewBook -> {
            NewBookFooter(
                onConfirmClick = {
                    if (isFormValid && currentNewBookRequest != null && !isSubmitting) {
                        scope.launch {
                            try {
                                PasswordBookApiClient.createPasswordBook(currentNewBookRequest)
                                onRefreshList()
                                onCloseCreateMode()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(e.message ?: "创建失败")
                            }
                        }
                    }
                },
                onCancelClick = { if (!isSubmitting) onCloseCreateMode() }
            )
        }
        currentViewBookId != null -> {
            ViewBookDetailFooter(
                onBackClick = onCloseDetailMode,
                onNewEntryClick = {
                    // 3. 点击此处将状态置为 true，完美触发上图弹窗
                    showAddEntryDialog = true
                }
            )
        }
        else -> {
            PasswordBookFooter(onNewBookClick = onOpenCreateMode)
        }
    }
}