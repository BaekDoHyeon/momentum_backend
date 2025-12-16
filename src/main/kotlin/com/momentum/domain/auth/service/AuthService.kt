package com.momentum.domain.auth.service

import com.momentum.domain.auth.dto.request.LoginRequest
import com.momentum.domain.auth.dto.request.SignupRequest
import com.momentum.domain.auth.dto.response.TokenResponse
import com.momentum.domain.auth.dto.response.UserResponse
import com.momentum.domain.user.entity.User
import com.momentum.domain.user.entity.enums.UserRole
import com.momentum.domain.user.repository.UserRepository
import com.momentum.global.security.jwt.JwtTokenProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun signup(request: SignupRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("이미 존재하는 이메일입니다.")
        }

        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name,
            role = UserRole.ROLE_USER
        )

        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        val token = jwtTokenProvider.generateToken(user)
        return TokenResponse(accessToken = token)
    }
}
