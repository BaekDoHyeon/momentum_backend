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
    user: User,
    satisfaction: Satisfaction,
    concentration: Concentration,
    achievement: String? = null,
    improvement: String? = null,
    memo: String? = null
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var satisfaction: Satisfaction = satisfaction     // 만족도
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var concentration: Concentration = concentration   // 집중도
        private set

    @Column(nullable = true)
    var achievement: String? = achievement           // 잘한점
        private set

    @Column(nullable = true)
    var improvement: String? = improvement           // 부족한점
        private set

    @Column(nullable = true)
    var memo: String? = memo
        private set
}