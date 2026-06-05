package com.mksword.passwordbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.entities.WeakLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordEntryFormContent(
    title: String, onTitleChange: (String) -> Unit,
    hasUsername: Boolean, onHasUsernameChange: (Boolean) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean, onPasswordVisibleChange: (Boolean) -> Unit,
    selectedTypeLabel: String, onTypeLabelChange: (String) -> Unit,
    typeMenuExpanded: Boolean, onTypeMenuExpandChange: (Boolean) -> Unit,
    typeOptions: List<Pair<String, Int>>,
    selectedStrengthLabel: String, onStrengthLabelChange: (String) -> Unit,
    strengthMenuExpanded: Boolean, onStrengthMenuExpandChange: (Boolean) -> Unit,
    strengthOptions: List<Pair<String, WeakLevel>>,
    remark: String, onRemarkChange: (String) -> Unit,
    isFormValid: Boolean,
    onDismissRequest: () -> Unit,
    onGeneratePasswordClick: () -> Unit,
    onConfirmClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "新增密码项", style = MaterialTheme.typography.titleLarge)

        // 标题输入框
        OutlinedTextField(
            value = title, onValueChange = onTitleChange,
            label = { Text("标题 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        // 是否需要账号开关整行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "需要用户名/账号")
            Switch(checked = hasUsername, onCheckedChange = onHasUsernameChange)
        }

        // 账号联动显隐
        if (hasUsername) {
            OutlinedTextField(
                value = username, onValueChange = onUsernameChange,
                label = { Text("用户名/账号 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }

        // 密码输入框与动作图标
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("密码 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row {
                    IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "显隐"
                        )
                    }
                    // 绑定生成事件
                    IconButton(onClick = onGeneratePasswordClick) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "生成", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )

        FormDropdownsAndButtons(
            selectedTypeLabel = selectedTypeLabel, onTypeLabelChange = onTypeLabelChange,
            typeMenuExpanded = typeMenuExpanded, onTypeMenuExpandChange = onTypeMenuExpandChange,
            typeOptions = typeOptions,
            selectedStrengthLabel = selectedStrengthLabel, onStrengthLabelChange = onStrengthLabelChange,
            strengthMenuExpanded = strengthMenuExpanded, onStrengthMenuExpandChange = onStrengthMenuExpandChange,
            strengthOptions = strengthOptions,
            remark = remark, onRemarkChange = onRemarkChange,
            isFormValid = isFormValid,
            onDismissRequest = onDismissRequest,
            onConfirmClick = onConfirmClick
        )
    }
}