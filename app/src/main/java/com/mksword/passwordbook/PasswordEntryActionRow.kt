package com.mksword.passwordbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.entities.PasswordEntry

@Composable
fun PasswordEntryActionRow(
    entry: PasswordEntry,
    onDeleteClick: (PasswordEntry) -> Unit,
    onRestoreClick: (PasswordEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        if (!entry.isDeleted) {
            // 状态 1：正常的橙黄色删除字样
            Row(
                modifier = Modifier.clickable { onDeleteClick(entry) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "删除",
                    tint = Color(0xFFFFA500),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(text = "删除", color = Color(0xFFFFA500), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            // 状态 2：红绿相间的"已删除+恢复"控制框
            Row(
                modifier = Modifier
                    .border(BorderStroke(1.dp, Color(0xFFFFD2D2)), RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFF5F5))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "已删除",
                        tint = Color(0xFFE57373),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(text = "已删除", color = Color(0xFFE57373), style = MaterialTheme.typography.bodyMedium)
                }
                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFFFFD2D2)))
                Row(
                    modifier = Modifier.clickable { onRestoreClick(entry) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "恢复",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(text = "恢复", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}