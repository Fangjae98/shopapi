# SKALA SHOP API

포인트 결제를 지원하는 온라인 쇼핑몰 백엔드 REST API입니다.
Spring Boot 기반의 계층형 구조(Controller → Service → Repository → DB)로 구현했으며,
동작 확인용 정적 프론트엔드 화면을 함께 제공합니다.

---

## 1. 실행 방법

### 요구 환경

| 항목 | 버전 |
|---|---|
| JDK | 21 |
| Spring Boot | 4.1.0 |
| 빌드 도구 | Gradle (Wrapper 포함, 별도 설치 불필요) |
| DB | H2 (인메모리, 별도 설치 불필요) |

### 실행

```bash
# macOS / Linux
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

### 접속 주소

| 대상 | 주소 |
|---|---|
| 쇼핑몰 화면 | http://localhost:8080 |
| Swagger UI (API 문서) | http://localhost:8080/swagger-ui.html |
| H2 콘솔 (DB 조회) | http://localhost:8080/h2-console |
| Actuator (상태 점검) | http://localhost:8080/actuator/health |

H2 콘솔 접속 정보 — JDBC URL: `jdbc:h2:mem:skala-shop`, 사용자명: `sa`, 비밀번호: 없음

### 테스트 계정

서버를 켜면 `data.sql`이 아래 계정과 상품 15건을 자동으로 넣어 둡니다.

| 아이디 | 비밀번호 | 보유 현금 | 보유 포인트 |
|---|---|---|---|
| `skala01` | `pw1234` | 1,000,000원 | 100,000P |
| `skala02` | `pw1234` | 500,000원 | 50,000P |

인메모리 DB이므로 **서버를 재시작하면 데이터가 초기 상태로 돌아갑니다.**

---

## 2. 차별화 포인트 — 포인트 결제 제도

기본 과제는 "포인트가 부족하면 주문 거부"라는 단일 화폐 구조입니다.
이 프로젝트는 **현금(잔액)과 포인트를 분리한 이중 화폐 구조**로 재설계했습니다.

### 정책

| 시점 | 규칙 |
|---|---|
| 회원가입 | 초기 자본 지급 + **자본의 10%를 포인트로 추가 지급** |
| 주문 | 포인트 사용 여부와 사용액을 **고객이 직접 선택**, 나머지는 현금 결제 |
| 적립 | **현금 결제분의 5%** 를 포인트로 적립 |
| 취소 | 결제 구성 비율대로 현금·포인트를 환급하고, 적립분은 회수 |

### 설계 시 고려한 지점

**① 포인트 무한 증식 차단**
적립을 총 주문금액이 아닌 **현금 결제분 기준**으로만 계산합니다.
총액 기준으로 적립하면 "포인트로 결제 → 다시 적립 → 또 결제"가 반복되며 포인트가 무한히 늘어납니다.

**② 부분 취소의 비율 정산**
20개를 주문하고 5개만 취소하는 경우, 결제 당시의 포인트·현금 사용 비율을 그대로 적용해 환급합니다.
이를 위해 `OrderItem`이 수량뿐 아니라 `pointUsed` / `cashUsed` / `pointEarned`를 함께 누적합니다.

**③ 적립 포인트 선사용 후 취소 어뷰징 차단**
적립받은 포인트를 이미 다 써버린 뒤 주문을 취소하면 회수할 포인트가 모자랍니다.
이때 부족분을 **현금 환급액에서 차감**해 총 자산이 부당하게 늘어나지 않도록 처리했습니다.

**④ 금액 변경 경로의 단일화**
잔액과 포인트는 **주문·취소 API를 통해서만** 변경됩니다.
`PUT /api/customers`는 비밀번호만 바꿀 수 있으며, 금액을 직접 덮어쓰는 경로는 열어두지 않았습니다.

---

## 3. API 목록

### 상품 (`/api/products`)

| Method | URI | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/products/list?offset=0&count=10` | 상품 목록 조회 (페이징) | – |
| GET | `/api/products/{id}` | 상품 상세 조회 | – |
| POST | `/api/products` | 상품 등록 | – |
| PUT | `/api/products` | 상품 수정 | – |
| DELETE | `/api/products` | 상품 삭제 | – |

### 고객 (`/api/customers`)

| Method | URI | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/customers/list?offset=0&count=10` | 고객 목록 조회 (페이징) | – |
| GET | `/api/customers/{customerId}` | 고객 상세 + 주문 목록 조회 | – |
| POST | `/api/customers` | 회원가입 | – |
| POST | `/api/customers/login` | 로그인 | – |
| POST | `/api/customers/logout` | 로그아웃 (쿠키 즉시 만료) | – |
| PUT | `/api/customers` | 비밀번호 변경 | 필요 |
| DELETE | `/api/customers` | 회원 탈퇴 | 필요 |
| POST | `/api/customers/order` | 상품 주문 | 필요 |
| POST | `/api/customers/cancel` | 주문 취소 | 필요 |

### 정책 (`/api/shop`)

| Method | URI | 설명 |
|---|---|---|
| GET | `/api/shop/policy` | 초기 자본·가입 포인트율·적립률 조회 |

### 호출 예시

```bash
# 1) 회원가입 — 초기 자본 1,000,000원 + 포인트 100,000P 지급
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"customerId":"skala03","customerPassword":"pw1234"}'

# 2) 로그인 — 이후 요청에 쓸 쿠키를 파일로 저장
curl -X POST http://localhost:8080/api/customers/login \
  -H "Content-Type: application/json" -c cookie.txt \
  -d '{"customerId":"skala03","customerPassword":"pw1234"}'

# 3) 주문 — 1번 상품 2개, 포인트 1,000P 사용
curl -X POST http://localhost:8080/api/customers/order \
  -H "Content-Type: application/json" -b cookie.txt \
  -d '{"productId":1,"quantity":2,"usePoint":true,"pointToUse":1000}'

# 4) 주문 취소 — 1개만 부분 취소
curl -X POST http://localhost:8080/api/customers/cancel \
  -H "Content-Type: application/json" -b cookie.txt \
  -d '{"productId":1,"quantity":1}'
```

### 공통 응답 형식

성공과 실패 모두 동일한 형태로 내려갑니다.

```json
{
  "code": 200,
  "message": "주문 완료 — 포인트 1,000P + 현금 2,000원 결제, 100P 적립",
  "body": { "customerId": "skala03", "customerBalance": 998000.0, "customerPoint": 99100.0 }
}
```

| 에러 코드 | HTTP | 발생 조건 |
|---|---|---|
| `DATA_NOT_FOUND` | 404 | 없는 상품·고객 조회 |
| `DATA_DUPLICATED` | 409 | 아이디·상품명 중복 |
| `INSUFFICIENT_FUNDS` | 400 | 현금 잔액 부족 |
| `INSUFFICIENT_POINT` | 400 | 보유 포인트보다 많이 사용 시도 |
| `INSUFFICIENT_QUANTITY` | 400 | 주문 수량보다 많이 취소 시도 |
| `NOT_AUTHENTICATED` | 401 | 미로그인 / 토큰 만료 / 타인 정보 변경 시도 |
| `LOCK_TIMEOUT` | 409 | 동시 주문 대기 시간(3초) 초과 |

---

## 4. 프로젝트 구조

```
shopapi/
├─ build.gradle                 빌드 스크립트 (플러그인·의존성)
├─ README.md
└─ src/main/
   ├─ java/com/sk/skala/shopapi/
   │  ├─ ShopapiApplication.java        시작점 (@SpringBootApplication)
   │  ├─ controller/                    API 인입 지점
   │  │   ├─ ProductController          상품 CRUD
   │  │   ├─ CustomerController         회원·인증·주문·취소
   │  │   └─ ShopPolicyController       정책값 조회
   │  ├─ service/                       비즈니스 로직 (@Transactional)
   │  │   ├─ ProductService
   │  │   └─ CustomerService
   │  ├─ repository/                    JPA 데이터 접근
   │  │   ├─ ProductRepository
   │  │   ├─ CustomerRepository         비관적 락 조회 포함
   │  │   └─ OrderItemRepository
   │  ├─ data/table/                    JPA 엔터티
   │  │   ├─ Product · Customer · OrderItem
   │  ├─ data/dto/                      요청·응답 전용 객체
   │  │   ├─ OrderRequest · CustomerSession
   │  │   └─ CustomerDto · OrderItemDto · OrderListDto
   │  ├─ common/                        전 계층 공통
   │  │   ├─ Response · PagedList       표준 응답 포맷
   │  │   ├─ SessionHandler             JWT 쿠키 발급·검증
   │  │   └─ ShopPolicy                 yml 정책값 바인딩
   │  ├─ exception/                     예외 처리
   │  │   ├─ Error                      에러 코드 정의
   │  │   ├─ ResponseException · ParameterException
   │  │   └─ GlobalExceptionHandler     전역 예외 처리
   │  ├─ aop/ApiLoggingAspect           API 호출·응답시간 로깅
   │  └─ tools/                         JwtTool · StringUtil
   └─ resources/
      ├─ application.yml                DB·세션·정책 설정
      ├─ data.sql                       초기 상품·고객 데이터
      └─ static/index.html              동작 확인용 화면
```

---

## 5. 주요 기술 적용 내역

### 동시성 제어 — 비관적 락

같은 고객이 동시에 주문을 넣으면 잔액이 잘못 계산될 수 있습니다.
`CustomerRepository`에 잠금 전용 조회 메서드를 두고 **주문·취소에서만** 사용합니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Customer> findWithLockByCustomerId(String customerId);
```

`select ... for update` SQL이 실행되어, 트랜잭션이 끝날 때까지 해당 고객 행에 대한
다른 주문은 대기합니다. 대기 한도는 `application.yml`의 `LOCK_TIMEOUT=3000`(3초)이며,
초과 시 `LOCK_TIMEOUT`(409)으로 응답합니다.

낙관적 락(`@Version`)은 충돌 시 요청이 실패하고 사용자가 재시도해야 하지만,
금액이 걸린 거래에서는 **대기 후 확실히 처리**되는 편이 사용자 경험상 낫다고 판단해 비관적 락을 택했습니다.

### 트랜잭션

주문·취소는 잔액 차감, 포인트 증감, 주문 내역 갱신이 **모두 성공하거나 모두 실패해야** 하므로
`@Transactional`로 묶었습니다. 중간에 예외가 나면 앞선 변경까지 함께 롤백됩니다.

### 인증 — 서버 세션(HttpSession)

로그인 성공 시 서버 세션에 고객 ID를 보관하고, 브라우저는 세션 번호표(JSESSIONID 쿠키)만
전달받습니다. 인증 정보 자체가 클라이언트로 나가지 않으므로 토큰 위·변조 위험이 없고,
로그아웃은 `session.invalidate()`로 서버 측 상태를 즉시 폐기합니다.

### AOP — API 호출 로깅

`ApiLoggingAspect`가 모든 컨트롤러 메서드의 요청 파라미터, 응답 시간, 예외 종류를 기록합니다.
컨트롤러 코드에 로깅 코드를 넣지 않고도 전체 API를 일괄 관측할 수 있습니다.

### 설정 외부화

초기 자본, 가입 포인트율, 적립률은 코드가 아닌 `application.yml`에 있으며
`ShopPolicy`(`@ConfigurationProperties`)로 주입됩니다.
화면도 `/api/shop/policy`로 같은 값을 받아 쓰므로, **정책 변경 시 yml 한 줄만 고치면 됩니다.**

### 예외 처리

`GlobalExceptionHandler`(`@RestControllerAdvice`)가 모든 예외를 한곳에서 잡아
동일한 `Response` 형식으로 변환합니다. 서비스 코드에는 try-catch가 없습니다.

---

## 6. 알려진 제약

학습용 프로젝트로, 실서비스 적용 시 아래 보완이 필요합니다.

- **비밀번호 평문 저장** — 실제로는 BCrypt 등 단방향 해시 적용 필요
- **관리자 권한 분리 없음** — 상품 CRUD와 고객 목록 조회에 권한 검증 미적용
- **인메모리 DB** — 재시작 시 데이터 소실. 운영 시 PostgreSQL 등으로 교체
- **금액 자료형이 `Double`** — 원 단위 반올림 오차 가능성이 있어 `BigDecimal` 또는 `Long`이 적합
