package com.momentum.global.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secretKey: String,

    @Value("\${jwt.expiration}")
    private val expirationTime: Long
) {

    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(secretKey.toByteArray())
    }

    fun generateToken(userDetails: UserDetails): String {
        val claims = mutableMapOf<String, Any>()
        claims["authorities"] = userDetails.authorities.map { it.authority }

        return Jwts.builder()
            .claims(claims)
            .subject(userDetails.username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationTime))
            .signWith(getSigningKey())
            .compact()
    }

    fun extractUsername(token: String): String {
        return extractAllClaims(token).subject
    }

    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return username == userDetails.username && !isTokenExpired(token)
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractAllClaims(token).expiration.before(Date())
    }
}
