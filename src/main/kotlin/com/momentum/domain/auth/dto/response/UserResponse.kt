package com.momentum.domain.auth.dto.response

import com.momentum.domain.user.entity.User
import com.momentum.domain.user.entity.enums.UserRole
import java.time.LocalDateTime

data class UserResponse(
    val id: Long?,
    val email: String,
    val name: String,
    val role: UserRole,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                name = user.name,
                role = user.role,
                createdAt = user.createdAt
            )
        }
    }
}
