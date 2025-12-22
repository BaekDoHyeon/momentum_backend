package com.momentum.global.dto

import com.momentum.global.enums.ErrorCode
import com.momentum.global.enums.SuccessCode

sealed interface ApiResponse<out T> {
    data class Success<T>(
        val data: T,
        val code: SuccessCode = SuccessCode.SUCCESS,
    ) : ApiResponse<T>

    data class Error(
        val code: ErrorCode,
    ) : ApiResponse<Nothing>
}