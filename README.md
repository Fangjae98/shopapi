# SKALA SHOP API

포인트 결제를 지원하는 온라인 쇼핑몰 백엔드 REST API입니다.
계층형 구조(Controller → Service → Repository → DB)로 구현했으며,
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
| Swagger UI (API 문서) | http://localhost:8080/swagger-ui/index.html |
| H2 콘솔 (DB 조회) | http://localhost:8080/h2-console |
| Actuator (상태 점검) | http://localhost:8080/actuator/health |

H2 콘솔 접속 정보 — JDBC URL: `jdbc:h2:mem:skala-shop`, 사용자명: `sa`, 비밀번호: 없음

### 테스트 계정

서버를 켜면 `data.sql`이 초기 상품과 아래 계정을 자동으로 넣어 둡니다.

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

**④ 금액 계산의 내림 일관 적용**

포인트 적립·환급을 모두 `Math.floor`로 내림 처리해, 사용자에게 유리한 반올림이 반복되며
포인트가 조금씩 늘어나는 것을 방지했습니다.

---

## 3. API 목록

### 상품 (`/api/products`)

| Method | URI | 설명 |
|---|---|---|
| GET | `/api/products/list?offset=0&count=10` | 상품 목록 조회 (페이징) |
| GET | `/api/products/{id}` | 상품 상세 조회 |
| POST | `/api/products` | 상품 등록 |
| PUT | `/api/products` | 상품 수정 |
| DELETE | `/api/products` | 상품 삭제 |

### 고객 (`/api/customers`)

| Method | URI | 설명 | 로그인 필요 |
|---|---|---|---|
| GET | `/api/customers/list?offset=0&count=10` | 고객 목록 조회 (페이징) | – |
| GET | `/api/customers/{customerId}` | 고객 상세 + 주문 목록 조회 | – |
| POST | `/api/customers` | 회원가입 | – |
| POST | `/api/customers/login` | 로그인 (세션 생성) | – |
| POST | `/api/customers/logout` | 로그아웃 (세션 폐기) | – |
| PUT | `/api/customers` | 고객 정보 수정 | – |
| DELETE | `/api/customers` | 회원 탈퇴 | – |
| POST | `/api/customers/order` | 상품 주문 | **필요** |
| POST | `/api/customers/cancel` | 주문 취소 | **필요** |

### 호출 예시

```bash
# 1) 회원가입 — 초기 자본과 가입 포인트가 정책에 따라 자동 지급
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"customerId":"skala03","customerPassword":"pw1234"}'

# 2) 로그인 — 이후 요청에 쓸 세션 쿠키를 파일로 저장
curl -X POST http://localhost:8080/api/customers/login \
  -H "Content-Type: application/json" -c cookie.txt \
  -d '{"customerId":"skala01","customerPassword":"pw1234"}'

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
  "body": { "customerId": "skala01", "customerBalance": 998000.0, "customerPoint": 99100.0 }
}
```

| 에러 코드 | HTTP | 발생 조건 |
|---|---|---|
| `DATA_NOT_FOUND` | 404 | 없는 상품·고객 조회 |
| `DATA_DUPLICATED` | 409 | 아이디·상품명 중복 |
| `INSUFFICIENT_FUNDS` | 400 | 현금 잔액 부족 |
| `INSUFFICIENT_POINT` | 400 | 보유 포인트보다 많이 사용 시도 |
| `INSUFFICIENT_QUANTITY` | 400 | 주문 수량보다 많이 취소 시도 |
| `NOT_AUTHENTICATED` | 401 | 미로그인 / 세션 만료 / 비밀번호 불일치 |
| `PARAMETER_EXCEPTION` | 400 | 필수 입력값 누락 |
| `LOCK_TIMEOUT` | 409 | 동시 요청 대기 시간(3초) 초과 |
| `INTERNAL_ERROR` | 500 | 그 외 서버 내부 오류 |

---

## 4. 프로젝트 구조

```
shopapi/
├─ build.gradle                 빌드 스크립트 (플러그인·의존성)
├─ README.md
└─ src/main/
   ├─ java/com/sk/skala/shopapi/
   │  ├─ ShopapiApplication.java        진입점 (@SpringBootApplication)
   │  ├─ controller/                    API 인입 지점
   │  │   ├─ ProductController          상품 CRUD
   │  │   └─ CustomerController         회원·인증·주문·취소
   │  ├─ service/                       비즈니스 로직 (@Transactional)
   │  │   ├─ ProductService
   │  │   └─ CustomerService
   │  ├─ repository/                    JPA 데이터 접근
   │  │   ├─ ProductRepository
   │  │   ├─ CustomerRepository         비관적 락 조회 포함
   │  │   └─ OrderItemRepository
   │  ├─ data/table/                    JPA 엔터티
   │  │   └─ Product · Customer · OrderItem
   │  ├─ data/dto/                      요청·응답 전용 객체
   │  │   ├─ CustomerSessionDto         가입·로그인 요청
   │  │   ├─ OrderRequestDto            주문·취소 요청
   │  │   └─ CustomerDto · OrderItemDto · OrderListDto
   │  ├─ common/                        전 계층 공통
   │  │   ├─ Response · PagedList       표준 응답 포맷
   │  │   ├─ SessionHandler             서버 세션 기반 로그인 상태 관리
   │  │   └─ ShopPolicy                 yml 정책값 바인딩
   │  ├─ exception/                     예외 처리
   │  │   ├─ Error                      에러 코드 정의
   │  │   ├─ ResponseException · ParameterException
   │  │   └─ GlobalExceptionHandler     전역 예외 처리
   │  ├─ aop/ApiLoggingAspect           API 호출·응답시간 로깅
   │  └─ tools/StringUtil               입력값 공백 검사
   └─ resources/
      ├─ application.yml                DB·세션·정책·로그 설정
      ├─ data.sql                       초기 상품·고객 데이터
      └─ static/index.html              동작 확인용 화면
```

### 파일별 역할

| 계층 | 파일 | 역할 |
|---|---|---|
| 진입점 | `ShopapiApplication` | 내장 톰캣 기동 및 하위 패키지 컴포넌트 스캔 |
| controller | `ProductController` | 상품 CRUD 5개 API를 서비스에 위임 |
| | `CustomerController` | 회원·인증·주문·취소 9개 API를 서비스에 위임 |
| service | `ProductService` | 상품 CRUD, 이름 중복·가격 유효성 검증 |
| | `CustomerService` | 포인트·현금 결제 계산, 적립, 부분 취소 정산, 트랜잭션·락 적용 |
| repository | `ProductRepository` | 상품 조회, 이름 중복 검사 |
| | `CustomerRepository` | 일반 조회와 비관적 락 조회를 분리 제공 |
| | `OrderItemRepository` | 고객별 주문 목록 및 특정 주문 건 조회 |
| data/table | `Product` | 상품 엔터티 (상품명·가격) |
| | `Customer` | 고객 엔터티 (현금 잔액·포인트 분리 보유) |
| | `OrderItem` | 고객↔상품 1:N 매핑, 결제 내역 누적 기록 |
| data/dto | `CustomerSessionDto` | 가입·로그인 요청 (아이디·비밀번호) |
| | `OrderRequestDto` | 주문·취소 요청 |
| | `CustomerDto` | 비밀번호를 제외한 고객 응답 |
| | `OrderItemDto` · `OrderListDto` | 주문 상품 및 주문 목록 응답 |
| common | `Response` · `PagedList` | 표준 응답 및 페이징 결과 포장 |
| | `SessionHandler` | 로그인 상태 관리, 인증 구현 격리 |
| | `ShopPolicy` | yml 정책값을 자바 객체로 주입 |
| exception | `Error` | 에러 코드 9종 (HTTP 상태 + 기본 메시지) |
| | `ResponseException` · `ParameterException` | 비즈니스 위반 / 입력 검증 실패 예외 |
| | `GlobalExceptionHandler` | 모든 예외를 표준 응답으로 변환 |
| aop | `ApiLoggingAspect` | 전체 컨트롤러의 요청·응답·예외 자동 기록 |
| tools | `StringUtil` | 여러 문자열의 공백 여부를 한 번에 검사 |

---

## 5. 주요 기술 적용 내역

### 동시성 제어 — 비관적 락

주문·취소는 고객의 잔액·포인트를 갱신하므로, 같은 고객의 요청이 동시에 들어오면
(더블클릭, 중복 탭 등) 두 요청이 같은 값을 읽고 각자 반영해 잔액이 어긋날 수 있습니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Customer> findWithLockByCustomerId(String customerId);
```

`select ... for update`로 해당 고객 행을 잠가 순차 처리되도록 했습니다.
대기 한도는 3초(`LOCK_TIMEOUT=3000`)이며 초과 시 409로 응답합니다.
다른 고객의 요청은 서로 다른 행이므로 영향을 받지 않으며,
단순 조회에는 잠금을 걸지 않도록 조회 메서드를 분리했습니다.

낙관적 락은 충돌 시 요청이 실패해 사용자가 재시도해야 하지만,
금액이 걸린 거래에서는 대기 후 확실히 처리되는 편이 낫다고 판단해 비관적 락을 택했습니다.

### 트랜잭션

주문·취소는 잔액 차감, 포인트 증감, 주문 내역 갱신이 **모두 성공하거나 모두 실패해야** 하므로
`@Transactional`로 묶었습니다. 중간에 예외가 나면 앞선 변경까지 함께 롤백됩니다.

### 인증 — 서버 세션

로그인 성공 시 서버 세션에 고객 ID를 보관하고, 브라우저는 세션 식별자만 쿠키로 전달받습니다.
인증 정보 자체가 클라이언트로 나가지 않으며, 로그아웃 시 `invalidate()`로 즉시 폐기됩니다.

| 설정 | 목적 |
|---|---|
| `http-only: true` | 자바스크립트 접근 차단 (XSS 방어) |
| `same-site: lax` | 외부 사이트발 요청에 쿠키 미첨부 (CSRF 방어) |
| `tracking-modes: cookie` | URL에 세션 ID가 노출되는 경로 차단 |
| `timeout: 30m` | 미활동 시 자동 만료 |
| `changeSessionId()` | 로그인 시 세션 ID 재발급 (세션 고정 공격 방어) |

인증 로직은 `SessionHandler` 한 곳에 격리되어 있어, 개발 중 JWT에서 세션 방식으로
전환할 때 이 파일 외에는 수정이 필요하지 않았습니다.

### AOP — API 호출 로깅

포인트컷 한 줄로 전체 컨트롤러에 일괄 적용되어 비즈니스 로직에는 로깅 코드가 없으며,
컨트롤러가 추가되어도 자동으로 로깅됩니다.
요청 파라미터·처리 시간·예외 종류를 기록하되, 잡은 예외는 그대로 다시 던져
전역 예외 처리 흐름을 방해하지 않습니다.
비밀번호는 `@ToString(exclude)`로 제외해 로그에 남지 않으며,
로그는 파일로 저장해 일 단위로 분리·7일간 보관합니다.

```
[API REQUEST ] CustomerController.loginCustomer(..) | Params: [CustomerSessionDto(customerId=skala01)]
[API RESPONSE] CustomerController.loginCustomer(..) | Duration: 152ms
```

### 설정 외부화

초기 자본, 가입 포인트율, 적립률은 코드가 아닌 `application.yml`에 있으며
`ShopPolicy`(`@ConfigurationProperties`)로 주입됩니다.
**정책 변경 시 재컴파일 없이 yml 한 줄만 수정하면 됩니다.**

### 예외 처리

`GlobalExceptionHandler`(`@RestControllerAdvice`)가 모든 예외를 한곳에서 잡아
동일한 `Response` 형식으로 변환합니다. 서비스 코드에는 try-catch가 없습니다.

### 요청 값 비신뢰 원칙

회원가입 시 클라이언트가 보낸 객체를 그대로 저장하지 않고 필요한 필드만 뽑아
새 객체를 만들며, 잔액·포인트는 클라이언트 값이 아닌 서버 정책에서 계산한 값을 사용합니다.

### 응답 시 DTO 사용

`CustomerDto`는 아이디·잔액·포인트만 담아 비밀번호가 응답에 실려 나가지 않도록 했습니다.

### 지연 로딩

`OrderItem`이 참조하는 `Customer`·`Product`를 `FetchType.LAZY`로 두어,
목록 조회 시 불필요한 조인을 피합니다.

---

## 6. 알려진 제약

학습용 프로젝트로, 실서비스 적용 시 아래 보완이 필요합니다.

- **비밀번호 평문 저장** — BCrypt 등 단방향 해시 적용 필요
- **관리자 권한 분리 없음** — 상품 CRUD, 고객 목록 조회에 권한 검증 미적용
- **고객 정보 수정·삭제 API에 본인 확인 미적용** — 현재 `PUT`/`DELETE /api/customers`는
  로그인 여부와 요청자 일치 여부를 검증하지 않는다. 잔액·포인트 역시 수정 대상에 포함되어 있어,
  운영 환경에서는 비밀번호만 변경 가능하도록 제한하고 세션의 로그인 ID와 대조하는 처리가 필요하다
- **금액 자료형이 `Double`** — 부동소수점 오차 가능성이 있어 `BigDecimal` 또는 정수(원 단위)가 적합.
  현재는 `Math.floor` 일관 적용으로 절충
- **인메모리 DB** — 재시작 시 데이터 소실, 운영 시 PostgreSQL 등으로 교체
- **서버 세션 방식** — 서버를 여러 대로 확장하면 세션 공유(Redis 등)가 필요.
  단일 서버 규모에서는 구현 단순성의 이점이 큼
- **재고 개념 없음** — 도입 시 여러 고객이 동일 상품을 두고 경쟁하므로 상품 행에도 별도 잠금 필요
- **API 로그가 파일 기반** — 조회·집계에 별도 도구가 필요하다. DB 테이블로 적재하면
  평균 응답시간·에러율 통계가 가능하며, 이때 로그 저장은 `Propagation.REQUIRES_NEW`로 분리해
  비즈니스 트랜잭션이 롤백되어도 시도 기록이 남도록 해야 한다