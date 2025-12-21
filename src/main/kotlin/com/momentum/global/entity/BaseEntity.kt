package com.momentum.global.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime
) {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = createdAt
        private set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = updatedAt
        private set
}
