package com.mksword.passwordbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 二级明细页面专用的底部栏组件
 *
 * @param onBackClick 点击返回主列表的回调
 * @param onNewEntryClick 点击在当前密码本下新增密码项的回调
 */
@Composable
fun ViewBookDetailFooter(
    onBackClick: () -> Unit,
    onNewEntryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 左侧：返回列表按钮
        OutlinedButton(
            onClick = onBackClick
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回密码本"
            )
            Text(text = "返回", modifier = Modifier.padding(start = 4.dp))
        }

        // 右侧：新增密码项主按钮
        Button(
            onClick = onNewEntryClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新增密码项"
            )
            Text(text = "在该密码本下新增密码项", modifier = Modifier.padding(start = 4.dp))
        }
    }
}