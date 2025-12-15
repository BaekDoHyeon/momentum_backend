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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var date: LocalDate,

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

    @Column(name = "is_memoir", nullable = false)
    var isMemoir: Boolean = false,

    @Column(name = "is_streak", nullable = false)
    var isStreak: Boolean = false

) : BaseEntity()
