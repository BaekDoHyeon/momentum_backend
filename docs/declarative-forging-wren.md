# Cursor 기반 페이지네이션 응답 구조 구현 계획

## 📋 요약

무한 스크롤을 위한 Cursor 기반 페이지네이션 응답 구조를 구현합니다.

**핵심 요구사항:**
- Cursor 방식: ID + Timestamp 복합 키
- 메타 정보: `hasNext`만 포함 (총 개수, 총 페이지 수 없음)
- 페이지 크기: 클라이언트가 `size` 파라미터로 지정 (기본값 20, 최대값 100)
- 기존 `ApiResponse` 구조와 통합

---

## 🤔 data class vs sealed interface 질문에 대한 답변

### 왜 ApiResponse는 sealed interface이고 DTO는 data class인가?

**설계 원칙:**

1. **sealed interface/class**: "이것 **또는** 저것" (상호 배타적 상태)
   - **ApiResponse**: Success **또는** Error (정확히 하나)
   - when 표현식으로 모든 케이스 강제 처리
   - 컴파일 타임 안전성

   ```kotlin
   when (response) {
       is ApiResponse.Success -> // data 접근
       is ApiResponse.Error -> // code만 접근
   }
   ```

2. **data class**: "이것 **그리고** 저것" (데이터 컨테이너)
   - **UserResponse, TokenResponse**: 단일 구조의 데이터
   - 상태 분기 없음, 데이터만 전송
   - equals/hashCode/copy 자동 생성

   ```kotlin
   data class UserResponse(
       val id: Long,
       val email: String,
       // ... 필드들
   )
   ```

**CursorPageResponse는 왜 data class?**
- 페이지네이션 응답은 **하나의 케이스**만 존재
- "성공" 케이스: `ApiResponse.Success`가 이미 처리
- "실패" 케이스: `ApiResponse.Error`가 이미 처리
- 추가 상태 분기가 불필요하므로 data class가 적합

---

## 🎯 구현할 파일 (생성 순서)

### 1. Cursor 값 객체
**파일**: `/Users/baegdohyeon/momentum/mo_backend/src/main/kotlin/com/momentum/global/dto/Cursor.kt`

**역할:**
- ID + Timestamp 복합 커서 표현
- Base64 인코딩/디코딩 (URL-safe)
- 타입 안전성 보장

**핵심 메서드:**
```kotlin
data class Cursor(
    val lastId: Long,
    val lastTimestamp: LocalDateTime,
) {
    fun encode(): String  // Base64 인코딩

    companion object {
        fun decode(encoded: String): Cursor?  // 디코딩 (실패 시 null)
    }
}
```

**인코딩 방식:**
- 원본: `"lastId:timestamp"` (예: `"123:2025-12-23T10:30:00"`)
- Base64 인코딩: `"MTIzOjIwMjUtMTItMjNUMTA6MzA6MDA="`

---

### 2. CursorPageResponse
**파일**: `/Users/baegdohyeon/momentum/mo_backend/src/main/kotlin/com/momentum/global/dto/CursorPageResponse.kt`

**역할:**
- Generic 페이지네이션 응답 (모든 엔티티/DTO 재사용 가능)
- size+1 조회 패턴으로 `hasNext` 자동 계산
- `nextCursor` 자동 생성

**핵심 필드:**
```kotlin
data class CursorPageResponse<T>(
    val content: List<T>,      // 실제 데이터
    val hasNext: Boolean,       // 다음 페이지 존재 여부
    val nextCursor: String?,    // 다음 요청에 사용할 커서 (Base64)
    val size: Int,              // 실제 반환된 데이터 개수
)
```

**팩토리 메서드:**
```kotlin
companion object {
    fun <T> of(
        content: List<T>,               // size+1로 조회된 데이터
        requestedSize: Int,             // 클라이언트가 요청한 크기
        cursorExtractor: (T) -> Cursor  // 마지막 요소에서 커서 추출
    ): CursorPageResponse<T>
}
```

**size+1 패턴:**
- 클라이언트가 20개 요청 → DB에서 21개 조회
- 21개가 조회되면 → `hasNext=true`, 마지막 1개 제거, 20개 반환
- 20개 이하 조회되면 → `hasNext=false`, 모두 반환

---

### 3. ErrorCode 추가
**파일**: `/Users/baegdohyeon/momentum/mo_backend/src/main/kotlin/com/momentum/global/enums/ErrorCode.kt`

**추가할 에러 코드:**
```kotlin
// 요청 검증 (V: Validation) 섹션에 추가
INVALID_PAGE_SIZE("V003", "페이지 크기는 1 이상 100 이하여야 합니다", HttpStatus.BAD_REQUEST),
INVALID_CURSOR("V004", "잘못된 커서 형식입니다", HttpStatus.BAD_REQUEST),
```

---

## 🔧 ApiResponse와의 통합

### 컨트롤러 사용 예시

```kotlin
@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse.Success<CursorPageResponse<NotificationResponse>>> {
        val response = notificationService.getNotifications(
            userId = user.id!!,
            cursorString = cursor,
            size = size
        )

        return ResponseEntity.ok(
            ApiResponse.Success(
                data = response,  // CursorPageResponse를 data로 사용
                code = SuccessCode.READ
            )
        )
    }
}
```

### 응답 JSON 구조

```json
{
  "data": {
    "content": [
      {
        "id": 123,
        "message": "알림 내용",
        "createdAt": "2025-12-23T10:30:00"
      }
    ],
    "hasNext": true,
    "nextCursor": "MTIzOjIwMjUtMTItMjNUMTA6MzA6MDA=",
    "size": 20
  },
  "code": "S003"
}
```

---

## 📊 Service Layer 구현 패턴

```kotlin
@Service
class NotificationService(...) {
    fun getNotifications(
        userId: Long,
        cursorString: String?,
        size: Int,
    ): CursorPageResponse<NotificationResponse> {
        // 1. 페이지 크기 검증
        val validatedSize = validatePageSize(size)

        // 2. 커서 디코딩
        val cursor = cursorString?.let {
            Cursor.decode(it) ?: throw BusinessException(ErrorCode.INVALID_CURSOR)
        }

        // 3. DB 조회 (size+1)
        val entities = repository.findWithCursor(cursor, validatedSize)

        // 4. 응답 생성
        return CursorPageResponse.of(
            content = entities.map { NotificationResponse.from(it) },
            requestedSize = validatedSize,
            cursorExtractor = { response ->
                Cursor(
                    lastId = response.id!!,
                    lastTimestamp = response.createdAt
                )
            }
        )
    }

    private fun validatePageSize(size: Int): Int {
        return when {
            size < 1 -> 20        // 기본값
            size > 100 -> 100     // 최대값
            else -> size
        }
    }
}
```

---

## 🗄️ Repository 쿼리 패턴

### JPA Query Method (간단한 경우)

```kotlin
interface NotificationRepository : JpaRepository<Notification, Long> {
    // 첫 페이지 (cursor 없음)
    fun findTop21ByUserOrderByCreatedAtDescIdDesc(
        user: User,
        pageable: Pageable
    ): List<Notification>
}
```

### QueryDSL (권장)

```kotlin
override fun findWithCursor(
    user: User,
    cursor: Cursor?,
    size: Int
): List<Notification> {
    return queryFactory
        .selectFrom(notification)
        .where(
            notification.user.eq(user),
            cursorCondition(cursor)
        )
        .orderBy(
            notification.createdAt.desc(),
            notification.id.desc()
        )
        .limit((size + 1).toLong())  // size+1 패턴
        .fetch()
}

private fun cursorCondition(cursor: Cursor?): BooleanExpression? {
    cursor ?: return null

    // (createdAt < cursor.timestamp) OR
    // (createdAt = cursor.timestamp AND id < cursor.id)
    return notification.createdAt.lt(cursor.lastTimestamp)
        .or(
            notification.createdAt.eq(cursor.lastTimestamp)
                .and(notification.id.lt(cursor.lastId))
        )
}
```

**WHERE 조건 설명:**
- 기본: `createdAt < cursor.timestamp`
- 동일 시간: `createdAt = cursor.timestamp AND id < cursor.id`
- 두 조건을 OR로 연결

---

## 🚀 클라이언트 사용 예시

### 첫 페이지 요청
```http
GET /api/notifications?size=20
```

### 다음 페이지 요청
```http
GET /api/notifications?cursor=MTIzOjIwMjUtMTItMjNUMTA6MzA6MDA=&size=20
```

### React 무한 스크롤
```typescript
const loadMore = async () => {
  const params = new URLSearchParams({
    size: '20',
    ...(cursor && { cursor })
  });

  const response = await fetch(`/api/notifications?${params}`);
  const { data } = await response.json();

  setItems(prev => [...prev, ...data.content]);
  setCursor(data.nextCursor);
  setHasNext(data.hasNext);
};
```

---

## ✅ 구현 체크리스트

### Phase 1: 핵심 구조 (필수)
- [ ] **Cursor.kt** 생성
  - [ ] data class 정의
  - [ ] encode() 메서드 (Base64)
  - [ ] decode() companion object 메서드
  - [ ] 단위 테스트 작성

- [ ] **CursorPageResponse.kt** 생성
  - [ ] data class 정의
  - [ ] of() 팩토리 메서드
  - [ ] 단위 테스트 작성

- [ ] **ErrorCode.kt** 수정
  - [ ] INVALID_PAGE_SIZE 추가
  - [ ] INVALID_CURSOR 추가

### Phase 2: 통합 테스트 (선택)
- [ ] 컨트롤러 통합 테스트
- [ ] Repository 쿼리 테스트
- [ ] 엔드-투-엔드 페이지네이션 테스트

---

## 📝 핵심 설계 결정사항

### 1. data class 선택 이유
- CursorPageResponse는 단일 케이스만 존재 (sealed 불필요)
- ApiResponse.Success의 data로 사용되어 계층 분리됨
- 기존 DTO 패턴과 일관성 유지

### 2. 복합 Cursor (ID + Timestamp)
- 단일 ID: 빠르지만 동일 시간 데이터 누락 가능
- 단일 Timestamp: 동일 시간 데이터 처리 복잡
- **복합 키**: 안정적 정렬 + 정확한 커서 위치

### 3. Base64 인코딩
- JSON보다 간결 (`"123:2025-12-23T10:30:00"` → 짧은 문자열)
- URL-safe (쿼리 파라미터에 안전)
- 표준 라이브러리 사용 (의존성 없음)

### 4. size+1 조회 패턴
- COUNT 쿼리 불필요 (성능 이득)
- hasNext를 정확히 판단 가능
- DB 인덱스 효율적 사용

---

## 🎯 기대 효과

### 장점
✅ **일관성**: 데이터 추가/삭제 시에도 중복/누락 없음
✅ **성능**: 큰 offset 없이 WHERE 조건만 사용 (인덱스 효율적)
✅ **확장성**: 무한 스크롤에 최적화
✅ **간단함**: hasNext만으로 충분 (COUNT 불필요)

### 제약사항
⚠️ 페이지 점프 불가 (특정 페이지로 직접 이동 불가)
⚠️ 총 개수 미제공 (필요 시 별도 API 구현 필요)
⚠️ 정렬 제한 (커서 필드 기준으로만 정렬)

---

## 📚 참고 파일

### 핵심 파일
- `/Users/baegdohyeon/momentum/mo_backend/src/main/kotlin/com/momentum/global/dto/ApiResponse.kt`
  → 기존 응답 구조, sealed interface 패턴 참고

- `/Users/baegdohyeon/momentum/mo_backend/src/main/kotlin/com/momentum/global/entity/BaseEntity.kt`
  → createdAt/updatedAt 필드 (커서 timestamp 소스)

- `/Users/baegdohyeon/momentum/mo_backend/src/main/kotlin/com/momentum/global/enums/ErrorCode.kt`
  → 에러 코드 추가 위치

### 참고할 DTO 패턴
- `/Users/baegdohyeon/momentum/mo_backend/src/main/kotlin/com/momentum/domain/auth/dto/response/UserResponse.kt`
- `/Users/baegdohyeon/momentum/mo_backend/src/main/kotlin/com/momentum/domain/auth/dto/response/TokenResponse.kt`
