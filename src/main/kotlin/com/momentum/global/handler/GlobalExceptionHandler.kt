package com.momentum.global.handler

import com.momentum.global.dto.ApiResponse
import com.momentum.global.exception.BusinessException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse.Error> {
        val response = ApiResponse.Error(
            code = e.errorCode,
        )

        return ResponseEntity
            .status(e.errorCode.httpStatus)
            .body(response)
    }
}