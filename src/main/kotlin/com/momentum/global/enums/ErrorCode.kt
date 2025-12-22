package com.momentum.global.enums

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus,
) {
    // 인증/인가 (A: Authentication)
    INVALID_TOKEN("A001", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("A002", "만료된 토큰입니다", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("A003", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("A004", "권한이 없습니다", HttpStatus.FORBIDDEN),

    // 사용자 (U: User)
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("U002", "이미 존재하는 이메일입니다", HttpStatus.CONFLICT),
    INVALID_PASSWORD("U003", "비밀번호가 일치하지 않습니다", HttpStatus.BAD_REQUEST),

    // 요청 검증 (V: Validation)
    INVALID_INPUT("V001", "입력값이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("V002", "필수 항목이 누락되었습니다", HttpStatus.BAD_REQUEST),

    // 리소스 (R: Resource)
    RESOURCE_NOT_FOUND("R001", "요청한 리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    RESOURCE_ALREADY_EXISTS("R002", "이미 존재하는 리소스입니다", HttpStatus.CONFLICT),

    // 서버 오류 (E: Error)
    INTERNAL_SERVER_ERROR("E999", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR("E998", "데이터베이스 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_API_ERROR("E997", "외부 API 호출 중 오류가 발생했습니다", HttpStatus.BAD_GATEWAY)
}