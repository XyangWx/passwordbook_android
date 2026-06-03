package com.mksword.passwordbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 完美复刻截图样式的登录界面组件
 */
@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White // 确保全屏背景为纯白色
    ) { innerPadding ->
        // 使用 Box 容器，方便对顶部标题和居中按钮进行精确定位
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. 顶部大标题：复刻截图左上角的"登录"
            Text(
                text = "登录",
                fontSize = 32.sp, // ➔ 采用标准大标题字号
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .statusBarsPadding() // ➔ 关键：紧贴状态栏下方，完美沉浸式
                    .padding(horizontal = 24.dp, vertical = 16.dp) // 预留截图中的左边距
            )

            // 2. 屏幕中央区域
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 3. 居中按钮：复刻截图中的椭圆高饱和圆角按钮
                Button(
                    onClick = onLoginClick,
                    colors = ButtonDefaults.buttonColors(
                        // ➔ 使用 Material 3 浅色浅调的浅蓝色/灰蓝色，对齐截图按钮底色
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        // ➔ 按钮文字颜色采用深蓝色/主色，对齐截图中的"登录"
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    // 增加四周的补白，让按钮看起来呈现丰满的椭圆形
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "登录",
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        }
    }
}