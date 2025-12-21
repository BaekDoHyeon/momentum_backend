package com.momentum.domain.deepwork.entity

import com.momentum.domain.user.entity.User
import com.momentum.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "deep_work_session")
class DeepWorkSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    user: User,
    startTime: LocalDateTime,
    endTime: LocalDateTime? = null,
    distractionCount: Long = 0,
    distractionOverrideCount: Long = 0

) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @Column(nullable = false)
    var startTime: LocalDateTime = startTime
        private set

    @Column(nullable = true)
    var endTime: LocalDateTime? = endTime
        private set

    @Column(nullable = false)
    var distractionCount: Long = distractionCount         // 앱 실행 차단 횟수
        private set

    @Column(nullable = false)
    var distractionOverrideCount: Long = distractionOverrideCount // 차단 무시 횟수
        private set
}