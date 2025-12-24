# Cursor 기반 페이지네이션 응답 구조 구현 계획

## 📋 요약

무한 스크롤을 위한 Cursor 기반 페이지네이션 응답 구조를 구현합니다.

**핵심 요구사항:**
- Cursor 방식: ID + Timestamp 복합 키
- 메타 정보: `hasNext`만 포함 (총 개수, 총 페이지 수 없음)
- 페이지 크기: 클라이언트가 `size` 파라미터로 지정 (기본값 20, 최대값 100)
- 기존 `ApiResponse` 구조와 통합

---

## 💡 왜 Cursor 방식인가? (Offset 방식과의 비교)

### 자바 전통 방식: Offset 기반 페이징

```java
// Spring Data JPA - 자바 개발자에게 익숙한 방식
Page<Notification> page = repository.findAll(
    PageRequest.of(0, 20)  // page=0, size=20
);

// SQL 예시
SELECT * FROM notifications
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;  -- 1페이지

SELECT * FROM notifications
ORDER BY created_at DESC
LIMIT 20 OFFSET 40; -- 3페이지
```

**문제점:**
```
데이터: [A, B, C, D, E, F, G, H]

1. 사용자가 1페이지 조회 (OFFSET 0, LIMIT 4)
   → 결과: [A, B, C, D]

2. 새 데이터 2개 추가 (X, Y가 맨 앞에 삽입)
   → 데이터: [X, Y, A, B, C, D, E, F, G, H]

3. 사용자가 2페이지 조회 (OFFSET 4, LIMIT 4)
   → 결과: [C, D, E, F]
   → 문제: C, D가 중복 조회됨! (1페이지에서 이미 봤음)
```

**성능 문제:**
```sql
-- OFFSET 10000은 10000개를 스캔 후 버림
SELECT * FROM notifications
ORDER BY created_at DESC
LIMIT 20 OFFSET 10000;  -- 매우 느림!
```

### Cursor 방식: "마지막으로 본 위치"를 기억

```kotlin
// 첫 요청
GET /api/notifications?size=20
→ 응답: { content: [...], nextCursor: "MTIzOjIwMjUtMTItMjNUMTA6MzA6MDA=" }

// 다음 요청 (마지막으로 본 위치를 cursor로 전달)
GET /api/notifications?cursor=MTIzOjIwMjUtMTItMjNUMTA6MzA6MDA=&size=20

// SQL
SELECT * FROM notifications
WHERE (created_at, id) < ('2025-12-23T10:30:00', 123)  -- cursor 조건
ORDER BY created_at DESC, id DESC
LIMIT 21;  -- size+1
```

**장점:**
- ✅ **일관성**: 데이터 추가/삭제 시에도 중복/누락 없음
- ✅ **성능**: OFFSET 없이 WHERE 조건만 사용 → 인덱스 효율적
- ✅ **무한 스크롤**: 소셜미디어 피드처럼 끝없이 스크롤

**비유:**
- **Offset 방식**: "책의 40페이지를 펼쳐줘" → 중간에 페이지 추가되면 위치 틀어짐
- **Cursor 방식**: "마지막으로 읽은 문장 다음부터 읽어줘" → 항상 정확한 위치

---

## 🔐 Cursor 인코딩: 필수인가 선택인가?

### 인코딩 하지 않으면?

```
❌ GET /api/notifications?cursor=123:2025-12-23T10:30:00&size=20

문제점:
1. URL 특수문자: ':' 문자가 인코딩 필요할 수 있음
2. 내부 구조 노출: DB 스키마 (id, timestamp) 그대로 노출
3. 조작 가능성: 사용자가 임의로 "9999:2030-01-01T00:00:00" 같은 값 시도
```

### 인코딩하면?

```
✅ GET /api/notifications?cursor=MTIzOjIwMjUtMTItMjNUMTA6MzA6MDA=&size=20

장점:
1. URL-safe: Base64는 URL에 안전한 문자만 사용
2. 불투명 토큰: 내부 구조를 숨김 (향후 cursor 필드 변경해도 클라이언트 수정 불필요)
3. 조작 어려움: 사용자가 임의 값 만들기 어려움 (완벽한 보안은 아님)
```

### 실무 선택 가이드

| 상황 | 권장 방식 | 이유 |
|------|----------|------|
| Public API (모바일 앱, 외부 연동) | ✅ **인코딩 필수** | 구조 숨김, 향후 변경 유연성 |
| Internal API (같은 회사 프론트/백) | ⚠️ 선택 가능 | 디버깅 편의성 vs 보안 트레이드오프 |
| 디버깅 중 | ❌ 인코딩 제거 고려 | 로그에서 바로 확인 가능 |

**결론:**
- 이 프로젝트는 **인코딩 사용 권장** (프로덕션 수준 구현)
- 디버깅이 어렵다면 개발환경에서만 인코딩 skip 가능 (환경변수로 분기)

---

## 📱 실제 무한 스크롤 동작 시나리오

### 사용자가 알림 피드를 스크롤하는 상황

```
[초기 상태] DB에 알림 100개 존재

┌─────────────────────────────┐
│  📱 사용자 화면              │
│                             │
│  [ 알림 1 ] createdAt: 10시 │ ← 화면에 보임
│  [ 알림 2 ] createdAt: 09시 │
│  [ 알림 3 ] createdAt: 08시 │
│  ...                        │
│  [ 알림 20 ] createdAt: 01시│ ← 마지막 데이터 (id=20, createdAt=01시)
│                             │
│  [로딩중...] ← 스크롤 끝     │
└─────────────────────────────┘

1️⃣ 첫 요청
GET /api/notifications?size=20

→ 서버: DB에서 21개 조회 (size+1)
→ 응답:
{
  "content": [...20개...],
  "hasNext": true,  // 21개가 조회됨 → 다음 페이지 있음
  "nextCursor": "MjA6MjAyNS0xMi0yM1QwMTowMDowMA==",  // 20번 알림의 cursor
  "size": 20
}


2️⃣ 사용자가 스크롤 다운 (새 알림 2개 추가됨!)
→ 프론트: nextCursor를 그대로 전달

GET /api/notifications?cursor=MjA6MjAyNS0xMi0yM1QwMTowMDowMA==&size=20
                                 ↑
                            디코딩하면: "20:2025-12-23T01:00:00"

→ 서버 쿼리:
SELECT * FROM notifications
WHERE (created_at, id) < ('2025-12-23T01:00:00', 20)  -- 20번 이후 데이터만
ORDER BY created_at DESC, id DESC
LIMIT 21;

→ 결과: 21~40번 알림 조회 (새로 추가된 알림은 무시됨!)
→ 중복 없음, 누락 없음


3️⃣ 마지막 페이지
GET /api/notifications?cursor=...&size=20

→ 서버: DB에서 15개만 조회됨 (size+1인 21개보다 적음)
→ 응답:
{
  "content": [...15개...],
  "hasNext": false,  // 더 이상 없음
  "nextCursor": null,
  "size": 15
}

→ 프론트: "더 이상 알림이 없습니다" 표시
```

---

## 🔄 자바 방식과 Kotlin Cursor 방식 비교

### 자바 전통 방식 (Spring Data JPA)

```java
// Controller
@GetMapping("/notifications")
public Page<NotificationResponse> getNotifications(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<Notification> result = repository.findAll(pageable);

    return result.map(NotificationResponse::from);
}

// 응답 JSON
{
  "content": [...],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalPages": 5,
  "totalElements": 100,
  "last": false
}

// 클라이언트 요청
GET /notifications?page=0&size=20  // 1페이지
GET /notifications?page=1&size=20  // 2페이지
GET /notifications?page=2&size=20  // 3페이지
```

### Kotlin Cursor 방식 (이 프로젝트)

```kotlin
// Controller
@GetMapping("/notifications")
fun getNotifications(
    @RequestParam cursor: String?,
    @RequestParam(defaultValue = "20") size: Int
): ResponseEntity<ApiResponse.Success<CursorPageResponse<NotificationResponse>>> {
    val response = service.getNotifications(cursor, size)
    return ResponseEntity.ok(ApiResponse.Success(data = response, code = SuccessCode.READ))
}

// 응답 JSON
{
  "data": {
    "content": [...],
    "hasNext": true,
    "nextCursor": "MTIzOjIwMjUtMTItMjNUMTA6MzA6MDA=",
    "size": 20
  },
  "code": "S003"
}

// 클라이언트 요청
GET /notifications?size=20  // 첫 요청
GET /notifications?cursor=MTIzOjIwMjUtMTItMjNUMTA6MzA6MDA=&size=20  // 다음 요청
GET /notifications?cursor=다음커서값&size=20  // 계속...
```

### 핵심 차이

| 항목 | 자바 Offset 방식 | Kotlin Cursor 방식 |
|------|-----------------|-------------------|
| 요청 파라미터 | `page=2` (페이지 번호) | `cursor=인코딩값` (마지막 위치) |
| SQL | `OFFSET 40 LIMIT 20` | `WHERE (created_at, id) < (...)` |
| 데이터 중복/누락 | ⚠️ 가능 (실시간 변경 시) | ✅ 없음 |
| 성능 (10000번째 데이터) | ❌ 느림 (10000개 스캔) | ✅ 빠름 (인덱스만) |
| 페이지 점프 | ✅ 가능 (특정 페이지 이동) | ❌ 불가 |
| 총 개수 제공 | ✅ `totalElements: 100` | ❌ 없음 |
| 사용 사례 | 게시판, 관리자 페이지 | 소셜 피드, 무한 스크롤 |

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
✅ **일관성**: 데이터 추가/삭제 시에도 중복/누락 없음.

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
