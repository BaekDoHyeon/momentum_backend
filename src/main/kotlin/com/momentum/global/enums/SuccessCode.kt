package com.momentum.global.enums

enum class SuccessCode(
    val code: String,
    val message: String,
) {
    // 일반 성공
    SUCCESS("S001", "요청에 성공하였습니다."),

    // CRUD 작업
    CREATE("S002", "성공적으로 추가되었습니다."),
    READ("S003", "성공적으로 조회되었습니다."),
    UPDATE("S004", "성공적으로 수정되었습니다."),
    DELETE("S005", "성공적으로 삭제되었습니다."),
}
