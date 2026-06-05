package com.mksword.passwordbook

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.mksword.passwordbook.entities.PasswordBook
import com.mksword.passwordbook.network.PasswordBookApiClient
import kotlinx.coroutines.launch

/**
 * 密码本删除二次确认对话框组件
 *
 * @param showDialog 是否展示对话框
 * @param selectedBook 当前选中的密码本实体对象
 * @param snackbarHostState 用于在删除失败时向界面弹出全局报错提示
 * @param onDismiss 点击取消或点击空白处关闭对话框的回调
 * @param onDeleteSuccess 异步网络删除成功后的回调（通常用于刷新列表和清空选中状态）
 */
@Composable
fun PasswordBookDeleteDialog(
    showDialog: Boolean,
    selectedBook: PasswordBook?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()

    if (showDialog && selectedBook != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("确认删除") },
            text = {
                Text("您确定要删除密码本 \"${selectedBook.name}\" 吗？此操作将永久抹除该密码本下的所有密码记录，且不可恢复！")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                // 调用 apiClient 执行后台安全物理删除
                                PasswordBookApiClient.deletePasswordBook(selectedBook.id)
                                onDeleteSuccess()
                            } catch (e: Exception) {
                                // 失败时停在原处，弹窗警告
                                snackbarHostState.showSnackbar(e.message ?: "删除失败")
                            }
                        }
                    }
                ) {
                    Text("确定删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
}