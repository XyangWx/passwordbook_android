package com.mksword.passwordbook

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NewBookFooter(
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        // 取消按钮
        NavigationBarItem(
            selected = false,
            onClick = onCancelClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "取消"
                )
            },
            label = { Text("取消") }
        )

        // 确定按钮
        NavigationBarItem(
            selected = false,
            onClick = onConfirmClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "确定"
                )
            },
            label = { Text("确定") }
        )
    }
}