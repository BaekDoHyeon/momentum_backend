# Kotlin 고유 무기 가이드 1탄 – 안전성과 표현력을 만드는 핵심 기능들

## 목차 (Table of Contents)

1. [프롤로그](#프롤로그)
   - [Kotlin 고유 기능 가이드를 따로 보는 이유](#kotlin-고유-기능-가이드를-따로-보는-이유)
   - [이 문서의 범위와 전제](#이-문서의-범위와-전제)
2. [Null-safety 타입 시스템](#null-safety-타입-시스템)
   - [Nullable과 Non-null 타입](#nullable과-non-null-타입)
   - [Safe Call과 Elvis 연산자](#safe-call과-elvis-연산자)
   - [Smart Cast](#smart-cast)
   - [!! 사용 기준](#-사용-기준)
3. [Data Class – 불변 데이터 모델링의 핵심](#data-class--불변-데이터-모델링의-핵심)
   - [자동 생성 메커니즘](#자동-생성-메커니즘)
   - [copy와 Destructuring](#copy와-destructuring)
   - [DTO에 적합한 이유](#dto에-적합한-이유)
4. [Sealed Class / Sealed Interface](#sealed-class--sealed-interface)
   - [닫힌 타입 계층](#닫힌-타입-계층)
   - [상태와 결과 모델링](#상태와-결과-모델링)
   - [When의 Exhaustive 보장](#when의-exhaustive-보장)
5. [Expression 중심 언어](#expression-중심-언어)
   - [if/when/try Expression](#ifwhentry-expression)
   - [함수형 스타일 강화](#함수형-스타일-강화)
6. [Default Parameter & Named Argument](#default-parameter--named-argument)
   - [API 설계 방식의 변화](#api-설계-방식의-변화)
   - [가독성 향상 포인트](#가독성-향상-포인트)
7. [Value Class (Inline Class)](#value-class-inline-class)
   - [타입 안정성 강화](#타입-안정성-강화)
   - [도메인 값 보호](#도메인-값-보호)
8. [1탄 요약 – Kotlin 사고방식 핵심 정리](#1탄-요약--kotlin-사고방식-핵심-정리)

---

## 프롤로그

### Kotlin 고유 기능 가이드를 따로 보는 이유

Kotlin은 단순히 "간결한 문법의 언어"가 아니다. 언어 설계 단계부터 **타입 안전성**, **null 안전성**, **불변성 우선**, **표현력 극대화**라는 명확한 철학을 가지고 만들어진 현대적 언어다.

이 가이드는 Kotlin만이 제공하는 고유 기능들을 다룬다. 이미 Java ↔ Kotlin 관례 가이드를 통해 명명 규칙, 코드 스타일, 상호 호환성을 학습했다면, 이제는 **Kotlin의 설계 철학이 어떻게 코드의 안전성과 표현력을 높이는지** 이해할 차례다.

### 이 문서의 범위와 전제

**이 문서가 다루는 것:**
- Kotlin 언어 고유의 타입 시스템과 표현 방식
- 컴파일 타임 안전성을 보장하는 메커니즘
- 불변성과 명시성을 강화하는 언어 기능
- 실무에서 언제, 왜, 어떻게 사용해야 하는지에 대한 기준

**이 문서가 다루지 않는 것:**
- Java와의 장황한 비교 (필요 시 최소한으로만 언급)
- 단순 문법 나열식 설명
- 모든 언어 기능의 백과사전식 정리

**전제 조건:**
- Kotlin 기본 문법 이해
- 객체 지향 프로그래밍 경험
- Java ↔ Kotlin 관례 가이드 숙지

---

## Null-safety 타입 시스템

### 한 줄 요약
**타입 시스템 차원에서 null을 제어하여 NullPointerException을 컴파일 타임에 방지한다.**

### 기능 개요

Kotlin의 타입 시스템은 모든 타입을 **nullable**과 **non-null**로 명확히 구분한다. 이는 단순한 편의 기능이 아니라, 언어 차원에서 null 안전성을 보장하는 핵심 설계 철학이다.

### Nullable과 Non-null 타입

```kotlin
// Non-null: null을 할당할 수 없음
val name: String = "백도현"
// val name: String = null  // 컴파일 에러

// Nullable: null을 허용
val email: String? = null
val age: Int? = getAge()  // null 반환 가능한 함수

// 컴파일러가 강제하는 null 체크
fun processEmail(email: String?) {
    // email.length  // 컴파일 에러: nullable 타입은 직접 접근 불가
    
    if (email != null) {
        println(email.length)  // Smart cast: String?에서 String으로 자동 변환
    }
}
```

### Safe Call과 Elvis 연산자

```kotlin
// Safe Call (?.) - null이면 전체 표현식이 null
val length: Int? = email?.length

// Elvis 연산자 (?:) - null일 때 기본값 제공
val displayName: String = user.nickname ?: user.email ?: "Unknown"

// 실무 체이닝 패턴
val city: String = user?.address?.city ?: "서울"

// let과 결합하여 non-null 블록 실행
email?.let { 
    sendNotification(it)  // it은 non-null String
    logEmail(it)
}
```

### Smart Cast

```kotlin
fun processValue(value: Any) {
    // 타입 체크 후 자동 캐스팅
    if (value is String) {
        println(value.length)  // value는 자동으로 String 타입
    }
    
    // null 체크 후 자동 non-null 변환
    if (value != null) {
        println(value.toString())  // value는 non-null
    }
    
    // when과 결합
    when (value) {
        is Int -> println(value + 1)
        is String -> println(value.uppercase())
        null -> println("null value")
    }
}

// Smart cast가 불가능한 경우
class User(var email: String?)

fun process(user: User) {
    if (user.email != null) {
        // println(user.email.length)  // 컴파일 에러
        // 이유: var는 다른 스레드에서 변경 가능하므로 smart cast 불가
    }
    
    // 해결: 로컬 변수로 복사
    val email = user.email
    if (email != null) {
        println(email.length)  // OK: val은 smart cast 가능
    }
}
```

### !! 사용 기준

```kotlin
// !! (Not-null assertion) - "내가 보장한다, null이 아니다"
val length: Int = email!!.length

// ❌ 절대 사용하지 말아야 할 케이스
fun badExample(user: User?) {
    val name = user!!.name  // NPE 위험 그대로, Kotlin의 장점 포기
}

// ✅ 허용 가능한 극히 제한적인 케이스
class Repository {
    private var cache: List<Item>? = null
    
    fun initialize() {
        cache = loadFromDatabase()
    }
    
    fun getItems(): List<Item> {
        // 아키텍처상 initialize()가 먼저 호출됨을 보장하는 경우에만
        return cache!!  // 주석으로 보장 근거 명시 필수
    }
}

// 더 나은 대안: lateinit
class BetterRepository {
    private lateinit var cache: List<Item>
    
    fun initialize() {
        cache = loadFromDatabase()
    }
    
    fun getItems(): List<Item> = cache  // 초기화 안 됐으면 명확한 에러
}
```

### 언어 설계 관점에서의 이유

Kotlin은 "null은 있어도 되지만 무분별하게 흘러다녀서는 안 된다"는 철학을 가진다. 타입 시스템에 nullable/non-null을 통합함으로써:

1. **컴파일 타임에 null 체크를 강제**하여 런타임 NPE를 대부분 방지
2. **API 설계 시 null 가능성을 명시적으로 표현**하여 계약을 명확히 함
3. **Smart cast를 통해 불필요한 타입 변환 코드를 제거**하여 가독성 향상

### 실무 사용 시나리오

**Domain Entity에서:**
```kotlin
data class Order(
    val id: Long,
    val userId: Long,
    val items: List<OrderItem>,  // 절대 null이 아님을 보장
    val couponId: Long? = null,   // 선택적 값은 명시적으로 nullable
    val deliveryNote: String? = null
)
```

**Repository/Service Layer에서:**
```kotlin
interface UserRepository {
    fun findById(id: Long): User?  // 없을 수 있음을 명시
    fun getById(id: Long): User    // 없으면 예외 발생을 명시
}

class UserService(private val repository: UserRepository) {
    fun getActiveUser(id: Long): User {
        return repository.findById(id)
            ?.takeIf { it.isActive }
            ?: throw UserNotFoundException(id)
    }
}
```

### 트레이드오프 / 남용 시 문제

**❌ 과도한 nullable 타입:**
```kotlin
// 나쁜 예: 모든 것을 nullable로
data class BadUser(
    val id: Long?,
    val name: String?,
    val email: String?
)
```

**❌ Safe call 체이닝 남용:**
```kotlin
// Law of Demeter 위반 + null 체크 과다
val result = user?.company?.department?.manager?.email?.uppercase()
```

**✅ 적절한 null 처리:**
```kotlin
// 명시적인 null 체크와 조기 리턴
fun processUser(user: User?) {
    val validUser = user ?: return
    // 이후 코드는 non-null 보장
    sendEmail(validUser.email)
}
```

### 사용 기준 체크리스트

**Do:**
- [ ] 비즈니스 로직상 null이 의미 있는 경우에만 nullable 타입 사용
- [ ] API 반환 타입에서 null 가능성을 명시적으로 표현
- [ ] Smart cast가 가능한 구조로 코드 작성 (val 우선, 로컬 변수 활용)
- [ ] Elvis 연산자로 기본값 제공 또는 조기 리턴
- [ ] lateinit은 DI 프레임워크 의존성 주입 등 명확한 초기화 시점이 있을 때만

**Don't:**
- [ ] !! 연산자를 편의상 사용하지 않기 (주석으로 근거 명시 없이는 금지)
- [ ] Safe call 체이닝을 3단계 이상 중첩하지 않기
- [ ] var 프로퍼티에 의존하여 smart cast 기대하지 않기
- [ ] 모든 타입을 습관적으로 nullable로 선언하지 않기

---

## Data Class – 불변 데이터 모델링의 핵심

### 한 줄 요약
**불변 데이터를 표현하는 클래스를 선언만으로 완성하며, equals/hashCode/toString/copy를 자동 생성한다.**

### 기능 개요

Data class는 "데이터를 담는 것"이 주 목적인 클래스를 위한 특별한 선언 방식이다. 보일러플레이트 코드 제거를 넘어, **불변성을 우선으로 하는 설계 패턴을 언어 차원에서 지원**한다.

### 자동 생성 메커니즘

```kotlin
// 단 한 줄로 완전한 불변 데이터 클래스
data class User(
    val id: Long,
    val name: String,
    val email: String
)

// 컴파일러가 자동 생성하는 것들:
// 1. equals() - 모든 프로퍼티 기반 동등성 비교
// 2. hashCode() - 일관된 해시값
// 3. toString() - "User(id=1, name=백도현, email=...)"
// 4. copy() - 불변 객체 수정을 위한 복사 메서드
// 5. componentN() - 구조 분해 선언용

val user1 = User(1, "백도현", "baek@example.com")
val user2 = User(1, "백도현", "baek@example.com")

println(user1 == user2)  // true: 구조적 동등성
println(user1 === user2) // false: 다른 인스턴스
```

### copy와 Destructuring

```kotlin
// copy: 불변 객체의 일부만 변경
val original = User(1, "백도현", "baek@example.com")
val updated = original.copy(email = "new@example.com")
// original은 그대로, updated는 새 인스턴스

// Named argument와 결합하여 명확성 확보
val modified = original.copy(
    name = "도현",
    email = "dohyun@example.com"
)

// Destructuring: 구조 분해 선언
val (id, name, email) = user
println("User $id: $name")

// 필요한 것만 추출
val (id, name) = user  // email은 무시
val (_, _, email) = user  // id, name 무시

// for loop에서 활용
data class Entry(val key: String, val value: Int)
val entries = listOf(Entry("A", 1), Entry("B", 2))

for ((key, value) in entries) {
    println("$key -> $value")
}
```

### DTO에 적합한 이유

```kotlin
// API Request/Response DTO
data class CreateOrderRequest(
    val userId: Long,
    val items: List<OrderItemDto>,
    val couponCode: String? = null
)

data class OrderResponse(
    val orderId: Long,
    val status: OrderStatus,
    val totalAmount: Int,
    val createdAt: LocalDateTime
)

// Domain Entity → DTO 변환
data class OrderEntity(
    val id: Long,
    val userId: Long,
    val status: OrderStatus,
    val totalAmount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    fun toResponse() = OrderResponse(
        orderId = id,
        status = status,
        totalAmount = totalAmount,
        createdAt = createdAt
    )
}

// 계층 간 데이터 전달
class OrderService {
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        val entity = saveOrder(request)
        return entity.toResponse()
    }
}
```

### 언어 설계 관점에서의 이유

Data class는 "데이터는 불변이어야 한다"는 함수형 프로그래밍 원칙을 객체 지향 언어에 자연스럽게 통합한다:

1. **기본이 val** - 생성자 파라미터는 자동으로 불변 프로퍼티
2. **구조적 동등성** - 값 기반 비교가 기본 (참조 비교가 아님)
3. **copy를 통한 변경** - 불변 객체를 수정하는 안전한 방법 제공
4. **보일러플레이트 제거** - 데이터 클래스에 집중, 부수 코드는 컴파일러가 처리

### 실무 사용 시나리오

**Event Sourcing / Domain Event:**
```kotlin
sealed interface OrderEvent {
    data class Created(
        val orderId: Long,
        val userId: Long,
        val amount: Int
    ) : OrderEvent
    
    data class Cancelled(
        val orderId: Long,
        val reason: String
    ) : OrderEvent
}
```

**Configuration:**
```kotlin
data class DatabaseConfig(
    val host: String,
    val port: Int = 3306,
    val username: String,
    val password: String,
    val poolSize: Int = 10
)

// 환경별 설정 복사
val devConfig = DatabaseConfig(
    host = "localhost",
    username = "dev",
    password = "dev123"
)

val prodConfig = devConfig.copy(
    host = "prod.db.example.com",
    password = System.getenv("DB_PASSWORD")
)
```

### 트레이드오프 / 남용 시 문제

**❌ 비즈니스 로직이 있는 Entity를 data class로:**
```kotlin
// 나쁜 예: 도메인 로직이 있는데 data class 사용
data class Order(
    val id: Long,
    val items: MutableList<OrderItem>  // 가변 컬렉션!
) {
    fun addItem(item: OrderItem) {
        items.add(item)  // 불변성 깨짐
    }
}
```

**❌ 과도한 프로퍼티:**
```kotlin
// 나쁜 예: 프로퍼티가 너무 많음 (10개 이상)
data class HugeDto(
    val prop1: String,
    val prop2: String,
    // ... 15개 더
    val prop17: String
)  // 구조 분해 시 혼란, 생성자 인자 순서 혼동
```

**✅ 적절한 사용:**
```kotlin
// Value Object로 활용
data class Money(
    val amount: BigDecimal,
    val currency: Currency = Currency.getInstance("KRW")
) {
    operator fun plus(other: Money): Money {
        require(currency == other.currency)
        return copy(amount = amount + other.amount)
    }
}
```

### 사용 기준 체크리스트

**Do:**
- [ ] DTO, VO, Event, Configuration 등 순수 데이터 표현에 사용
- [ ] 모든 프로퍼티를 val로 선언하여 불변성 유지
- [ ] 프로퍼티는 5~7개 이내로 제한 (가독성)
- [ ] 컬렉션 프로퍼티는 불변 타입 사용 (List, Set, Map)
- [ ] copy()를 활용한 불변 수정 패턴 적용

**Don't:**
- [ ] 비즈니스 로직이 복잡한 Domain Entity에 사용하지 않기
- [ ] var 프로퍼티를 data class에 포함하지 않기
- [ ] Mutable 컬렉션을 프로퍼티로 노출하지 않기
- [ ] equals/hashCode 커스터마이징이 필요하면 data class 쓰지 않기

---

## Sealed Class / Sealed Interface

### 한 줄 요약
**제한된 타입 계층을 컴파일 타임에 보장하여, 모든 경우를 강제로 처리하게 만든다.**

### 기능 개요

Sealed class/interface는 "이 타입의 하위 타입은 이것들뿐이다"를 선언하는 기능이다. 이는 enum보다 유연하고, 일반 상속보다 안전한 **닫힌 타입 계층(Closed Type Hierarchy)**을 만든다.

### 닫힌 타입 계층

```kotlin
// Sealed class: 제한된 하위 클래스
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// Sealed interface (Kotlin 1.5+): 다중 상속 가능
sealed interface UiState {
    data object Loading : UiState
    data class Success(val data: List<Item>) : UiState
    data class Error(val message: String) : UiState
}

// 같은 파일 내에서만 상속 가능 (Kotlin 1.7+는 같은 패키지도 허용)
// 외부에서는 절대 상속 불가 → 타입 안전성 보장
```

### 상태와 결과 모델링

```kotlin
// API 호출 결과 모델링
sealed class ApiResult<out T> {
    data class Success<T>(
        val data: T,
        val cached: Boolean = false
    ) : ApiResult<T>()
    
    data class Error(
        val code: Int,
        val message: String,
        val throwable: Throwable? = null
    ) : ApiResult<Nothing>()
    
    data object NetworkError : ApiResult<Nothing>()
    data object Unauthorized : ApiResult<Nothing>()
}

// 사용 예시
class UserRepository {
    suspend fun getUser(id: Long): ApiResult<User> {
        return try {
            val response = api.getUser(id)
            when (response.code) {
                200 -> ApiResult.Success(response.body)
                401 -> ApiResult.Unauthorized
                else -> ApiResult.Error(response.code, response.message)
            }
        } catch (e: IOException) {
            ApiResult.NetworkError
        } catch (e: Exception) {
            ApiResult.Error(500, "Unknown error", e)
        }
    }
}

// 화면 상태 모델링
sealed interface ScreenState {
    data object Initial : ScreenState
    data object Loading : ScreenState
    
    data class Content(
        val items: List<Item>,
        val hasMore: Boolean
    ) : ScreenState
    
    data class Error(
        val message: String,
        val canRetry: Boolean
    ) : ScreenState
}
```

### When의 Exhaustive 보장

```kotlin
// when에서 모든 케이스를 강제로 처리
fun handleResult(result: ApiResult<User>) {
    when (result) {
        is ApiResult.Success -> {
            val user = result.data
            displayUser(user)
        }
        is ApiResult.Error -> {
            showError(result.message)
        }
        ApiResult.NetworkError -> {
            showNetworkError()
        }
        ApiResult.Unauthorized -> {
            navigateToLogin()
        }
        // else 불필요: 모든 경우를 처리했으므로
    }
}

// 새로운 타입 추가 시 컴파일 에러 발생
sealed class NewResult<out T> {
    data class Success<T>(val data: T) : NewResult<T>()
    data class Error(val msg: String) : NewResult<Nothing>()
    data object Timeout : NewResult<Nothing>()  // 새로 추가
}

fun handle(result: NewResult<String>) {
    when (result) {
        is NewResult.Success -> {}
        is NewResult.Error -> {}
        // 컴파일 에러: NewResult.Timeout 처리 누락!
    }
}

// Expression으로 활용하여 값 반환 강제
fun getDisplayMessage(state: ScreenState): String = when (state) {
    ScreenState.Initial -> "초기화 중"
    ScreenState.Loading -> "로딩 중"
    is ScreenState.Content -> "${state.items.size}개 항목"
    is ScreenState.Error -> state.message
    // 모든 경우를 처리하지 않으면 컴파일 에러
}
```

### 언어 설계 관점에서의 이유

Sealed class/interface는 "타입으로 상태를 표현하라"는 타입 주도 설계(Type-Driven Design)를 강제한다:

1. **닫힌 세계 가정** - 가능한 모든 타입을 컴파일 타임에 알 수 있음
2. **Exhaustiveness 검사** - when에서 모든 경우를 처리했는지 컴파일러가 검증
3. **타입 안전성** - 각 하위 타입이 자신만의 데이터를 가질 수 있음
4. **리팩토링 안전성** - 타입 추가/제거 시 모든 처리 지점에서 컴파일 에러

### 실무 사용 시나리오

**도메인 이벤트:**
```kotlin
sealed interface OrderEvent {
    val orderId: Long
    
    data class Created(
        override val orderId: Long,
        val userId: Long,
        val items: List<OrderItem>
    ) : OrderEvent
    
    data class PaymentCompleted(
        override val orderId: Long,
        val paymentId: String,
        val amount: Int
    ) : OrderEvent
    
    data class Shipped(
        override val orderId: Long,
        val trackingNumber: String
    ) : OrderEvent
    
    data class Cancelled(
        override val orderId: Long,
        val reason: String
    ) : OrderEvent
}

class OrderEventHandler {
    fun handle(event: OrderEvent) = when (event) {
        is OrderEvent.Created -> handleCreated(event)
        is OrderEvent.PaymentCompleted -> handlePayment(event)
        is OrderEvent.Shipped -> handleShipped(event)
        is OrderEvent.Cancelled -> handleCancelled(event)
    }
}
```

**유효성 검증 결과:**
```kotlin
sealed interface ValidationResult {
    data object Valid : ValidationResult
    
    data class Invalid(
        val errors: List<ValidationError>
    ) : ValidationResult {
        data class ValidationError(
            val field: String,
            val message: String
        )
    }
}

fun validate(request: CreateUserRequest): ValidationResult {
    val errors = mutableListOf<ValidationResult.Invalid.ValidationError>()
    
    if (request.email.isBlank()) {
        errors += ValidationResult.Invalid.ValidationError("email", "필수 값입니다")
    }
    
    return if (errors.isEmpty()) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(errors)
    }
}
```

### 트레이드오프 / 남용 시 문제

**❌ 과도한 계층 분할:**
```kotlin
// 나쁜 예: 지나치게 세분화
sealed class UserStatus {
    data object Active : UserStatus()
    data object Inactive : UserStatus()
    data object Suspended : UserStatus()
    data object PendingVerification : UserStatus()
    data object PendingApproval : UserStatus()
    data object TemporarilyBlocked : UserStatus()
    // 10개 이상...
}
// → enum으로 충분한 경우가 많음
```

**❌ 데이터 없는 타입:**
```kotlin
// 나쁜 예: 모든 하위 타입이 데이터 없음
sealed class Action {
    object Save : Action()
    object Delete : Action()
    object Update : Action()
}
// → enum으로 대체 가능
```

**✅ 적절한 사용:**
```kotlin
// 좋은 예: 각 타입이 고유한 데이터를 가짐
sealed class PaymentMethod {
    data class CreditCard(
        val cardNumber: String,
        val cvv: String
    ) : PaymentMethod()
    
    data class BankTransfer(
        val bankCode: String,
        val accountNumber: String
    ) : PaymentMethod()
    
    data object Cash : PaymentMethod()
}
```

### 사용 기준 체크리스트

**Do:**
- [ ] API 결과, 화면 상태, 도메인 이벤트 모델링에 활용
- [ ] 각 하위 타입이 서로 다른 데이터를 가질 때 사용
- [ ] when을 expression으로 사용하여 모든 케이스 처리 강제
- [ ] data class/object와 결합하여 간결성 확보

**Don't:**
- [ ] 단순 열거형은 enum 사용 (sealed는 과도)
- [ ] 하위 타입이 10개 이상이면 설계 재검토
- [ ] 외부 라이브러리 확장이 필요하면 sealed 쓰지 않기
- [ ] 런타임에 타입이 추가될 가능성이 있으면 사용 금지

---

## Expression 중심 언어

### 한 줄 요약
**제어 구조를 값으로 취급하여, 불변 변수 할당과 함수형 스타일을 자연스럽게 만든다.**

### 기능 개요

Kotlin에서 if, when, try는 단순한 제어문이 아니라 **값을 반환하는 표현식(expression)**이다. 이는 변수 할당, 함수 반환, 체이닝이 가능하며, 불변성 우선 코드를 작성하게 유도한다.

### if/when/try Expression

```kotlin
// if expression: 값을 반환
val max = if (a > b) a else b

val status = if (user.isActive) {
    "활성"
} else {
    "비활성"
}

// when expression: 다중 분기 값 반환
val message = when (statusCode) {
    200 -> "성공"
    404 -> "찾을 수 없음"
    500 -> "서버 오류"
    else -> "알 수 없는 상태"
}

// when with type checking
val description = when (value) {
    is String -> "문자열: ${value.length}자"
    is Int -> "정수: $value"
    is List<*> -> "리스트: ${value.size}개"
    else -> "기타: ${value::class.simpleName}"
}

// when without argument: if-else chain 대체
val grade = when {
    score >= 90 -> "A"
    score >= 80 -> "B"
    score >= 70 -> "C"
    else -> "F"
}

// try expression: 예외 처리 결과를 값으로
val result = try {
    parseJson(input)
} catch (e: JsonParseException) {
    defaultValue
}

val number = try {
    input.toInt()
} catch (e: NumberFormatException) {
    0
}
```

### 함수형 스타일 강화

```kotlin
// 단일 표현식 함수 (Single-expression function)
fun max(a: Int, b: Int) = if (a > b) a else b

fun getGrade(score: Int) = when {
    score >= 90 -> "A"
    score >= 80 -> "B"
    else -> "F"
}

// 불변 변수 초기화를 강제
fun getUserDisplayName(user: User): String {
    val displayName = when {
        !user.nickname.isNullOrBlank() -> user.nickname
        !user.name.isBlank() -> user.name
        else -> user.email
    }
    // displayName은 val, 재할당 불가
    return displayName
}

// 중첩 expression
fun calculateDiscount(user: User, amount: Int): Int {
    return when (user.grade) {
        UserGrade.VIP -> when {
            amount >= 100000 -> (amount * 0.2).toInt()
            amount >= 50000 -> (amount * 0.15).toInt()
            else -> (amount * 0.1).toInt()
        }
        UserGrade.GOLD -> (amount * 0.1).toInt()
        UserGrade.SILVER -> (amount * 0.05).toInt()
        else -> 0
    }
}

// Collection과 결합
val activeUsers = users.filter { it.isActive }
    .map { user ->
        when (user.role) {
            Role.ADMIN -> user.copy(displayName = "[관리자] ${user.name}")
            Role.MANAGER -> user.copy(displayName = "[매니저] ${user.name}")
            else -> user
        }
    }
```

### 언어 설계 관점에서의 이유

Expression 중심 설계는 "변수는 한 번만 할당되어야 한다"는 불변성 원칙을 언어 차원에서 지원한다:

1. **val 우선 사용** - expression으로 초기화하면 자연스럽게 val 사용
2. **조건부 할당 제거** - if-else로 여러 번 할당하는 패턴 제거
3. **가독성 향상** - "이 변수는 이렇게 결정된다"가 한눈에 보임
4. **함수형 프로그래밍 친화** - map, filter 등과 자연스럽게 결합

### 실무 사용 시나리오

**API Response 생성:**
```kotlin
fun createResponse(result: ServiceResult): ResponseEntity<ApiResponse> {
    return when (result) {
        is ServiceResult.Success -> ResponseEntity.ok(
            ApiResponse(data = result.data)
        )
        is ServiceResult.NotFound -> ResponseEntity.status(404).body(
            ApiResponse(error = "Not found")
        )
        is ServiceResult.Unauthorized -> ResponseEntity.status(401).body(
            ApiResponse(error = "Unauthorized")
        )
        is ServiceResult.Error -> ResponseEntity.status(500).body(
            ApiResponse(error = result.message)
        )
    }
}
```

**도메인 로직:**
```kotlin
fun calculateShippingFee(order: Order): Int {
    return when {
        order.totalAmount >= 50000 -> 0  // 무료 배송
        order.isRemoteArea -> 5000
        order.isIsland -> 3000
        else -> 2500
    }
}

fun determineOrderStatus(order: Order): OrderStatus {
    return if (order.cancelledAt != null) {
        OrderStatus.CANCELLED
    } else if (order.shippedAt != null) {
        OrderStatus.SHIPPED
    } else if (order.paidAt != null) {
        OrderStatus.PAID
    } else {
        OrderStatus.PENDING
    }
}
```

### 트레이드오프 / 남용 시 문제

**❌ 과도하게 복잡한 expression:**
```kotlin
// 나쁜 예: 가독성 저하
val result = if (condition1) {
    if (condition2) {
        if (condition3) {
            value1
        } else {
            value2
        }
    } else {
        value3
    }
} else {
    if (condition4) {
        value4
    } else {
        value5
    }
}
// → 함수로 분리하거나 when 사용
```

**❌ Side effect가 있는 expression:**
```kotlin
// 나쁜 예: expression이지만 부수 효과 발생
val message = when (status) {
    Status.SUCCESS -> {
        sendNotification()  // Side effect!
        "성공"
    }
    else -> "실패"
}
// → Side effect는 분리
```

**✅ 적절한 사용:**
```kotlin
// 좋은 예: 단순하고 명확한 값 결정
fun getButtonColor(enabled: Boolean) = if (enabled) {
    Color.Blue
} else {
    Color.Gray
}
```

### 사용 기준 체크리스트

**Do:**
- [ ] 값을 결정하는 조건문은 expression으로 작성
- [ ] 단일 표현식 함수는 = 문법 활용
- [ ] when을 if-else chain 대신 사용
- [ ] try-catch 결과를 변수에 직접 할당

**Don't:**
- [ ] Expression 내부에 side effect 넣지 않기
- [ ] 3단계 이상 중첩된 if expression 피하기
- [ ] 복잡한 로직은 함수로 분리
- [ ] when에서 else를 생략할 수 있으면 sealed 타입 사용 고려

---

## Default Parameter & Named Argument

### 한 줄 요약
**함수 파라미터에 기본값을 제공하고 이름으로 인자를 전달하여, API 설계를 단순화하고 가독성을 극대화한다.**

### 기능 개요

Default parameter와 named argument는 함수 오버로딩 없이도 유연한 API를 만들고, 호출 지점에서 의도를 명확히 표현할 수 있게 한다.

### API 설계 방식의 변화

```kotlin
// Default parameter: 오버로딩 제거
fun createUser(
    name: String,
    email: String,
    age: Int = 0,
    isActive: Boolean = true,
    role: UserRole = UserRole.USER
) {
    // ...
}

// 다양한 호출 방식
createUser("백도현", "baek@example.com")
createUser("백도현", "baek@example.com", age = 30)
createUser("백도현", "baek@example.com", role = UserRole.ADMIN)

// Named argument로 가독성 확보
createUser(
    name = "백도현",
    email = "baek@example.com",
    age = 30,
    isActive = false
)

// Builder 패턴 불필요
data class SearchFilter(
    val keyword: String? = null,
    val category: Category? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val page: Int = 1,
    val size: Int = 20
)

// 필요한 것만 지정
val filter = SearchFilter(
    keyword = "노트북",
    minPrice = 500000,
    page = 2
)
```

### 가독성 향상 포인트

```kotlin
// 불린 파라미터는 named argument로 의미 명확화
fun sendEmail(
    to: String,
    subject: String,
    body: String,
    isHtml: Boolean = false,
    attachFiles: Boolean = false,
    sendImmediately: Boolean = true
)

// ❌ 의미 불명확
sendEmail("user@example.com", "제목", "내용", true, false, true)

// ✅ 의도 명확
sendEmail(
    to = "user@example.com",
    subject = "제목",
    body = "내용",
    isHtml = true,
    sendImmediately = false
)

// 순서 변경 가능
fun updateProfile(
    userId: Long,
    name: String? = null,
    email: String? = null,
    phone: String? = null
)

updateProfile(
    userId = 1,
    email = "new@example.com",  // 순서 무관
    name = "새이름"
)

// DSL 스타일 코드 작성
fun query(
    table: String,
    where: String? = null,
    orderBy: String? = null,
    limit: Int? = null
)

query(
    table = "users",
    where = "age > 20",
    orderBy = "created_at DESC",
    limit = 10
)
```

### 언어 설계 관점에서의 이유

Default parameter와 named argument는 "함수 시그니처가 곧 문서"라는 철학을 구현한다:

1. **오버로딩 지옥 방지** - 하나의 함수로 다양한 사용 사례 커버
2. **API 진화 용이** - 새 파라미터 추가 시 기존 코드 영향 최소화
3. **자기 문서화** - 호출 지점에서 파라미터 의미가 드러남
4. **선택적 파라미터** - 필수/선택 구분이 타입 시스템에 녹아있음

### 실무 사용 시나리오

**Service Layer 메서드:**
```kotlin
class OrderService {
    fun createOrder(
        userId: Long,
        items: List<OrderItem>,
        couponCode: String? = null,
        deliveryNote: String? = null,
        usePoints: Int = 0,
        paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD
    ): Order {
        // ...
    }
}

// 호출 시 필요한 것만 명시
orderService.createOrder(
    userId = user.id,
    items = cartItems,
    couponCode = "WELCOME10",
    usePoints = 5000
)
```

**Configuration 빌더:**
```kotlin
data class CacheConfig(
    val enabled: Boolean = true,
    val ttl: Duration = Duration.ofMinutes(10),
    val maxSize: Int = 1000,
    val evictionPolicy: EvictionPolicy = EvictionPolicy.LRU
)

// 환경별 설정
val devCache = CacheConfig()
val prodCache = CacheConfig(
    ttl = Duration.ofHours(1),
    maxSize = 10000
)
```

**Test 코드:**
```kotlin
fun createTestUser(
    id: Long = 1L,
    name: String = "테스트유저",
    email: String = "test@example.com",
    role: UserRole = UserRole.USER,
    isActive: Boolean = true
) = User(id, name, email, role, isActive)

// 테스트마다 필요한 것만 커스터마이징
@Test
fun `관리자 권한 테스트`() {
    val admin = createTestUser(role = UserRole.ADMIN)
    // ...
}

@Test
fun `비활성 사용자 테스트`() {
    val inactiveUser = createTestUser(isActive = false)
    // ...
}
```

### 트레이드오프 / 남용 시 문제

**❌ 과도한 default parameter:**
```kotlin
// 나쁜 예: 파라미터가 너무 많음
fun processData(
    data: String,
    option1: Boolean = false,
    option2: Boolean = false,
    option3: Int = 0,
    option4: String = "",
    option5: Double = 0.0,
    option6: List<String> = emptyList()
    // ... 10개 이상
)
// → 별도 Config 클래스로 분리
```

**❌ 순서 의존적 named argument:**
```kotlin
// 나쁜 예: 순서가 바뀌면 의미 변경
fun transfer(from: Account, to: Account, amount: Int)

transfer(
    to = accountB,     // 혼란 가능
    from = accountA,
    amount = 10000
)
// → 도메인 메서드로 명확히
```

**✅ 적절한 사용:**
```kotlin
// 좋은 예: 명확한 의도의 선택적 파라미터
fun sendNotification(
    userId: Long,
    message: String,
    priority: Priority = Priority.NORMAL,
    channels: Set<Channel> = setOf(Channel.EMAIL)
)
```

### 사용 기준 체크리스트

**Do:**
- [ ] Boolean/Int 파라미터는 named argument로 호출
- [ ] 3개 이상 파라미터는 named argument 권장
- [ ] 선택적 파라미터는 default value 제공
- [ ] API 진화 시 끝에 파라미터 추가 (default value 포함)

**Don't:**
- [ ] 파라미터 7개 이상이면 Config 객체로 묶기
- [ ] 필수 파라미터에 default value 넣지 않기
- [ ] 도메인 의미가 중요한 경우 named argument 강제 고려
- [ ] 순서 변경이 혼란을 야기하면 타입 강화

---

## Value Class (Inline Class)

### 한 줄 요약
**원시 타입을 감싸 타입 안정성을 확보하되, 런타임 오버헤드는 제거한다.**

### 기능 개요

Value class는 단일 값을 감싸는 wrapper 클래스를 만들되, 컴파일러가 런타임에 래퍼를 제거하여 성능 손실 없이 타입 안전성을 얻는다.

### 타입 안정성 강화

```kotlin
// 문제 상황: 같은 타입이지만 의미가 다름
fun transfer(fromUserId: Long, toUserId: Long, amount: Long) {
    // fromUserId와 toUserId를 바꿔도 컴파일 에러 없음
}

// Value class로 해결
@JvmInline
value class UserId(val value: Long)

@JvmInline
value class OrderId(val value: Long)

@JvmInline
value class Amount(val value: Long) {
    init {
        require(value >= 0) { "금액은 0 이상이어야 합니다" }
    }
}

fun transfer(from: UserId, to: UserId, amount: Amount) {
    // transfer(orderId, userId, amount)  // 컴파일 에러!
}

// 사용
val userId = UserId(123)
val amount = Amount(10000)
transfer(UserId(1), UserId(2), amount)

// 런타임에는 Long으로 컴파일되어 오버헤드 없음
```

### 도메인 값 보호

```kotlin
// Email value class
@JvmInline
value class Email(val value: String) {
    init {
        require(value.matches(EMAIL_REGEX)) {
            "유효하지 않은 이메일 형식: $value"
        }
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$".toRegex()
    }
}

// PhoneNumber value class
@JvmInline
value class PhoneNumber(val value: String) {
    init {
        require(value.matches(PHONE_REGEX)) {
            "유효하지 않은 전화번호: $value"
        }
    }
    
    fun formatted(): String = when {
        value.startsWith("02") -> 
            "${value.substring(0, 2)}-${value.substring(2, 6)}-${value.substring(6)}"
        else -> 
            "${value.substring(0, 3)}-${value.substring(3, 7)}-${value.substring(7)}"
    }
    
    companion object {
        private val PHONE_REGEX = "^01[0-9]\\d{7,8}\$".toRegex()
    }
}

// 도메인 entity에서 사용
data class User(
    val id: UserId,
    val email: Email,
    val phoneNumber: PhoneNumber
)

// 잘못된 값 생성 시도 시 즉시 실패
val invalidEmail = Email("invalid")  // IllegalArgumentException
```

### 언어 설계 관점에서의 이유

Value class는 "타입은 비용이 아니라 안전장치"라는 철학을 구현한다:

1. **Zero-cost abstraction** - 컴파일 타임 타입 체크, 런타임 오버헤드 없음
2. **Primitive Obsession 방지** - 원시 타입 남용을 언어 차원에서 해결
3. **도메인 모델링** - 비즈니스 의미를 타입으로 표현
4. **유효성 검증 내장** - init 블록으로 불변 조건 보장

### 실무 사용 시나리오

**도메인 식별자:**
```kotlin
@JvmInline
value class ProductId(val value: Long)

@JvmInline
value class CategoryId(val value: Long)

class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) {
    fun getProduct(id: ProductId): Product {
        return productRepository.findById(id.value) 
            ?: throw ProductNotFoundException(id)
    }
    
    // categoryId를 productId 자리에 넣는 실수 방지
    fun getProductsByCategory(categoryId: CategoryId): List<Product> {
        return productRepository.findByCategoryId(categoryId.value)
    }
}
```

**측정 단위:**
```kotlin
@JvmInline
value class Kilogram(val value: Double) {
    operator fun plus(other: Kilogram) = Kilogram(value + other.value)
    operator fun times(multiplier: Int) = Kilogram(value * multiplier)
}

@JvmInline
value class Meter(val value: Double)

data class Product(
    val id: ProductId,
    val weight: Kilogram,
    val length: Meter
)

// val total: Kilogram = product.weight + product.length  // 컴파일 에러!
val totalWeight: Kilogram = product.weight + Kilogram(5.0)  // OK
```

**보안 관련 값:**
```kotlin
@JvmInline
value class Password(private val value: String) {
    init {
        require(value.length >= 8) { "비밀번호는 8자 이상이어야 합니다" }
    }
    
    // value를 private으로 하여 직접 접근 차단
    fun matches(input: String): Boolean {
        return value == input  // 실제로는 해싱 비교
    }
    
    // toString 오버라이드로 로그 유출 방지
    override fun toString() = "Password(***)"
}

@JvmInline
value class AccessToken(private val value: String) {
    fun isExpired(): Boolean {
        // JWT 만료 체크 로직
        return false
    }
    
    override fun toString() = "AccessToken(***)"
}
```

### 트레이드오프 / 남용 시 문제

**❌ 제한사항 이해 부족:**
```kotlin
// 나쁜 예: value class는 단일 프로퍼티만 가능
// @JvmInline
// value class UserInfo(val name: String, val age: Int)  // 컴파일 에러

// 해결: data class 사용
data class UserInfo(val name: String, val age: Int)
```

**❌ 과도한 사용:**
```kotlin
// 나쁜 예: 모든 String을 value class로
@JvmInline value class FirstName(val value: String)
@JvmInline value class LastName(val value: String)
@JvmInline value class MiddleName(val value: String)
@JvmInline value class Nickname(val value: String)
// → 실제로 타입 구분이 가치 있는 경우에만 사용
```

**✅ 적절한 사용:**
```kotlin
// 좋은 예: 혼동 가능성이 높고 도메인 의미가 명확한 경우
@JvmInline value class CustomerId(val value: Long)
@JvmInline value class OrderId(val value: Long)
```

### 사용 기준 체크리스트

**Do:**
- [ ] 도메인 식별자 (UserId, OrderId 등) 타입화
- [ ] 측정 단위가 다른 수치 (Meter, Kilogram 등)
- [ ] 유효성 검증이 필요한 값 (Email, PhoneNumber)
- [ ] 보안이 중요한 값 (Password, Token)
- [ ] init 블록으로 불변 조건 검증

**Don't:**
- [ ] 여러 프로퍼티가 필요하면 data class 사용
- [ ] 타입 구분의 실익이 없으면 원시 타입 유지
- [ ] 성능이 극도로 중요한 hot path에서는 측정 후 결정
- [ ] 외부 라이브러리 통합 시 호환성 확인

---

## 1탄 요약 – Kotlin 사고방식 핵심 정리

### 안전성 원칙

1. **Null은 타입 시스템의 일부다** - nullable/non-null을 명시하여 NPE를 컴파일 타임에 방지
2. **Smart cast를 신뢰하라** - 타입 체크 후 자동 캐스팅을 활용, 불필요한 변환 제거
3. **!! 연산자는 금기다** - 반드시 주석으로 안전성 근거 명시, lateinit 우선 고려
4. **Safe call 체이닝은 3단계까지** - 과도한 체이닝은 Law of Demeter 위반 신호

### 불변성 원칙

5. **Data class는 순수 데이터만** - 비즈니스 로직이 있으면 일반 class 사용
6. **모든 프로퍼티는 val이 기본** - 변경이 필요하면 copy() 사용
7. **컬렉션도 불변이 우선** - List, Set, Map (Mutable 버전은 명시적으로만)

### 표현력 원칙

8. **Sealed로 상태를 타입화하라** - API 결과, 화면 상태, 도메인 이벤트 모델링
9. **When은 exhaustive하게** - else 없이 모든 경우를 처리, 컴파일러가 검증
10. **Expression으로 값을 결정하라** - if, when, try를 값 반환 용도로 활용

### 가독성 원칙

11. **Boolean 파라미터는 named argument로** - 호출 지점에서 의도 명확화
12. **Default parameter로 오버로딩 제거** - 하나의 함수로 다양한 케이스 커버
13. **단일 표현식 함수는 = 문법** - 간결하고 함수형 스타일 강화

### 타입 안정성 원칙

14. **Value class로 도메인 값 보호** - 식별자, 측정 단위, 검증 필요 값 타입화
15. **원시 타입 남용을 경계하라** - Long 3개보다 UserId, OrderId, Amount가 안전

### 실무 적용 원칙

16. **언어 기능은 팀 규칙과 함께** - 강력한 기능일수록 남용 기준 명확히
17. **컴파일러를 믿어라** - 타입 시스템이 보장하는 안전성 활용
18. **가독성과 안전성이 충돌하면 안전성 우선** - 하지만 둘 다 챙길 방법 먼저 고려
