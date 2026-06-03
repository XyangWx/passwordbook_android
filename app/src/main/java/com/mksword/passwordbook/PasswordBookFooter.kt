package com.mksword.passwordbook

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PasswordBookFooter(
    onNewBookClick: () -> Unit
) {
    Button(
        onClick = onNewBookClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        Icon(Icons.Default.Add, "新建")
        Spacer(modifier = Modifier.width(8.dp))
        Text("新建密码本", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}