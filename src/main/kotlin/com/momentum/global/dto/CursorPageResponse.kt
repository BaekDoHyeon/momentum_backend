package com.momentum.global.dto

import com.momentum.global.enums.ErrorCode
import com.momentum.global.exception.BusinessException

/**
 * 커서 기반 페이지네이션 응답 클래스
 *
 * 무한 스크롤을 위한 페이지네이션 응답을 표현합니다.
 * size+1 조회 패턴을 통해 hasNext를 자동으로 계산합니다.
 *
 * @param T 응답 데이터 타입 (Generic)
 * @property content 실제 데이터 리스트
 * @property hasNext 다음 페이지 존재 여부
 * @property nextCursor 다음 요청에 사용할 커서 (Base64 인코딩된 문자열)
 * @property size 실제 반환된 데이터 개수
 */
data class CursorPageResponse<T>(
    val content: List<T>,
    val hasNext: Boolean,
    val nextCursor: String?,
    val size: Int,
) {
    companion object {
        /**
         * 페이지네이션 응답을 생성하는 팩토리 메서드
         *
         * size+1 조회 패턴을 사용하여 hasNext와 nextCursor를 자동으로 계산합니다.
         * - content.size > requestedSize: hasNext=true, 마지막 요소 제거
         * - content.size <= requestedSize: hasNext=false, 모든 요소 반환
         *
         * @param contentWithOneExtra size+1로 조회된 데이터 리스트 (반드시 requestedSize+1 이하)
         * @param requestedSize 클라이언트가 요청한 페이지 크기
         * @param cursorExtractor content의 마지막 요소에서 Cursor를 추출하는 람다 함수
         * @return 생성된 CursorPageResponse 객체
         * @throws BusinessException contentWithOneExtra 크기가 requestedSize+1을 초과하는 경우
         */
        fun <T> of(
            contentWithOneExtra: List<T>,
            requestedSize: Int,
            cursorExtractor: (T) -> Cursor, // 고차 함수(High-Order Function) -> 자바로 표현하면 Function<T, Cursor> cursorExtractor (T 타입 객체를 받아서 Cursor 타입 객체를 반환하는 함수)
        ): CursorPageResponse<T> {
            // Validation: contentWithOneExtra 크기가 requestedSize+1을 초과하면 안됨
            if (contentWithOneExtra.size > requestedSize + 1) {
                throw BusinessException(ErrorCode.INVALID_CURSOR_CONTENT_SIZE)
            }

            // size+1 조회 패턴: hasNext 판단용
            val hasNext = contentWithOneExtra.size > requestedSize
            val actualContent = if (hasNext) {
                contentWithOneExtra.dropLast(1)  // 마지막 요소 제거 (hasNext 판단용으로만 사용)
            } else {
                contentWithOneExtra
            }

            // 다음 페이지가 있고 실제 컨텐츠가 비어있지 않으면 nextCursor 생성
            val nextCursor = if (hasNext && actualContent.isNotEmpty()) {
                cursorExtractor(actualContent.last()).encode()
            } else {
                null
            }

            return CursorPageResponse(
                content = actualContent,
                hasNext = hasNext,
                nextCursor = nextCursor,
                size = actualContent.size
            )
        }
    }
}
