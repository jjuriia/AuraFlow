package com.auraflow.domain.auth

object CredentialsValidator {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun name(value: String): String? {
        val length = value.trim().length
        return when {
            length < 2 -> "Минимум 2 символа"
            length > 40 -> "Не длиннее 40 символов"
            else -> null
        }
    }

    fun email(value: String): String? =
        if (EMAIL.matches(value.trim())) null else "Проверьте адрес, например name@mail.ru"

    fun password(value: String): String? = when {
        value.length < 8 -> "Минимум 8 символов"
        value.none { it.isLetter() } || value.none { it.isDigit() } -> "Нужны и буквы, и цифры"
        else -> null
    }

    fun confirmation(password: String, confirmation: String): String? =
        if (password == confirmation) null else "Пароли не совпадают"
}
