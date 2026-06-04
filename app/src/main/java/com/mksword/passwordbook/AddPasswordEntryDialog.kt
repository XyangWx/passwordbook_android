package com.mksword.passwordbook

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mksword.passwordbook.entities.AllowedType
import com.mksword.passwordbook.entities.CreatePasswordRequest
import com.mksword.passwordbook.entities.GetRandomPasswordRequest
import com.mksword.passwordbook.entities.GetRandomPasswordResponse
import com.mksword.passwordbook.entities.WeakLevel
import com.mksword.passwordbook.network.PasswordBookApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordEntryDialog(
    passwordBookId: String,
    snackbarHostState: SnackbarHostState,
    onDismissRequest: () -> Unit,
    onConfirm: (CreatePasswordRequest) -> Unit
) {
    val scope = rememberCoroutineScope()

    // 表单基础输入状态
    var title by remember { mutableStateOf("") }
    var hasUsername by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var remark by remember { mutableStateOf("") }

    // 密码类型菜单状态与映射
    var selectedTypeLabel by remember { mutableStateOf("纯数字") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    val typeOptions = listOf("纯数字" to 0, "数字+字母" to 1, "混合字符" to 2)

    // 密码强度菜单状态与映射
    var selectedStrengthLabel by remember { mutableStateOf("Strong") }
    var strengthMenuExpanded by remember { mutableStateOf(false) }
    val strengthOptions = listOf(
        "Weak" to WeakLevel.WEAK,
        "Medium" to WeakLevel.MEDIUM,
        "Strong" to WeakLevel.STRONG,
        "Very Strong" to WeakLevel.VERY_STRONG
    )

    // 表单动态合法性校验
    val isFormValid = title.isNotBlank() &&
            password.isNotBlank() &&
            (!hasUsername || username.isNotBlank())

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            AddPasswordEntryFormContent(
                title = title, onTitleChange = { title = it },
                hasUsername = hasUsername, onHasUsernameChange = { hasUsername = it },
                username = username, onUsernameChange = { username = it },
                password = password, onPasswordChange = { password = it },
                passwordVisible = passwordVisible, onPasswordVisibleChange = { passwordVisible = it },
                selectedTypeLabel = selectedTypeLabel, onTypeLabelChange = { selectedTypeLabel = it },
                typeMenuExpanded = typeMenuExpanded, onTypeMenuExpandChange = { typeMenuExpanded = it },
                typeOptions = typeOptions,
                selectedStrengthLabel = selectedStrengthLabel, onStrengthLabelChange = { selectedStrengthLabel = it },
                strengthMenuExpanded = strengthMenuExpanded, onStrengthMenuExpandChange = { strengthMenuExpanded = it },
                strengthOptions = strengthOptions,
                remark = remark, onRemarkChange = { remark = it },
                isFormValid = isFormValid,
                onDismissRequest = onDismissRequest,
                onGeneratePasswordClick = {
                    val targetStrengthEnum = strengthOptions.first { it.first == selectedStrengthLabel }.second
                    val targetTypeInt = typeOptions.first { it.first == selectedTypeLabel }.second
                    scope.launch {
                        try {
                            val response = PasswordBookApiClient.generateRandomPassword(
                                GetRandomPasswordRequest(
                                    passwordBookId = passwordBookId,
                                    passwordType = AllowedType.fromValue(targetTypeInt),
                                    weakLevel = targetStrengthEnum
                                )
                            )
                            password = response.password
                            passwordVisible = true
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(e.message ?: "生成密码失败")
                        }
                    }
                },
                onConfirmClick = {
                    val targetTypeInt = typeOptions.first { it.first == selectedTypeLabel }.second
                    val targetStrengthEnum = strengthOptions.first { it.first == selectedStrengthLabel }.second
                    onConfirm(
                        CreatePasswordRequest(
                            title = title,
                            hasUsername = hasUsername,
                            username = if (hasUsername) username.trim() else null,
                            passwordType = targetTypeInt,
                            weakLevel = targetStrengthEnum,
                            password = password,
                            remark = remark.takeIf { it.isNotBlank() }
                        )
                    )
                }
            )
        }
    }
}