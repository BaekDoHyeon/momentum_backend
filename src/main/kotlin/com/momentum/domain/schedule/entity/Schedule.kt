package com.momentum.domain.schedule.entity

import com.momentum.domain.schedule.entity.enums.ScheduleCategory
import com.momentum.domain.schedule.entity.enums.ScheduleNotifyMinutes
import com.momentum.domain.schedule.entity.enums.ScheduleStatus
import com.momentum.domain.user.entity.User
import com.momentum.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "schedule")
class Schedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var startAt: LocalDateTime,

    @Column(nullable = false)
    var endAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var notifyMinutes: ScheduleNotifyMinutes,   // 일정 알람

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: ScheduleCategory,             // 일정 카테고리

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ScheduleStatus,                // 일정 상태

    @Column(nullable = true)
    var memo: String? = null
) : BaseEntity()