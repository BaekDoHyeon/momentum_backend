# 코틀린 공통 API 응답 클래스 완벽 가이드

## 목차
1. [sealed class/interface 완벽 이해하기](#1-sealed-classinterface-완벽-이해하기)
2. [out T (공변성) 완벽 이해하기](#2-out-t-공변성-완벽-이해하기)
3. [공통 응답 클래스 설계하기](#3-공통-응답-클래스-설계하기)
4. [Exception Handler 구현하기](#4-exception-handler-구현하기)
5. [실전 사용 예시](#5-실전-사용-예시)

---

## 1. sealed class/interface 완벽 이해하기

### 1.1 왜 sealed를 써야 하나? - 문제 상황부터 이해하기

#### 시나리오: API 응답 처리

API 응답은 크게 두 가지입니다:
- **성공**: 데이터가 있음
- **실패**: 에러 정보가 있음

이걸 코드로 어떻게 표현할까요?

### 1.2 방법 1: 단일 클래스 (자바에서 흔한 방식)

```kotlin
// 방법 1: 모든 것을 하나의 클래스에
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val errorCode: String?,
    val errorMessage: String?
)

// 사용
fun getUser(): ApiResponse<User> {
    val user = findUser()
    if (user != null) {
        return ApiResponse(
            success = true,
            data = user,
            errorCode = null,
            errorMessage = null
        )
    } else {
        return ApiResponse(
            success = false,
            data = null,
            errorCode = "U001",
            errorMessage = "사용자 없음"
        )
    }
}

// Controller에서 처리
val response = userService.getUser()
if (response.success) {
    val user = response.data  // ⚠️ user가 null일 수도 있음!
    if (user != null) {  // null 체크 필요
        println(user.name)
    }
} else {
    println(response.errorMessage)
}
```

#### 문제점

1. **타입 안전성 부족**
```kotlin
// 이상한 조합이 가능함
ApiResponse(
    success = true,  // 성공이라고 했는데
    data = null,     // 데이터는 null?
    errorCode = "E001",  // 에러 코드도 있음?
    errorMessage = "에러"  // 에러 메시지도?
)
// 컴파일러가 막지 못함!
```

2. **null 체크 지옥**
```kotlin
if (response.success) {
    if (response.data != null) {  // 첫 번째 null 체크
        val user = response.data
        // 사용
    }
} else {
    if (response.errorMessage != null) {  // 두 번째 null 체크
        println(response.errorMessage)
    }
}
```

3. **필드 낭비**
```kotlin
// 성공 응답인데 errorCode, errorMessage 필드가 있음
// 실패 응답인데 data 필드가 있음
// 메모리 낭비 + 혼란스러움
```

### 1.3 방법 2: 일반 상속 (open class)

```kotlin
// 부모 클래스
open class ApiResponse<T>

// 성공 클래스
data class SuccessResponse<T>(
    val data: T
) : ApiResponse<T>()

// 실패 클래스
data class ErrorResponse(
    val code: String,
    val message: String
) : ApiResponse<Nothing>()

// 사용
fun getUser(): ApiResponse<User> {
    val user = findUser()
    return if (user != null) {
        SuccessResponse(user)
    } else {
        ErrorResponse("U001", "사용자 없음")
    }
}

// Controller에서 처리
val response = userService.getUser()
if (response is SuccessResponse) {
    val user = response.data
    println(user.name)
} else if (response is ErrorResponse) {
    println(response.message)
} else {
    // ⚠️ 다른 타입이 추가되면?
    // 여기로 빠질 수 있음!
}
```

#### 문제점

1. **무제한 상속 가능**
```kotlin
// 다른 파일, 다른 패키지에서도 상속 가능
class WeirdResponse : ApiResponse<String>()
class PendingResponse : ApiResponse<User>()
class LoadingResponse : ApiResponse<Data>()

// 컴파일러가 모든 자식 클래스를 알 수 없음
// when이나 if-else로 처리할 때 빠뜨릴 수 있음
```

2. **완전성 체크 불가능**
```kotlin
when (response) {
    is SuccessResponse -> println(response.data)
    is ErrorResponse -> println(response.message)
    // else를 반드시 써야 함!
    else -> println("알 수 없는 응답")
}
// 나중에 누군가 PendingResponse를 추가하면?
// 위 코드는 수정하지 않아도 컴파일됨
// 런타임에 else로 빠져서 버그 발생!
```

### 1.4 방법 3: sealed class/interface (코틀린의 해법!)

```kotlin
// sealed = "봉인된, 제한된"
sealed interface ApiResponse<out T>

// 이 파일 안에서만 상속 가능!
data class Success<T>(
    val data: T
) : ApiResponse<T>

data class Error(
    val code: String,
    val message: String
) : ApiResponse<Nothing>

// 다른 파일에서 상속 시도
// class Pending : ApiResponse<User>  // ❌ 컴파일 에러!
```

#### 장점 1: 완전성 체크 (Exhaustive Check)

```kotlin
// when에서 else 없이 모든 케이스 처리 가능
fun handle(response: ApiResponse<User>) {
    when (response) {
        is Success -> println(response.data)
        is Error -> println(response.message)
        // else 불필요!
        // 컴파일러가 "Success와 Error만 있다"는 걸 알고 있음
    }
}

// 만약 케이스를 빠뜨리면?
fun handle(response: ApiResponse<User>) {
    when (response) {
        is Success -> println(response.data)
        // is Error 빠뜨림!
    }
}
// 컴파일 에러: 'when' expression must be exhaustive
```

#### 장점 2: 타입 안전성

```kotlin
// 성공 응답
val success: ApiResponse<User> = Success(user)
// success는 무조건 User 데이터를 가짐

// 실패 응답
val error: ApiResponse<User> = Error("U001", "에러")
// error는 데이터가 없음 (필드 자체가 없음!)

// 이상한 조합 불가능
// Success(null)  // ❌ 컴파일 에러
// Error("E001", "에러", user)  // ❌ Error는 data 파라미터 없음
```

#### 장점 3: 스마트 캐스팅

```kotlin
fun handle(response: ApiResponse<User>) {
    when (response) {
        is Success -> {
            // 이 블록 안에서 response는 Success 타입
            println(response.data.name)  // .data 접근 가능
            // 캐스팅 불필요!
        }
        is Error -> {
            // 이 블록 안에서 response는 Error 타입
            println(response.code)  // .code 접근 가능
            println(response.message)  // .message 접근 가능
        }
    }
}
```

### 1.5 자바와 비교

#### 자바 방식
```java
// 자바 - open class와 비슷
public abstract class ApiResponse<T> { }

public class SuccessResponse<T> extends ApiResponse<T> {
    private final T data;

    public SuccessResponse(T data) {
        this.data = data;
    }

    public T getData() { return data; }
}

public class ErrorResponse extends ApiResponse<Void> {
    private final String code;
    private final String message;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}

// 사용
ApiResponse<User> response = userService.getUser();

if (response instanceof SuccessResponse) {
    SuccessResponse<User> success = (SuccessResponse<User>) response;
    User user = success.getData();
    System.out.println(user.getName());
} else if (response instanceof ErrorResponse) {
    ErrorResponse error = (ErrorResponse) response;
    System.out.println(error.getMessage());
} else {
    // ⚠️ 다른 타입이 추가되면?
    System.out.println("Unknown response");
}
```

#### 코틀린 sealed 방식
```kotlin
sealed interface ApiResponse<out T>

data class Success<T>(val data: T) : ApiResponse<T>
data class Error(val code: String, val message: String) : ApiResponse<Nothing>

// 사용
val response = userService.getUser()

when (response) {
    is Success -> println(response.data.name)  // 캐스팅 자동!
    is Error -> println(response.message)
    // else 불필요 - 컴파일러가 완전성 체크
}
```

### 1.6 sealed class vs sealed interface

```kotlin
// sealed class
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val code: String) : ApiResponse<Nothing>()
}

// sealed interface
sealed interface ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>
    data class Error(val code: String) : ApiResponse<Nothing>
}
```

**차이점:**
- `sealed class`: 자식 클래스가 `()` 붙여서 상속 (생성자 호출)
- `sealed interface`: 자식 클래스가 그냥 상속

**언제 뭘 쓰나?**
- 부모에 공통 필드/메서드 있으면: `sealed class`
- 부모가 타입만 정의하면: `sealed interface` (권장)

```kotlin
// 공통 필드가 있는 경우 - sealed class
sealed class ApiResponse<out T>(
    val timestamp: Long = System.currentTimeMillis()  // 공통 필드
) {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val code: String) : ApiResponse<Nothing>()
}

// 공통 필드 없는 경우 - sealed interface (더 유연)
sealed interface ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>
    data class Error(val code: String) : ApiResponse<Nothing>
}
```

### 1.7 실전 예시: 다른 상황들

#### 예시 1: 네트워크 상태
```kotlin
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val exception: Exception) : NetworkResult<Nothing>
    object Loading : NetworkResult<Nothing>
}

// 사용
when (networkResult) {
    is NetworkResult.Success -> showData(networkResult.data)
    is NetworkResult.Error -> showError(networkResult.exception)
    NetworkResult.Loading -> showLoading()
    // else 불필요! 컴파일러가 완전성 체크
}
```

#### 예시 2: UI 상태
```kotlin
sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val error: String) : UiState
}

// 사용
when (uiState) {
    UiState.Idle -> hideAll()
    UiState.Loading -> showProgress()
    is UiState.Success -> showSuccess(uiState.message)
    is UiState.Error -> showError(uiState.error)
}
```

### 1.8 왜 sealed를 써야 하는가? - 최종 정리

| 특성 | 단일 클래스 | open class | sealed class |
|------|------------|-----------|-------------|
| **타입 안전성** | ❌ 이상한 조합 가능 | ✅ | ✅ |
| **null 체크** | ❌ 매번 필요 | ⚠️ 필요할 수 있음 | ✅ 불필요 |
| **완전성 체크** | ❌ | ❌ | ✅ when에서 else 불필요 |
| **상속 제한** | N/A | ❌ 무제한 상속 | ✅ 같은 파일만 |
| **컴파일러 지원** | ❌ | ⚠️ 제한적 | ✅ 강력 |
| **스마트 캐스팅** | ❌ | ⚠️ | ✅ 자동 |
| **유지보수성** | ❌ | ⚠️ | ✅ |

**결론: sealed는 "가능한 경우의 수를 컴파일 타임에 제한"하여 안전한 코드를 만든다!**

---

## 2. out T (공변성) 완벽 이해하기

### 2.1 제네릭 기본 개념

#### 제네릭이란?
"타입을 나중에 정하겠다"는 의미

```kotlin
// 제네릭 없이
class IntBox(val value: Int)
class StringBox(val value: String)
class UserBox(val value: User)
// 타입마다 클래스 만들어야 함!

// 제네릭으로
class Box<T>(val value: T)

val intBox = Box(123)        // Box<Int>
val stringBox = Box("hello") // Box<String>
val userBox = Box(user)      // Box<User>
```

### 2.2 제네릭의 불변성 (Invariance) - 기본 동작

```kotlin
open class Animal
class Dog : Animal()
class Cat : Animal()

// Dog는 Animal의 자식
val dog: Animal = Dog()  // ✅ OK

// 하지만!
val dogs: List<Dog> = listOf(Dog(), Dog())
val animals: List<Animal> = dogs  // ❌ 컴파일 에러!
```

**왜 에러가 날까?**

```kotlin
// 만약 위 코드가 가능하다면?
val dogs: MutableList<Dog> = mutableListOf(Dog())
val animals: MutableList<Animal> = dogs  // 만약 가능하다면?

animals.add(Cat())  // Animal 리스트니까 Cat 추가 가능?
// 그럼 dogs 리스트에 Cat이 들어가버림!
// dogs는 Dog만 있어야 하는데!

val dog: Dog = dogs[0]  // Cat을 Dog로 취급 → 💥 런타임 에러!
```

이를 방지하기 위해 **제네릭은 기본적으로 불변(invariant)**입니다.

### 2.3 공변성 (Covariance) - out 키워드

#### 문제 상황

```kotlin
sealed interface ApiResponse<T>  // out 없음

data class Success<T>(val data: T) : ApiResponse<T>
data class Error(val code: String) : ApiResponse<Nothing>

// 사용
fun getUser(): ApiResponse<User> {
    // ...
    return Error("U001")  // ❌ 컴파일 에러!
}

// 에러: Type mismatch
// Required: ApiResponse<User>
// Found: Error (which is ApiResponse<Nothing>)
```

**왜 에러?**
- `Error`는 `ApiResponse<Nothing>`
- 반환 타입은 `ApiResponse<User>`
- `Nothing`과 `User`는 다른 타입
- 제네릭은 불변이므로 호환 안 됨!

#### 해결: out 키워드

```kotlin
sealed interface ApiResponse<out T>  // ← out 추가!

data class Success<T>(val data: T) : ApiResponse<T>
data class Error(val code: String) : ApiResponse<Nothing>

// 사용
fun getUser(): ApiResponse<User> {
    return Error("U001")  // ✅ OK!
}
```

**out의 의미:**
- "T는 이 타입에서 나가기만(out) 한다"
- "생산(produce)만 하고, 소비(consume)하지 않는다"
- `ApiResponse<Nothing>`을 `ApiResponse<User>`로 취급 가능

### 2.4 out이 허용하는 것, 금지하는 것

#### out T는 "반환 위치"에만 사용 가능

```kotlin
interface Producer<out T> {
    fun produce(): T        // ✅ OK - T를 반환 (나감)
    val value: T            // ✅ OK - T를 반환 (나감)

    // fun consume(t: T)    // ❌ 불가 - T를 파라미터로 받음 (들어옴)
}
```

**왜 consume은 안 될까?**

```kotlin
// 만약 가능하다면?
interface Container<out T> {
    fun add(item: T)  // 만약 가능하다면?
    fun get(): T
}

val dogContainer: Container<Dog> = createContainer()
val animalContainer: Container<Animal> = dogContainer  // out이면 가능

animalContainer.add(Cat())  // Animal이니까 Cat 추가?
// dogContainer에 Cat이 들어감!
// dogContainer.get() → Dog로 기대했는데 Cat이 나옴 → 💥

// 그래서 out을 쓰면 add(T) 같은 메서드는 금지됨!
```

### 2.5 ApiResponse에서 out이 동작하는 원리

```kotlin
sealed interface ApiResponse<out T> {
    data class Success<T>(
        val data: T  // ✅ T를 반환하는 위치
    ) : ApiResponse<T>

    data class Error(
        val code: String  // T를 사용하지 않음
    ) : ApiResponse<Nothing>
}
```

#### Success 클래스
```kotlin
data class Success<T>(val data: T) : ApiResponse<T>

val success = Success(user)  // Success<User>
// data getter는 User를 반환 → out 조건 만족
```

#### Error 클래스
```kotlin
data class Error(val code: String) : ApiResponse<Nothing>

// Nothing은 모든 타입의 하위 타입
// ApiResponse<Nothing>은 ApiResponse<Any>로 취급 가능
```

#### 실제 사용
```kotlin
fun getUser(): ApiResponse<User> {
    // Case 1: Success
    val user = findUser()
    if (user != null) {
        return Success(user)  // Success<User> → ApiResponse<User> ✅
    }

    // Case 2: Error
    return Error("U001")  // Error (ApiResponse<Nothing>) → ApiResponse<User> ✅
    // out 덕분에 가능!
}
```

### 2.6 Nothing 타입 이해하기

```kotlin
// Nothing은 "값이 없는 타입"
val x: Nothing = ???  // 불가능! Nothing 값은 존재하지 않음

// Nothing은 모든 타입의 하위 타입
val user: User = throw Exception()  // throw는 Nothing 반환
val number: Int = TODO()            // TODO()는 Nothing 반환

// Nothing?는 null만 가능
val nothing: Nothing? = null
```

**ApiResponse<Nothing>의 의미:**
```kotlin
data class Error(val code: String) : ApiResponse<Nothing>

// "이 응답에는 T 타입의 데이터가 없다"
// Nothing을 사용함으로써 "데이터 없음"을 타입으로 표현
```

**out과 Nothing의 조합:**
```kotlin
sealed interface ApiResponse<out T>

// ApiResponse<Nothing>은 ApiResponse<User>, ApiResponse<Product> 등
// 모든 ApiResponse<*>의 하위 타입으로 취급됨
// → Error를 어느 타입에나 반환 가능!
```

### 2.7 자바와 비교

#### 자바의 와일드카드
```java
// 자바 - 공변성
interface ApiResponse<T> { }

class Success<T> implements ApiResponse<T> {
    private final T data;
    public Success(T data) { this.data = data; }
    public T getData() { return data; }
}

class Error implements ApiResponse<Void> {
    private final String code;
    public Error(String code) { this.code = code; }
}

// 사용
public ApiResponse<User> getUser() {
    // return new Error("E001");  // ❌ 에러!
    // Type mismatch: cannot convert from Error to ApiResponse<User>
}

// 해결 1: 와일드카드
public ApiResponse<? extends User> getUser() {
    return new Error("E001");  // 여전히 에러!
}

// 해결 2: 제네릭 타입 파라미터
public <T> ApiResponse<T> error(String code) {
    return new Error(code);
}

public ApiResponse<User> getUser() {
    return error("E001");  // ✅ OK
}
```

#### 코틀린의 out
```kotlin
sealed interface ApiResponse<out T>  // 한 번만 선언!

fun getUser(): ApiResponse<User> {
    return Error("E001")  // ✅ OK
}

fun getProduct(): ApiResponse<Product> {
    return Error("E001")  // ✅ OK
}

// out 한 번 선언으로 모든 곳에서 작동!
```

### 2.8 in 키워드 (반공변성, Contravariance)

out의 반대 개념

```kotlin
// in T = "T는 들어오기만(in) 한다"
interface Consumer<in T> {
    fun consume(item: T)  // ✅ OK - T를 파라미터로 받음
    // fun produce(): T   // ❌ 불가 - T를 반환 불가
}

// 사용
val animalConsumer: Consumer<Animal> = object : Consumer<Animal> {
    override fun consume(item: Animal) {
        println("Consuming ${item}")
    }
}

val dogConsumer: Consumer<Dog> = animalConsumer  // ✅ OK
// Dog는 Animal의 하위 타입
// Consumer<Animal>은 Consumer<Dog>의 하위 타입 (역방향!)

dogConsumer.consume(Dog())  // Animal로 처리 가능하므로 안전
```

**API 응답에서는 in을 안 쓰는 이유:**
- 응답은 데이터를 "생산"만 함 (반환)
- 데이터를 "소비"하지 않음 (파라미터로 받지 않음)
- 따라서 out만 필요!

### 2.9 variance 정리표

| 키워드 | 의미 | 사용 위치 | 예시 |
|--------|------|----------|------|
| **out** | 공변성 (Covariance) | 반환 타입만 | `interface Producer<out T>` |
| **in** | 반공변성 (Contravariance) | 파라미터 타입만 | `interface Consumer<in T>` |
| 없음 | 불변성 (Invariance) | 양쪽 다 | `class Box<T>` |

```kotlin
// 예시
interface Producer<out T> {
    fun get(): T       // ✅ out - 반환
}

interface Consumer<in T> {
    fun accept(t: T)   // ✅ in - 파라미터
}

interface Box<T> {
    fun get(): T       // 양쪽 다 사용
    fun set(t: T)      // variance 사용 불가
}
```

### 2.10 실전 예시

#### List의 out
```kotlin
// 코틀린 표준 라이브러리
public interface List<out E> : Collection<E> {
    public operator fun get(index: Int): E  // E를 반환만 함
    // public fun add(element: E)  // 이런 건 없음! (MutableList에만 있음)
}

// 덕분에 가능한 일
val dogs: List<Dog> = listOf(Dog())
val animals: List<Animal> = dogs  // ✅ OK - out 덕분!
```

#### MutableList는 out 없음
```kotlin
// MutableList는 out 없음!
public interface MutableList<E> : List<E> {
    public fun add(element: E): Boolean  // E를 파라미터로 받음
    public fun get(index: Int): E         // E를 반환도 함
}

val dogs: MutableList<Dog> = mutableListOf(Dog())
val animals: MutableList<Animal> = dogs  // ❌ 에러!
// add(Cat())이 가능해지므로 타입 안전성 깨짐
```

### 2.11 왜 ApiResponse에 out을 붙이는가? - 최종 정리

```kotlin
sealed interface ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>
    data class Error(val code: String) : ApiResponse<Nothing>
}
```

**이유:**
1. **Error를 모든 타입에 재사용 가능**
```kotlin
fun getUser(): ApiResponse<User> = Error("E001")      // ✅
fun getProduct(): ApiResponse<Product> = Error("E001") // ✅
fun getData(): ApiResponse<Data> = Error("E001")       // ✅
```

2. **공통 에러 처리 함수 작성 가능**
```kotlin
fun handleError(response: ApiResponse<Any>) {
    when (response) {
        is Success -> log("Success")
        is Error -> log("Error: ${response.code}")
    }
}

handleError(Error("E001"))  // ✅ OK - out 덕분!
```

3. **타입 안전성 유지**
```kotlin
// out이므로 T를 파라미터로 받는 메서드를 만들 수 없음
// 실수로 타입 안전성을 깨뜨릴 수 없음
```

**out 없이 작성하면:**
```kotlin
sealed interface ApiResponse<T>  // out 없음

fun getUser(): ApiResponse<User> {
    return Error("E001")  // ❌ 에러!
    // ApiResponse<Nothing>을 ApiResponse<User>에 할당 불가
}

// 해결: 타입 캐스팅 (추악함)
fun getUser(): ApiResponse<User> {
    return Error("E001") as ApiResponse<User>  // 😱
}
```

---

## 3. 공통 응답 클래스 설계하기

### 3.1 전체 구조

```
com/momentum/global/
├── response/
│   ├── ApiResponse.kt       # sealed interface + Success/Error
│   ├── SuccessCode.kt       # 성공 코드 enum
│   └── ErrorCode.kt         # 에러 코드 enum
```

### 3.2 SuccessCode.kt

```kotlin
package com.momentum.global.response

enum class SuccessCode(
    val code: String,
    val message: String
) {
    // 일반 성공
    SUCCESS("S001", "요청이 성공했습니다"),

    // CRUD 작업
    CREATED("S002", "생성되었습니다"),
    UPDATED("S003", "수정되었습니다"),
    DELETED("S004", "삭제되었습니다")
}
```

**enum 클래스 문법:**
```kotlin
// enum 클래스 기본
enum class Color {
    RED, GREEN, BLUE
}

// enum에 프로퍼티 추가
enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}

// 사용
val color = Color.RED
println(color.rgb)  // 16711680
```

### 3.3 ErrorCode.kt

```kotlin
package com.momentum.global.response

enum class ErrorCode(
    val code: String,
    val message: String
) {
    // 인증/인가 (A: Authentication)
    INVALID_TOKEN("A001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN("A002", "만료된 토큰입니다"),
    UNAUTHORIZED("A003", "인증이 필요합니다"),
    FORBIDDEN("A004", "권한이 없습니다"),

    // 사용자 (U: User)
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL("U002", "이미 존재하는 이메일입니다"),
    INVALID_PASSWORD("U003", "비밀번호가 일치하지 않습니다"),

    // 요청 검증 (V: Validation)
    INVALID_INPUT("V001", "입력값이 올바르지 않습니다"),
    MISSING_REQUIRED_FIELD("V002", "필수 항목이 누락되었습니다"),

    // 리소스 (R: Resource)
    RESOURCE_NOT_FOUND("R001", "요청한 리소스를 찾을 수 없습니다"),
    RESOURCE_ALREADY_EXISTS("R002", "이미 존재하는 리소스입니다"),

    // 서버 오류 (E: Error)
    INTERNAL_SERVER_ERROR("E999", "서버 내부 오류가 발생했습니다"),
    DATABASE_ERROR("E998", "데이터베이스 오류가 발생했습니다"),
    EXTERNAL_API_ERROR("E997", "외부 API 호출 중 오류가 발생했습니다")
}
```

### 3.4 ApiResponse.kt

```kotlin
package com.momentum.global.response

/**
 * 공통 API 응답 인터페이스
 *
 * @param T 응답 데이터 타입
 */
sealed interface ApiResponse<out T> {

    /**
     * 성공 응답
     *
     * @param T 응답 데이터 타입
     * @property data 실제 응답 데이터
     * @property code 성공 코드 (기본값: SUCCESS)
     */
    data class Success<T>(
        val data: T,
        val code: SuccessCode = SuccessCode.SUCCESS
    ) : ApiResponse<T>

    /**
     * 실패 응답
     *
     * @property code 에러 코드
     * @property data 추가 에러 데이터 (선택, validation errors 등)
     */
    data class Error<T>(
        val code: ErrorCode,
        val data: T? = null
    ) : ApiResponse<Nothing>
}
```

**코틀린 문법 설명:**

1. **data class**
```kotlin
// 일반 클래스
class User(val name: String, val age: Int)

// data class - equals, hashCode, toString, copy 자동 생성
data class User(val name: String, val age: Int)

val user1 = User("John", 20)
val user2 = User("John", 20)

println(user1 == user2)  // data class: true, 일반 class: false
println(user1)           // User(name=John, age=20)

val user3 = user1.copy(age = 21)  // copy 메서드
```

2. **val vs var**
```kotlin
val x = 10    // 읽기 전용 (final)
x = 20        // ❌ 에러

var y = 10    // 변경 가능
y = 20        // ✅ OK
```

3. **기본 파라미터 값**
```kotlin
// 자바
public void greet(String name) {
    greet(name, "Hello");
}

public void greet(String name, String greeting) {
    System.out.println(greeting + " " + name);
}

// 코틀린
fun greet(name: String, greeting: String = "Hello") {
    println("$greeting $name")
}

greet("John")              // Hello John
greet("John", "Hi")        // Hi John
greet(greeting = "Hey", name = "John")  // Named parameter
```

4. **nullable 타입**
```kotlin
// non-null
val name: String = "John"
// name = null  // ❌ 에러

// nullable
val name: String? = null  // ✅ OK
println(name.length)      // ❌ 에러
println(name?.length)     // ✅ OK (Safe call)
println(name ?: "default") // ✅ OK (Elvis operator)
```

### 3.5 사용 예시

#### Service
```kotlin
@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun getUser(id: Long): ApiResponse<UserResponse> {
        val user = userRepository.findById(id)
            ?: return ApiResponse.Error(ErrorCode.USER_NOT_FOUND)

        return ApiResponse.Success(
            data = UserResponse.from(user)
        )
    }

    fun createUser(request: CreateUserRequest): ApiResponse<UserResponse> {
        if (userRepository.existsByEmail(request.email)) {
            return ApiResponse.Error(ErrorCode.DUPLICATE_EMAIL)
        }

        val user = User(
            email = request.email,
            name = request.name
        )

        val savedUser = userRepository.save(user)

        return ApiResponse.Success(
            data = UserResponse.from(savedUser),
            code = SuccessCode.CREATED
        )
    }
}
```

**코틀린 문법: Elvis operator**
```kotlin
// 자바
User user = userRepository.findById(id);
if (user == null) {
    return ApiResponse.error(ErrorCode.USER_NOT_FOUND);
}
return ApiResponse.success(user);

// 코틀린
val user = userRepository.findById(id)
    ?: return ApiResponse.Error(ErrorCode.USER_NOT_FOUND)
return ApiResponse.Success(user)
```

#### Controller
```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): ResponseEntity<*> {
        return when (val response = userService.getUser(id)) {
            is ApiResponse.Success -> ResponseEntity.ok(mapOf(
                "code" to response.code.code,
                "message" to response.code.message,
                "data" to response.data
            ))
            is ApiResponse.Error -> ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf(
                    "code" to response.code.code,
                    "message" to response.code.message
                ))
        }
    }
}
```

**코틀린 문법: when expression**
```kotlin
// 자바
if (response instanceof Success) {
    Success success = (Success) response;
    return ResponseEntity.ok(success.getData());
} else if (response instanceof Error) {
    Error error = (Error) response;
    return ResponseEntity.badRequest().body(error.getMessage());
} else {
    return ResponseEntity.internalServerError().build();
}

// 코틀린
when (response) {
    is Success -> ResponseEntity.ok(response.data)
    is Error -> ResponseEntity.badRequest().body(response.message)
    // sealed이므로 else 불필요!
}

// when을 변수에 할당
val result = when (response) {
    is Success -> "OK"
    is Error -> "FAIL"
}
```

**코틀린 문법: Smart cast**
```kotlin
val response: ApiResponse<User> = getUser()

// when 안에서 자동 캐스팅
when (response) {
    is ApiResponse.Success -> {
        // response는 여기서 Success<User> 타입
        println(response.data.name)  // .data 접근 가능
        println(response.code)        // .code 접근 가능
    }
    is ApiResponse.Error -> {
        // response는 여기서 Error 타입
        println(response.code)   // .code 접근 가능
    }
}
```

---

## 4. Exception Handler 구현하기

### 4.1 Custom Exception 클래스

```kotlin
package com.momentum.global.exception

import com.momentum.global.response.ErrorCode

/**
 * 비즈니스 로직 예외의 최상위 클래스
 *
 * @property errorCode 에러 코드
 */
open class BusinessException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)

/**
 * 리소스를 찾을 수 없을 때
 */
class NotFoundException(
    errorCode: ErrorCode
) : BusinessException(errorCode)

/**
 * 인증 실패
 */
class UnauthorizedException(
    errorCode: ErrorCode = ErrorCode.UNAUTHORIZED
) : BusinessException(errorCode)

/**
 * 권한 없음
 */
class ForbiddenException(
    errorCode: ErrorCode = ErrorCode.FORBIDDEN
) : BusinessException(errorCode)

/**
 * 중복 리소스
 */
class DuplicateException(
    errorCode: ErrorCode
) : BusinessException(errorCode)

/**
 * 잘못된 입력
 */
class InvalidInputException(
    errorCode: ErrorCode = ErrorCode.INVALID_INPUT
) : BusinessException(errorCode)
```

**코틀린 문법: 클래스 상속**
```kotlin
// 자바
public class NotFoundException extends BusinessException {
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}

// 코틀린 - 훨씬 간결
class NotFoundException(
    errorCode: ErrorCode
) : BusinessException(errorCode)
```

**코틀린 문법: open 키워드**
```kotlin
// 코틀린은 기본적으로 클래스가 final (상속 불가)
class User  // 상속 불가

// open을 붙여야 상속 가능
open class User  // 상속 가능

class Admin : User()  // ✅ OK
```

### 4.2 GlobalExceptionHandler

```kotlin
package com.momentum.global.handler

import com.momentum.global.exception.*
import com.momentum.global.response.ApiResponse
import com.momentum.global.response.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse.Error<Nothing>> {
        val response = ApiResponse.Error<Nothing>(
            code = e.errorCode
        )
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }

    /**
     * 리소스 없음 예외 (404)
     */
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException): ResponseEntity<ApiResponse.Error<Nothing>> {
        val response = ApiResponse.Error<Nothing>(
            code = e.errorCode
        )
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response)
    }

    /**
     * 인증 실패 예외 (401)
     */
    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(e: UnauthorizedException): ResponseEntity<ApiResponse.Error<Nothing>> {
        val response = ApiResponse.Error<Nothing>(
            code = e.errorCode
        )
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(response)
    }

    /**
     * 권한 없음 예외 (403)
     */
    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(e: ForbiddenException): ResponseEntity<ApiResponse.Error<Nothing>> {
        val response = ApiResponse.Error<Nothing>(
            code = e.errorCode
        )
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(response)
    }

    /**
     * Validation 예외 (Spring @Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException
    ): ResponseEntity<ApiResponse.Error<List<String>>> {
        val errors = e.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }

        val response = ApiResponse.Error(
            code = ErrorCode.INVALID_INPUT,
            data = errors
        )
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }

    /**
     * 예상치 못한 예외 (500)
     */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse.Error<Nothing>> {
        e.printStackTrace()

        val response = ApiResponse.Error<Nothing>(
            code = ErrorCode.INTERNAL_SERVER_ERROR
        )
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response)
    }
}
```

**코틀린 문법: 함수**
```kotlin
// 자바
public ResponseEntity<ApiResponse> handleException(Exception e) {
    // ...
    return ResponseEntity.ok(response);
}

// 코틀린
fun handleException(e: Exception): ResponseEntity<ApiResponse> {
    // ...
    return ResponseEntity.ok(response)
}

// expression body (한 줄 함수)
fun add(a: Int, b: Int): Int = a + b

// 타입 추론
fun add(a: Int, b: Int) = a + b  // 반환 타입 생략 가능
```

**코틀린 문법: 컬렉션**
```kotlin
// map
val numbers = listOf(1, 2, 3)
val doubled = numbers.map { it * 2 }  // [2, 4, 6]

// filter
val evens = numbers.filter { it % 2 == 0 }  // [2]

// joinToString
val errors = listOf("error1", "error2")
val message = errors.joinToString(", ")  // "error1, error2"

// 메서드 체이닝
val result = bindingResult.fieldErrors
    .map { "${it.field}: ${it.defaultMessage}" }
    .filter { it.isNotEmpty() }
    .joinToString(", ")
```

---

## 5. 실전 사용 예시

### 5.1 Service Layer 리팩토링

#### Before (Exception Handler 없이)
```kotlin
@Service
class AuthService(
    private val userRepository: UserRepository
) {
    fun login(request: LoginRequest): ApiResponse<TokenResponse> {
        val user = userRepository.findByEmail(request.email)
            ?: return ApiResponse.Error(ErrorCode.USER_NOT_FOUND)

        if (!passwordMatches(request.password, user.password)) {
            return ApiResponse.Error(ErrorCode.INVALID_PASSWORD)
        }

        val token = generateToken(user)
        return ApiResponse.Success(token)
    }
}
```

#### After (Exception Handler 사용)
```kotlin
@Service
class AuthService(
    private val userRepository: UserRepository
) {
    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw NotFoundException(ErrorCode.USER_NOT_FOUND)

        if (!passwordMatches(request.password, user.password)) {
            throw UnauthorizedException(ErrorCode.INVALID_PASSWORD)
        }

        return generateToken(user)
    }
}
```

**장점:**
- Service는 비즈니스 로직에만 집중
- ApiResponse를 신경 쓰지 않아도 됨
- Exception Handler가 자동으로 ApiResponse로 변환

### 5.2 Controller Layer 간소화

#### Before
```kotlin
@PostMapping("/login")
fun login(@RequestBody request: LoginRequest): ResponseEntity<*> {
    return when (val response = authService.login(request)) {
        is ApiResponse.Success -> ResponseEntity.ok(mapOf(
            "code" to response.code.code,
            "message" to response.code.message,
            "data" to response.data
        ))
        is ApiResponse.Error -> ResponseEntity.badRequest().body(mapOf(
            "code" to response.code.code,
            "message" to response.code.message
        ))
    }
}
```

#### After
```kotlin
@PostMapping("/login")
fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiResponse.Success<TokenResponse>> {
    val token = authService.login(request)
    return ResponseEntity.ok(ApiResponse.Success(token))
}
```

### 5.3 전체 흐름

```
Client 요청
   ↓
Controller
   ↓
Service
   - userRepository.findByEmail() → null
   - throw NotFoundException(ErrorCode.USER_NOT_FOUND)
   ↓
GlobalExceptionHandler가 자동으로 catch
   - handleNotFoundException() 실행
   - ApiResponse.Error 생성
   - ResponseEntity 반환
   ↓
Client에게 JSON 응답
{
  "code": "U001",
  "message": "사용자를 찾을 수 없습니다"
}
```

### 5.4 Validation 예시

```kotlin
// DTO
data class SignupRequest(
    @field:Email(message = "이메일 형식이 올바르지 않습니다")
    val email: String,

    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다")
    val name: String
)

// Controller
@PostMapping("/signup")
fun signup(@RequestBody @Valid request: SignupRequest): ResponseEntity<ApiResponse.Success<UserResponse>> {
    val user = authService.signup(request)
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.Success(user, SuccessCode.CREATED))
}

// Validation 실패 시 자동으로 GlobalExceptionHandler가 처리
// JSON 응답:
{
  "code": "V001",
  "message": "입력값이 올바르지 않습니다",
  "data": [
    "email: 이메일 형식이 올바르지 않습니다",
    "password: 비밀번호는 8자 이상이어야 합니다"
  ]
}
```

**코틀린 문법: Annotation**
```kotlin
// 자바
@Email(message = "이메일 형식 오류")
private String email;

// 코틀린 - @field: 필요
@field:Email(message = "이메일 형식 오류")
val email: String

// 왜? 코틀린의 프로퍼티는 field + getter + setter
// @field:를 붙여야 필드에 annotation 적용
```

### 5.5 복잡한 에러 응답 예시

```kotlin
// Validation Error DTO
data class ValidationError(
    val field: String,
    val message: String
)

// Service
fun validateUser(request: SignupRequest) {
    val errors = mutableListOf<ValidationError>()

    if (!isValidEmail(request.email)) {
        errors.add(ValidationError("email", "이메일 형식이 올바르지 않습니다"))
    }

    if (request.password.length < 8) {
        errors.add(ValidationError("password", "비밀번호는 8자 이상이어야 합니다"))
    }

    if (errors.isNotEmpty()) {
        throw InvalidInputException(ErrorCode.INVALID_INPUT).apply {
            // 에러에 data 담기
            val response = ApiResponse.Error(
                code = ErrorCode.INVALID_INPUT,
                data = errors
            )
            // 이 방식보다는 GlobalExceptionHandler에서 처리하는 게 나음
        }
    }
}
```

---

## 부록: 자주 사용하는 코틀린 문법

### A. 변수 선언
```kotlin
// val = 읽기 전용 (Java final)
val name = "John"
val age: Int = 20

// var = 변경 가능
var count = 0
count++

// nullable
val name: String? = null
```

### B. 함수
```kotlin
// 기본
fun add(a: Int, b: Int): Int {
    return a + b
}

// Expression body
fun add(a: Int, b: Int) = a + b

// 기본 파라미터
fun greet(name: String, greeting: String = "Hello") {
    println("$greeting $name")
}

// Named parameter
greet(greeting = "Hi", name = "John")
```

### C. 클래스
```kotlin
// 기본 클래스
class User(val name: String, var age: Int)

// data class
data class User(val name: String, val age: Int)

// 상속
open class Animal
class Dog : Animal()

// sealed
sealed class Result
data class Success(val data: String) : Result()
data class Error(val message: String) : Result()
```

### D. null 안전성
```kotlin
// Safe call
val length = name?.length

// Elvis operator
val length = name?.length ?: 0

// Not-null assertion
val length = name!!.length  // null이면 NPE
```

### E. when
```kotlin
// if-else 대체
when (x) {
    1 -> println("One")
    2 -> println("Two")
    else -> println("Other")
}

// 타입 체크
when (obj) {
    is String -> println(obj.length)
    is Int -> println(obj * 2)
    else -> println("Unknown")
}

// expression
val result = when (x) {
    1 -> "One"
    2 -> "Two"
    else -> "Other"
}
```

### F. 컬렉션
```kotlin
// List (불변)
val list = listOf(1, 2, 3)

// MutableList
val mutableList = mutableListOf(1, 2, 3)
mutableList.add(4)

// map
val doubled = list.map { it * 2 }

// filter
val evens = list.filter { it % 2 == 0 }

// 체이닝
val result = list
    .filter { it > 1 }
    .map { it * 2 }
    .sum()
```

### G. 람다
```kotlin
// 기본
val sum = { a: Int, b: Int -> a + b }

// 파라미터 1개면 it 사용
val doubled = list.map { it * 2 }

// 마지막 파라미터가 람다면 밖으로
list.filter { it > 0 }
```

### H. 문자열
```kotlin
// String template
val name = "John"
val message = "Hello, $name"
val message2 = "Length: ${name.length}"

// 멀티라인
val text = """
    Line 1
    Line 2
    Line 3
""".trimIndent()
```

### I. 확장 함수
```kotlin
// String에 함수 추가
fun String.isEmail(): Boolean {
    return this.contains("@")
}

// 사용
"test@test.com".isEmail()  // true
```

---

## 정리

### 핵심 포인트

1. **sealed class/interface**
   - "봉인된" 상속 → 같은 파일에서만 상속 가능
   - when에서 완전성 체크 → else 불필요
   - 타입 안전성 + 스마트 캐스팅
   - API 응답처럼 "제한된 경우의 수"를 표현할 때 최적

2. **out T (공변성)**
   - "T는 나가기만 한다" (반환만, 파라미터 불가)
   - `ApiResponse<Nothing>`을 `ApiResponse<User>`로 취급 가능
   - Error를 모든 타입에 재사용 가능하게 만듦

3. **Exception Handler**
   - Service는 Exception throw
   - GlobalExceptionHandler가 자동으로 ApiResponse로 변환
   - 깔끔한 코드 + 일관된 에러 처리

### 자바 개발자를 위한 변환표

| 자바 | 코틀린 |
|------|--------|
| `class User { }` | `class User` |
| `final int x = 10` | `val x = 10` |
| `String name = null` | `var name: String? = null` |
| `if (x != null) x.length()` | `x?.length` |
| `x != null ? x : 0` | `x ?: 0` |
| `@Override public void method()` | `override fun method()` |
| `extends`, `implements` | `:` (둘 다) |
| `List<? extends T>` | `List<out T>` |
| `List<? super T>` | `List<in T>` |

이제 이해됐어?
