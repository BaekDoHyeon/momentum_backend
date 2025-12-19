# Java 개발자를 위한 Kotlin + Spring 실무 가이드

> **대상 독자**: Java/Spring 경력자이면서 Kotlin은 처음인 개발자  
> **목표**: "Java에서 이렇게 하던 것을 Kotlin에서는 이렇게 한다"를 중심으로 실무에서 바로 적용 가능한 가이드 제공

---

## 프롤로그

### 이 가이드가 해결하는 문제 7가지

1. **"왜 내 엔티티 클래스에서 프록시가 작동하지 않지?"** — Kotlin의 기본 final과 Spring/JPA 프록시 메커니즘 충돌
2. **"Jackson이 왜 기본값을 무시하고 null을 넣지?"** — Kotlin의 기본 생성자 + Jackson 역직렬화 불일치
3. **"data class로 Entity 만들면 왜 이상하게 동작하지?"** — equals/hashCode 자동 생성의 JPA 함정
4. **"lateinit으로 DI 받으면 왜 테스트에서 터지지?"** — 생성자 주입 vs 필드 주입의 Kotlin 특성
5. **"nullable 타입이랑 @NotNull이랑 뭐가 다르지?"** — 컴파일타임 null-safety vs 런타임 validation
6. **"왜 컬렉션을 수정하면 영속성 컨텍스트가 꼬이지?"** — Kotlin의 List vs MutableList와 JPA
7. **"all-open, no-arg 플러그인이 뭔데 왜 필요하지?"** — Kotlin + JPA 조합의 필수 플러그인 이해

---

### Java → Kotlin 사고방식 전환 12가지

| # | Java 사고방식 | Kotlin 사고방식 | 왜 중요한가 |
|---|---------------|-----------------|-------------|
| 1 | `field` 선언 후 getter/setter 생성 | `property`가 기본 단위 (getter/setter 자동 포함) | Lombok 없이도 깔끔한 코드, 하지만 JPA lazy loading과 충돌 가능 |
| 2 | 기본이 mutable, `final` 명시 필요 | 기본이 `val`(immutable), `var`로 mutable 선언 | 불변 우선 설계가 자연스러움 |
| 3 | 클래스 기본이 상속 가능 | 클래스 기본이 `final`, `open` 명시 필요 | Spring AOP 프록시가 기본적으로 작동하지 않음 |
| 4 | `null` 체크는 런타임 방어 | `null`이 타입 시스템에 포함 (`String?` vs `String`) | 컴파일타임에 NPE 방지 가능 |
| 5 | `Optional<T>`로 null 표현 | `T?`로 직접 표현, `Optional` 불필요 | 더 간결하고 체이닝 가능 (`?.`, `?:`, `let`) |
| 6 | `static` 키워드 사용 | `companion object` 또는 top-level function | static이라는 개념 자체가 없음 |
| 7 | `new` 키워드로 객체 생성 | `new` 없이 직접 호출 `Person("name")` | 팩토리 메서드와 생성자 구분이 흐릿해짐 |
| 8 | checked exception 강제 처리 | checked exception 없음 (전부 unchecked) | try-catch 강제 없음, 하지만 예외 누락 주의 |
| 9 | primitive vs wrapper (`int` vs `Integer`) | 전부 객체 (`Int`, `Long`), 컴파일러가 최적화 | null 가능성에 따라 자동으로 primitive/wrapper 결정 |
| 10 | Builder 패턴으로 객체 생성 | named argument + default parameter로 대체 | `@Builder` 필요 없음 |
| 11 | getter에서 계산 로직 = 별도 메서드 | computed property로 자연스럽게 표현 | `val fullName get() = "$firstName $lastName"` |
| 12 | Stream API로 컬렉션 처리 | 확장함수 기반 컬렉션 API (map, filter 등) | `.stream().collect()` 불필요, 더 직관적 |

---

### Kotlin/Spring에서 가장 많이 터지는 지점 Top 10

#### 🔥 1위: JPA Entity의 `final` 클래스 문제
```kotlin
// ❌ 기본이 final이라 프록시 생성 불가 → Lazy loading 실패
class User(val name: String)

// ✅ open 또는 all-open 플러그인 사용
open class User(var name: String)
```

#### 🔥 2위: data class로 Entity 만들기
```kotlin
// ❌ equals/hashCode가 모든 필드 포함 → 영속성 컨텍스트 혼란
data class User(val id: Long, val name: String)

// ✅ 일반 class + 필요시 직접 equals/hashCode 구현
class User(val id: Long?, var name: String)
```

#### 🔥 3위: Jackson 역직렬화 시 기본값 무시
```kotlin
// ❌ JSON에 필드가 없으면 기본값 대신 null로 처리됨
data class Request(val page: Int = 1)

// ✅ jackson-module-kotlin 의존성 필수 + @JsonProperty 또는 no-arg constructor
```

#### 🔥 4위: lateinit var의 초기화 체크 누락
```kotlin
// ❌ 테스트나 특정 경로에서 초기화 안 되면 UninitializedPropertyAccessException
class Service {
    @Autowired lateinit var repository: UserRepository
}

// ✅ 생성자 주입 사용
class Service(private val repository: UserRepository)
```

#### 🔥 5위: MutableList 외부 노출로 인한 캡슐화 깨짐
```kotlin
// ❌ 외부에서 직접 add/remove 가능
class Order {
    val items: MutableList<OrderItem> = mutableListOf()
}

// ✅ 읽기 전용 List 노출, 내부에서만 변경
class Order {
    private val _items: MutableList<OrderItem> = mutableListOf()
    val items: List<OrderItem> get() = _items.toList()
}
```

#### 🔥 6위: @Transactional이 작동하지 않음 (final 메서드)
```kotlin
// ❌ 메서드도 기본 final → 프록시 불가
class UserService {
    @Transactional
    fun createUser() { ... } // 트랜잭션 적용 안 됨
}

// ✅ all-open 플러그인으로 @Transactional 메서드 자동 open
```

#### 🔥 7위: JPA 기본 생성자 부재
```kotlin
// ❌ Hibernate가 리플렉션으로 인스턴스 생성 불가
class User(val name: String)

// ✅ no-arg 플러그인 또는 기본값 제공
class User(val name: String = "")
```

#### 🔥 8위: !! 남발로 인한 런타임 NPE
```kotlin
// ❌ nullable을 강제 언래핑 → 결국 NPE 발생
val name: String = user.name!!

// ✅ 안전한 호출 또는 기본값
val name: String = user.name ?: "Unknown"
```

#### 🔥 9위: copy()로 Entity 복사 시 영속성 문제
```kotlin
// ❌ data class의 copy()는 새 인스턴스 → 영속성 컨텍스트에서 분리됨
val updated = existingUser.copy(name = "newName")

// ✅ Entity는 상태 변경 메서드 사용
existingUser.updateName("newName")
```

#### 🔥 10위: Kotlin Collection과 Java Collection 호환 문제
```kotlin
// ❌ Kotlin의 List는 불변인 것처럼 보이지만 Java에서는 수정 가능
fun getItems(): List<Item> = items // Java 코드에서 .add() 호출 가능

// ✅ 방어적 복사
fun getItems(): List<Item> = items.toList()
```

---

## 1. Kotlin 기초를 "Java 대비"로 (실무 중심)

### 1.1 val/var, 기본 final, open의 의미

#### 결론 (권장안 1줄)
> **기본적으로 `val`을 사용하고, 변경이 꼭 필요한 경우에만 `var`를 사용한다. Spring 프록시가 필요한 클래스는 `open`을 붙이거나 all-open 플러그인을 사용한다.**

#### Java에서는
```java
// Java: 기본이 mutable, final을 명시해야 불변
public class User {
    private String name;           // mutable
    private final String id;       // immutable (final 명시 필요)
    
    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    // getter/setter 필요
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getId() { return id; }
}

// 클래스 상속: 기본이 상속 가능, final로 막아야 함
public class ParentService { }           // 상속 가능
public final class ChildService { }      // 상속 불가
```

#### Kotlin에서는
```kotlin
// Kotlin: val이 기본(불변 권장), var로 가변 명시
class User(
    val id: String,      // 불변 (getter만 생성)
    var name: String     // 가변 (getter + setter 생성)
)

// 클래스 상속: 기본이 final, open으로 열어야 함
class ParentService           // 상속 불가 (기본 final)
open class OpenService        // 상속 가능 (open 명시)

// Spring에서 문제가 되는 케이스
class UserService {           // final 클래스
    @Transactional
    fun createUser() { }      // final 메서드 → 프록시 불가!
}

// 해결: all-open 플러그인 적용 시 @Service, @Transactional 등이 붙은 클래스/메서드 자동 open
@Service
class UserService {           // 플러그인이 자동으로 open 처리
    @Transactional
    fun createUser() { }      // 프록시 정상 작동
}
```

#### 왜 (이유)
- **불변 우선 설계**: Kotlin은 함수형 프로그래밍 영향으로 불변을 기본으로 권장. 버그 감소, 스레드 안전성 향상.
- **final 기본**: Java에서 "상속 남발"로 인한 문제를 언어 차원에서 방지. Effective Java의 "상속보다 컴포지션" 원칙 강제.
- **Spring 프록시 충돌**: Spring AOP는 CGLIB 프록시(상속 기반)를 사용. final 클래스/메서드는 프록시 생성 불가 → `@Transactional`, `@Cacheable` 등 작동 안 함.

#### 트레이드오프

| 장점 | 단점 |
|------|------|
| 불변 기본으로 안전한 코드 | JPA Entity에서 setter 필요할 때 충돌 |
| 무분별한 상속 방지 | Spring 프록시를 위해 open 필요 |
| 컴파일타임에 의도 명확화 | Java 라이브러리와 호환 시 주의 필요 |

**대안**: 
- all-open 플러그인: `@Entity`, `@Service`, `@Transactional` 등에 자동 open 적용
- kotlin-spring 플러그인: Spring 관련 애노테이션에 자동 open

#### 실무 체크리스트
- [ ] 모든 변수 선언 시 `val` 먼저 고려, 필요한 경우에만 `var` 사용
- [ ] JPA Entity 필드 중 변경 가능한 것만 `var`, 나머지는 `val`
- [ ] `build.gradle.kts`에 `kotlin-spring` 플러그인 적용 확인
- [ ] `@Transactional` 메서드가 정상 작동하는지 테스트로 검증
- [ ] final로 남겨야 할 클래스는 `all-open` 예외 처리

---

### 1.2 property vs field / backing field / custom getter-setter / private set

#### 결론 (권장안 1줄)
> **Kotlin의 property는 getter/setter를 포함하는 개념이다. 외부에는 읽기만 허용하려면 `private set`을 사용하고, 계산된 값은 backing field 없는 computed property로 표현한다.**

#### Java에서는
```java
public class User {
    // field 선언
    private String name;
    private String firstName;
    private String lastName;
    
    // getter/setter는 별도로 정의 (또는 Lombok)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    // 계산된 값: 별도 메서드로 정의
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    // 읽기 전용: setter 안 만들면 됨
    public String getCreatedAt() { return createdAt; }
    // setCreatedAt() 없음
}
```

#### Kotlin에서는
```kotlin
class User(
    // property 선언 = field + getter + setter 한 번에
    var name: String,              // getter + setter 자동 생성
    val firstName: String,         // getter만 자동 생성 (val이므로)
    val lastName: String
) {
    // computed property (backing field 없음)
    // 호출할 때마다 계산됨
    val fullName: String
        get() = "$firstName $lastName"
    
    // backing field 있는 custom getter
    var email: String = ""
        get() = field.lowercase()  // field는 backing field 참조
        set(value) {
            field = value.trim()   // setter에서 가공
        }
    
    // private set: 외부에서 읽기만 가능
    var status: String = "ACTIVE"
        private set  // 클래스 내부에서만 변경 가능
    
    fun deactivate() {
        status = "INACTIVE"  // 내부에서는 변경 가능
    }
}

// 사용
val user = User("John", "John", "Doe")
println(user.name)       // getter 호출
user.name = "Jane"       // setter 호출
println(user.fullName)   // computed property
user.status = "X"        // ❌ 컴파일 에러! private set
user.deactivate()        // ✅ 메서드를 통한 상태 변경
```

#### 왜 (이유)
- **property = field + accessor**: Java에서 필드와 getter/setter를 분리하던 것을 하나로 통합. Lombok 없이도 깔끔.
- **backing field**: `field` 키워드로 실제 저장 공간 접근. custom getter/setter에서 무한 재귀 방지.
- **computed property**: 매번 계산되는 값. backing field가 없어 메모리 사용 없음.
- **private set**: 캡슐화를 위한 핵심 도구. 외부 노출은 읽기만, 변경은 도메인 메서드로.

#### 트레이드오프

| 패턴 | 사용 시점 | 주의점 |
|------|-----------|--------|
| `var` | 외부에서 자유롭게 변경 가능해야 할 때 | 무분별한 변경 가능 |
| `val` | 생성 후 변경 불가 | 컬렉션은 내부 변경 가능 |
| `private set` | 읽기는 공개, 변경은 내부만 | setter 직접 호출 불가 |
| computed property | 매번 계산이 필요한 값 | 비용 큰 계산은 지양 |
| backing field | 값 저장 + 가공 필요 시 | `field` 키워드 필수 |

#### 실무 체크리스트
- [ ] Entity의 상태 변경 필드는 `private set` + 도메인 메서드 조합 사용
- [ ] 단순 조합/계산 값은 computed property로 표현
- [ ] custom setter에서 validation 로직 추가 검토
- [ ] computed property에 비용 큰 연산 넣지 않기 (캐싱 필요하면 `by lazy`)
- [ ] JPA Entity에서 `private set` 사용 시 프록시/리플렉션 호환 확인

---

### 1.3 null-safety: ?, ?:, !!, lateinit, by lazy

#### 결론 (권장안 1줄)
> **`!!`는 절대 사용하지 말고, `?.`와 `?:`로 안전하게 처리한다. DI는 생성자 주입으로, 지연 초기화가 필요하면 `by lazy`를 우선 고려한다.**

#### Java에서는
```java
// null 체크는 런타임에 방어적으로
public String getUserName(User user) {
    if (user == null) {
        return "Unknown";
    }
    if (user.getName() == null) {
        return "Unknown";
    }
    return user.getName();
}

// Optional 사용
public String getUserNameOpt(User user) {
    return Optional.ofNullable(user)
        .map(User::getName)
        .orElse("Unknown");
}

// @Autowired 필드 주입
@Service
public class UserService {
    @Autowired
    private UserRepository repository;  // null일 수 있는 시점 존재
}
```

#### Kotlin에서는
```kotlin
// null이 타입에 포함됨
fun getUserName(user: User?): String {
    // ?. (safe call): null이면 뒤에 실행 안 함
    // ?: (elvis operator): null이면 대체값 반환
    return user?.name ?: "Unknown"
}

// 다양한 null 처리 연산자
val user: User? = findUser()

// ?. - Safe call: null이면 null 반환
val name: String? = user?.name

// ?: - Elvis operator: null이면 대체값
val nameOrDefault: String = user?.name ?: "Unknown"

// ?.let - null이 아닐 때만 블록 실행
user?.let { 
    println("User found: ${it.name}")
}

// !! - 강제 언래핑 (NPE 발생 가능) ❌ 사용 금지
val dangerousName: String = user!!.name  // user가 null이면 NPE

// 실무에서 !! 대신 사용할 패턴
val safeName: String = user?.name 
    ?: throw IllegalStateException("User must not be null")

// lateinit - 나중에 초기화 (DI 등)
class OldStyleService {
    @Autowired
    lateinit var repository: UserRepository  // ❌ 권장하지 않음
    
    fun doSomething() {
        if (::repository.isInitialized) {  // 초기화 여부 체크 가능
            repository.findAll()
        }
    }
}

// by lazy - 최초 접근 시 초기화 (thread-safe)
class BetterService {
    // 처음 접근할 때 한 번만 초기화
    val expensiveResource: Resource by lazy {
        println("Initializing...")
        Resource()
    }
}

// ✅ 권장: 생성자 주입
@Service
class BestService(
    private val repository: UserRepository  // 생성자에서 주입 (non-null 보장)
)
```

#### 왜 (이유)
- **컴파일타임 null-safety**: `String`과 `String?`는 다른 타입. null 가능성이 타입에 명시되어 컴파일러가 체크.
- **`!!` 위험성**: Java의 NPE 문제를 그대로 가져옴. Kotlin을 쓰는 의미가 없어짐.
- **`lateinit` 한계**: primitive 타입 불가, 초기화 여부 체크 필요, 테스트에서 문제 발생 가능.
- **생성자 주입 우선**: null이 불가능한 타입으로 선언 가능, 순환 참조 컴파일타임 감지, 테스트 용이.

#### 트레이드오프

| 방식 | 적합한 상황 | 부적합한 상황 |
|------|-------------|---------------|
| 생성자 주입 | DI, 필수 의존성 | 순환 참조(설계 문제) |
| `lateinit var` | 프레임워크 주입, 테스트 픽스처 | primitive, val, 늦은 초기화 보장 어려울 때 |
| `by lazy` | 비용 큰 초기화, 싱글톤 | var 필요할 때, 매번 새 값 필요할 때 |
| nullable + `?.` | 값이 없을 수 있는 정상 케이스 | 절대 null이면 안 되는 경우 |
| `!!` | (거의 없음) 100% null 아님을 확신할 때 | 대부분의 경우 |

#### 실무 체크리스트
- [ ] `!!` 사용 시 코드 리뷰에서 반드시 사유 확인 (거의 허용하지 않음)
- [ ] `@Autowired lateinit var` 대신 생성자 주입 사용
- [ ] nullable 파라미터에는 `@RequestParam(required = false)` 명시
- [ ] 외부 라이브러리 반환값은 platform type 주의 (`!` 표시)
- [ ] `by lazy`는 thread-safe 모드 확인 (`LazyThreadSafetyMode`)

---

### 1.4 data class의 진짜 의미

#### 결론 (권장안 1줄)
> **data class는 DTO/값 객체에 적합하다. JPA Entity에는 절대 사용하지 않는다.**

#### Java에서는
```java
// Java 14+ record (불변 데이터 클래스)
public record UserDto(String name, String email) { }

// Java 14 이전: Lombok 활용
@Data  // getter, setter, equals, hashCode, toString
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String name;
    private String email;
}
```

#### Kotlin에서는
```kotlin
// data class: equals, hashCode, toString, copy, componentN 자동 생성
data class UserDto(
    val name: String,
    val email: String
)

// 자동 생성되는 것들
val dto1 = UserDto("John", "john@example.com")
val dto2 = UserDto("John", "john@example.com")

dto1 == dto2           // true (equals: 모든 프로퍼티 비교)
dto1.hashCode()        // name, email 기반 해시
dto1.toString()        // "UserDto(name=John, email=john@example.com)"
dto1.copy(name = "Jane")  // 새 인스턴스 생성, email은 유지

// 구조 분해 (componentN)
val (name, email) = dto1  // name = "John", email = "john@example.com"

// ❌ Entity에 data class 사용 시 문제
@Entity
data class User(
    @Id @GeneratedValue
    val id: Long? = null,
    var name: String
)

// 문제 1: equals/hashCode가 모든 필드 포함
val user1 = User(id = 1, name = "John")
val user2 = User(id = 1, name = "John Updated")
user1 == user2  // false! (name이 다름) → Set에서 중복 처리 실패

// 문제 2: copy()로 새 인스턴스 생성 → 영속성 컨텍스트 분리
val updated = user1.copy(name = "Jane")  // 새 객체 → 영속성 컨텍스트에서 관리 안 됨

// 문제 3: toString()에서 Lazy 프록시 강제 로딩
@Entity
data class Order(
    @OneToMany(fetch = LAZY)
    val items: List<OrderItem> = emptyList()  // toString()에서 N+1 쿼리!
)
```

#### 왜 (이유)
- **DTO에 적합**: 값의 동등성으로 비교, 불변, 직렬화/역직렬화에 편리.
- **Entity에 부적합**: 
  - JPA는 id 기반 동등성 사용
  - 영속성 컨텍스트에서 동일 id는 같은 인스턴스여야 함
  - `copy()`가 새 인스턴스를 만들어 영속성 관리 방해
  - `toString()`이 lazy 필드 접근하여 의도치 않은 쿼리 발생

#### 트레이드오프

| 구분 | data class | 일반 class |
|------|------------|------------|
| equals/hashCode | 모든 프로퍼티 기반 | 직접 구현 필요 |
| copy() | 자동 제공 | 없음 |
| toString() | 모든 프로퍼티 출력 | 직접 구현 필요 |
| JPA Entity | ❌ 부적합 | ✅ 적합 |
| DTO/Command | ✅ 적합 | 과한 보일러플레이트 |

#### 실무 체크리스트
- [ ] JPA Entity에 `data class` 사용 금지 (코드 리뷰 필수 체크)
- [ ] DTO는 `data class` 적극 활용
- [ ] data class의 프로퍼티는 `val` 권장 (불변)
- [ ] 민감 정보 포함 시 `toString()` 오버라이드 고려
- [ ] copy() 사용 시 새 인스턴스임을 인지

---

### 1.5 object / companion object / top-level function

#### 결론 (권장안 1줄)
> **Java의 static 메서드는 top-level function으로 대체하고, static 필드가 필요하면 companion object를 사용한다. 싱글톤은 object로 선언한다.**

#### Java에서는
```java
// static 메서드
public class StringUtils {
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}

// static 필드 + 메서드
public class User {
    private static final Logger log = LoggerFactory.getLogger(User.class);
    public static final String DEFAULT_ROLE = "USER";
    
    public static User createGuest() {
        return new User("guest");
    }
}

// 싱글톤 패턴
public class DatabaseConnection {
    private static final DatabaseConnection INSTANCE = new DatabaseConnection();
    private DatabaseConnection() {}
    public static DatabaseConnection getInstance() { return INSTANCE; }
}
```

#### Kotlin에서는
```kotlin
// 1. Top-level function: static 유틸 메서드 대체
// StringUtils.kt 파일
fun String?.isEmpty(): Boolean = this == null || this.isEmpty()

// 사용: import 후 직접 호출
import com.example.isEmpty
val result = "hello".isEmpty()

// 2. companion object: static 필드 + 팩토리 메서드
class User private constructor(val name: String) {
    
    companion object {
        private val log = LoggerFactory.getLogger(User::class.java)
        const val DEFAULT_ROLE = "USER"  // 컴파일타임 상수
        
        // 팩토리 메서드
        fun createGuest(): User = User("guest")
        
        // Java에서 static처럼 호출하려면
        @JvmStatic
        fun createAdmin(): User = User("admin")
    }
}

// 사용
User.DEFAULT_ROLE      // "USER"
User.createGuest()     // User("guest")
User.Companion         // companion object 자체 참조

// 3. object: 싱글톤
object DatabaseConnection {
    init {
        println("Initialized")  // 최초 접근 시 한 번만 실행
    }
    
    fun connect() { ... }
}

// 사용
DatabaseConnection.connect()  // 싱글톤 인스턴스의 메서드 호출

// 4. object expression: 익명 객체 (Java의 익명 클래스)
val comparator = object : Comparator<String> {
    override fun compare(a: String, b: String): Int = a.length - b.length
}
```

#### 왜 (이유)
- **static이 없는 이유**: Kotlin은 모든 것이 객체. top-level function은 JVM에서 static 메서드로 컴파일됨.
- **companion object**: 클래스와 연관된 상수/팩토리가 필요할 때. Java interop을 위해 `@JvmStatic` 사용.
- **object**: thread-safe 싱글톤을 언어 차원에서 지원. lazy initialization 보장.

#### 트레이드오프

| 패턴 | 사용 시점 | Java interop |
|------|-----------|--------------|
| top-level function | 유틸리티 함수, 확장 함수 | `FileKt.functionName()` |
| companion object | 클래스 연관 상수/팩토리 | `Class.Companion.method()` 또는 `@JvmStatic` |
| object | 싱글톤 | `Object.INSTANCE.method()` |

#### 실무 체크리스트
- [ ] 유틸리티 함수는 top-level function 또는 확장 함수로
- [ ] 상수는 `companion object` 내 `const val`로 선언
- [ ] Java에서 호출 필요하면 `@JvmStatic` 추가
- [ ] 싱글톤이 필요하면 `object` 사용 (Spring Bean 아닌 경우)
- [ ] 로거는 companion object에 선언

---

### 1.6 sealed class / enum / value class (inline class)

#### 결론 (권장안 1줄)
> **한정된 타입 계층은 sealed class로, 단순 상수는 enum으로, 타입 안전한 wrapper는 value class로 사용한다.**

#### Java에서는
```java
// enum: 상수 집합
public enum Status {
    ACTIVE, INACTIVE, DELETED
}

// sealed class (Java 17+)
public sealed interface Result permits Success, Failure { }
public record Success(String data) implements Result { }
public record Failure(Exception error) implements Result { }

// wrapper 타입 (primitive 래핑)
public class UserId {
    private final long value;
    public UserId(long value) { this.value = value; }
    public long getValue() { return value; }
}
```

#### Kotlin에서는
```kotlin
// 1. enum class: 상수 집합
enum class Status {
    ACTIVE, INACTIVE, DELETED;
    
    fun isActive(): Boolean = this == ACTIVE
}

// enum with property
enum class HttpStatus(val code: Int, val message: String) {
    OK(200, "OK"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_ERROR(500, "Internal Server Error");
    
    fun isSuccess(): Boolean = code in 200..299
}

// 2. sealed class: 한정된 타입 계층 (when에서 else 불필요)
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// when 사용 시 컴파일러가 모든 케이스 체크
fun handleResult(result: Result<String>): String = when (result) {
    is Result.Success -> "Data: ${result.data}"
    is Result.Failure -> "Error: ${result.error.message}"
    is Result.Loading -> "Loading..."
    // else 불필요! 모든 케이스 커버됨
}

// sealed interface (Kotlin 1.5+)
sealed interface ApiResponse {
    data class Success(val body: String) : ApiResponse
    data class Error(val code: Int, val message: String) : ApiResponse
}

// 3. value class (inline class): 런타임 오버헤드 없는 wrapper
@JvmInline
value class UserId(val value: Long)

@JvmInline
value class Email(val value: String) {
    init {
        require(value.contains("@")) { "Invalid email" }
    }
}

// 사용: 타입 안전성 + 성능
fun findUser(id: UserId): User? = ...
fun sendEmail(email: Email): Unit = ...

val userId = UserId(123L)
val email = Email("test@example.com")

findUser(userId)      // ✅
findUser(email.value) // ❌ 컴파일 에러! Long 직접 전달 불가
sendEmail(email)      // ✅
```

#### 왜 (이유)
- **sealed class**: 상속 계층이 컴파일타임에 확정되어 when에서 모든 케이스 체크 가능. 새 하위 타입 추가 시 컴파일 에러로 누락 방지.
- **enum vs sealed**: enum은 싱글톤 인스턴스, sealed는 각각 다른 프로퍼티 가질 수 있음.
- **value class**: 런타임에는 unwrap되어 primitive처럼 동작하지만 컴파일타임에는 타입 안전성 제공.

#### 트레이드오프

| 타입 | 사용 시점 | 제한사항 |
|------|-----------|----------|
| enum | 고정된 상수 집합 | 인스턴스별 다른 데이터 불가 |
| sealed class | 한정된 타입 계층, 각각 다른 데이터 | 같은 패키지(또는 파일)에 정의 필요 |
| value class | 타입 안전한 primitive wrapper | 단일 프로퍼티만, 다른 클래스 상속 불가 |

#### 실무 체크리스트
- [ ] API 응답 모델링에 sealed class 활용 (Success/Error/Loading)
- [ ] 도메인 ID는 value class로 감싸서 타입 안전성 확보
- [ ] enum에 비즈니스 로직 메서드 추가 고려
- [ ] sealed class의 하위 타입은 같은 파일에 정의 (가독성)
- [ ] when 사용 시 else 대신 모든 케이스 명시 (sealed의 장점 활용)

---

## 2. Spring + Kotlin 관례

### 2.1 생성자 주입 (Primary Constructor) 권장 패턴

#### 결론 (권장안 1줄)
> **모든 의존성은 primary constructor를 통해 주입받는다. `@Autowired` 필드 주입과 `lateinit var`는 사용하지 않는다.**

#### Java에서는
```java
// 필드 주입 (권장하지 않음)
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}

// 생성자 주입 (권장)
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// Lombok으로 간소화
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}
```

#### Kotlin에서는
```kotlin
// ❌ 필드 주입 (절대 사용 금지)
@Service
class UserService {
    @Autowired
    lateinit var userRepository: UserRepository
}

// ✅ 생성자 주입 (권장)
@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    // 비즈니스 로직
}

// ✅ 선택적 의존성이 있는 경우
@Service
class UserService(
    private val userRepository: UserRepository,
    private val cacheService: CacheService? = null  // nullable + 기본값
) {
    fun findUser(id: Long): User? {
        return cacheService?.get(id) ?: userRepository.findById(id).orElse(null)
    }
}

// ✅ @Value로 설정값 주입
@Service
class NotificationService(
    private val userRepository: UserRepository,
    @Value("\${notification.enabled:true}")
    private val enabled: Boolean
)

// ✅ @Qualifier 사용
@Service
class PaymentService(
    @Qualifier("stripeClient")
    private val paymentClient: PaymentClient
)
```

#### 왜 (이유)
- **불변성**: `val`로 선언하여 재할당 불가. 런타임에 의존성 변경 위험 제거.
- **null-safety**: non-null 타입으로 선언. 컴파일타임에 의존성 보장.
- **테스트 용이**: 생성자에 mock 전달만으로 테스트 가능. `@SpringBootTest` 불필요한 경우 많음.
- **순환 참조 감지**: 생성 시점에 모든 의존성 필요. 순환 참조 있으면 앱 시작 실패.

#### 실무 체크리스트
- [ ] 모든 Spring Bean은 생성자 주입 사용
- [ ] `@Autowired`, `lateinit var` 조합 코드 리뷰에서 reject
- [ ] 의존성 5개 초과 시 클래스 분리 검토
- [ ] 선택적 의존성은 nullable + default value로 처리
- [ ] 테스트에서 생성자로 mock 직접 주입 가능한지 확인

---

### 2.2 all-open/no-arg 플러그인의 필요성과 적용 이유

#### 결론 (권장안 1줄)
> **kotlin-spring 플러그인(all-open 포함)과 kotlin-jpa 플러그인(no-arg 포함)은 Spring + JPA 프로젝트에서 필수이다.**

#### 문제 상황
```kotlin
// ❌ 플러그인 없이 작성한 Entity
@Entity
class User(  // final 클래스 → Lazy loading 프록시 불가
    @Id val id: Long,
    var name: String
)  // 기본 생성자 없음 → Hibernate 인스턴스화 불가

// 에러 메시지:
// - "No default constructor for entity"
// - "final class cannot be proxied"
```

#### build.gradle.kts 설정
```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"  // all-open 포함
    kotlin("plugin.jpa") version "1.9.22"     // no-arg 포함
}

// all-open 커스텀 설정 (필요시)
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

// no-arg 커스텀 설정 (필요시)
noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

#### 왜 (이유)

| 플러그인 | 역할 | 해결하는 문제 |
|----------|------|---------------|
| kotlin-spring (all-open) | @Component, @Service, @Configuration, @Transactional 등에 자동 open | CGLIB 프록시 생성, AOP 적용 |
| kotlin-jpa (no-arg) | @Entity, @MappedSuperclass, @Embeddable에 기본 생성자 생성 | Hibernate 리플렉션 인스턴스화 |

#### 실무 체크리스트
- [ ] Spring Boot + JPA 프로젝트에 두 플러그인 모두 적용
- [ ] 플러그인 버전을 Kotlin 버전과 일치시킴
- [ ] IDE에서 "No default constructor" 경고가 뜨지 않는지 확인
- [ ] 프록시 동작 확인 (디버그 모드에서 클래스명에 `$$EnhancerBySpringCGLIB` 포함 여부)

---

### 2.3 AOP/프록시/트랜잭션에서 Kotlin이 흔히 겪는 함정

#### 결론 (권장안 1줄)
> **self-invocation 문제를 피하고, 확장 함수에 @Transactional을 붙이지 않는다.**

```kotlin
// ❌ self-invocation 문제
@Service
class OrderService {
    @Transactional
    fun processOrder(orderId: Long) { ... }
    
    fun batchProcess(orderIds: List<Long>) {
        orderIds.forEach { id ->
            processOrder(id)  // ❌ this.processOrder() → 프록시 우회!
        }
    }
}

// ✅ 해결: 별도 클래스로 분리
@Service
class OrderService(private val orderProcessor: OrderProcessor) {
    fun batchProcess(orderIds: List<Long>) {
        orderIds.forEach { orderProcessor.processOrder(it) }
    }
}

@Service
class OrderProcessor {
    @Transactional
    fun processOrder(orderId: Long) { ... }
}

// ❌ 확장 함수에 @Transactional 적용 안 됨
@Transactional
fun UserRepository.findActiveUsers(): List<User> { ... }  // AOP 미적용!
```

#### 실무 체크리스트
- [ ] 같은 클래스 내 @Transactional 메서드 호출 시 self-invocation 확인
- [ ] 확장 함수에 AOP 애노테이션 붙이지 않기
- [ ] private 메서드에 AOP 애노테이션 붙이지 않기

---

## 3. JPA/Hibernate + Kotlin (핵심 섹션)

### 3.1 Kotlin Entity 기본 템플릿 (권장 베이스)

#### 결론 (권장안 1줄)
> **id는 nullable Long으로, 상태 변경은 도메인 메서드로, 연관관계 컬렉션은 내부에서만 변경 가능하게 설계한다.**

```kotlin
@Entity
@Table(name = "users")
class User(
    @Column(nullable = false)
    val email: String,
    
    @Column(nullable = false)
    var name: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = UserStatus.ACTIVE
        private set
    
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _orders: MutableList<Order> = mutableListOf()
    val orders: List<Order> get() = _orders.toList()
    
    // 도메인 메서드
    fun deactivate() {
        require(status == UserStatus.ACTIVE) { "이미 비활성화됨" }
        status = UserStatus.INACTIVE
    }
    
    fun addOrder(order: Order) {
        _orders.add(order)
    }
    
    // equals/hashCode: id 기반
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = this::class.hashCode()
    
    // toString: lazy 필드 제외
    override fun toString(): String = "User(id=$id, email=$email, status=$status)"
}
```

#### 실무 체크리스트
- [ ] Entity는 일반 class 사용 (data class 금지)
- [ ] id는 `val id: Long? = null`로 선언
- [ ] 상태 변경은 `private set` + 도메인 메서드
- [ ] 컬렉션은 `private MutableList` + `public List` getter
- [ ] equals/hashCode는 id 기반, toString에서 lazy 필드 제외

---

### 3.2 equals/hashCode 전략

#### 결론 (권장안 1줄)
> **Entity의 equals/hashCode는 id 기반으로 직접 구현한다. data class는 Entity에 절대 사용하지 않는다.**

```kotlin
// ❌ data class로 Entity
@Entity
data class User(val id: Long?, var name: String)  // 모든 필드 기반 equals!

// ✅ 일반 class + id 기반 equals/hashCode
@Entity
class User(var name: String) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = this::class.hashCode()  // 상수 반환
}
```

---

### 3.3 연관관계 컬렉션 패턴

#### 결론 (권장안 1줄)
> **내부는 `private MutableList`, 외부는 `List`로 읽기 전용 노출. 양방향은 편의 메서드로 동기화.**

```kotlin
@Entity
class Order(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
    
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _items: MutableList<OrderItem> = mutableListOf()
    val items: List<OrderItem> get() = _items.toList()
    
    fun addItem(product: Product, quantity: Int): OrderItem {
        val item = OrderItem(order = this, product = product, quantity = quantity)
        _items.add(item)
        return item
    }
}
```

---

### 3.4 @Embeddable 값 타입 설계

#### 결론 (권장안 1줄)
> **값 타입은 `@Embeddable` + `data class`로 정의하고, 완전한 불변으로 설계한다.**

```kotlin
@Embeddable
data class Address(
    @Column(nullable = false, length = 50)
    val city: String,
    
    @Column(nullable = false, length = 100)
    val street: String,
    
    @Column(nullable = false, length = 10)
    val zipCode: String
)

@Embeddable
data class Money(
    @Column(nullable = false, precision = 19, scale = 4)
    val amount: BigDecimal,
    
    @Column(nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    val currency: Currency
) {
    operator fun plus(other: Money): Money {
        require(currency == other.currency)
        return Money(amount + other.amount, currency)
    }
}
```

---

### 3.5 Auditing 사용 시 주의

#### 결론 (권장안 1줄)
> **BaseEntity 패턴으로 감사 필드를 정의하고, `protected set`으로 외부 변경을 차단한다.**

```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set
    
    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set
}

// JPA 설정
@Configuration
@EnableJpaAuditing
class JpaConfig
```

---

### 3.6 setter 최소화 + 도메인 메서드 + dirty checking

#### 결론 (권장안 1줄)
> **public setter를 제거하고, 도메인 메서드로 상태를 변경한다. 트랜잭션 내에서 dirty checking이 자동으로 UPDATE한다.**

```kotlin
@Entity
class Order(
    @ManyToOne(fetch = FetchType.LAZY)
    val user: User
) : BaseEntity() {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
    
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING
        private set  // 외부에서 변경 불가
    
    var cancelReason: String? = null
        private set
    
    fun complete() {
        require(status == OrderStatus.PROCESSING) { "처리 중인 주문만 완료 가능" }
        status = OrderStatus.COMPLETED
    }
    
    fun cancel(reason: String) {
        require(status in listOf(OrderStatus.PENDING, OrderStatus.PROCESSING))
        status = OrderStatus.CANCELLED
        cancelReason = reason
    }
}

// Service
@Service
class OrderService(private val orderRepository: OrderRepository) {
    @Transactional
    fun completeOrder(orderId: Long) {
        val order = orderRepository.findById(orderId).orElseThrow()
        order.complete()  // dirty checking으로 자동 UPDATE
        // save() 호출 불필요!
    }
}
```

---

## 4. DTO/Command/Response 설계

### 4.1 DTO를 data class로

#### 결론 (권장안 1줄)
> **DTO는 data class로 선언한다. jackson-module-kotlin 의존성을 반드시 추가한다.**

```kotlin
// Request DTO
data class CreateUserRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    val name: String,
    
    @field:Email @field:NotBlank
    val email: String,
    
    val nickname: String? = null  // 선택 필드
)

// Response DTO
data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime
)

// 변환: 확장 함수
fun User.toResponse() = UserResponse(
    id = id!!,
    name = name,
    email = email,
    createdAt = createdAt
)
```

---

### 4.2 Validation과 nullable 관계

#### 결론 (권장안 1줄)
> **Kotlin nullable과 Bean Validation은 다른 시점에 동작한다. `@field:` 사용 위치에 주의한다.**

```kotlin
data class CreateProductRequest(
    @field:NotBlank(message = "상품명은 필수")
    @field:Size(min = 2, max = 100)
    val name: String,
    
    @field:NotNull @field:Min(0)
    val price: BigDecimal,
    
    @field:Size(max = 1000)
    val description: String? = null
)

// Controller
@PostMapping("/products")
fun create(@Valid @RequestBody request: CreateProductRequest): ProductResponse
```

---

### 4.3 API 응답 표준 포맷

```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorResponse?
) {
    companion object {
        fun <T> success(data: T) = ApiResponse(true, data, null)
        fun <T> error(error: ErrorResponse) = ApiResponse<T>(false, null, error)
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val details: List<FieldError>? = null
)

data class FieldError(val field: String, val message: String)
```

---

## 5. 예외/에러 처리

### 5.1 도메인 예외 설계

#### 결론 (권장안 1줄)
> **비즈니스 예외는 sealed class 계층으로 정의한다.**

```kotlin
sealed class BusinessException(
    val code: String,
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)

sealed class UserException(code: String, message: String, status: HttpStatus = HttpStatus.BAD_REQUEST)
    : BusinessException(code, message, status) {
    
    class NotFound(userId: Long) : UserException(
        "USER_NOT_FOUND", "사용자를 찾을 수 없습니다: $userId", HttpStatus.NOT_FOUND
    )
    
    class DuplicateEmail(email: String) : UserException(
        "DUPLICATE_EMAIL", "이미 사용 중인 이메일: $email", HttpStatus.CONFLICT
    )
}
```

### 5.2 전역 예외 처리

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = e.bindingResult.fieldErrors.map {
            FieldError(it.field, it.defaultMessage ?: "Invalid")
        }
        return ResponseEntity.badRequest().body(
            ApiResponse.error(ErrorResponse("VALIDATION_ERROR", "입력값 검증 실패", errors))
        )
    }
    
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(e.status).body(
            ApiResponse.error(ErrorResponse(e.code, e.message))
        )
    }
}
```

---

## 6. 테스트

### 6.1 JUnit5 + Kotlin 스타일

```kotlin
@SpringBootTest
@Transactional
class UserServiceTest @Autowired constructor(
    private val userService: UserService,
    private val userRepository: UserRepository
) {
    @Nested
    @DisplayName("createUser")
    inner class CreateUser {
        
        @Test
        fun `정상적인 요청으로 사용자를 생성한다`() {
            // given
            val request = CreateUserRequest(name = "John", email = "john@example.com")
            
            // when
            val result = userService.create(request)
            
            // then
            assertThat(result.id).isNotNull()
            assertThat(result.name).isEqualTo("John")
        }
        
        @Test
        fun `중복 이메일로 가입하면 예외 발생`() {
            // given
            userRepository.save(User(name = "Existing", email = "john@example.com"))
            
            // when & then
            assertThatThrownBy { 
                userService.create(CreateUserRequest("John", "john@example.com"))
            }.isInstanceOf(UserException.DuplicateEmail::class.java)
        }
    }
}
```

---

## 7. Java 습관 → Kotlin 대체표

| # | Java 습관 | Kotlin 대체 | 예시 |
|---|-----------|-------------|------|
| 1 | `@Getter/@Setter` | `val`/`var` property | `var name: String` |
| 2 | `@Builder` | named argument + default | `User(name = "John")` |
| 3 | `@AllArgsConstructor` | primary constructor | `class User(val name: String)` |
| 4 | `@NoArgsConstructor` | no-arg 플러그인 | `kotlin-jpa` 플러그인 |
| 5 | `@Data` | `data class` | `data class UserDto(...)` |
| 6 | `Optional<T>` | `T?` nullable | `fun find(): User?` |
| 7 | `Optional.map().orElse()` | `?.let { } ?: default` | `user?.name ?: "Unknown"` |
| 8 | `static` 메서드 | top-level / companion | `fun helper()` |
| 9 | `static final` 상수 | `const val` | `const val MAX = 100` |
| 10 | `instanceof` + cast | `is` + smart cast | `if (x is String) x.length` |
| 11 | `switch` | `when` | `when (status) { ... }` |
| 12 | `Stream.map().filter()` | `.map { }.filter { }` | `list.filter { it > 0 }` |
| 13 | `Collections.singletonList()` | `listOf()` | `listOf(item)` |
| 14 | `new ArrayList<>()` | `mutableListOf()` | `mutableListOf<String>()` |
| 15 | `StringUtils.isEmpty()` | `isNullOrBlank()` | `str.isNullOrBlank()` |
| 16 | try-with-resources | `.use { }` | `file.use { it.readText() }` |
| 17 | `for (int i = 0; ...)` | `for (i in 0 until n)` | `for (i in 0..9)` |
| 18 | `getClass()` | `this::class` / `javaClass` | `javaClass.simpleName` |
| 19 | `synchronized` 블록 | `synchronized()` 함수 | `synchronized(lock) { }` |
| 20 | Checked exception | 그냥 호출 (unchecked) | 강제 try-catch 없음 |

---

## 부록 A: Gradle Kotlin DSL 템플릿

```kotlin
// build.gradle.kts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Database
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

---

## 부록 B: 코드 템플릿 10종

### B.1 BaseEntity (감사 필드 포함)
```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set
    
    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set
}
```

### B.2 양방향 연관관계 Entity
```kotlin
@Entity
@Table(name = "orders")
class Order(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(nullable = false, unique = true, length = 20)
    val orderNumber: String
) : BaseEntity() {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PENDING
        private set
    
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _items: MutableList<OrderItem> = mutableListOf()
    val items: List<OrderItem> get() = _items.toList()
    
    fun addItem(product: Product, quantity: Int, unitPrice: BigDecimal): OrderItem {
        val item = OrderItem(
            order = this,
            product = product,
            quantity = quantity,
            unitPrice = unitPrice
        )
        _items.add(item)
        return item
    }
    
    fun complete() {
        require(status == OrderStatus.PROCESSING) { "처리 중인 주문만 완료 가능" }
        status = OrderStatus.COMPLETED
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Order) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = this::class.hashCode()
    
    override fun toString(): String = "Order(id=$id, orderNumber=$orderNumber, status=$status)"
}

@Entity
@Table(name = "order_items")
class OrderItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,
    
    @Column(nullable = false)
    var quantity: Int,
    
    @Column(nullable = false)
    val unitPrice: BigDecimal
) : BaseEntity() {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
    
    val totalPrice: BigDecimal
        get() = unitPrice * quantity.toBigDecimal()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderItem) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = this::class.hashCode()
}
```

### B.3 값 타입 (Embeddable)
```kotlin
@Embeddable
data class Address(
    @Column(nullable = false, length = 50)
    val city: String,
    
    @Column(nullable = false, length = 100)
    val street: String,
    
    @Column(nullable = false, length = 10)
    val zipCode: String
)

@Embeddable
data class Money(
    @Column(nullable = false, precision = 19, scale = 4)
    val amount: BigDecimal,
    
    @Column(nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    val currency: Currency
) {
    init {
        require(amount >= BigDecimal.ZERO) { "금액은 0 이상이어야 합니다" }
    }
    
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "통화가 다릅니다" }
        return Money(amount + other.amount, currency)
    }
    
    operator fun times(multiplier: Int): Money {
        return Money(amount * multiplier.toBigDecimal(), currency)
    }
}

enum class Currency { KRW, USD, EUR, JPY }
```

### B.4 Request DTO + Validation
```kotlin
data class CreateOrderRequest(
    @field:NotNull(message = "사용자 ID는 필수입니다")
    val userId: Long,
    
    @field:NotEmpty(message = "주문 항목은 최소 1개 필요합니다")
    val items: List<OrderItemRequest>,
    
    @field:Valid
    val shippingAddress: AddressRequest,
    
    val couponCode: String? = null
)

data class OrderItemRequest(
    @field:NotNull(message = "상품 ID는 필수입니다")
    val productId: Long,
    
    @field:Min(value = 1, message = "수량은 1 이상이어야 합니다")
    val quantity: Int
)

data class AddressRequest(
    @field:NotBlank(message = "도시는 필수입니다")
    val city: String,
    
    @field:NotBlank(message = "주소는 필수입니다")
    val street: String,
    
    @field:Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자입니다")
    val zipCode: String
) {
    fun toAddress() = Address(city, street, zipCode)
}
```

### B.5 Response DTO
```kotlin
data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val status: String,
    val items: List<OrderItemResponse>,
    val totalAmount: BigDecimal,
    val shippingAddress: AddressResponse?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(order: Order) = OrderResponse(
            id = order.id!!,
            orderNumber = order.orderNumber,
            status = order.status.name,
            items = order.items.map { OrderItemResponse.from(it) },
            totalAmount = order.items.sumOf { it.totalPrice },
            shippingAddress = order.shippingAddress?.let { AddressResponse.from(it) },
            createdAt = order.createdAt
        )
    }
}

data class OrderItemResponse(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal
) {
    companion object {
        fun from(item: OrderItem) = OrderItemResponse(
            productId = item.product.id!!,
            productName = item.product.name,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            totalPrice = item.totalPrice
        )
    }
}

data class AddressResponse(
    val city: String,
    val street: String,
    val zipCode: String
) {
    companion object {
        fun from(address: Address) = AddressResponse(
            city = address.city,
            street = address.street,
            zipCode = address.zipCode
        )
    }
}
```

### B.6 Service (트랜잭션 경계)
```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    @Transactional
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        val user = userRepository.findById(request.userId)
            .orElseThrow { UserException.NotFound(request.userId) }
        
        val orderNumber = generateOrderNumber()
        val order = Order(user = user, orderNumber = orderNumber)
        
        request.items.forEach { itemRequest ->
            val product = productRepository.findById(itemRequest.productId)
                .orElseThrow { ProductException.NotFound(itemRequest.productId) }
            
            order.addItem(
                product = product,
                quantity = itemRequest.quantity,
                unitPrice = product.price
            )
        }
        
        request.shippingAddress?.let {
            order.updateShippingAddress(it.toAddress())
        }
        
        val saved = orderRepository.save(order)
        log.info("Order created: id={}, orderNumber={}", saved.id, saved.orderNumber)
        
        return OrderResponse.from(saved)
    }
    
    @Transactional(readOnly = true)
    fun findById(id: Long): OrderResponse {
        val order = orderRepository.findByIdWithItems(id)
            ?: throw OrderException.NotFound(id)
        return OrderResponse.from(order)
    }
    
    @Transactional
    fun completeOrder(id: Long): OrderResponse {
        val order = orderRepository.findById(id)
            .orElseThrow { OrderException.NotFound(id) }
        
        order.complete()
        // dirty checking으로 자동 UPDATE
        
        return OrderResponse.from(order)
    }
    
    private fun generateOrderNumber(): String {
        return "ORD-${System.currentTimeMillis()}"
    }
}
```

### B.7 Repository
```kotlin
interface OrderRepository : JpaRepository<Order, Long> {
    
    @Query("""
        SELECT o FROM Order o 
        JOIN FETCH o.user 
        WHERE o.id = :id
    """)
    fun findByIdWithUser(@Param("id") id: Long): Order?
    
    @Query("""
        SELECT DISTINCT o FROM Order o 
        JOIN FETCH o.user 
        LEFT JOIN FETCH o.items i 
        LEFT JOIN FETCH i.product 
        WHERE o.id = :id
    """)
    fun findByIdWithItems(@Param("id") id: Long): Order?
    
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Order>
    
    fun findByStatus(status: OrderStatus): List<Order>
    
    @Query("""
        SELECT o FROM Order o 
        WHERE o.user.id = :userId 
        AND o.status = :status 
        ORDER BY o.createdAt DESC
    """)
    fun findByUserIdAndStatus(
        @Param("userId") userId: Long,
        @Param("status") status: OrderStatus
    ): List<Order>
}
```

### B.8 Controller
```kotlin
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService
) {
    @PostMapping
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<ApiResponse<OrderResponse>> {
        val order = orderService.createOrder(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(order))
    }
    
    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: Long): ApiResponse<OrderResponse> {
        return ApiResponse.success(orderService.findById(id))
    }
    
    @PostMapping("/{id}/complete")
    fun completeOrder(@PathVariable id: Long): ApiResponse<OrderResponse> {
        return ApiResponse.success(orderService.completeOrder(id))
    }
}
```

### B.9 ControllerAdvice
```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = e.bindingResult.fieldErrors.map {
            FieldError(it.field, it.defaultMessage ?: "Invalid value")
        }
        log.warn("Validation failed: {}", errors)
        
        return ResponseEntity.badRequest().body(
            ApiResponse.error(ErrorResponse(
                code = "VALIDATION_ERROR",
                message = "입력값 검증에 실패했습니다",
                details = errors
            ))
        )
    }
    
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("Business exception: code={}, message={}", e.code, e.message)
        
        return ResponseEntity.status(e.status).body(
            ApiResponse.error(ErrorResponse(code = e.code, message = e.message))
        )
    }
    
    @ExceptionHandler(Exception::class)
    fun handleUnknown(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unexpected error", e)
        
        return ResponseEntity.internalServerError().body(
            ApiResponse.error(ErrorResponse(
                code = "INTERNAL_ERROR",
                message = "서버 내부 오류가 발생했습니다"
            ))
        )
    }
}
```

### B.10 테스트 (단위/통합)
```kotlin
// 단위 테스트
class OrderTest {
    
    @Test
    fun `주문을 완료한다`() {
        // given
        val user = User(name = "John", email = "john@example.com")
        val order = Order(user = user, orderNumber = "ORD-001")
        order.startProcessing()
        
        // when
        order.complete()
        
        // then
        assertThat(order.status).isEqualTo(OrderStatus.COMPLETED)
    }
    
    @Test
    fun `대기 중인 주문은 완료할 수 없다`() {
        // given
        val user = User(name = "John", email = "john@example.com")
        val order = Order(user = user, orderNumber = "ORD-001")
        
        // when & then
        assertThatThrownBy { order.complete() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("처리 중인 주문만")
    }
}

// 통합 테스트
@SpringBootTest
@Transactional
class OrderServiceIntegrationTest @Autowired constructor(
    private val orderService: OrderService,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository
) {
    @Test
    fun `주문을 생성한다`() {
        // given
        val user = userRepository.save(User(name = "John", email = "john@example.com"))
        val product = productRepository.save(Product(name = "Test Product", price = BigDecimal("10000")))
        
        val request = CreateOrderRequest(
            userId = user.id!!,
            items = listOf(OrderItemRequest(productId = product.id!!, quantity = 2)),
            shippingAddress = AddressRequest("Seoul", "Gangnam-ro 1", "06000")
        )
        
        // when
        val result = orderService.createOrder(request)
        
        // then
        assertThat(result.id).isNotNull()
        assertThat(result.status).isEqualTo("PENDING")
        assertThat(result.items).hasSize(1)
        assertThat(result.totalAmount).isEqualByComparingTo(BigDecimal("20000"))
    }
}
```

---

## 부록 C: 팀 코딩 컨벤션 체크리스트 (40개)

### Entity 관련 (10개)
- [ ] 1. Entity에 `data class` 사용 금지
- [ ] 2. id는 `val id: Long? = null`로 선언
- [ ] 3. 상태 변경 필드는 `private set` + 도메인 메서드
- [ ] 4. 컬렉션은 `private MutableList` + `public List` getter
- [ ] 5. equals/hashCode는 id 기반으로 직접 구현
- [ ] 6. toString에서 lazy 연관관계 필드 제외
- [ ] 7. `@ManyToOne`은 기본 `FetchType.LAZY`
- [ ] 8. 양방향 연관관계는 편의 메서드로 동기화
- [ ] 9. `kotlin-jpa`, `kotlin-spring` 플러그인 필수
- [ ] 10. BaseEntity 상속으로 감사 필드 통일

### DTO 관련 (8개)
- [ ] 11. DTO는 `data class` 사용
- [ ] 12. Request/Response DTO 분리
- [ ] 13. Validation 애노테이션에 `@field:` 사용
- [ ] 14. 필수 필드는 non-null, 선택 필드는 `nullable + 기본값`
- [ ] 15. Response 컬렉션은 빈 리스트 반환 (null 아님)
- [ ] 16. `jackson-module-kotlin` 의존성 필수
- [ ] 17. Entity를 Controller에서 직접 반환 금지
- [ ] 18. 변환 로직 위치 통일 (확장 함수 또는 Mapper 클래스)

### Null Safety 관련 (6개)
- [ ] 19. `!!` 사용 금지 (예외적 허용 시 리뷰 필수)
- [ ] 20. `lateinit var` + `@Autowired` 사용 금지
- [ ] 21. 생성자 주입 필수
- [ ] 22. nullable 반환 시 `?.`, `?:`, `let` 활용
- [ ] 23. platform type(`!`) 주의: Java 반환값은 명시적 타입 지정
- [ ] 24. 외부 API 응답은 nullable로 처리

### 컬렉션 관련 (4개)
- [ ] 25. 외부에 `MutableList` 직접 노출 금지
- [ ] 26. 방어적 복사 `toList()` 사용
- [ ] 27. 대용량 데이터 처리 시 `sequence` 고려
- [ ] 28. Java 코드와 공유 시 불변성 보장 확인

### 트랜잭션/AOP 관련 (4개)
- [ ] 29. self-invocation 주의 (같은 클래스 내 @Transactional 호출)
- [ ] 30. 확장 함수에 AOP 애노테이션 붙이지 않기
- [ ] 31. private 메서드에 AOP 애노테이션 붙이지 않기
- [ ] 32. 조회 전용 메서드에 `@Transactional(readOnly = true)`

### 예외 처리 관련 (4개)
- [ ] 33. 비즈니스 예외는 sealed class 계층으로
- [ ] 34. 예외에 에러 코드, 메시지, HTTP 상태 포함
- [ ] 35. GlobalExceptionHandler로 통일된 응답 포맷
- [ ] 36. 민감 정보 예외 메시지에 포함 금지

### 로깅/성능 관련 (4개)
- [ ] 37. 로거는 `companion object`에 선언
- [ ] 38. Entity 전체 로깅 금지 (필요한 필드만)
- [ ] 39. N+1 쿼리 모니터링 (fetch join 사용)
- [ ] 40. `OSIV = false` 설정

---

## 마무리

이 가이드는 Java 개발자가 Kotlin + Spring 환경에서 겪는 주요 문제들을 해결하기 위해 작성되었습니다. 핵심 원칙을 정리하면:

1. **불변 우선**: `val`, `private set`, 도메인 메서드
2. **Null Safety 활용**: `?.`, `?:`, 생성자 주입
3. **플러그인 필수**: `kotlin-spring`, `kotlin-jpa`
4. **Entity ≠ data class**: 일반 class + id 기반 equals/hashCode
5. **캡슐화**: `private MutableList` + `public List`
6. **표준화**: API 응답 포맷, 예외 처리, 코딩 컨벤션

팀 전체가 이 가이드를 기반으로 일관된 코드를 작성하면, Kotlin의 장점을 최대한 활용하면서 Spring/JPA와의 호환 문제를 최소화할 수 있습니다.
