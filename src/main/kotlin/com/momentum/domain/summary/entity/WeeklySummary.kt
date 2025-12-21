package com.momentum.domain.summary.entity

import com.momentum.domain.schedule.entity.enums.ScheduleCategory
import com.momentum.domain.user.entity.User
import com.momentum.global.entity.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

@Entity
@Table(
    name = "weekly_summary",
    indexes = [
        Index(name = "idx_user_week", columnList = "user_id,week_start_date")
    ]
)
class WeeklySummary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "w_summary_id")
    val id: Long? = null,
    user: User,
    weekStartDate: LocalDate,
    weekEndDate: LocalDate,
    totalDeepworkTime: Int = 0,
    totalPlannedTime: Int = 0,
    totalScheduleCount: Int = 0,
    completeScheduleCount: Int = 0,
    deepworkSessionCount: Int = 0,
    avgDeepworkSession: Int = 0,
    memoirCount: Int = 0,
    activeDays: Int = 0,
    streakDays: Int = 0,
    mostProductiveDay: DayOfWeek,
    mostCompletedCategory: ScheduleCategory,
    leastCompletedCategory: ScheduleCategory,
    prevWeekDeepworkTime: Int = 0,
    growthRate: BigDecimal = BigDecimal.ZERO

) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @Column(name = "week_start_date", nullable = false)
    var weekStartDate: LocalDate = weekStartDate
        private set

    @Column(name = "week_end_date", nullable = false)
    var weekEndDate: LocalDate = weekEndDate
        private set

    @Column(name = "total_deepwork_time", nullable = false)
    var totalDeepworkTime: Int = totalDeepworkTime
        private set

    @Column(name = "total_planned_time", nullable = false)
    var totalPlannedTime: Int = totalPlannedTime
        private set

    @Column(name = "total_schedule_count", nullable = false)
    var totalScheduleCount: Int = totalScheduleCount
        private set

    @Column(name = "complete_schedule_count", nullable = false)
    var completeScheduleCount: Int = completeScheduleCount
        private set

    @Column(name = "deepwork_session_count", nullable = false)
    var deepworkSessionCount: Int = deepworkSessionCount
        private set

    @Column(name = "avg_deepwork_session", nullable = false)
    var avgDeepworkSession: Int = avgDeepworkSession
        private set

    @Column(name = "memoir_count", nullable = false)
    var memoirCount: Int = memoirCount
        private set

    @Column(name = "active_days", nullable = false)
    var activeDays: Int = activeDays
        private set

    @Column(name = "streak_days", nullable = false)
    var streakDays: Int = streakDays
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "most_productive_day", nullable = false, length = 10)
    var mostProductiveDay: DayOfWeek = mostProductiveDay
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "most_completed_category", nullable = false, length = 50)
    var mostCompletedCategory: ScheduleCategory = mostCompletedCategory
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "least_completed_category", nullable = false, length = 50)
    var leastCompletedCategory: ScheduleCategory = leastCompletedCategory
        private set

    @Column(name = "prev_week_deepwork_time", nullable = false)
    var prevWeekDeepworkTime: Int = prevWeekDeepworkTime
        private set

    @Column(name = "growth_rate", nullable = false, precision = 5, scale = 2)
    var growthRate: BigDecimal = growthRate
        private set
}
