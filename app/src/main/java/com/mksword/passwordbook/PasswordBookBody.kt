package com.mksword.passwordbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mksword.passwordbook.entities.AllowedType
import com.mksword.passwordbook.entities.PasswordBook

@Composable
fun PasswordBookBody(
    modifier: Modifier = Modifier,
    passwordBooks: List<PasswordBook>,
    isListLoading: Boolean,
    onBookClick: (String) -> Unit
) {
    if (isListLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (passwordBooks.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "暂无密码本，请点击下方新建", fontSize = 16.sp, color = Color(0xFF9E9E9E))
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().background(Color.White), // 页面大背景保持纯白
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // 精准对齐截图中的卡片外间距
        ) {
            items(passwordBooks, key = { it.id }) { book ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    // 【视觉微调】：调大 M3 卡片的自带圆角，使其边缘呈现柔和饱满的弧度
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F7FA) // ➔ 拾取截图中极淡极雅致的灰蓝色卡片底色
                    ),
                    onClick = { onBookClick(book.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 【核心复刻】：左侧精致的圆底蓝衬锁头图标
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCEFEF)), // ➔ 精准拾取截图中略带一丝丝青绿的浅蓝色柔和圆垫
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock, // 采用线条秀气的空心锁头
                                contentDescription = "安全锁",
                                tint = Color(0xFF1976D2), // ➔ 提取截图中标准的纯净海蓝色锁身
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 2. 中间文本堆叠区 (标题 + 描述 + 允许类型)
                        Column(modifier = Modifier.weight(1f)) {
                            // 主标题：如 chsi、gmail
                            Text(
                                text = book.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF212121)
                            )

                            // 描述文字：如 学信网、GMail密码本
                            if (!book.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = book.description,
                                    fontSize = 14.sp,
                                    color = Color(0xFF757575), // 采用优雅的副标题中灰色
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // 【核心复刻】：底部的加密允许类型标签 (如 General / NumericOnly)
                            val allowedTypeLabel = AllowedType.fromValue(book.allowedType).label
                            Text(
                                text = allowedTypeLabel,
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E) // 采用浅灰色细字对齐截图
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 3. 【核心修复】：将之前报错的 ChevronRight 替换为绝对安全且样式相同的右边小箭头
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "进入详情",
                            tint = Color(0xFFBDBDBD), // 保持截图中精致的浅中灰
                            modifier = Modifier.size(24.dp)) // 调整为 24.dp 视觉大小更加饱满
                    }
                }
            }
        }
    }
}