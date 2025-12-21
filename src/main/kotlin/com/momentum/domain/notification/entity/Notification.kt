package com.momentum.domain.notification.entity

import com.momentum.domain.notification.entity.enums.NotificationCategory
import com.momentum.domain.user.entity.User
import com.momentum.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "notification")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    user: User,
    category: NotificationCategory,
    content: String? = null,
    isCheck: Boolean = false
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: NotificationCategory = category
        private set

    @Column(nullable = true)
    var content: String? = content
        private set

    @Column(nullable = false)
    var isCheck: Boolean = isCheck
        private set
}