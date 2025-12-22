package com.momentum.domain.schedule.entity.enums

/**
 * 일정 상태 Enum
 */
enum class ScheduleStatus {
    PENDING,     // 예정
    IN_PROGRESS, // 진행중
    COMPLETED,   // 완료
    FAILED,      // 실패
}