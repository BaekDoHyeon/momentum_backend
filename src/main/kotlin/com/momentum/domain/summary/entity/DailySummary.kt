package com.momentum.domain.summary.entity

import com.momentum.domain.user.entity.User
import com.momentum.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "daily_summary",
    indexes = [
        Index(name = "idx_user_date", columnList = "user_id,date")
    ]
)
class DailySummary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "d_summary_id")
    val id: Long? = null,
    user: User,
    date: LocalDate,
    totalDeepworkTime: Int = 0,
    totalPlannedTime: Int = 0,
    totalScheduleCount: Int = 0,
    completeScheduleCount: Int = 0,
    deepworkSessionCount: Int = 0,
    avgDeepworkSession: Int = 0,
    isMemoir: Boolean = false,
    isStreak: Boolean = false

) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @Column(nullable = false)
    var date: LocalDate = date
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

    @Column(name = "is_memoir", nullable = false)
    var isMemoir: Boolean = isMemoir
        private set

    @Column(name = "is_streak", nullable = false)
    var isStreak: Boolean = isStreak
        private set
}
