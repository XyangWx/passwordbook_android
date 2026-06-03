package com.mksword.passwordbook

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.entities.PasswordBook
import com.mksword.passwordbook.network.PasswordBookApiClient
import kotlinx.coroutines.launch

/**
 * 1. 独立出来的底部单条目操作菜单
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordBookActionBottomSheet(
    showBottomSheet: Boolean,
    sheetState: SheetState,
    selectedBookName: String,
    onDismiss: () -> Unit,
    onViewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "密码本: $selectedBookName",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("查看密码本", style = MaterialTheme.typography.bodyLarge) },
                    selected = false,
                    onClick = onViewClick,
                    icon = { Icon(Icons.Default.Visibility, contentDescription = "查看", tint = MaterialTheme.colorScheme.primary) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("删除密码本", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = onDeleteClick,
                    icon = { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    }
}