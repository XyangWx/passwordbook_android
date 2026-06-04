package com.mksword.passwordbook.entities

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// 1. 业务核心枚举类 (Enums)
// ==========================================

enum class AllowedType(val value: Int, val label: String) {
    NUMERIC_ONLY(0, "NumericOnly"),
    GENERAL(1, "General");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: GENERAL
    }
}

enum class WeakLevel(val value: Int, val label: String) {
    VERY_WEAK(0, "VeryWeak"),
    WEAK(1, "Weak"),
    MEDIUM(2, "Medium"),
    STRONG(3, "Strong"),
    VERY_STRONG(4, "VeryStrong");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: STRONG
    }
}

// ==========================================
// 2. 核心数据模型 (Data Models)
// ==========================================

@Serializable
data class PasswordHistory(
    val id: String = "",
    val passwordEntryId: String = "",
    val passwordValue: String = "",
    val isCurrent: Boolean = false,
    val creationTime: String? = null
)

@Serializable
data class PasswordEntry(
    val id: String = "",
    val passwordBookId: String = "",
    val title: String = "",
    val hasUsername: Boolean = false,
    val username: String? = null,
    val passwordType: Int = 1,
    val weakLevel: Int? = null,
    val remark: String? = null,
    val currentPassword: String? = null,
    val isDeleted: Boolean = false,
    val creationTime: String? = null,
    val lastModificationTime: String? = null,
    val passwordHistories: List<PasswordHistory> = emptyList()
)

@Serializable
data class PasswordBook(
    val id: String = "",
    val ownerId: String? = null,
    val name: String = "",
    val description: String? = null,
    val allowedType: Int = 1,
    val minLength: Int = 8,
    val maxLength: Int = 20,
    val creationTime: String? = null,
    val lastModificationTime: String? = null,
    val isDeleted: Boolean = false,
    val entryCount: Int = 0,
    val passwordEntries: List<PasswordEntry> = emptyList()
)

// ==========================================
// 3. 网络响应实体模型 (Response Models)
// ==========================================

@Serializable
data class PasswordBookListResponse(
    val items: List<PasswordBook> = emptyList()
)

@Serializable
data class GetRandomPasswordResponse(
    val password: String = ""
)

// 对应别名：typedef ViewPasswordBookResponse = PasswordBook;
typealias ViewPasswordBookResponse = PasswordBook

// ==========================================
// 4. 网络请求提交模型 (Request Models)
// ==========================================

@Serializable
data class NewPasswordBookRequest(
    val name: String,
    val description: String? = null,
    val minLength: Int = 8,
    val maxLength: Int = 20,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
    val requireSpecialChar: Boolean = true,
    val specialChars: String = "",
    // 在传输时，通过自定义序列化或Int值传递
    private val allowedTypeValue: Int = AllowedType.GENERAL.value
) {
    // 供业务代码便捷调用的强类型扩展属性
    val allowedType: AllowedType
        get() = AllowedType.fromValue(allowedTypeValue)

    // 辅助次级构造函数，方便您在 Compose 页面中像 Flutter 构造请求一样快捷传参
    constructor(
        name: String,
        description: String? = null,
        minLength: Int = 8,
        maxLength: Int = 20,
        requireUppercase: Boolean = true,
        requireLowercase: Boolean = true,
        requireDigit: Boolean = true,
        requireSpecialChar: Boolean = true,
        specialChars: String = "",
        allowedType: AllowedType = AllowedType.GENERAL
    ) : this(
        name, description, minLength, maxLength, requireUppercase,
        requireLowercase, requireDigit, requireSpecialChar, specialChars, allowedType.value
    )
}

@Serializable
data class GetRandomPasswordRequest(
    val passwordBookId: String,
    val minLength: Int = 8,
    val maxLength: Int = 20,
    private val passwordTypeValue: Int = AllowedType.GENERAL.value,
    private val weakLevelValue: Int = WeakLevel.STRONG.value
) {
    val passwordType: AllowedType get() = AllowedType.fromValue(passwordTypeValue)
    val weakLevel: WeakLevel get() = WeakLevel.fromValue(weakLevelValue)

    constructor(
        passwordBookId: String,
        minLength: Int = 8,
        maxLength: Int = 20,
        passwordType: AllowedType = AllowedType.GENERAL,
        weakLevel: WeakLevel = WeakLevel.STRONG
    ) : this(passwordBookId, minLength, maxLength, passwordType.value, weakLevel.value)
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreatePasswordRequest(
    val title: String,
    @EncodeDefault
    val hasUsername: Boolean = true,
    val username: String? = null,
    @EncodeDefault
    val passwordType: Int = 0,
    private val weakLevelValue: Int = WeakLevel.VERY_STRONG.value,
    val password: String,
    val remark: String? = null
) {
    val weakLevel: WeakLevel get() = WeakLevel.fromValue(weakLevelValue)

    constructor(
        title: String,
        hasUsername: Boolean = true,
        username: String? = null,
        passwordType: Int = 0,
        weakLevel: WeakLevel = WeakLevel.VERY_STRONG,
        password: String,
        remark: String? = null
    ) : this(
        title = title,
        hasUsername = hasUsername,
        username = username,
        passwordType = passwordType,
        weakLevelValue = weakLevel.value,
        password = password,
        remark = remark
    )
}