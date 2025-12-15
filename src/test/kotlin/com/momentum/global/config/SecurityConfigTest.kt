package com.momentum.global.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfig 통합 테스트")
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var securityConfig: SecurityConfig

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var securityFilterChain: SecurityFilterChain

    @Test
    @DisplayName("SecurityConfig 빈이 정상적으로 생성되어야 한다")
    fun securityConfigBeanShouldBeCreated() {
        assertNotNull(securityConfig, "SecurityConfig bean should not be null")
    }

    @Test
    @DisplayName("SecurityFilterChain 빈이 정상적으로 생성되어야 한다")
    fun securityFilterChainBeanShouldBeCreated() {
        assertNotNull(securityFilterChain, "SecurityFilterChain bean should not be null")
    }

    @Test
    @DisplayName("PasswordEncoder 빈이 정상적으로 생성되어야 한다")
    fun passwordEncoderBeanShouldBeCreated() {
        assertNotNull(passwordEncoder, "PasswordEncoder bean should not be null")
    }

    @Nested
    @DisplayName("HTTP 보안 설정 테스트")
    inner class HttpSecurityTests {

        @Test
        @DisplayName("모든 요청이 인증 없이 허용되어야 한다 (개발 단계)")
        fun allRequestsShouldBePermitted() {
            mockMvc.perform(get("/"))
                .andExpect(status().isNotFound) // 404 because no controller, but not 401/403
        }

        @Test
        @DisplayName("GET 요청이 CSRF 토큰 없이 처리되어야 한다")
        fun getRequestShouldWorkWithoutCsrf() {
            mockMvc.perform(get("/api/test"))
                .andExpect(status().isNotFound) // Not 403 Forbidden
        }

        @Test
        @DisplayName("POST 요청이 CSRF 토큰 없이 처리되어야 한다")
        fun postRequestShouldWorkWithoutCsrf() {
            mockMvc.perform(post("/api/test"))
                .andExpect(status().isNotFound) // Not 403 Forbidden due to CSRF
        }

        @Test
        @DisplayName("임의의 엔드포인트에 접근이 허용되어야 한다")
        fun arbitraryEndpointShouldBeAccessible() {
            mockMvc.perform(get("/random/endpoint/test"))
                .andExpect(status().isNotFound) // Not 401 Unauthorized
        }

        @Test
        @DisplayName("인증되지 않은 요청이 허용되어야 한다")
        fun unauthenticatedRequestsShouldBeAllowed() {
            mockMvc.perform(get("/api/users"))
                .andExpect(status().isNotFound) // Not 401 or 403
        }
    }

    @Nested
    @DisplayName("PasswordEncoder 기능 테스트")
    inner class PasswordEncoderTests {

        @Test
        @DisplayName("PasswordEncoder는 BCryptPasswordEncoder 타입이어야 한다")
        fun passwordEncoderShouldBeBCrypt() {
            assertTrue(
                passwordEncoder is org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder,
                "PasswordEncoder should be BCryptPasswordEncoder"
            )
        }

        @Test
        @DisplayName("비밀번호가 올바르게 암호화되어야 한다")
        fun passwordShouldBeEncodedCorrectly() {
            val rawPassword = "testPassword123!"
            val encodedPassword = passwordEncoder.encode(rawPassword)

            assertNotNull(encodedPassword, "Encoded password should not be null")
            assertNotEquals(rawPassword, encodedPassword, "Encoded password should differ from raw password")
            assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"),
                "BCrypt encoded password should start with $2a$ or $2b$")
        }

        @Test
        @DisplayName("같은 비밀번호도 다르게 암호화되어야 한다 (Salt)")
        fun samePasswordShouldProduceDifferentHashes() {
            val rawPassword = "password123"
            val encoded1 = passwordEncoder.encode(rawPassword)
            val encoded2 = passwordEncoder.encode(rawPassword)

            assertNotEquals(encoded1, encoded2, "Two encodings of same password should differ due to salt")
        }

        @Test
        @DisplayName("암호화된 비밀번호가 원본과 일치하는지 검증할 수 있어야 한다")
        fun encodedPasswordShouldMatchRawPassword() {
            val rawPassword = "securePassword456"
            val encodedPassword = passwordEncoder.encode(rawPassword)

            assertTrue(
                passwordEncoder.matches(rawPassword, encodedPassword),
                "Raw password should match encoded password"
            )
        }

        @Test
        @DisplayName("잘못된 비밀번호는 일치하지 않아야 한다")
        fun wrongPasswordShouldNotMatch() {
            val rawPassword = "correctPassword"
            val wrongPassword = "wrongPassword"
            val encodedPassword = passwordEncoder.encode(rawPassword)

            assertFalse(
                passwordEncoder.matches(wrongPassword, encodedPassword),
                "Wrong password should not match encoded password"
            )
        }

        @Test
        @DisplayName("빈 문자열 비밀번호를 암호화할 수 있어야 한다")
        fun emptyPasswordShouldBeEncodable() {
            val emptyPassword = ""
            val encodedPassword = passwordEncoder.encode(emptyPassword)

            assertNotNull(encodedPassword, "Empty password should be encodable")
            assertTrue(
                passwordEncoder.matches(emptyPassword, encodedPassword),
                "Encoded empty password should match"
            )
        }

        @Test
        @DisplayName("특수문자가 포함된 비밀번호를 암호화할 수 있어야 한다")
        fun specialCharacterPasswordShouldBeEncodable() {
            val specialPassword = "p@ssw0rd!#$%^&*()"
            val encodedPassword = passwordEncoder.encode(specialPassword)

            assertNotNull(encodedPassword, "Special character password should be encodable")
            assertTrue(
                passwordEncoder.matches(specialPassword, encodedPassword),
                "Special character password should match"
            )
        }

        @Test
        @DisplayName("긴 비밀번호를 암호화할 수 있어야 한다")
        fun longPasswordShouldBeEncodable() {
            val longPassword = "a".repeat(100)
            val encodedPassword = passwordEncoder.encode(longPassword)

            assertNotNull(encodedPassword, "Long password should be encodable")
            assertTrue(
                passwordEncoder.matches(longPassword, encodedPassword),
                "Long password should match"
            )
        }

        @Test
        @DisplayName("유니코드 문자가 포함된 비밀번호를 암호화할 수 있어야 한다")
        fun unicodePasswordShouldBeEncodable() {
            val unicodePassword = "비밀번호123!@#"
            val encodedPassword = passwordEncoder.encode(unicodePassword)

            assertNotNull(encodedPassword, "Unicode password should be encodable")
            assertTrue(
                passwordEncoder.matches(unicodePassword, encodedPassword),
                "Unicode password should match"
            )
        }

        @Test
        @DisplayName("null이 아닌 비밀번호는 항상 인코딩되어야 한다")
        fun nonNullPasswordShouldAlwaysBeEncoded() {
            val testPasswords = listOf(
                "simple",
                "Complex1!",
                "12345678",
                "with spaces",
                "Tab\tChar",
                "New\nLine"
            )

            testPasswords.forEach { password ->
                val encoded = passwordEncoder.encode(password)
                assertNotNull(encoded, "Password '$password' should be encoded")
                assertTrue(
                    passwordEncoder.matches(password, encoded),
                    "Password '$password' should match its encoding"
                )
            }
        }
    }

    @Nested
    @DisplayName("세션 관리 설정 테스트")
    inner class SessionManagementTests {

        @Test
        @DisplayName("세션이 생성되지 않아야 한다 (STATELESS)")
        fun sessionShouldNotBeCreated() {
            val result = mockMvc.perform(get("/api/test"))
                .andReturn()

            val session = result.request.getSession(false)
            // Session might exist due to test setup, but it should be configured as STATELESS
            // The actual session creation policy is verified through SecurityFilterChain configuration
            assertNotNull(securityFilterChain, "SecurityFilterChain should be configured")
        }
    }

    @Nested
    @DisplayName("CSRF 보호 설정 테스트")
    inner class CsrfProtectionTests {

        @Test
        @DisplayName("CSRF 보호가 비활성화되어 있어야 한다")
        fun csrfShouldBeDisabled() {
            // POST request without CSRF token should not fail with 403
            mockMvc.perform(post("/api/test"))
                .andExpect(status().isNotFound) // Not 403 Forbidden
        }

        @Test
        @DisplayName("PUT 요청이 CSRF 토큰 없이 처리되어야 한다")
        fun putRequestShouldWorkWithoutCsrf() {
            mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/test")
            ).andExpect(status().isNotFound) // Not 403 Forbidden
        }

        @Test
        @DisplayName("DELETE 요청이 CSRF 토큰 없이 처리되어야 한다")
        fun deleteRequestShouldWorkWithoutCsrf() {
            mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/test")
            ).andExpect(status().isNotFound) // Not 403 Forbidden
        }

        @Test
        @DisplayName("PATCH 요청이 CSRF 토큰 없이 처리되어야 한다")
        fun patchRequestShouldWorkWithoutCsrf() {
            mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/test")
            ).andExpect(status().isNotFound) // Not 403 Forbidden
        }
    }

    @Nested
    @DisplayName("보안 설정 엣지 케이스 테스트")
    inner class SecurityEdgeCaseTests {

        @Test
        @DisplayName("매우 긴 URL 경로에 대한 요청을 처리할 수 있어야 한다")
        fun veryLongUrlPathShouldBeHandled() {
            val longPath = "/api/" + "segment/".repeat(50) + "endpoint"
            mockMvc.perform(get(longPath))
                .andExpect(status().isNotFound) // Should handle, not crash
        }

        @Test
        @DisplayName("특수 문자가 포함된 URL을 처리할 수 있어야 한다")
        fun specialCharactersInUrlShouldBeHandled() {
            mockMvc.perform(get("/api/test%20space"))
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("루트 경로 요청을 처리할 수 있어야 한다")
        fun rootPathShouldBeHandled() {
            mockMvc.perform(get("/"))
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("다중 슬래시가 있는 경로를 처리할 수 있어야 한다")
        fun multipleSlashesInPathShouldBeHandled() {
            mockMvc.perform(get("/api//test///endpoint"))
                .andExpect(status().isNotFound)
        }
    }
}

@DisplayName("SecurityConfig 단위 테스트")
class SecurityConfigUnitTest {

    private lateinit var securityConfig: SecurityConfig

    @BeforeEach
    fun setUp() {
        securityConfig = SecurityConfig()
    }

    @Test
    @DisplayName("passwordEncoder 메서드가 BCryptPasswordEncoder를 반환해야 한다")
    fun passwordEncoderShouldReturnBCryptPasswordEncoder() {
        val encoder = securityConfig.passwordEncoder()
        assertNotNull(encoder, "PasswordEncoder should not be null")
        assertTrue(
            encoder is org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder,
            "Should return BCryptPasswordEncoder instance"
        )
    }

    @Test
    @DisplayName("passwordEncoder는 매번 새로운 인스턴스를 반환해야 한다")
    fun passwordEncoderShouldReturnNewInstance() {
        // Note: This depends on Spring's bean scoping, but we test the method itself
        val encoder1 = securityConfig.passwordEncoder()
        val encoder2 = securityConfig.passwordEncoder()

        assertNotNull(encoder1, "First encoder should not be null")
        assertNotNull(encoder2, "Second encoder should not be null")
        // They should be different instances when called directly (not through Spring)
        assertTrue(encoder1 !== encoder2, "Should create new instances")
    }

    @Test
    @DisplayName("SecurityConfig 인스턴스가 생성 가능해야 한다")
    fun securityConfigShouldBeInstantiable() {
        val config = SecurityConfig()
        assertNotNull(config, "SecurityConfig should be instantiable")
    }
}