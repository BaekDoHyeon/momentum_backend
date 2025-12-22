package com.momentum.domain.schedule.entity.enums

/**
 * 일정 알림 시간 Enum
 */
enum class ScheduleNotifyMinutes {
    NONE,       // 알림 없음
    MINUTES_5,  // 5분 전
    MINUTES_10, // 10분 전
    MINUTES_30, // 30분 전
    HOURS_1,    // 1시간 전
    HOURS_2,    // 2시간 전
    HOURS_6,    // 6시간 전
    HOURS_12,   // 12시간 전
    DAYS_1      // 1일 전
}