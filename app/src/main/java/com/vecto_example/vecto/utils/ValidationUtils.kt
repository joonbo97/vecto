package com.vecto_example.vecto.utils

object ValidationUtils {
    /*   유효성 검사   */

    enum class ValidationResult {
        VALID, EMPTY, INVALID_FORMAT
    }

    fun isValidId(id: String): ValidationResult{
        /*   아이디 형식 체크   */

        val idRegex = Regex("^[a-zA-Z0-9]{4,20}$")

        return when{
            id.isEmpty() -> ValidationResult.EMPTY
            !idRegex.matches(id) -> ValidationResult.INVALID_FORMAT
            else -> ValidationResult.VALID
        }

    }

    fun isValidNickname(nickname: String): ValidationResult{
        /*   닉네임 형식 체크   */

        return when{
            nickname.isEmpty() -> ValidationResult.EMPTY
            nickname.length > 10 -> ValidationResult.INVALID_FORMAT
            else -> ValidationResult.VALID
        }
    }

    fun isValidPw(pw: String): ValidationResult{
        /*   비밀번호 형식 체크   */

        val pwRegex = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$")

        return when{
            pw.isEmpty() -> ValidationResult.EMPTY
            !pwRegex.matches(pw) -> ValidationResult.INVALID_FORMAT
            else -> ValidationResult.VALID
        }
    }

    fun isValidEmail(email: String): ValidationResult{
        /*   이메일 형식 체크   */

        val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}")

        return when{
            email.isEmpty() -> ValidationResult.EMPTY
            !emailRegex.matches(email) -> ValidationResult.INVALID_FORMAT
            else -> ValidationResult.VALID
        }
    }

    fun isValidEmailVerify(code: String): ValidationResult{
        /*   이메일 인증 체크   */

        return when{
            code.isEmpty() -> ValidationResult.EMPTY
            else -> ValidationResult.VALID
        }
    }

}