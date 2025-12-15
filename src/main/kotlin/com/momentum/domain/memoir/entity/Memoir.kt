package com.momentum.domain.memoir.entity

import com.momentum.domain.memoir.entity.enums.Concentration
import com.momentum.domain.memoir.entity.enums.Satisfaction
import com.momentum.domain.user.entity.User
import com.momentum.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "memoir")
class Memoir(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var satisfaction: Satisfaction,     // 만족도

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var concentration: Concentration,   // 집중도

    @Column(nullable = true)
    var achievement: String? = null,           // 잘한점

    @Column(nullable = true)
    var improvement: String? = null,           // 부족한점

    @Column(nullable = true)
    var memo: String? = null
) : BaseEntity()