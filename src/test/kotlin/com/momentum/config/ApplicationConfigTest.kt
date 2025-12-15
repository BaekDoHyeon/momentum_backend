package com.momentum.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Configuration 테스트")
class ApplicationConfigTest {

    @Autowired
    private lateinit var environment: Environment

    @Value("\${spring.application.name}")
    private lateinit var applicationName: String

    @Value("\${spring.datasource.url}")
    private lateinit var datasourceUrl: String

    @Value("\${spring.datasource.driver-class-name}")
    private lateinit var driverClassName: String

    @Value("\${spring.jpa.hibernate.ddl-auto}")
    private lateinit var ddlAuto: String

    @Value("\${spring.jpa.show-sql}")
    private var showSql: Boolean = false

    @Value("\${server.port}")
    private var serverPort: Int = 0

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration}")
    private var jwtExpiration: Long = 0

    @Nested
    @DisplayName("Spring Application 설정 테스트")
    inner class SpringApplicationConfigTests {

        @Test
        @DisplayName("애플리케이션 이름이 설정되어 있어야 한다")
        fun applicationNameShouldBeSet() {
            assertNotNull(applicationName, "Application name should not be null")
            assertTrue(applicationName.isNotBlank(), "Application name should not be blank")
        }

        @Test
        @DisplayName("Environment 객체가 주입되어야 한다")
        fun environmentShouldBeInjected() {
            assertNotNull(environment, "Environment should be injected")
        }

        @Test
        @DisplayName("테스트 프로파일이 활성화되어 있어야 한다")
        fun testProfileShouldBeActive() {
            val activeProfiles = environment.activeProfiles
            assertTrue(
                activeProfiles.contains("test"),
                "Test profile should be active"
            )
        }
    }

    @Nested
    @DisplayName("Datasource 설정 테스트")
    inner class DatasourceConfigTests {

        @Test
        @DisplayName("Datasource URL이 설정되어 있어야 한다")
        fun datasourceUrlShouldBeSet() {
            assertNotNull(datasourceUrl, "Datasource URL should not be null")
            assertTrue(datasourceUrl.isNotBlank(), "Datasource URL should not be blank")
        }

        @Test
        @DisplayName("테스트 환경에서는 H2 데이터베이스를 사용해야 한다")
        fun shouldUseH2DatabaseInTest() {
            assertTrue(
                datasourceUrl.contains("h2"),
                "Test environment should use H2 database"
            )
        }

        @Test
        @DisplayName("Driver class name이 설정되어 있어야 한다")
        fun driverClassNameShouldBeSet() {
            assertNotNull(driverClassName, "Driver class name should not be null")
            assertTrue(
                driverClassName.isNotBlank(),
                "Driver class name should not be blank"
            )
        }

        @Test
        @DisplayName("테스트 환경에서는 H2 드라이버를 사용해야 한다")
        fun shouldUseH2DriverInTest() {
            assertTrue(
                driverClassName.contains("h2", ignoreCase = true),
                "Test environment should use H2 driver"
            )
        }

        @Test
        @DisplayName("Datasource 사용자명이 환경 변수에서 읽을 수 있어야 한다")
        fun datasourceUsernameShouldBeReadable() {
            val username = environment.getProperty("spring.datasource.username")
            assertNotNull(username, "Datasource username should be readable")
        }

        @Test
        @DisplayName("Datasource 비밀번호가 환경 변수에서 읽을 수 있어야 한다")
        fun datasourcePasswordShouldBeReadable() {
            val password = environment.getProperty("spring.datasource.password")
            // Password might be empty string in test, that's okay
            assertNotNull(password, "Datasource password property should exist")
        }
    }

    @Nested
    @DisplayName("JPA 설정 테스트")
    inner class JpaConfigTests {

        @Test
        @DisplayName("Hibernate DDL auto 설정이 되어 있어야 한다")
        fun hibernateDdlAutoShouldBeSet() {
            assertNotNull(ddlAuto, "Hibernate DDL auto should not be null")
            assertTrue(ddlAuto.isNotBlank(), "Hibernate DDL auto should not be blank")
        }

        @Test
        @DisplayName("테스트 환경에서는 create-drop DDL 전략을 사용해야 한다")
        fun shouldUseCreateDropInTest() {
            assertEquals(
                "create-drop",
                ddlAuto,
                "Test environment should use create-drop DDL strategy"
            )
        }

        @Test
        @DisplayName("SQL 로깅이 설정되어 있어야 한다")
        fun showSqlShouldBeSet() {
            // showSql should be true for development/testing
            assertTrue(showSql, "SQL logging should be enabled")
        }

        @Test
        @DisplayName("Hibernate format_sql 속성이 설정되어 있어야 한다")
        fun formatSqlShouldBeSet() {
            val formatSql = environment.getProperty("spring.jpa.properties.hibernate.format_sql")
            assertNotNull(formatSql, "Format SQL property should be set")
        }

        @Test
        @DisplayName("Hibernate dialect가 설정되어 있어야 한다")
        fun hibernateDialectShouldBeSet() {
            val dialect = environment.getProperty("spring.jpa.properties.hibernate.dialect")
            assertNotNull(dialect, "Hibernate dialect should be set")
            assertTrue(
                dialect?.isNotBlank() == true,
                "Hibernate dialect should not be blank"
            )
        }

        @Test
        @DisplayName("테스트 환경에서는 H2 Dialect를 사용해야 한다")
        fun shouldUseH2DialectInTest() {
            val dialect = environment.getProperty("spring.jpa.properties.hibernate.dialect")
            assertTrue(
                dialect?.contains("H2", ignoreCase = true) == true,
                "Test environment should use H2 dialect"
            )
        }
    }

    @Nested
    @DisplayName("Redis 설정 테스트")
    inner class RedisConfigTests {

        @Test
        @DisplayName("Redis host가 설정되어 있어야 한다")
        fun redisHostShouldBeSet() {
            val redisHost = environment.getProperty("spring.data.redis.host")
            assertNotNull(redisHost, "Redis host should be set")
            assertTrue(
                redisHost?.isNotBlank() == true,
                "Redis host should not be blank"
            )
        }

        @Test
        @DisplayName("Redis port가 설정되어 있어야 한다")
        fun redisPortShouldBeSet() {
            val redisPort = environment.getProperty("spring.data.redis.port")
            assertNotNull(redisPort, "Redis port should be set")
        }

        @Test
        @DisplayName("Redis port는 유효한 포트 번호여야 한다")
        fun redisPortShouldBeValid() {
            val redisPort = environment.getProperty("spring.data.redis.port", Int::class.java)
            assertNotNull(redisPort, "Redis port should be a valid integer")
            assertTrue(
                redisPort!! in 1..65535,
                "Redis port should be in valid range (1-65535)"
            )
        }

        @Test
        @DisplayName("Redis host는 localhost 또는 유효한 호스트명이어야 한다")
        fun redisHostShouldBeValidHostname() {
            val redisHost = environment.getProperty("spring.data.redis.host")
            assertTrue(
                redisHost == "localhost" || redisHost?.matches(Regex("^[a-zA-Z0-9.-]+$")) == true,
                "Redis host should be localhost or valid hostname"
            )
        }
    }

    @Nested
    @DisplayName("Server 설정 테스트")
    inner class ServerConfigTests {

        @Test
        @DisplayName("서버 포트가 설정되어 있어야 한다")
        fun serverPortShouldBeSet() {
            // In test, port might be 0 for random port
            assertTrue(
                serverPort >= 0,
                "Server port should be set (0 for random port)"
            )
        }

        @Test
        @DisplayName("테스트 환경에서는 랜덤 포트(0)를 사용해야 한다")
        fun testEnvironmentShouldUseRandomPort() {
            assertEquals(
                0,
                serverPort,
                "Test environment should use random port (0)"
            )
        }

        @Test
        @DisplayName("서버 포트는 유효한 범위여야 한다")
        fun serverPortShouldBeInValidRange() {
            assertTrue(
                serverPort in 0..65535,
                "Server port should be in valid range"
            )
        }
    }

    @Nested
    @DisplayName("JWT 설정 테스트")
    inner class JwtConfigTests {

        @Test
        @DisplayName("JWT secret이 설정되어 있어야 한다")
        fun jwtSecretShouldBeSet() {
            assertNotNull(jwtSecret, "JWT secret should not be null")
            assertTrue(jwtSecret.isNotBlank(), "JWT secret should not be blank")
        }

        @Test
        @DisplayName("JWT secret은 최소 길이를 가져야 한다")
        fun jwtSecretShouldHaveMinimumLength() {
            assertTrue(
                jwtSecret.length >= 32,
                "JWT secret should have at least 32 characters for security"
            )
        }

        @Test
        @DisplayName("JWT expiration이 설정되어 있어야 한다")
        fun jwtExpirationShouldBeSet() {
            assertTrue(jwtExpiration > 0, "JWT expiration should be positive")
        }

        @Test
        @DisplayName("JWT expiration은 합리적인 범위여야 한다")
        fun jwtExpirationShouldBeReasonable() {
            // Should be between 5 minutes and 7 days
            val fiveMinutes = 5 * 60 * 1000L
            val sevenDays = 7 * 24 * 60 * 60 * 1000L
            assertTrue(
                jwtExpiration in fiveMinutes..sevenDays,
                "JWT expiration should be reasonable (5 min to 7 days)"
            )
        }

        @Test
        @DisplayName("테스트 환경의 JWT secret은 프로덕션과 달라야 한다")
        fun testJwtSecretShouldBeDifferentFromProduction() {
            assertTrue(
                jwtSecret.contains("test", ignoreCase = true),
                "Test JWT secret should indicate it's for testing"
            )
        }
    }

    @Nested
    @DisplayName("Configuration 통합 테스트")
    inner class ConfigurationIntegrationTests {

        @Test
        @DisplayName("모든 필수 속성들이 로드되어야 한다")
        fun allRequiredPropertiesShouldBeLoaded() {
            val requiredProperties = listOf(
                "spring.application.name",
                "spring.datasource.url",
                "spring.datasource.driver-class-name",
                "spring.jpa.hibernate.ddl-auto",
                "spring.data.redis.host",
                "spring.data.redis.port",
                "server.port",
                "jwt.secret",
                "jwt.expiration"
            )

            requiredProperties.forEach { property ->
                val value = environment.getProperty(property)
                assertNotNull(
                    value,
                    "Required property '$property' should be loaded"
                )
            }
        }

        @Test
        @DisplayName("Environment에서 모든 속성을 읽을 수 있어야 한다")
        fun allPropertiesShouldBeReadableFromEnvironment() {
            assertNotNull(environment.getProperty("spring.application.name"))
            assertNotNull(environment.getProperty("spring.datasource.url"))
            assertNotNull(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
            assertNotNull(environment.getProperty("jwt.secret"))
        }

        @Test
        @DisplayName("프로파일별 설정이 올바르게 적용되어야 한다")
        fun profileSpecificConfigShouldBeApplied() {
            // Test profile uses H2, production uses PostgreSQL
            val activeProfiles = environment.activeProfiles
            if (activeProfiles.contains("test")) {
                assertTrue(
                    datasourceUrl.contains("h2"),
                    "Test profile should use H2 database"
                )
            }
        }
    }

    @Nested
    @DisplayName("Configuration 엣지 케이스 테스트")
    inner class ConfigurationEdgeCaseTests {

        @Test
        @DisplayName("존재하지 않는 속성은 null을 반환해야 한다")
        fun nonExistentPropertyShouldReturnNull() {
            val nonExistent = environment.getProperty("non.existent.property")
            assertEquals(null, nonExistent, "Non-existent property should return null")
        }

        @Test
        @DisplayName("속성을 다양한 타입으로 읽을 수 있어야 한다")
        fun propertiesShouldBeReadableAsVariousTypes() {
            // String
            val appName = environment.getProperty("spring.application.name", String::class.java)
            assertNotNull(appName)

            // Integer
            val serverPort = environment.getProperty("server.port", Int::class.java)
            assertNotNull(serverPort)

            // Long
            val jwtExp = environment.getProperty("jwt.expiration", Long::class.java)
            assertNotNull(jwtExp)

            // Boolean
            val showSql = environment.getProperty("spring.jpa.show-sql", Boolean::class.java)
            assertNotNull(showSql)
        }

        @Test
        @DisplayName("기본값을 가진 속성 읽기가 작동해야 한다")
        fun propertyReadingWithDefaultValueShouldWork() {
            val value = environment.getProperty(
                "non.existent.property",
                "default-value"
            )
            assertEquals("default-value", value, "Should return default value")
        }

        @Test
        @DisplayName("빈 문자열 속성도 처리할 수 있어야 한다")
        fun emptyStringPropertyShouldBeHandleable() {
            // Some properties might be empty strings (like test datasource password)
            val password = environment.getProperty("spring.datasource.password")
            assertNotNull(password, "Password property should exist (even if empty)")
        }
    }
}