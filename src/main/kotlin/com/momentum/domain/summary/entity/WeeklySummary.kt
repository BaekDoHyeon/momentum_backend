package com.momentum.domain.summary.entity

import com.momentum.domain.user.entity.User
import com.momentum.global.entity.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "week_start_date", nullable = false)
    var weekStartDate: LocalDate,

    @Column(name = "week_end_date", nullable = false)
    var weekEndDate: LocalDate,

    @Column(name = "total_deepwork_time", nullable = false)
    var totalDeepworkTime: Int = 0,

    @Column(name = "total_planned_time", nullable = false)
    var totalPlannedTime: Int = 0,

    @Column(name = "total_schedule_count", nullable = false)
    var totalScheduleCount: Int = 0,

    @Column(name = "complete_schedule_count", nullable = false)
    var completeScheduleCount: Int = 0,

    @Column(name = "deepwork_session_count", nullable = false)
    var deepworkSessionCount: Int = 0,

    @Column(name = "avg_deepwork_session", nullable = false)
    var avgDeepworkSession: Int = 0,

    @Column(name = "memoir_count", nullable = false)
    var memoirCount: Int = 0,

    @Column(name = "active_days", nullable = false)
    var activeDays: Int = 0,

    @Column(name = "streak_days", nullable = false)
    var streakDays: Int = 0,

    // TODO : ENUM 으로 관리
    @Column(name = "most_productive_day", nullable = false, length = 10)
    var mostProductiveDay: String,

    // TODO : Category 는 ENUM 으로 관리
    @Column(name = "most_completed_category", nullable = false, length = 50)
    var mostCompletedCategory: String,

    // TODO : Category 는 ENUM 으로 관리
    @Column(name = "least_completed_category", nullable = false, length = 50)
    var leastCompletedCategory: String,

    @Column(name = "prev_week_deepwork_time", nullable = false)
    var prevWeekDeepworkTime: Int = 0,

    @Column(name = "growth_rate", nullable = false, precision = 5, scale = 2)
    var growthRate: BigDecimal = BigDecimal.ZERO

) : BaseEntity()
