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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var startTime: LocalDateTime,

    @Column(nullable = true)
    var endTime: LocalDateTime? = null,

    @Column(nullable = false)
    var distractionCount: Long = 0,         // 앱 실행 차단 횟수

    @Column(nullable = false)
    var distractionOverrideCount: Long = 0, // 차단 무시 횟수

    ) : BaseEntity() {

}