package com.mksword.passwordbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.entities.AllowedType
import com.mksword.passwordbook.entities.NewPasswordBookRequest
import kotlin.math.roundToInt

@Composable
fun CreatePasswordBookBody(
    modifier: Modifier = Modifier,
    onFormChange: (NewPasswordBookRequest, isValid: Boolean) -> Unit
) {
    // 1. 表单状态
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var minLength by remember { mutableFloatStateOf(8f) }
    var maxLength by remember { mutableFloatStateOf(20f) }
    var requireUppercase by remember { mutableStateOf(true) }
    var requireLowercase by remember { mutableStateOf(true) }
    var requireDigit by remember { mutableStateOf(true) }
    var requireSpecialChar by remember { mutableStateOf(true) }
    var specialChars by remember { mutableStateOf("") }
    // 🟢 新增：密码本类型状态，默认 General
    var allowedType by remember { mutableStateOf(AllowedType.GENERAL) }

    // 2. 联动逻辑：如果选了仅数字，自动关闭英文字母和特殊字符的开关
    LaunchedEffect(allowedType) {
        if (allowedType == AllowedType.NUMERIC_ONLY) {
            requireUppercase = false
            requireLowercase = false
            requireSpecialChar = false
            requireDigit = true
        }
    }

    // 3. 数据流出监听
    LaunchedEffect(
        name, description, minLength, maxLength,
        requireUppercase, requireLowercase, requireDigit,
        requireSpecialChar, specialChars, allowedType
    ) {
        val request = NewPasswordBookRequest(
            name = name.trim(),
            description = description.trim().ifBlank { null },
            minLength = minLength.roundToInt(),
            maxLength = maxLength.roundToInt(),
            requireUppercase = requireUppercase,
            requireLowercase = requireLowercase,
            requireDigit = requireDigit,
            requireSpecialChar = requireSpecialChar,
            specialChars = specialChars,
            allowedType = allowedType
        )
        val isValid = name.isNotBlank() && minLength <= maxLength
        onFormChange(request, isValid)
    }

    // 4. UI 渲染
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "创建新密码本",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // 基础信息
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("密码本名称 (*必填)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("描述/备注 (选填)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 🟢 新增：AllowedType 单选区域
        Text(text = "密码本类型", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AllowedType.values().forEach { type ->
                Row(
                    modifier = Modifier
                        .selectable(
                            selected = (allowedType == type),
                            onClick = { allowedType = type },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (allowedType == type),
                        onClick = { allowedType = type }
                    )
                    Text(
                        text = if (type == AllowedType.NUMERIC_ONLY) "Number Only" else "General",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(text = "安全生成策略设置", style = MaterialTheme.typography.titleMedium)

        // 长度控制
        Column {
            Text(text = "最小长度: ${minLength.roundToInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = minLength,
                onValueChange = { minLength = it },
                valueRange = 4f..32f,
                steps = 27
            )
        }

        Column {
            Text(text = "最大长度: ${maxLength.roundToInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = maxLength,
                onValueChange = { maxLength = it },
                valueRange = 4f..64f,
                steps = 59
            )
        }

        // 策略开关选项（当仅数字模式时，部分开关处于置灰或禁用状态更合理）
        FormSwitchRow(
            title = "包含大写字母",
            checked = requireUppercase,
            enabled = allowedType == AllowedType.GENERAL
        ) { requireUppercase = it }

        FormSwitchRow(
            title = "包含小写字母",
            checked = requireLowercase,
            enabled = allowedType == AllowedType.GENERAL
        ) { requireLowercase = it }

        FormSwitchRow(
            title = "包含数字",
            checked = requireDigit,
            enabled = allowedType == AllowedType.GENERAL // 纯数字模式下默认强制为 true
        ) { requireDigit = it }

        FormSwitchRow(
            title = "包含特殊字符",
            checked = requireSpecialChar,
            enabled = allowedType == AllowedType.GENERAL
        ) { requireSpecialChar = it }

        // 特殊字符指定框
        if (requireSpecialChar && allowedType == AllowedType.GENERAL) {
            OutlinedTextField(
                value = specialChars,
                onValueChange = { specialChars = it },
                label = { Text("指定特殊字符 (留空则使用默认集合)") },
                placeholder = { Text("例如: !@#$%^&*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}