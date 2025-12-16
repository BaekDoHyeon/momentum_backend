package com.momentum.domain.auth.dto.response

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer"
)
