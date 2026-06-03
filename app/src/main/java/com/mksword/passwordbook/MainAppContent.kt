package com.mksword.passwordbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mksword.passwordbook.entities.PasswordBook

@Composable
fun MainAppContent(
    userName: String,
    passwordBooks: List<PasswordBook>,
    isListLoading: Boolean,
    isLoggingOut: Boolean = false,
    onLogoutClick: () -> Unit
) {
    // 1. 引入状态：是否处于新建模式
    var isCreatingNewBook by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PasswordBookHeader(
                userName = userName,
                isLoggingOut = isLoggingOut,
                onLogoutClick = onLogoutClick
            )
        },
        bottomBar = {
            // 2. 根据状态条件渲染底部按钮
            if (!isCreatingNewBook) {
                PasswordBookFooter(
                    onNewBookClick = { isCreatingNewBook = true }
                )
            } else {
                NewBookFooter(
                    onConfirmClick = {
                        // 执行保存逻辑
                        isCreatingNewBook = false
                    },
                    onCancelClick = {
                        isCreatingNewBook = false
                    }
                )
            }
        }
    ) { innerPadding ->
        // 3. 根据状态条件渲染主体内容
        if (!isCreatingNewBook) {
            PasswordBookBody(
                modifier = Modifier.padding(innerPadding),
                passwordBooks = passwordBooks,
                isListLoading = isListLoading,
                onBookClick = { bookId -> /* 触发进入二级明细逻辑 */ }
            )
        } else {
            // 新建状态下的白板内容（可在内部放置输入框）
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // 暂时留空作为白板
            }
        }
    }
}