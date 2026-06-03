package com.mksword.passwordbook

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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

    when {
        isCreatingNewBook -> {
            NewBookFooter(
                onConfirmClick = {
                    if (isFormValid && currentNewBookRequest != null && !isSubmitting) {
                        scope.launch {
                            try {
                                // 执行网络请求并回调通知
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
                onNewEntryClick = { /* 触发二级新增逻辑 */ }
            )
        }
        else -> {
            PasswordBookFooter(onNewBookClick = onOpenCreateMode)
        }
    }
}