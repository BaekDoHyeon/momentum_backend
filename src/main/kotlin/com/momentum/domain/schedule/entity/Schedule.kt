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
    user: User,
    title: String,
    startAt: LocalDateTime,
    endAt: LocalDateTime,
    notifyMinutes: ScheduleNotifyMinutes,   // 일정 알람
    category: ScheduleCategory,             // 일정 카테고리
    status: ScheduleStatus,                // 일정 상태
    memo: String? = null
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @Column(nullable = false)
    var title: String = title
        private set

    @Column(nullable = false)
    var startAt : LocalDateTime = startAt
        private set

    @Column(nullable = false)
    var endAt : LocalDateTime = endAt
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var notifyMinutes : ScheduleNotifyMinutes = notifyMinutes
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category : ScheduleCategory = category
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status : ScheduleStatus = status
        private set

    @Column(nullable = true)
    var memo : String? = memo
        private set

}