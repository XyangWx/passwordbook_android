package com.mksword.passwordbook

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
    Scaffold(
        topBar = {
            PasswordBookHeader(
                userName = userName,
                isLoggingOut = isLoggingOut,
                onLogoutClick = onLogoutClick
            )
        },
        bottomBar = {
            PasswordBookFooter(
                onNewBookClick = { /* 触发新建逻辑 */ }
            )
        }
    ) { innerPadding ->
        PasswordBookBody(
            modifier = Modifier.padding(innerPadding),
            passwordBooks = passwordBooks,
            isListLoading = isListLoading,
            onBookClick = { bookId -> /* 触发进入二级明细逻辑 */ }
        )
    }
}