package com.momentum

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MomentumApplication 통합 테스트")
class MomentumApplicationTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    @DisplayName("Spring Context가 정상적으로 로드되어야 한다")
    fun contextLoads() {
        assertNotNull(applicationContext, "ApplicationContext should not be null")
    }

    @Test
    @DisplayName("MomentumApplication 빈이 정상적으로 생성되어야 한다")
    fun momentumApplicationBeanShouldBeCreated() {
        val bean = applicationContext.getBean(MomentumApplication::class.java)
        assertNotNull(bean, "MomentumApplication bean should be created")
    }

    @Test
    @DisplayName("필수 Spring Boot Starter 빈들이 정상적으로 로드되어야 한다")
    fun essentialSpringBootBeansLoaded() {
        // Check if essential beans are loaded
        assertTrue(
            applicationContext.containsBean("securityFilterChain"),
            "Security filter chain bean should be loaded"
        )
        assertTrue(
            applicationContext.containsBean("passwordEncoder"),
            "Password encoder bean should be loaded"
        )
    }

    @Test
    @DisplayName("애플리케이션이 예외 없이 시작되어야 한다")
    fun applicationStartsWithoutExceptions() {
        assertDoesNotThrow {
            applicationContext.getBean(MomentumApplication::class.java)
        }
    }

    @Test
    @DisplayName("환경 프로파일이 올바르게 설정되어야 한다")
    fun environmentProfileShouldBeSet() {
        val environment = applicationContext.environment
        assertNotNull(environment, "Environment should not be null")
        val activeProfiles = environment.activeProfiles
        assertTrue(
            activeProfiles.contains("test"),
            "Test profile should be active"
        )
    }

    @Test
    @DisplayName("애플리케이션 이름이 올바르게 설정되어야 한다")
    fun applicationNameShouldBeSet() {
        val environment = applicationContext.environment
        val appName = environment.getProperty("spring.application.name")
        assertNotNull(appName, "Application name should be set")
    }
}

@DisplayName("MomentumApplication Main Function 테스트")
class MomentumApplicationMainTest {

    @Test
    @DisplayName("main 함수가 예외 없이 실행 가능해야 한다")
    fun mainFunctionShouldBeCallable() {
        // This test verifies the main function exists and is callable
        // We can't actually run it in a test without starting a full application
        // but we can verify the function signature exists
        assertDoesNotThrow {
            val mainMethod = Class.forName("com.momentum.MomentumApplicationKt")
                .getDeclaredMethod("main", Array<String>::class.java)
            assertNotNull(mainMethod, "Main method should exist")
        }
    }
}