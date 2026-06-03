package com.mksword.passwordbook

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(passwordBooks, key = { it.id }) { book ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = { onBookClick(book.id) }
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, "密码本", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = book.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (!book.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = book.description!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            if (!book.creationTime.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "创建于: ${book.creationTime}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                            Text(text = "${book.entryCount} 个密码", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}