# Kotlin 고유 무기 가이드 2탄 – 생산성을 폭발시키지만 통제가 필요한 고유 무기들

## 목차 (Table of Contents)

1. [프롤로그](#프롤로그)
   - [Kotlin 코드 품질이 팀마다 갈리는 이유](#kotlin-코드-품질이-팀마다-갈리는-이유)
2. [Extension Function / Property](#extension-function--property)
   - [유틸 함수 대체](#유틸-함수-대체)
   - [도메인 오염 경계](#도메인-오염-경계)
3. [Scope Functions](#scope-functions)
   - [let / run / apply / also / with](#let--run--apply--also--with)
   - [선택 기준과 중첩 제한](#선택-기준과-중첩-제한)
4. [Top-level Function / Property](#top-level-function--property)
   - [Static 대체](#static-대체)
   - [패키지 구조 기준](#패키지-구조-기준)
5. [Object / Companion Object](#object--companion-object)
   - [싱글톤](#싱글톤)
   - [팩토리 패턴](#팩토리-패턴)
6. [Inline Function & Reified](#inline-function--reified)
   - [제네릭 타입 안전성](#제네릭-타입-안전성)
   - [성능 고려 사항](#성능-고려-사항)
7. [Destructuring Declaration](#destructuring-declaration)
   - [Pair / Triple 남용 경계](#pair--triple-남용-경계)
   - [명확한 의도 표현](#명확한-의도-표현)
8. [Collection API & Sequence](#collection-api--sequence)
   - [Eager vs Lazy](#eager-vs-lazy)
   - [성능 기준](#성능-기준)
9. [남용 주의 블랙리스트](#남용-주의-블랙리스트)
   - [Scope Function 중첩](#scope-function-중첩)
   - [Extension 남발](#extension-남발)
   - [Pair / Triple 과다](#pair--triple-과다)
   - [Inline 남용](#inline-남용)
10. [2탄 요약 – 팀 Kotlin 사용 규칙 체크리스트](#2탄-요약--팀-kotlin-사용-규칙-체크리스트)

---

## 프롤로그

### Kotlin 코드 품질이 팀마다 갈리는 이유

Kotlin은 표현력이 강한 만큼, **잘못 사용하면 독이 되는 기능**들이 많다. Extension function, scope function, inline 등은 생산성을 극대화하지만, 남용하면 코드 가독성을 파괴하고 유지보수를 어렵게 만든다.

1탄에서 다룬 기능들은 "언어 설계 철학"에 따라 사용하면 자연스럽게 좋은 코드가 나온다. 하지만 2탄의 기능들은 **"언제 쓰지 말아야 하는가"**를 명확히 정의하지 않으면, 팀 코드베이스가 빠르게 혼란스러워진다.

이 가이드는 Kotlin의 강력한 생산성 도구들을 **실전에서 어떻게 통제하며 써야 하는지**에 집중한다. 각 기능의 Do/Don't를 명확히 하여, 코드 리뷰 기준으로 바로 활용할 수 있도록 구성했다.

---

## Extension Function / Property

### 한 줄 요약
**기존 클래스에 함수/프로퍼티를 추가하되, 남발하면 코드베이스가 마법처럼 보이기 시작한다.**

### 기능 개요

Extension은 클래스 수정 없이 새로운 함수나 프로퍼티를 추가하는 기능이다. 유틸리티 함수를 자연스럽게 만들지만, 과도하게 사용하면 "이 메서드는 어디서 왔지?"라는 혼란을 야기한다.

### 유틸 함수 대체

```kotlin
// 기존 유틸 클래스 방식
object StringUtils {
    fun isValidEmail(email: String): Boolean {
        return email.matches(EMAIL_REGEX)
    }
}

// StringUtils.isValidEmail(user.email)  // 정적 호출

// Extension으로 전환
fun String.isValidEmail(): Boolean {
    return matches(EMAIL_REGEX)
}

// user.email.isValidEmail()  // 자연스러운 호출

// 실무 예시: Nullable 확장
fun String?.isNullOrEmpty(): Boolean = this == null || isEmpty()
fun String?.orDefault(default: String): String = this ?: default

val displayName = user.nickname.orDefault("익명")

// Collection 확장
fun <T> List<T>.second(): T = this[1]
fun <T> List<T>.secondOrNull(): T? = getOrNull(1)

val items = listOf(1, 2, 3)
val secondItem = items.secondOrNull() ?: 0

// 도메인 특화 확장
fun LocalDateTime.isBusinessDay(): Boolean {
    return dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
}

fun LocalDateTime.toKoreanFormat(): String {
    return format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
}

val now = LocalDateTime.now()
if (now.isBusinessDay()) {
    println(now.toKoreanFormat())
}
```

### 도메인 오염 경계

```kotlin
// ✅ 좋은 예: 패키지 내부에서만 사용
package com.example.order.utils

internal fun Order.calculateTotalWithTax(): Int {
    return total + (total * TAX_RATE).toInt()
}

// ❌ 나쁜 예: 너무 일반적인 타입에 도메인 로직 추가
fun String.toOrder(): Order {
    // JSON 파싱해서 Order로 변환
    return Json.decodeFromString(this)
}
// 문제: 모든 String이 Order로 변환 가능해 보임, IDE 자동완성에 노이즈

// ❌ 나쁜 예: Extension으로 캡슐화 깨기
fun User.changePassword(newPassword: String) {
    // User의 내부 상태를 외부에서 변경
    // → User 클래스 내부 메서드여야 함
}

// ✅ 좋은 예: 외부 라이브러리 타입 확장
fun HttpResponse.isSuccess(): Boolean = status in 200..299
fun Response<*>.bodyOrThrow(): T = body() ?: throw EmptyBodyException()

// ✅ 좋은 예: DSL 스타일 빌더
fun ViewGroup.linearLayout(init: LinearLayout.() -> Unit): LinearLayout {
    val layout = LinearLayout(context)
    layout.init()
    addView(layout)
    return layout
}
```

### 언어 설계 관점에서의 이유

Extension은 "기존 코드 수정 없이 기능 확장"이라는 Open-Closed Principle을 언어 차원에서 지원한다:

1. **정적 디스패치** - 컴파일 타임에 결정, 성능 오버헤드 없음
2. **Import 제어** - 필요한 extension만 import하여 네임스페이스 관리
3. **Receiver 타입 안전성** - this의 타입이 명확히 보장됨
4. **캡슐화 유지** - private 멤버 접근 불가, 공개 API만 사용

### 실무 사용 시나리오

**DTO 변환 Extension:**
```kotlin
// Entity → DTO 변환
fun User.toResponse() = UserResponse(
    id = id,
    name = name,
    email = email
)

fun List<User>.toResponses() = map { it.toResponse() }

// Service에서 사용
class UserService {
    fun getUsers(): List<UserResponse> {
        return userRepository.findAll().toResponses()
    }
}
```

**Repository Extension:**
```kotlin
// Spring Data JPA Repository 확장
fun UserRepository.findByIdOrThrow(id: Long): User {
    return findById(id).orElseThrow { 
        UserNotFoundException("User not found: $id") 
    }
}

fun <T, ID> JpaRepository<T, ID>.findByIdOrNull(id: ID): T? {
    return findById(id).orElse(null)
}
```

**Validation Extension:**
```kotlin
fun String.validateEmail(): ValidationResult {
    return when {
        isBlank() -> ValidationResult.Invalid("이메일은 필수입니다")
        !matches(EMAIL_REGEX) -> ValidationResult.Invalid("이메일 형식이 올바르지 않습니다")
        else -> ValidationResult.Valid
    }
}

fun Int.validatePositive(fieldName: String): ValidationResult {
    return if (this > 0) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid("$fieldName 는 양수여야 합니다")
    }
}
```

### 트레이드오프 / 남용 시 문제

**❌ 과도한 Extension으로 코드 추적 어려움:**
```kotlin
// 10개 파일에 걸쳐 String extension이 흩어져 있음
fun String.toUser(): User = ...
fun String.toOrder(): Order = ...
fun String.encrypt(): String = ...
fun String.validateKorean(): Boolean = ...
// IDE에서 String. 입력 시 수백 개 메서드 노출
```

**❌ 비즈니스 로직을 Extension으로:**
```kotlin
// 나쁜 예
fun Order.cancel(reason: String) {
    // 주문 취소 로직
    // → Order 클래스 내부 메서드여야 함
}
```

**✅ 적절한 범위 제한:**
```kotlin
// 좋은 예: internal로 패키지 내부 제한
internal fun Order.toDto() = OrderDto(...)

// 좋은 예: 특정 파일에서만 사용하는 private extension
private fun String.sanitize() = trim().lowercase()
```

### 사용 기준 체크리스트

**Do:**
- [ ] 유틸리티 성격의 함수를 extension으로 전환
- [ ] 외부 라이브러리 타입 확장 (HttpResponse, List 등)
- [ ] DTO 변환 함수를 extension으로 정의
- [ ] Internal/private으로 가시성 제한
- [ ] 함수명은 명확하고 자기 설명적으로

**Don't:**
- [ ] 비즈니스 로직을 extension으로 빼지 않기
- [ ] 너무 일반적인 타입(String, Int)에 도메인 로직 추가 금지
- [ ] Extension으로 캡슐화 깨지 않기
- [ ] 한 타입에 10개 이상 extension 만들지 않기
- [ ] Public extension은 팀 코드 리뷰 필수

---

## Scope Functions

### 한 줄 요약
**객체 컨텍스트 내에서 코드 블록을 실행하되, 5종류를 구분 없이 쓰면 혼란의 시작이다.**

### 기능 개요

Scope function은 객체의 컨텍스트 내에서 코드 블록을 실행하는 표준 함수들이다. let, run, apply, also, with 5가지가 있으며, 각각 용도가 다르다.

### let / run / apply / also / with

```kotlin
// let: nullable 체크 후 non-null 블록 실행
val length: Int? = nullableString?.let { 
    println(it.length)
    it.length  // 반환값
}

// also: 객체 자체를 반환하며 부수 효과 수행
val user = User("백도현", "baek@example.com").also {
    println("User created: ${it.name}")
    log.info("Email: ${it.email}")
}

// apply: 객체 초기화, this로 접근
val config = DatabaseConfig().apply {
    host = "localhost"
    port = 3306
    username = "dev"
}

// run: 객체 컨텍스트에서 계산 후 결과 반환
val port: Int = config.run {
    if (isProduction) 3306 else 3307
}

// with: 확장 함수가 아님, 여러 호출 묶기
with(user) {
    println(name)
    println(email)
    save()
}

// 실무 예시: Nullable 체이닝
user?.let { u ->
    u.email?.let { email ->
        sendEmail(email)
    }
}
// 더 나은 방식
if (user != null && user.email != null) {
    sendEmail(user.email)
}

// Builder 스타일
fun createUser() = User().apply {
    name = "백도현"
    email = "baek@example.com"
    role = UserRole.ADMIN
}

// 로깅과 함께
val result = repository.save(user).also {
    log.info("Saved user: ${it.id}")
}
```

### 선택 기준과 중첩 제한

```kotlin
/**
 * Scope Function 선택 가이드:
 * 
 * let: nullable 체크 후 non-null 처리, 변환
 * also: 부수 효과 (로깅, 검증), 체이닝 유지
 * apply: 객체 초기화, Builder 패턴
 * run: 객체 컨텍스트에서 계산
 * with: 확장 함수 아님, 여러 메서드 호출
 */

// ✅ 좋은 예: 명확한 용도
fun processUser(user: User?) {
    user?.let { u ->
        validate(u)
        save(u)
    }
}

// ✅ 좋은 예: apply로 초기화
val request = CreateUserRequest().apply {
    name = "백도현"
    email = "baek@example.com"
}

// ❌ 나쁜 예: 과도한 중첩
user?.let { u ->
    u.company?.let { company ->
        company.department?.let { dept ->
            dept.manager?.let { manager ->
                // 4단계 중첩!
                sendEmail(manager.email)
            }
        }
    }
}

// ✅ 개선: 조기 리턴
fun sendManagerEmail(user: User?) {
    val manager = user?.company?.department?.manager ?: return
    sendEmail(manager.email)
}

// ❌ 나쁜 예: let과 run 혼용
val result = user?.let { u ->
    u.company?.run {
        departments.filter { it.isActive }
    }
}
// → 가독성 저하, 의도 불명확

// ✅ 개선: 명확한 변수 할당
val company = user?.company ?: return
val activeDepartments = company.departments.filter { it.isActive }
```

### 언어 설계 관점에서의 이유

Scope function은 "컨텍스트를 명시적으로 제어"하여 가독성을 높인다:

1. **Lambda receiver** - this/it으로 컨텍스트 객체 접근
2. **반환값 제어** - 블록 결과 반환 vs 객체 자체 반환
3. **Null 안전성** - ?. 와 결합하여 nullable 처리 간소화
4. **함수형 체이닝** - 여러 변환을 연속적으로 표현

### 실무 사용 시나리오

**Service Layer:**
```kotlin
class OrderService {
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        return Order().apply {
            userId = request.userId
            items = request.items
            totalAmount = calculateTotal(items)
        }.also {
            validate(it)
        }.let { order ->
            repository.save(order)
        }.toResponse()
    }
}
```

**Test Code:**
```kotlin
@Test
fun `주문 생성 테스트`() {
    val user = createTestUser().apply {
        role = UserRole.VIP
    }
    
    val order = createTestOrder().apply {
        userId = user.id
        items = listOf(createTestItem())
    }.also {
        println("Test order: ${it.id}")
    }
    
    orderService.createOrder(order).run {
        assertThat(status).isEqualTo(OrderStatus.CREATED)
        assertThat(totalAmount).isGreaterThan(0)
    }
}
```

**Nullable 처리:**
```kotlin
// let으로 nullable 체이닝 제거
fun getUserEmail(userId: Long): String? {
    return userRepository.findByIdOrNull(userId)?.email
}

// also로 부수 효과 추가
fun saveWithLogging(user: User): User {
    return repository.save(user).also {
        log.info("Saved user: ${it.id}")
        eventPublisher.publish(UserCreatedEvent(it.id))
    }
}
```

### 트레이드오프 / 남용 시 문제

**❌ Scope function 남용:**
```kotlin
// 나쁜 예: 모든 곳에 scope function
fun process() {
    val result = getData().let { data ->
        data.filter { it.isActive }.let { filtered ->
            filtered.map { it.toDto() }.let { dtos ->
                dtos.sortedBy { it.name }.also { sorted ->
                    log.info("Sorted: ${sorted.size}")
                }
            }
        }
    }
}

// 좋은 예: 필요한 곳에만
fun process() {
    val activeData = getData().filter { it.isActive }
    val dtos = activeData.map { it.toDto() }
    val sorted = dtos.sortedBy { it.name }
    log.info("Sorted: ${sorted.size}")
    return sorted
}
```

**❌ this vs it 혼동:**
```kotlin
// 혼란스러운 코드
user.apply {
    email = "new@example.com"  // this.email
    age = 30  // this.age
}.let {
    println(it.email)  // it.email
}
// → apply와 let을 섞으면 this/it이 헷갈림
```

### 사용 기준 체크리스트

**Do:**
- [ ] let: nullable 체크, 변환, 지역 변수 스코프 제한
- [ ] also: 부수 효과(로깅, 검증), 체이닝 유지
- [ ] apply: 객체 초기화, Builder 패턴
- [ ] run: 객체 컨텍스트에서 계산
- [ ] with: 동일 객체의 여러 메서드 호출

**Don't:**
- [ ] 2단계 이상 중첩 금지
- [ ] let/run/apply/also 혼용 금지
- [ ] 단순 null 체크는 if문 사용
- [ ] 의미 없는 scope function 남발 금지
- [ ] 가독성이 떨어지면 명시적 변수 사용

---

## Top-level Function / Property

### 한 줄 요약
**클래스 없이 함수/프로퍼티를 정의하되, 무분별하게 사용하면 전역 네임스페이스가 오염된다.**

### 기능 개요

Top-level function/property는 클래스 밖에서 직접 선언하는 함수와 프로퍼티다. 유틸리티 함수나 상수를 정의할 때 유용하지만, 남용하면 코드 구조가 무너진다.

### Static 대체

```kotlin
// 파일: OrderUtils.kt
package com.example.order

// Top-level 함수
fun calculateTax(amount: Int): Int {
    return (amount * TAX_RATE).toInt()
}

fun validateOrder(order: Order): ValidationResult {
    // 검증 로직
}

// Top-level 프로퍼티 (상수)
const val TAX_RATE = 0.1
const val MAX_ORDER_ITEMS = 100

private const val INTERNAL_CONSTANT = "internal"

// 사용: import해서 직접 호출
import com.example.order.calculateTax
import com.example.order.TAX_RATE

val tax = calculateTax(10000)

// ✅ 좋은 예: 순수 함수를 top-level로
fun formatCurrency(amount: Int): String {
    return NumberFormat.getCurrencyInstance(Locale.KOREA).format(amount)
}

fun parseDate(dateString: String): LocalDate {
    return LocalDate.parse(dateString, DATE_FORMATTER)
}

// ❌ 나쁜 예: 상태를 가진 top-level property
var globalCounter = 0  // Mutable global state!

fun incrementCounter() {
    globalCounter++  // 위험: 전역 상태 변경
}
```

### 패키지 구조 기준

```kotlin
// 파일: com/example/utils/StringUtils.kt
package com.example.utils

fun String.toSnakeCase(): String = // ...
fun String.toCamelCase(): String = // ...

// 파일: com/example/utils/DateUtils.kt
package com.example.utils

fun LocalDate.isWeekend(): Boolean = // ...
fun LocalDate.addBusinessDays(days: Int): LocalDate = // ...

// 파일: com/example/order/OrderValidation.kt
package com.example.order

internal fun validateOrderItems(items: List<OrderItem>): ValidationResult = // ...
internal fun validateOrderAmount(amount: Int): ValidationResult = // ...

// ✅ 좋은 예: 도메인별로 패키지 분리
// com/example/order/OrderConstants.kt
const val MAX_ORDER_AMOUNT = 10000000
const val MIN_ORDER_AMOUNT = 1000

// com/example/user/UserConstants.kt
const val PASSWORD_MIN_LENGTH = 8
const val MAX_LOGIN_ATTEMPTS = 5

// ❌ 나쁜 예: 모든 유틸을 하나의 파일에
// Utils.kt
fun formatCurrency(...) = ...
fun validateEmail(...) = ...
fun calculateTax(...) = ...
fun parseDate(...) = ...
// → 파일이 너무 커지고 책임이 불명확
```

### 언어 설계 관점에서의 이유

Top-level declaration은 "모든 것이 클래스일 필요는 없다"는 실용주의를 반영한다:

1. **JVM static 메서드로 컴파일** - 성능 오버헤드 없음
2. **패키지 단위 네임스페이스** - import로 명시적 사용
3. **Private 제한 가능** - 파일 내부에서만 사용하는 헬퍼 함수
4. **객체 지향과 절차 지향 혼합** - 상황에 맞는 패러다임 선택

### 실무 사용 시나리오

**Constants 정의:**
```kotlin
// ApiConstants.kt
package com.example.api

const val API_VERSION = "v1"
const val BASE_URL = "https://api.example.com"
const val TIMEOUT_SECONDS = 30L

object HttpHeaders {
    const val AUTHORIZATION = "Authorization"
    const val CONTENT_TYPE = "Content-Type"
}
```

**Factory 함수:**
```kotlin
// UserFactory.kt
package com.example.user

fun createUser(
    name: String,
    email: String,
    role: UserRole = UserRole.USER
): User {
    return User(
        id = generateId(),
        name = name,
        email = email,
        role = role,
        createdAt = LocalDateTime.now()
    )
}

fun createAdminUser(name: String, email: String): User {
    return createUser(name, email, UserRole.ADMIN)
}
```

**Validation 함수:**
```kotlin
// Validators.kt
package com.example.validation

fun validateEmail(email: String): ValidationResult {
    return when {
        email.isBlank() -> ValidationResult.Invalid("이메일은 필수입니다")
        !email.matches(EMAIL_REGEX) -> ValidationResult.Invalid("형식이 올바르지 않습니다")
        else -> ValidationResult.Valid
    }
}

fun validatePassword(password: String): ValidationResult {
    return when {
        password.length < PASSWORD_MIN_LENGTH -> 
            ValidationResult.Invalid("비밀번호는 ${PASSWORD_MIN_LENGTH}자 이상이어야 합니다")
        !password.any { it.isDigit() } -> 
            ValidationResult.Invalid("숫자를 포함해야 합니다")
        else -> ValidationResult.Valid
    }
}
```

### 트레이드오프 / 남용 시 문제

**❌ 전역 상태 남용:**
```kotlin
// 나쁜 예
var currentUser: User? = null  // Global mutable state
var isLoggedIn = false

fun login(user: User) {
    currentUser = user
    isLoggedIn = true
}
// → 상태 관리 클래스로 캡슐화해야 함
```

**❌ 너무 많은 top-level 함수:**
```kotlin
// Utils.kt - 500줄
fun function1() = ...
fun function2() = ...
// ... 50개 함수
// → 응집도 낮음, 파일 분리 필요
```

**✅ 적절한 사용:**
```kotlin
// 좋은 예: 순수 함수, 패키지별 정리
// com/example/utils/FormatUtils.kt
fun formatPhoneNumber(phone: String): String = ...
fun formatCurrency(amount: Int): String = ...

// private 헬퍼 함수
private fun sanitize(input: String): String = ...
```

### 사용 기준 체크리스트

**Do:**
- [ ] 순수 함수는 top-level로 정의
- [ ] 상수는 const val로 top-level 선언
- [ ] 패키지 단위로 관련 함수/상수 그룹화
- [ ] Private으로 파일 내부 제한
- [ ] Factory 함수는 top-level 허용

**Don't:**
- [ ] Mutable top-level property 금지
- [ ] 한 파일에 top-level 함수 20개 이상 금지
- [ ] 전역 상태 관리를 top-level로 하지 않기
- [ ] 비즈니스 로직은 클래스로 캡슐화
- [ ] 테스트하기 어려운 top-level 함수 지양

---

## Object / Companion Object

### 한 줄 요약
**싱글톤과 정적 멤버를 언어 차원에서 지원하되, 남용하면 테스트 불가능한 전역 상태가 된다.**

### 기능 개요

Object는 싱글톤을 선언하고, Companion object는 클래스의 정적 멤버를 제공한다. 편리하지만, 전역 상태와 강결합을 만들기 쉽다.

### 싱글톤

```kotlin
// Object: 싱글톤 선언
object DatabaseConfig {
    const val HOST = "localhost"
    const val PORT = 3306
    
    fun createConnection(): Connection {
        // 연결 생성
    }
}

// 사용: 직접 접근
val conn = DatabaseConfig.createConnection()

// ✅ 좋은 예: 상태 없는 유틸리티
object JsonParser {
    private val mapper = ObjectMapper()
    
    fun <T> parse(json: String, clazz: Class<T>): T {
        return mapper.readValue(json, clazz)
    }
}

// ✅ 좋은 예: Enum 처럼 사용
sealed class Result {
    data class Success(val data: String) : Result()
    data class Error(val message: String) : Result()
    object Loading : Result()  // 상태 없는 싱글톤
}

// ❌ 나쁜 예: 가변 상태를 가진 싱글톤
object UserSession {
    var currentUser: User? = null  // Global mutable state
    var loginTime: LocalDateTime? = null
    
    fun login(user: User) {
        currentUser = user
        loginTime = LocalDateTime.now()
    }
}
// 문제: 테스트 격리 불가, 멀티스레드 위험

// ✅ 개선: 상태를 외부에서 주입
class UserSession(
    private var currentUser: User? = null
) {
    fun login(user: User) {
        currentUser = user
    }
    
    fun getCurrentUser(): User? = currentUser
}
```

### 팩토리 패턴

```kotlin
// Companion object: 정적 멤버 제공
class User private constructor(
    val id: Long,
    val name: String,
    val email: String
) {
    companion object {
        // Factory 함수
        fun create(name: String, email: String): User {
            validateEmail(email)
            return User(
                id = generateId(),
                name = name,
                email = email
            )
        }
        
        fun fromDto(dto: UserDto): User {
            return User(
                id = dto.id,
                name = dto.name,
                email = dto.email
            )
        }
        
        // 상수
        private const val EMAIL_REGEX = "..."
        
        // Private 헬퍼
        private fun validateEmail(email: String) {
            require(email.matches(EMAIL_REGEX.toRegex()))
        }
        
        private fun generateId(): Long {
            return System.currentTimeMillis()
        }
    }
}

// 사용
val user = User.create("백도현", "baek@example.com")
val userFromDto = User.fromDto(dto)

// ✅ 좋은 예: Named constructor 패턴
data class Money(
    val amount: BigDecimal,
    val currency: Currency
) {
    companion object {
        fun won(amount: Long) = Money(
            amount = BigDecimal(amount),
            currency = Currency.getInstance("KRW")
        )
        
        fun dollar(amount: Double) = Money(
            amount = BigDecimal(amount),
            currency = Currency.getInstance("USD")
        )
        
        val ZERO = Money(BigDecimal.ZERO, Currency.getInstance("KRW"))
    }
}

val price = Money.won(10000)
val usdPrice = Money.dollar(100.0)

// ✅ 좋은 예: Extension on companion
interface UserRepository {
    companion object
}

fun UserRepository.Companion.create(dataSource: DataSource): UserRepository {
    return JdbcUserRepository(dataSource)
}

val repository = UserRepository.create(dataSource)
```

### 언어 설계 관점에서의 이유

Object와 companion object는 "정적 멤버도 타입 안전하게"를 구현한다:

1. **싱글톤 보장** - object는 스레드 안전하게 지연 초기화됨
2. **네임스페이스** - companion 내부는 클래스와 별도 네임스페이스
3. **인터페이스 구현 가능** - object/companion도 인터페이스 구현 가능
4. **확장 가능** - companion object에 extension 함수 추가 가능

### 실무 사용 시나리오

**Configuration:**
```kotlin
object AppConfig {
    val databaseUrl: String = System.getenv("DB_URL") ?: "jdbc:mysql://localhost:3306/db"
    val apiKey: String = System.getenv("API_KEY") ?: throw IllegalStateException("API_KEY required")
    val isProduction: Boolean = System.getenv("ENV") == "production"
}

// 사용
if (AppConfig.isProduction) {
    // production 로직
}
```

**Logging:**
```kotlin
class OrderService {
    companion object {
        private val log = LoggerFactory.getLogger(OrderService::class.java)
    }
    
    fun createOrder(request: CreateOrderRequest) {
        log.info("Creating order for user: ${request.userId}")
        // ...
    }
}
```

**DSL Builder:**
```kotlin
class HtmlBuilder {
    companion object {
        fun html(init: HtmlTag.() -> Unit): HtmlTag {
            return HtmlTag("html").apply(init)
        }
    }
}

val page = HtmlBuilder.html {
    head {
        title("My Page")
    }
    body {
        h1("Hello World")
    }
}
```

### 트레이드오프 / 남용 시 문제

**❌ 테스트 불가능한 싱글톤:**
```kotlin
// 나쁜 예
object EmailService {
    fun sendEmail(to: String, subject: String, body: String) {
        // 실제 이메일 전송
        SMTP.send(to, subject, body)
    }
}

class UserService {
    fun registerUser(user: User) {
        // EmailService에 강결합
        EmailService.sendEmail(user.email, "Welcome", "...")
    }
}
// 테스트 시 실제 이메일이 발송됨!

// 좋은 예: 인터페이스 + DI
interface EmailService {
    fun sendEmail(to: String, subject: String, body: String)
}

class SmtpEmailService : EmailService {
    override fun sendEmail(to: String, subject: String, body: String) {
        SMTP.send(to, subject, body)
    }
}

class UserService(private val emailService: EmailService) {
    fun registerUser(user: User) {
        emailService.sendEmail(user.email, "Welcome", "...")
    }
}
```

**❌ Companion에 비즈니스 로직:**
```kotlin
// 나쁜 예
class Order {
    companion object {
        fun calculateDiscount(order: Order): Int {
            // 복잡한 비즈니스 로직
            return when {
                order.amount > 100000 -> order.amount * 0.2.toInt()
                order.amount > 50000 -> order.amount * 0.1.toInt()
                else -> 0
            }
        }
    }
}
// → 인스턴스 메서드여야 함
```

### 사용 기준 체크리스트

**Do:**
- [ ] 상수 정의는 companion object 활용
- [ ] Factory 함수는 companion object에 배치
- [ ] 로거는 companion object에 private으로
- [ ] 상태 없는 유틸리티는 object 허용
- [ ] Sealed class의 싱글톤 케이스는 object 사용

**Don't:**
- [ ] Mutable 상태를 object에 두지 않기
- [ ] 외부 의존성이 있으면 인터페이스 + DI 사용
- [ ] Companion에 복잡한 비즈니스 로직 금지
- [ ] 테스트 대역(Mock) 필요하면 싱글톤 지양
- [ ] Object보다 top-level 함수가 나은지 고려

---

## Inline Function & Reified

### 한 줄 요약
**함수 호출을 인라인화하고 제네릭 타입을 런타임에 보존하되, 남용하면 바이트코드 크기 폭발한다.**

### 기능 개요

Inline function은 함수 호출을 호출 지점에 직접 삽입하며, reified는 제네릭 타입 정보를 런타임에 유지한다. 람다 오버헤드 제거와 타입 안전성 강화에 유용하다.

### 제네릭 타입 안전성

```kotlin
// 일반 제네릭: 타입 소거로 런타임에 타입 정보 없음
fun <T> parseJson(json: String, clazz: Class<T>): T {
    return ObjectMapper().readValue(json, clazz)
}

// 사용: 클래스 명시 필요
val user = parseJson(json, User::class.java)  // 불편

// Inline + reified: 타입 정보 보존
inline fun <reified T> parseJson(json: String): T {
    return ObjectMapper().readValue(json, T::class.java)
}

// 사용: 타입 추론
val user: User = parseJson(json)
val users: List<User> = parseJson(json)

// ✅ 좋은 예: 타입 안전한 캐스팅
inline fun <reified T> Any.safeCast(): T? {
    return this as? T
}

val value: Any = "hello"
val str: String? = value.safeCast()  // OK
val num: Int? = value.safeCast()  // null

// ✅ 좋은 예: Collection 필터링
inline fun <reified T> List<*>.filterIsInstance(): List<T> {
    return this.mapNotNull { it as? T }
}

val mixed: List<Any> = listOf(1, "two", 3, "four")
val strings: List<String> = mixed.filterIsInstance()  // ["two", "four"]

// Inline으로 람다 오버헤드 제거
inline fun <T> measureTime(block: () -> T): T {
    val start = System.currentTimeMillis()
    val result = block()
    val end = System.currentTimeMillis()
    println("Took ${end - start}ms")
    return result
}

// 사용: 람다가 inline되어 객체 생성 없음
val result = measureTime {
    complexCalculation()
}
```

### 성능 고려 사항

```kotlin
// ✅ 좋은 예: 작은 함수를 inline
inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T {
    if (condition) block()
    return this
}

val user = User().applyIf(isAdmin) {
    role = UserRole.ADMIN
}

// ❌ 나쁜 예: 큰 함수를 inline
inline fun processLargeData(data: List<Data>) {
    // 100줄의 복잡한 로직
    // ...
}
// 문제: 호출하는 모든 곳에 100줄이 복사됨 → 바이트코드 크기 증가

// ✅ 개선: 핵심 부분만 inline
inline fun processData(data: List<Data>, transform: (Data) -> Data) {
    data.forEach { transform(it) }  // 람다만 inline
}

fun actualProcessing(data: Data): Data {
    // 복잡한 로직은 별도 함수로
    return data
}

// noinline: 특정 람다만 inline 제외
inline fun performOperation(
    data: List<Int>,
    inline operation: (Int) -> Int,  // inline됨
    noinline logger: (String) -> Unit  // inline 안 됨 (다른 함수에 전달 가능)
) {
    val result = data.map(operation)
    logger("Processed ${result.size} items")
}

// crossinline: non-local return 방지
inline fun runInBackground(crossinline block: () -> Unit) {
    thread {
        block()  // 여기서 return하면 안 되므로 crossinline
    }
}
```

### 언어 설계 관점에서의 이유

Inline과 reified는 "성능과 타입 안전성을 동시에"를 추구한다:

1. **람다 객체 생성 제거** - 고차 함수의 성능 오버헤드 제거
2. **타입 소거 문제 해결** - 제네릭 타입을 런타임에 사용 가능
3. **컴파일 타임 최적화** - 함수 호출 오버헤드 제거
4. **DSL 성능 향상** - 빌더 패턴, fluent API의 성능 개선

### 실무 사용 시나리오

**API 응답 파싱:**
```kotlin
inline fun <reified T> Response.parseBody(): T {
    val json = body?.string() ?: throw EmptyBodyException()
    return Json.decodeFromString(json)
}

// 사용
val user: User = response.parseBody()
val orders: List<Order> = response.parseBody()
```

**의존성 주입:**
```kotlin
inline fun <reified T : Any> ApplicationContext.getBean(): T {
    return getBean(T::class.java)
}

// Spring에서 사용
val userService = applicationContext.getBean<UserService>()
```

**로깅 최적화:**
```kotlin
inline fun Logger.debugIf(condition: Boolean, message: () -> String) {
    if (isDebugEnabled && condition) {
        debug(message())  // condition false면 message() 실행 안 됨
    }
}

// 사용: 조건부 로깅의 성능 최적화
logger.debugIf(request.isInternal) {
    "Processing internal request: ${request.toDetailedString()}"  // 조건 false면 실행 안 됨
}
```

### 트레이드오프 / 남용 시 문제

**❌ 큰 함수를 inline:**
```kotlin
// 나쁜 예: 50줄 함수를 inline
inline fun complexBusinessLogic(data: Data): Result {
    // 50줄의 복잡한 로직
    // ...
    return result
}
// 호출하는 100곳에 50줄씩 복사 → 바이트코드 5000줄 증가
```

**❌ 불필요한 reified:**
```kotlin
// 나쁜 예: reified가 필요 없는 경우
inline fun <reified T> printType() {
    println(T::class.simpleName)  // 타입 정보 사용 안 함
}
// → 일반 제네릭으로 충분
```

**✅ 적절한 사용:**
```kotlin
// 좋은 예: 작고 자주 호출되는 함수
inline fun <T> T.also(block: (T) -> Unit): T {
    block(this)
    return this
}
```

### 사용 기준 체크리스트

**Do:**
- [ ] 람다 파라미터가 있는 고차 함수는 inline 고려
- [ ] 제네릭 타입 정보가 런타임에 필요하면 reified 사용
- [ ] 함수가 10줄 이하로 작으면 inline 허용
- [ ] Collection API 확장 함수는 inline 적용
- [ ] noinline/crossinline으로 세밀하게 제어

**Don't:**
- [ ] 큰 함수(30줄 이상)는 inline 금지
- [ ] 타입 정보 사용 안 하면 reified 쓰지 않기
- [ ] 모든 함수를 습관적으로 inline하지 않기
- [ ] 바이트코드 크기 증가 모니터링
- [ ] Public API는 inline 신중히 결정 (바이너리 호환성)

---

## Destructuring Declaration

### 한 줄 요약
**객체를 여러 변수로 분해하되, Pair/Triple 남용은 가독성을 파괴한다.**

### 기능 개요

Destructuring declaration은 객체의 프로퍼티를 여러 변수로 한 번에 추출한다. Data class와 Pair/Triple에서 유용하지만, 과도하게 사용하면 의미를 잃는다.

### Pair / Triple 남용 경계

```kotlin
// Data class destructuring
data class User(val id: Long, val name: String, val email: String)

val user = User(1, "백도현", "baek@example.com")
val (id, name, email) = user
println("$id: $name")

// ✅ 좋은 예: for loop에서 활용
data class Entry(val key: String, val value: Int)
val entries = listOf(Entry("A", 1), Entry("B", 2))

for ((key, value) in entries) {
    println("$key -> $value")
}

// Map도 가능
val map = mapOf("A" to 1, "B" to 2)
for ((key, value) in map) {
    println("$key: $value")
}

// ❌ 나쁜 예: Pair 남용
fun getUserInfo(): Pair<String, Int> {
    return "백도현" to 30
}

val (name, age) = getUserInfo()
// 문제: name, age가 무엇인지 함수 시그니처만으로 불명확

// ❌ 더 나쁜 예: Triple 사용
fun getOrderInfo(): Triple<Long, String, Int> {
    return Triple(1, "백도현", 10000)
}

val (orderId, userName, amount) = getOrderInfo()
// 문제: 세 값의 관계를 코드로 알 수 없음

// ✅ 개선: Data class 사용
data class OrderInfo(
    val orderId: Long,
    val userName: String,
    val amount: Int
)

fun getOrderInfo(): OrderInfo {
    return OrderInfo(1, "백도현", 10000)
}

val orderInfo = getOrderInfo()
// 또는 필요하면 destructuring
val (orderId, userName, amount) = getOrderInfo()
```

### 명확한 의도 표현

```kotlin
// ✅ 좋은 예: 일부만 추출
val (id, name, _) = user  // email 무시
val (_, _, email) = user  // id, name 무시

// ✅ 좋은 예: 지역 변수 스코프 제한
fun processUser(user: User) {
    val (id, name) = user
    // id, name만 이 함수에서 사용
    println("Processing $id: $name")
}

// ✅ 좋은 예: 함수에서 여러 값 반환 (간단한 경우)
data class ValidationResult(val isValid: Boolean, val message: String)

fun validate(input: String): ValidationResult {
    return if (input.isBlank()) {
        ValidationResult(false, "필수 값입니다")
    } else {
        ValidationResult(true, "")
    }
}

val (isValid, message) = validate(input)
if (!isValid) {
    println(message)
}

// ❌ 나쁜 예: 너무 많은 프로퍼티
data class HugeData(
    val p1: String,
    val p2: String,
    // ... 15개 더
)

val (v1, v2, v3, v4, v5, v6, v7, v8) = hugeData
// 가독성 저하, 순서 혼동 위험

// 커스텀 destructuring (componentN)
class Point(val x: Int, val y: Int) {
    operator fun component1() = x
    operator fun component2() = y
}

val point = Point(10, 20)
val (x, y) = point
```

### 언어 설계 관점에서의 이유

Destructuring은 "필요한 것만 추출"하여 코드를 간결하게 만든다:

1. **componentN 함수 활용** - Data class가 자동 생성, 커스텀 가능
2. **순서 기반 추출** - 프로퍼티 이름이 아닌 선언 순서로 매칭
3. **일부 무시 가능** - _ 로 불필요한 값 제외
4. **Loop와 통합** - for, map 등에서 자연스럽게 사용

### 실무 사용 시나리오

**함수 다중 반환:**
```kotlin
// ✅ 간단한 경우: Pair 허용
fun divide(a: Int, b: Int): Pair<Int, Int> {
    return (a / b) to (a % b)
}

val (quotient, remainder) = divide(10, 3)

// ✅ 복잡한 경우: Data class
data class SearchResult(
    val items: List<Item>,
    val totalCount: Int,
    val hasMore: Boolean
)

fun search(keyword: String): SearchResult {
    // ...
}

val (items, totalCount, hasMore) = search("kotlin")
```

**Collection 처리:**
```kotlin
// Map entries
val configs = mapOf(
    "host" to "localhost",
    "port" to "3306"
)

configs.forEach { (key, value) ->
    println("Config[$key] = $value")
}

// Grouped data
data class Product(val category: String, val price: Int)
val products = listOf(
    Product("전자", 1000),
    Product("의류", 500)
)

val grouped = products.groupBy { it.category }
grouped.forEach { (category, products) ->
    println("$category: ${products.size}개")
}
```

**Lambda 파라미터:**
```kotlin
val users = listOf(
    User(1, "Alice", "alice@example.com"),
    User(2, "Bob", "bob@example.com")
)

// Destructuring in lambda
users.map { (id, name, _) ->
    "$id: $name"
}
```

### 트레이드오프 / 남용 시 문제

**❌ Pair/Triple의 과도한 사용:**
```kotlin
// 나쁜 예: 도메인 모델을 Pair로
fun getUser(): Pair<String, String> {
    return "백도현" to "baek@example.com"
}
// → Data class로 명확히 표현

// 나쁜 예: Triple 사용
fun getTransaction(): Triple<Long, Int, String> {
    return Triple(1L, 10000, "completed")
}
// → 의미 불명, Data class 필수
```

**❌ 순서 의존성:**
```kotlin
data class User(val name: String, val email: String, val age: Int)

val (email, name, age) = user  // 잘못된 순서!
// email에 name이, name에 email이 할당됨
```

**✅ 명시적 사용:**
```kotlin
// 좋은 예: 의도가 명확한 경우에만 사용
val (isSuccess, errorMessage) = validateInput(input)
```

### 사용 기준 체크리스트

**Do:**
- [ ] Data class는 필요 시 destructuring 활용
- [ ] For loop에서 map entry, pair 추출
- [ ] 간단한 함수 반환값 (2개 이하)은 Pair 허용
- [ ] 일부 프로퍼티만 필요하면 _ 활용

**Don't:**
- [ ] Triple 사용 금지, Data class로 대체
- [ ] 5개 이상 프로퍼티 destructuring 지양
- [ ] 도메인 모델을 Pair/Triple로 표현 금지
- [ ] 순서 의존성 주의, 명확한 네이밍 필수
- [ ] Public API 반환값에 Pair/Triple 지양

---

## Collection API & Sequence

### 한 줄 요약
**강력한 함수형 API로 컬렉션을 조작하되, eager/lazy 구분 없이 쓰면 성능 문제가 발생한다.**

### 기능 개요

Kotlin의 Collection API는 map, filter, reduce 등 강력한 함수형 연산을 제공한다. Sequence는 lazy evaluation으로 대용량 데이터 처리에 적합하다.

### Eager vs Lazy

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)

// Eager evaluation: 즉시 실행, 중간 결과 생성
val result1 = numbers
    .filter { it > 2 }  // 새 List 생성
    .map { it * 2 }     // 또 다른 List 생성
    .sum()

// Lazy evaluation (Sequence): 최종 연산 시 한 번에 실행
val result2 = numbers.asSequence()
    .filter { it > 2 }  // 중간 List 생성 안 함
    .map { it * 2 }
    .sum()              // 최종 연산

// ✅ 좋은 예: 대용량 데이터는 Sequence
fun processLargeFile(file: File): List<String> {
    return file.bufferedReader()
        .lineSequence()  // Lazy sequence
        .filter { it.isNotBlank() }
        .map { it.trim() }
        .take(1000)  // 앞 1000개만
        .toList()
}

// ❌ 나쁜 예: Eager로 대용량 처리
fun processLargeFileEager(file: File): List<String> {
    return file.readLines()  // 전체 파일을 메모리에!
        .filter { it.isNotBlank() }
        .map { it.trim() }
        .take(1000)
}

// Sequence의 특징: 중간 연산 지연
val seq = sequenceOf(1, 2, 3, 4, 5)
    .filter { 
        println("filter: $it")
        it > 2 
    }
    .map { 
        println("map: $it")
        it * 2 
    }

// 아직 실행 안 됨
println("Starting")
val result = seq.toList()  // 이제 실행
// 출력:
// Starting
// filter: 1
// filter: 2
// filter: 3
// map: 3
// filter: 4
// map: 4
// filter: 5
// map: 5
```

### 성능 기준

```kotlin
// ✅ Collection (Eager) 사용 기준
// - 데이터 크기가 작음 (수백~수천 개)
// - 여러 번 순회할 예정
// - 중간 결과를 캐싱하고 싶음

val users = userRepository.findAll()  // 100명
val activeUsers = users.filter { it.isActive }  // OK: 작은 데이터
val adminUsers = users.filter { it.role == ADMIN }  // users 재사용

// ✅ Sequence (Lazy) 사용 기준
// - 데이터 크기가 큼 (수만~수백만 개)
// - 한 번만 순회
// - Early termination (take, first 등)

fun findFirstMatchingUser(users: List<User>): User? {
    return users.asSequence()
        .filter { it.isActive }
        .map { enrichUser(it) }  // 비용이 큰 작업
        .firstOrNull { it.score > 100 }
    // 첫 번째 매칭되면 즉시 리턴, 나머지는 계산 안 함
}

// 성능 비교: 백만 개 데이터
val millionNumbers = (1..1_000_000).toList()

// Eager: 중간 컬렉션 2개 생성 (메모리 많이 사용)
val eagerResult = millionNumbers
    .filter { it % 2 == 0 }  // 50만 개 List 생성
    .map { it * 2 }           // 또 다른 50만 개 List 생성
    .take(10)

// Lazy: 중간 컬렉션 생성 없음, 10개만 계산
val lazyResult = millionNumbers.asSequence()
    .filter { it % 2 == 0 }
    .map { it * 2 }
    .take(10)
    .toList()

// ❌ 나쁜 예: Sequence를 여러 번 순회
val seq = largeList.asSequence().filter { it.isActive }
val count = seq.count()  // 한 번 순회
val items = seq.toList()  // 또 한 번 순회
// → Sequence는 재사용 비효율적, 즉시 toList()로 변환

// ✅ 개선
val filteredList = largeList.asSequence().filter { it.isActive }.toList()
val count = filteredList.size
val items = filteredList
```

### 언어 설계 관점에서의 이유

Collection API와 Sequence는 "함수형 프로그래밍을 성능 저하 없이" 제공한다:

1. **Fluent API** - 체이닝으로 가독성 높은 데이터 변환
2. **Lazy evaluation** - 필요한 만큼만 계산하여 성능 최적화
3. **Null 안전성** - mapNotNull, filterNotNull 등으로 안전한 변환
4. **Type-safe builders** - 타입 안전한 컬렉션 생성

### 실무 사용 시나리오

**DTO 변환:**
```kotlin
class UserService {
    fun getActiveUsers(): List<UserResponse> {
        return userRepository.findAll()
            .filter { it.isActive }
            .map { it.toResponse() }
    }
}
```

**데이터 집계:**
```kotlin
fun calculateRevenue(orders: List<Order>): Map<String, Int> {
    return orders
        .filter { it.status == OrderStatus.COMPLETED }
        .groupBy { it.productCategory }
        .mapValues { (_, orders) -> 
            orders.sumOf { it.amount }
        }
}
```

**파일 처리:**
```kotlin
fun processLogFile(file: File): List<ErrorLog> {
    return file.bufferedReader()
        .lineSequence()  // Lazy
        .filter { line -> line.contains("ERROR") }
        .mapNotNull { line -> parseErrorLog(line) }
        .take(100)  // 처음 100개만
        .toList()
}
```

**데이터베이스 결과 스트리밍:**
```kotlin
fun streamLargeDataset(): Sequence<ProcessedData> {
    return repository.findAllAsStream()
        .asSequence()
        .filter { it.needsProcessing() }
        .map { process(it) }
    // toList() 호출 시점에 실행
}
```

### 트레이드오프 / 남용 시 문제

**❌ 작은 데이터에 Sequence:**
```kotlin
// 나쁜 예: 10개 데이터에 Sequence
val result = listOf(1, 2, 3).asSequence()
    .filter { it > 1 }
    .map { it * 2 }
    .toList()
// → Sequence 생성 오버헤드만 추가, 이득 없음
```

**❌ 과도한 체이닝:**
```kotlin
// 나쁜 예: 가독성 저하
val result = data
    .filter { it.isActive }
    .map { it.toDto() }
    .groupBy { it.category }
    .mapValues { it.value.sumOf { it.amount } }
    .filterValues { it > 1000 }
    .toList()
    .sortedByDescending { it.second }
    .take(10)
// → 중간에 변수로 분리하여 의도 명확히
```

**✅ 적절한 분리:**
```kotlin
// 좋은 예
val activeDtos = data
    .filter { it.isActive }
    .map { it.toDto() }

val categoryRevenue = activeDtos
    .groupBy { it.category }
    .mapValues { (_, items) -> items.sumOf { it.amount } }

val topCategories = categoryRevenue
    .filterValues { it > 1000 }
    .toList()
    .sortedByDescending { (_, revenue) -> revenue }
    .take(10)
```

### 사용 기준 체크리스트

**Do:**
- [ ] 데이터 크기 고려: 작으면 List, 크면 Sequence
- [ ] Early termination (take, first)에는 Sequence 유리
- [ ] 여러 번 순회할 데이터는 List 유지
- [ ] 파일/스트림 처리는 lineSequence 활용
- [ ] 체이닝은 3~5단계까지, 그 이상은 변수 분리

**Don't:**
- [ ] 습관적으로 asSequence 남발 금지
- [ ] Sequence를 여러 번 순회하지 않기
- [ ] 10단계 이상 체이닝 금지 (가독성)
- [ ] 부수 효과 있는 연산은 forEach 명시적 사용
- [ ] map/filter보다 for 루프가 명확하면 for 사용

---

## 남용 주의 블랙리스트

### Scope Function 중첩

```kotlin
// ❌ 최악의 예
user?.let { u ->
    u.company?.let { company ->
        company.address?.run {
            city.also { c ->
                println(c)
            }
        }
    }
}

// ✅ 개선
val city = user?.company?.address?.city ?: return
println(city)
```

**규칙:**
- Scope function 중첩은 절대 2단계 이하
- let 체이닝보다 Elvis + early return
- this/it 혼용 금지

### Extension 남발

```kotlin
// ❌ 나쁜 예: 모든 곳에 extension
fun String.toUser(): User = Json.decodeFromString(this)
fun String.toOrder(): Order = Json.decodeFromString(this)
fun String.toProduct(): Product = Json.decodeFromString(this)
// ... 20개 더

// ✅ 개선: 명시적 파서 클래스
object JsonParser {
    inline fun <reified T> parse(json: String): T = 
        Json.decodeFromString(json)
}
```

**규칙:**
- 일반 타입(String, Int)에 도메인 로직 extension 금지
- Public extension은 팀 합의 필수
- 한 파일에 extension 10개 이상 금지

### Pair / Triple 과다

```kotlin
// ❌ 나쁜 예
fun getOrderInfo(): Triple<Long, String, Int> = 
    Triple(1, "백도현", 10000)

fun processTransaction(): Pair<Pair<String, Int>, Boolean> = 
    Pair(Pair("TXN001", 5000), true)

// ✅ 개선
data class OrderInfo(val id: Long, val userName: String, val amount: Int)
data class Transaction(val id: String, val amount: Int, val isSuccess: Boolean)
```

**규칙:**
- Triple 사용 절대 금지
- Pair는 내부 구현에만, API 반환 금지
- 3개 이상 값은 무조건 Data class

### Inline 남용

```kotlin
// ❌ 나쁜 예: 큰 함수를 inline
inline fun processComplexData(data: List<Data>): Result {
    // 100줄의 복잡한 로직
    // ...
}

// ✅ 개선: 핵심만 inline
inline fun <T> measureTime(block: () -> T): T {
    val start = System.currentTimeMillis()
    return block().also {
        println("Took ${System.currentTimeMillis() - start}ms")
    }
}

fun processComplexData(data: List<Data>): Result = measureTime {
    // 복잡한 로직은 일반 함수로
    actualProcessing(data)
}
```

**규칙:**
- 함수가 30줄 이상이면 inline 금지
- Reified가 필요 없으면 inline 쓰지 않기
- 바이트코드 크기 모니터링

---

## 2탄 요약 – 팀 Kotlin 사용 규칙 체크리스트

### Extension Function
1. **Public extension은 팀 코드 리뷰 필수** - 전역 네임스페이스 오염 방지
2. **String, Int 등 일반 타입에 도메인 로직 금지** - 특정 도메인 타입에만 extension
3. **Internal/Private으로 가시성 제한** - 패키지 내부에서만 사용
4. **한 타입에 extension 10개 이상 금지** - 응집도 저하 신호
5. **비즈니스 로직은 클래스 메서드로** - Extension은 유틸리티 성격만

### Scope Function
6. **Scope function 중첩 2단계 이하** - 3단계부터 가독성 급격히 저하
7. **Let 체이닝보다 Elvis + early return** - 명확한 null 처리
8. **This/it 혼용 금지** - 한 블록에서 this(apply, run)와 it(let, also) 동시 사용 금지
9. **의미 없는 scope function 남발 금지** - 단순 변수 할당은 let 불필요
10. **선택 기준 명확히: let(null 체크), apply(초기화), also(로깅)** - 용도 구분

### Top-level & Object
11. **Mutable top-level property 금지** - 전역 상태는 클래스로 캡슐화
12. **Top-level 함수는 순수 함수만** - 부수 효과 없는 유틸리티 함수
13. **Object의 mutable 상태 금지** - 싱글톤에 가변 상태 두지 않기
14. **의존성 있는 기능은 인터페이스 + DI** - Object보다 의존성 주입
15. **Companion에 복잡한 비즈니스 로직 금지** - Factory 함수, 상수만

### Inline & Reified
16. **함수 30줄 이상이면 inline 금지** - 바이트코드 크기 폭발
17. **Reified 필요 없으면 일반 제네릭** - 타입 정보 사용 안 하면 inline 불필요
18. **Public API inline은 신중히** - 바이너리 호환성 고려
19. **람다 파라미터 있어야 inline 고려** - 람다 없으면 inline 이점 없음

### Destructuring
20. **Triple 사용 절대 금지** - Data class로 대체
21. **Pair는 내부 구현만** - Public API 반환 타입 금지
22. **5개 이상 프로퍼티 destructuring 지양** - 순서 혼동 위험

### Collection & Sequence
23. **데이터 크기 고려: 작으면 List, 크면 Sequence** - 수천 개 기준
24. **Sequence 여러 번 순회 금지** - 한 번 순회 후 toList()
25. **체이닝은 3~5단계까지** - 그 이상은 변수로 분리
26. **10단계 이상 체이닝 절대 금지** - 가독성 파괴

### 일반 규칙
27. **Kotlin 기능은 "왜"를 설명할 수 있을 때만** - 편리함보다 명확성
28. **코드 리뷰에서 "이거 무슨 뜻이야?" 나오면 리팩토링** - 가독성 최우선
29. **신규 팀원이 이해 못하면 과도한 사용** - 진입 장벽 고려
30. **성능 최적화는 측정 후 적용** - 추측 금지, 프로파일링 필수
31. **언어 기능보다 도메인 로직이 먼저 보여야** - 기술보다 비즈니스
32. **모든 규칙은 예외 가능, 단 근거 명시** - 맹목적 규칙 금지

### 팀 합의 필수 사항
- [ ] Extension function naming convention (접두사/접미사)
- [ ] Scope function 사용 가이드 (언제 어떤 걸 쓸지)
- [ ] Sequence 사용 기준 (데이터 크기 threshold)
- [ ] Inline 허용 범위 (함수 크기, 호출 빈도)
- [ ] Code review에서 반드시 지적할 anti-pattern 목록

**마지막 원칙: "Kotlin다운 코드 < 읽기 쉬운 코드"**
