package com.mksword.passwordbook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.entities.PasswordEntry

@Composable
fun PasswordEntryItem(
    entry: PasswordEntry,
    modifier: Modifier = Modifier,
    onDeleteClick: (PasswordEntry) -> Unit = {},
    onRestoreClick: (PasswordEntry) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 头部摘要行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "密码项",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
                    val accountText = if (entry.hasUsername && !entry.username.isNullOrBlank()) {
                        "账号: ${entry.username}"
                    } else { "仅限匿名凭证" }
                    Text(text = accountText, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开"
                )
            }

            // 展开后的隐藏操作区
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // 🟢 引入拆分出的：删除与恢复状态控制行
                    PasswordEntryActionRow(
                        entry = entry,
                        onDeleteClick = onDeleteClick,
                        onRestoreClick = onRestoreClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🟢 引入拆分出的：密码明文与无损拷贝行
                    PasswordEntryPasswordRow(
                        currentPassword = entry.currentPassword ?: ""
                    )
                }
            }
        }
    }
}