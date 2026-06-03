package com.mksword.passwordbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun PasswordBookFooter(
    onNewBookClick: () -> Unit
) {
    // 1. 最外层控制容器：确保底栏完全充满宽度，并且优雅地避开手机最底部的系统导航黑条（全面屏手势指示线）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White) // 整体外层底色保持纯白
            .navigationBarsPadding() // ➔ 关键：防止全面屏手势黑条把按钮文案死死挡住
    ) {
        // 2. 核心按钮实体：完美复刻截图中的深蓝色、紧凑高度与字形
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp) // 精准对齐截图中的 Footer 视觉高度
                .background(Color(0xFF126180)) // ➔ 精准拾取截图中的深蓝色 Hex 颜色值
                .clickable { onNewBookClick() }, // 让整个深蓝区域具备原生的点击水波纹反馈
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // 白色加号图标
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建图标",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // 白色加粗文案
                Text(
                    text = "新建密码本",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}