package com.mksword.passwordbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mksword.passwordbook.entities.WeakLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun FormDropdownsAndButtons(
    selectedTypeLabel: String, onTypeLabelChange: (String) -> Unit,
    typeMenuExpanded: Boolean, onTypeMenuExpandChange: (Boolean) -> Unit,
    typeOptions: List<Pair<String, Int>>,
    selectedStrengthLabel: String, onStrengthLabelChange: (String) -> Unit,
    strengthMenuExpanded: Boolean, onStrengthMenuExpandChange: (Boolean) -> Unit,
    strengthOptions: List<Pair<String, WeakLevel>>,
    remark: String, onRemarkChange: (String) -> Unit,
    isFormValid: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit
) {
    // 密码类型下拉选择
    ExposedDropdownMenuBox(
        expanded = typeMenuExpanded,
        onExpandedChange = onTypeMenuExpandChange
    ) {
        OutlinedTextField(
            value = selectedTypeLabel, onValueChange = {}, readOnly = true,
            label = { Text("密码类型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { onTypeMenuExpandChange(false) }) {
            typeOptions.forEach { (label, _) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onTypeLabelChange(label); onTypeMenuExpandChange(false) })
            }
        }
    }

    // 密码强度下拉选择
    ExposedDropdownMenuBox(
        expanded = strengthMenuExpanded,
        onExpandedChange = onStrengthMenuExpandChange
    ) {
        OutlinedTextField(
            value = selectedStrengthLabel, onValueChange = {}, readOnly = true,
            label = { Text("密码强度") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strengthMenuExpanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = strengthMenuExpanded, onDismissRequest = { onStrengthMenuExpandChange(false) }) {
            strengthOptions.forEach { (label, _) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onStrengthLabelChange(label); onStrengthMenuExpandChange(false) })
            }
        }
    }

    // 备注
    OutlinedTextField(
        value = remark, onValueChange = onRemarkChange,
        label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), maxLines = 3
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 底部控制按钮
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismissRequest) { Text("取消") }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onConfirmClick, enabled = isFormValid) { Text("创建") }
    }
}