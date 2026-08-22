# Schedule Manager

학원/교육 운영 과정에서 발생하는 일정과 참여자를 관리하기 위한 협업 일정 관리 REST API 프로젝트입니다.

기존에는 엑셀이나 카카오톡 등으로 일정을 관리하면서 일정 변경, 참여자 확인, 진행 상태를 한곳에서 관리하기 어려웠습니다.  
과거 학원 웹사이트 개발 업무에서 비슷한 기능을 구현한 경험이 있었고, 당시 부족했던 부분을 보완하면서 Spring Boot와 JPA를 적용해 다시 만들어보고자 시작했습니다.

---

## 1. 주요 기능

- 회원가입 / 로그인 / 로그아웃
- Redis Session 기반 인증
- 일정 등록 / 조회 / 수정 / 삭제
- 일정 상태 변경 및 변경 이력 관리
- 일정 참여자 초대
- 초대 목록 조회
- 초대 수락 / 거절
- 작성자와 참여자 관계에 따른 접근 권한 제어
- Cursor 기반 일정 목록 조회
- 공통 ErrorResponse 및 traceId 기반 오류 추적
- Swagger / OpenAPI 문서화
- GitHub Actions 기반 테스트 자동 실행

---

## 2. 기술 스택

| 기술 | 적용 내용 |
| :--- | :--- |
| Java 21 | 애플리케이션 개발 |
| Spring Boot 4.0.6 | REST API 및 애플리케이션 구성 |
| Spring Security | 로그인 인증 및 접근 권한 처리 |
| Spring Data JPA | 엔티티 매핑 및 데이터 접근 |
| MariaDB 11.4 | 관계형 데이터 저장 |
| Redis 7.4 | HTTP Session 저장소 |
| Spring Session | Redis 기반 세션 관리 |
| Bean Validation | 요청 데이터 검증 |
| Swagger / OpenAPI | API 문서화 |
| JUnit / MockMvc | Service 및 API 통합 테스트 |
| Docker | 애플리케이션 실행 환경 구성 |
| GitHub Actions | Java 21 환경 테스트 자동화 |


## 3. 실행 환경

- Java 21
- Spring Boot 4.0.6
- MariaDB 11.4
- Redis 7.4
- Docker

---

## 4. 환경 변수

| 환경 변수 | 설명 | 기본값 |
| :--- | :--- | :--- |
| DB_HOST | MariaDB 호스트 | localhost |
| DB_PORT | MariaDB 포트 | 3306 |
| DB_NAME | 데이터베이스 이름 | schedule_manager |
| DB_USERNAME | DB 사용자 | root |
| DB_PASSWORD | DB 비밀번호 | - |
| REDIS_HOST | Redis 호스트 | localhost |
| REDIS_PORT | Redis 포트 | 6379 |
| PORT | 애플리케이션 포트 | 18080 |
| CSRF_ENABLED | CSRF 활성화 여부 | true |
| COOKIE_SECURE | Secure Cookie 활성화 여부 | true |

DB 비밀번호와 환경별 접속 정보는 저장소에 포함하지 않고 환경 변수로 전달합니다.

---

## 5. 로컬 실행

개발 환경에서는 `local` 프로필을 사용합니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

`local` 프로필에서는 로컬 HTTP 환경에서 API를 테스트할 수 있도록 다음 설정을 적용합니다.

- CSRF 비활성화
- Secure Cookie 비활성화
- `ddl-auto: update`
- SQL 출력
- Spring Security / Hibernate SQL DEBUG 로그

기본 프로필은 CSRF와 Secure Session Cookie가 활성화되어 있습니다.

---

## 6. Docker 실행

### 6.1 JAR 생성

프로젝트 루트에서 실행합니다.

```bash
./mvnw package
```

테스트가 모두 통과하면 `target/` 디렉터리에 실행 가능한 JAR가 생성됩니다.

### 6.2 Docker 이미지 생성

```bash
docker build -t schedule-manager .
```

Docker 이미지에는 DB 비밀번호 등의 환경별 비밀값을 포함하지 않습니다.

### 6.3 `.env` 작성

프로젝트 루트에 `.env` 파일을 생성합니다.

```env
DB_HOST=host.docker.internal
DB_PORT=3306
DB_NAME=schedule_manager
DB_USERNAME=root
DB_PASSWORD=YOUR_PASSWORD

REDIS_HOST=host.docker.internal
REDIS_PORT=6379

CSRF_ENABLED=false
COOKIE_SECURE=false
```

`.env`는 `.gitignore`와 `.dockerignore`에 등록하여 Git 저장소와 Docker 빌드 컨텍스트에서 제외합니다.

Mac/Windows Docker Desktop에서 호스트 머신에 실행 중인 MariaDB와 Redis에 접근하기 위해 `host.docker.internal`을 사용합니다.

### 6.4 컨테이너 실행

로컬 Docker 실행에서는 `local` 프로필을 사용합니다.

`local` 프로필에서는 `ddl-auto: update`가 적용되어 빈 MariaDB에서도 개발용 테이블이 생성됩니다.

```bash
docker run \
  --env-file .env \
  -e SPRING_PROFILES_ACTIVE=local \
  -p 18080:18080 \
  schedule-manager
```

정상 실행되면 애플리케이션은 다음 주소에서 접근할 수 있습니다.

```text
http://localhost:18080
```

Swagger UI:

```text
http://localhost:18080/swagger-ui/index.html
```

### 6.5 Redis Session 확인

Docker 환경에서 회원가입 및 로그인 후 Redis에 다음 형식의 Session Key가 생성되는 것을 확인했습니다.

```text
schedule-manager:session:sessions:{sessionId}
```

확인 예시:

```text
schedule-manager:session:sessions:e24180da-...
```

Docker 컨테이너에서 실행된 Spring Boot 애플리케이션이 호스트의 MariaDB와 Redis에 연결하고, 로그인 세션을 Redis에 저장하는 것까지 확인했습니다.

---

## 7. 인증 방식

### Redis Session을 선택한 이유

#### Context

운영 관리형 서비스에서는 사용자 차단, 강제 로그아웃, 권한 변경 후 로그인 상태를 서버에서 통제할 필요가 있다고 판단했습니다.

JWT도 고려했지만 Access Token 방식은 발급된 토큰을 만료 전에 즉시 무효화하려면 별도의 관리 방식이 필요합니다.

#### Decision

Redis Session 기반 인증을 사용합니다.

```text
Client
   ↓
Session Cookie
   ↓
Spring Security
   ↓
Spring Session
   ↓
Redis
```

#### Consequences

- 서버에서 로그인 세션을 관리할 수 있습니다.
- 애플리케이션 서버와 세션 저장소를 분리할 수 있습니다.
- Redis 장애가 인증에 영향을 줄 수 있으므로 Redis 운영이 필요합니다.
- 마이크로서비스 또는 외부 API 중심 구조에서는 다른 인증 방식이 더 적합할 수 있습니다.

현재 사용자 차단 또는 권한 변경 시 해당 사용자의 기존 세션을 즉시 찾아 삭제하는 기능까지는 구현하지 않았습니다.

Redis Session을 사용한다는 것만으로 강제 로그아웃이 자동으로 해결되는 것은 아니며, 사용자별 세션 조회 및 무효화 정책이 추가로 필요합니다.

---

## 8. 도메인 구조

### users

로그인 사용자 정보를 관리합니다.

- `role`: USER / ADMIN
- `status`: ACTIVE / BLOCKED

차단된 사용자의 로그인이나 주요 요청을 제한할 수 있도록 구성합니다.

### schedules

일정의 핵심 정보를 관리합니다.

- 작성자
- 제목
- 설명
- 예정 시간
- 일정 상태
- version

`version`은 JPA 낙관적 락을 이용한 동시 수정 충돌 감지에 사용합니다.

### schedule_participants

User와 Schedule의 다대다 관계를 중간 엔티티로 분리했습니다.

`@ManyToMany`를 직접 사용하지 않고 별도의 엔티티를 두어 참여 상태와 같은 관계 자체의 정보를 관리할 수 있도록 했습니다.

현재 참여 상태:

```text
PENDING
ACCEPTED
REJECTED
```

### schedule_status_history

일정의 상태 변경 이력을 저장합니다.

누가 어떤 상태로 변경했는지 기록하여 일정 변경 흐름을 확인할 수 있도록 구성했습니다.

### confirm_requests

향후 구현 예정입니다.

참여자가 업무 완료 후 담당자에게 승인을 요청하고 피드백을 받을 수 있는 워크플로우를 구성할 예정입니다.

일정 자체의 상태와 승인 요청 상태를 분리하여 수정 및 재요청 이력을 관리하는 구조를 계획하고 있습니다.

---

## 9. 일정 상태 전이

현재 허용하는 상태 전이는 다음과 같습니다.

```text
PLANNED
 ├─→ IN_PROGRESS
 └─→ CANCELED

IN_PROGRESS
 ├─→ DONE
 └─→ CANCELED

DONE
 └─→ 변경 불가

CANCELED
 └─→ 변경 불가
```

잘못된 상태 전이나 요청한 `version`이 현재 리소스와 충돌하면 `409 Conflict`를 반환합니다.

---

## 10. 권한 정책

사용자와 일정의 관계를 다음과 같이 구분합니다.

```text
OWNER
PENDING
ACCEPTED
REJECTED
NONE
```

권한 판정은 `ScheduleRelationResolver`와 `ScheduleAccessPolicy`로 분리했습니다.

### 일정 접근

| 관계 | 일정 조회 | 상태 이력 조회 | 참여자 목록 |
| :--- | :---: | :---: | :--- |
| OWNER | O | O | 전체 |
| ACCEPTED | O | O | ACCEPTED만 |
| PENDING | X | X | X |
| REJECTED | X | X | X |
| NONE | X | X | X |

일정 수정, 삭제, 상태 변경 및 참여자 관리는 작성자만 수행할 수 있습니다.

---

## 11. 초대 생명주기 정책

일정 상태와 초대 처리가 서로 끊기지 않도록 초대 생성, 조회, 수락, 거절에 동일한 일정 생명주기 기준을 적용했습니다.

| 일정 상태 | 신규 초대 | PENDING 초대 조회 | 수락 | 거절 |
| :--- | :---: | :---: | :---: | :---: |
| PLANNED | O | O | O | O |
| IN_PROGRESS | O | O | O | O |
| DONE | X | X | X | X |
| CANCELED | X | X | X | X |

`IN_PROGRESS` 상태에서도 새로운 참여자가 일정에 합류할 수 있도록 허용합니다.

반면 이미 종료되거나 취소된 일정에는 새로운 참여자를 추가하지 않습니다.

API 통합 테스트에서 `IN_PROGRESS` 일정의 신규 초대 생성, PENDING 초대 목록 조회, 초대 수락 및 거절 동작을 각각 확인했습니다.

`DONE`, `CANCELED` 일정에서는 신규 초대 생성과 기존 PENDING 초대의 조회, 수락, 거절이 제한되는 것을 확인했습니다.

---

## 12. 현재 구현 API

| 메서드 | URL | 설명 | 요청 | 응답 | 인증/권한 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| POST | `/api/auth/signup` | 회원가입 | `{email,password,nickname}` | `201 + UserResponse` | X |
| POST | `/api/auth/login` | 로그인 | `{email,password}` | `200 + UserResponse` | X |
| POST | `/api/auth/logout` | 로그아웃 | - | `204` | 로그인 |
| GET | `/api/users/me` | 내 정보 조회 | - | `200 + UserResponse` | 로그인 |
| POST | `/api/schedules` | 일정 등록 | `{title,description,scheduledAt}` | `201` | 로그인 |
| GET | `/api/schedules` | 작성·참여 일정 Cursor 조회 | 검색 조건 | `200 + CursorResponse` | 로그인 |
| GET | `/api/schedules/{id}` | 일정 단건 조회 | - | `200 + ScheduleResponse` | OWNER / ACCEPTED |
| PATCH | `/api/schedules/{id}` | 일정 부분 수정 | `{title?,description?,scheduledAt?,version}` | `200 + ScheduleResponse` | OWNER |
| DELETE | `/api/schedules/{id}` | 예정 일정 삭제 | - | `204` | OWNER |
| PATCH | `/api/schedules/{id}/status` | 일정 상태 변경 | `{toStatus,version}` | `200 + ScheduleResponse` | OWNER |
| GET | `/api/schedules/{id}/history` | 상태 변경 이력 | - | `200 + List` | OWNER / ACCEPTED |
| POST | `/api/schedules/{id}/participants` | 참여자 초대 | `{userId}` | `201` | OWNER |
| GET | `/api/schedules/{id}/participants` | 참여자 목록 | - | `200 + List` | OWNER / ACCEPTED |
| DELETE | `/api/schedules/{id}/participants/{userId}` | 참여자 제거 | - | `204` | OWNER |
| GET | `/api/schedules/invitations` | 내 PENDING 초대 목록 | - | `200 + List` | 로그인 |
| PATCH | `/api/schedules/{id}/participants/me/accept` | 초대 수락 | - | `204` | 초대받은 사용자 |
| PATCH | `/api/schedules/{id}/participants/me/reject` | 초대 거절 | - | `204` | 초대받은 사용자 |

---

## 13. 공통 오류 응답

입력 검증 실패 시 공통 오류 코드와 필드별 메시지를 반환합니다.

비밀번호 같은 민감값이 응답에 다시 노출되지 않도록 `rejectedValue`는 포함하지 않습니다.

```json
{
  "status": 400,
  "code": "E_400_001",
  "message": "입력값이 올바르지 않습니다.",
  "traceId": "요청별-trace-id",
  "fieldErrors": [
    {
      "field": "password",
      "message": "크기가 8에서 100 사이여야 합니다"
    }
  ]
}
```

주요 HTTP 상태 코드는 다음 기준으로 사용합니다.

```text
200 OK          → 조회/수정 성공
201 Created     → 리소스 생성 성공
204 No Content  → 처리 성공, 응답 Body 없음
400 Bad Request → 입력값 오류
401 Unauthorized→ 인증 필요
403 Forbidden   → 접근 권한 없음
404 Not Found   → 리소스 없음
409 Conflict    → 상태 전이 또는 현재 리소스 상태와 충돌
```

ErrorResponse에는 `traceId`를 포함하여 클라이언트의 오류 응답과 서버 로그를 연결할 수 있도록 구성했습니다.

---

## 14. 테스트 및 CI

다음 영역을 테스트하고 있습니다.

- 회원가입 / 로그인
- 입력값 검증
- 일정 CRUD
- 일정 상태 전이
- 낙관적 락 충돌
- 일정 접근 권한
- 참여자 공개 범위
- 초대 생성 / 조회 / 수락 / 거절
- 일정 상태별 초대 생명주기
- Cursor 페이징
- Repository 조회
- N+1 조회 문제

GGitHub Actions에서는 Java 21 환경에서 `./mvnw test`를 실행합니다.

현재 CI가 자동으로 확인하는 범위는 테스트 통과 여부까지입니다.

다음 항목은 로컬 환경에서 직접 확인했습니다.

- `./mvnw package` 실행 및 실행 JAR 생성
- Docker 이미지 빌드
- Docker 컨테이너 실행
- MariaDB 연결
- Redis Session 생성

현재 CI에는 JAR 패키징과 Docker 이미지 빌드 단계가 포함되어 있지 않습니다.

또한 `main` 브랜치에 필수 상태 검사를 설정하지 않아, 현재 테스트 실패가 병합이나 직접 push를 자동으로 차단하지는 않습니다.

자동 배포 파이프라인은 아직 구성하지 않았습니다.

---

## 15. 트러블슈팅

### 15.1 일정 목록의 안정적인 Cursor 페이징

#### 문제

`scheduledAt`만 Cursor로 사용하면 같은 시각의 일정이 여러 건 존재할 때 다음 페이지에서 데이터가 누락되거나 중복될 수 있었습니다.

#### 원인

`scheduledAt` 하나만으로는 전체 데이터의 정렬 순서가 결정되지 않았습니다.

#### 해결

```text
scheduledAt DESC
id DESC
```

복합 정렬을 사용했습니다.

다음 페이지 조회 조건 역시 `scheduledAt + id`를 함께 사용하도록 맞췄습니다.

또한 요청한 `size`보다 한 건을 더 조회하여 별도의 COUNT 쿼리 없이 `hasNext`를 판단하도록 구성했습니다.

#### 검증

같은 `scheduledAt`을 가진 일정과 작성·참여 일정을 섞은 통합 테스트에서 페이지 사이에 누락과 중복이 없는지 확인했습니다.

---

### 15.2 참여자 목록 N+1

#### 문제

참여자 3명의 nickname을 응답 DTO에 포함하는 과정에서 SQL이 4번 실행됐습니다.

#### 원인

`ScheduleParticipant.user`가 LAZY 관계이고 DTO 변환 과정에서 User에 접근하면서 참여자마다 추가 조회가 발생했습니다.

#### 해결

해당 Repository 조회에 다음 설정을 적용했습니다.

```java
@EntityGraph(attributePaths = "user")
```

#### 결과

참여자 상세 조회 Repository 호출에서 조회 쿼리를 4회에서 1회로 줄였습니다.

실제 Service 요청에서는 일정 조회 및 접근 권한 확인을 위한 쿼리가 추가로 발생합니다.

#### 선택 이유

현재 조회는 to-one 관계인 User를 함께 조회하며 페이징을 사용하지 않기 때문에 `EntityGraph`를 적용했습니다.

---

## 16. 향후 구현 계획

### Redis Cache

다가오는 일정 조회에 Redis Cache를 적용하고 적용 전후 응답 시간을 비교할 예정입니다.

```text
GET /api/schedules/upcoming
```

### Confirm Request

참여자가 직접 일정을 `DONE`으로 변경하는 대신 담당자에게 완료 승인을 요청하는 흐름을 추가할 예정입니다.

| 메서드 | URL | 설명 |
| :--- | :--- | :--- |
| POST | `/api/confirm-requests` | 완료 승인 요청 |
| PATCH | `/api/confirm-requests/{id}/status` | 승인 / 반려 |
| GET | `/api/confirm-requests` | 보낸/받은 요청 조회 |

### 운영 환경

- 운영/개발 설정 추가 분리
- HTTPS 환경에서 CSRF Cookie/Header 검증
- 애플리케이션과 MariaDB/Redis를 함께 실행할 수 있는 Compose 환경 검토

---

## 17. 패키지 구조

```text
auth/
└─ 회원가입, 로그인, 로그아웃 및 Session 인증

user/
└─ 사용자 정보 조회 및 상태 관리

schedule/
└─ 일정, 참여자, 초대, 상태 이력 및 권한 정책

common/
└─ 공통 예외 처리 및 유틸리티

config/
└─ Security 등 애플리케이션 설정
```

---

## 18. 이메일 중복 방어

회원가입 시 이메일 중복을 Service와 Database 두 단계에서 확인합니다.

### 1단계 - Service

회원가입 전에 `exists` 조회로 이메일 중복 여부를 확인합니다.

이미 존재하는 이메일이라면 INSERT 요청 전에 차단할 수 있습니다.

다만 거의 동시에 동일한 이메일로 두 요청이 들어오면 두 요청 모두 `exists` 검사를 통과할 가능성이 있습니다.

### 2단계 - Database

DB Unique Constraint를 적용하여 동일한 이메일이 실제로 중복 저장되는 것을 막습니다.

따라서 Service 검사는 빠른 사용자 피드백을 담당하고, DB Unique Constraint가 최종 데이터 무결성을 보장합니다.

---

## 19. ERD

초기 설계에서는 일정 작성자가 참여자를 지정하고, 참여자가 업무를 수행한 뒤 작성자가 최종 상태를 관리하는 구조를 기준으로 설계했습니다.

![ERD](https://github.com/user-attachments/assets/188f24e2-98a5-4460-985a-a9cd4ab353ae)

---

## 이력서 한 줄 소개

Redis Session 기반 인증과 관계별 접근 제어, 참여자 초대·승인, 일정 상태 이력을 구현하고 통합 테스트와 Docker 실행 환경까지 구성한 협업 일정 관리 REST API 프로젝트