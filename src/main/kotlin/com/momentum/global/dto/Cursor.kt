package com.momentum.global.dto

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * 커서 기반 페이지네이션을 위한 복합 커서 값 객체
 *
 * ID와 Timestamp를 조합하여 안정적인 정렬 및 커서 위치를 보장합니다.
 *
 * @property lastId 마지막으로 조회된 엔티티의 ID
 * @property lastTimestamp 마지막으로 조회된 엔티티의 생성/수정 시각
 */
data class Cursor(
    val lastId: Long,
    val lastTimestamp: LocalDateTime,
) {
    /**
     * 커서를 Base64로 인코딩하여 URL-safe 문자열로 변환
     *
     * 형식: "lastId:timestamp" → Base64
     * 예시: "123:2025-12-23T10:30:00" → "MTIzOjIwMjUtMTItMjNUMTA6MzA6MDA="
     *
     * @return Base64로 인코딩된 커서 문자열
     */
    fun encode(): String {
        val timestamp = lastTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val raw = "$lastId:$timestamp"
        return Base64.getUrlEncoder().encodeToString(raw.toByteArray())
    }

    companion object {
        /**
         * Base64로 인코딩된 문자열을 Cursor 객체로 디코딩
         *
         * 잘못된 형식이거나 파싱에 실패하면 null을 반환합니다.
         *
         * @param encoded Base64로 인코딩된 커서 문자열
         * @return 디코딩된 Cursor 객체, 형식이 잘못된 경우 null
         */
        fun decode(encoded: String): Cursor? {
            return try {
                val decoded = String(Base64.getUrlDecoder().decode(encoded))
                val parts = decoded.split(":", limit = 2)

                if (parts.size != 2) return null

                val lastId = parts[0].toLongOrNull() ?: return null
                val lastTimestamp = LocalDateTime.parse(
                    parts[1],
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                )

                Cursor(lastId, lastTimestamp)
            } catch (e: Exception) {
                null
            }
        }
    }
}
