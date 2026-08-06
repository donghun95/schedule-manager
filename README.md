# schedule-manager
- 주제

프로젝트명 : 학원/교육 운영 스케줄 관리 시스템  
한 줄 설명 : 운영에 필요한 스케쥴 관리 시스템  
누가 쓰는 서비스인지: 학원/교육 운영자  
어떤 문제를 해결하는지: 기존 엑셀이나 카톡으로 일정을 관리했던 것을 시스템화해서 관리하고자 하였습니다.  
왜 이 주제를 선택했는지 : 학원 웹사이트 회사 재직 당시에 만들었었는데 부족한 면이 있어서 Spring boot를 적용해 만들어 보고자 합니다.   

- 기술 적용 계획

| 기술 | 어디에 적용할지 |
| :--- | :--- |
| JPA | 엔티티 연관관계 매핑 및 Fetch Join을 통한 성능 최적화에 적용 |
| Redis | Session 저장소 구축 및 다가오는 일정 데이터 캐싱에 적용 |
| Spring Security | 로그인 인증 및 접근 권한 체크 기능에 적용 |
| RESTful API | 일관된 상태 코드 반환, Location 헤더 활용, 공통 ErrorResponse 구조 설계에 적용 |
| Test | Service, Controller, Repository 계층별 테스트 코드 작성에 적용 |
| Flyway | DB 스키마 버전 관리 및 변경 이력 추적에 적용 |
| Logging | 운영 로그에 traceId를 부여하여 에러 응답과 로그를 연직선상에서 연결하는 데 적용 |

- 환경 변수 명시

 실행 환경
 - Java 21
 - Spring Boot 4.0.6
 - MariaDB 11.4
 - Redis 7.4 
 
 환경 변수
 - DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD
 - REDIS_HOST / REDIS_PORT
## 로컬 실행

수업용 HTTP 요청은 `local` 프로필로 실행합니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```  
기본 프로필은 CSRF 방어와 Secure 세션 쿠키를 활성화한 배포 안전 기본값입니다. Spring Security 7의 `csrf.spa()`가 `XSRF-TOKEN` 쿠키와 `X-XSRF-TOKEN` 헤더를 처리합니다. 로컬의 `http://localhost:18080`에서 `requests-week14.http`를 실행할 때만 `local` 프로필을 사용합니다. 기본 프로필을 실제 배포에 사용하려면 HTTPS뿐 아니라 프론트엔드가 쿠키의 토큰을 헤더로 다시 보내도록 연결해야 합니다.

권한 정책 + 상태 전이 규칙 작성
권한 정책
- 일반 사용자 / 관리자 / 차단 사용자
- 작성자만 가능한 기능
- 참여자도 가능한 기능
- 관리자가 참여자를 지정하여 작성이 가능한 기능

상태 전이 규칙
- PLANNED -> IN_PROGRESS
- PLANNED -> CANCELED
- IN_PROGRESS -> DONE
- IN_PROGRESS -> CANCELED
- DONE/CANCELED 이후 변경 불가

ERD 작성
초기 구상  
1. 담당자를 지정하고 그 담당자와 같이 하는 사람을 지정을해서 그 담당자가 최종으로 컨펌을 하는 구조입니다.
2. 처음 구성은 일단 로그인합니다.
3. 그리고서는 자신에게 할당된 업무를 확인하고
4. 그 업무를 수행하며 컨펌 받습니다.
<img width="1366" height="856" alt="Untitled" src="https://github.com/user-attachments/assets/188f24e2-98a5-4460-985a-a9cd4ab353ae" />

각 테이블 역할

users — 로그인 사용자. role 은 USER/ADMIN, status 는 ACTIVE/BLOCKED. 차단된
유저는 로그인이나 주요 요청을 막을 수 있어야 합니다.    
schedules — 핵심 도메인. owner_id 는 작성자. version 은 JPA 낙관적 락에 사용.  
schedule_participants — User와 Schedule의 다대다 관계를 풀어낸 중간 테이블.  
@ManyToMany 를 바로 쓰기보다 중간 엔티티를 두면 나중에 joinedAt, 역할, 초대 상태 같은 컬럼
을 추가 가능.  
schedule_status_history — 상태 변경 이력.  
confirm_requests  — 협업자가 업무 완료 후 담당자에게 승인을 요청하고 피드백을 받는 핵심 워크플로우 테이블입니다.   
status를 통해 컨펌 진행 상황을 관리하며, feedback 컬럼을 두어 반려 사유나 수정 요청 사항을 텍스트로 보존합니다.   
스케줄 자체의 상태와 분리되어 있어,
한 업무 내에서 발생한 수정 및 재요청 이력을 추적할 수 있습니다.


## 현재 구현 API (14주차)


| 메서드 | URL | 설명 | 요청 | 응답 | 인증 | 
| :--- | :--- |:--- |:--- |:--- |:--- |
| POST | /api/auth/signup | 회원가입 | {email,password,nickname} | 201 + UserResponse | X |
| POST | /api/auth/login | 로그인 | {email,password} | 200 + UserResponse | X |
| POST | /api/auth/logout | 로그아웃 | - | 204 | O |
| GET | /api/users/me | 내 정보 조회 | - | 200 + UserResponse | O | 
| POST | /api/schedules | 스케줄 등록 | {title,description,scheduledAt} | 201 + Location | O | 
| GET | /api/schedules | 작성·참여 일정 검색 + Cursor 목록 | `status`, `fromAt`, `toAt`, `cursorScheduledAt`, `cursorId`, `size` | 200 + CursorResponse<ScheduleSummaryResponse> | O | 
| GET | /api/schedules/{id} | 스케줄 단건 조회 | - | 200 + ScheduleResponse | O(작성자/참여자) |
| PATCH | /api/schedules/{id} | 스케줄 부분 수정 | {title?,description?,scheduledAt?,version} | 200 + ScheduleResponse | O(작성자) | 
| DELETE | /api/schedules/{id} | 예정 일정 삭제 | - | 204 | O(작성자) |
| POST | /api/schedules/{id}/participants | 참여자 추가 | {userId} | 201 + Location | O(작성자) |
| GET | /api/schedules/{id}/participants | 참여자 목록 조회 | - | 200 + List<ScheduleParticipantDetailResponse> | O(작성자/참여자) |
| DELETE | /api/schedules/{id}/participants/{userId} | 참여자 제거 | - | 204 | O(작성자) |
| PATCH | /api/schedules/{id}/status | 상태 변경 | {toStatus,version} | 200 + ScheduleResponse | O(작성자) |
| GET | /api/schedules/{id}/history | 상태 변경 이력 | - | 200 + List<ScheduleStatusHistoryResponse> | O(작성자/참여자) |

### 13주차 권한 및 생명주기 정책

- 작성자만 참여자 추가·제거와 상태 변경을 할 수 있습니다.
- 참여자는 배정된 일정과 상태 이력을 조회할 수 있습니다.
- 참여자가 직접 `DONE`으로 바꾸는 대신, 향후 `confirm_requests`로 완료 요청을 보내고 작성자가 승인하도록 설계합니다.
- `PLANNED` 일정만 삭제할 수 있습니다. 시작된 일정은 이력을 보존하기 위해 삭제하지 않고 `DONE` 또는 `CANCELED`로 종료합니다.
- 상태 전이 규칙이나 요청 version이 현재 리소스와 충돌하면 `409 Conflict`를 반환합니다.

### 공통 오류 응답

입력 검증 실패는 공통 오류 코드와 함께 필드별 메시지를 반환합니다. 비밀번호 같은 민감값이 다시 노출되지 않도록 `rejectedValue`는 포함하지 않습니다.

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

Swagger/OpenAPI에는 실제 HTTP 계약과 동일하게 등록 `201`, 삭제·로그아웃 `204`, 검증 실패 `400`, 버전 충돌 `409`를 명시합니다.

## 향후 구현 계획 (15주차 이후)

아래 API는 설계안이며 현재 코드에는 아직 구현되지 않았습니다.

| 메서드 | URL | 설명 | 요청 | 응답 | 인증 |
| :--- | :--- |:--- |:--- |:--- |:--- |
| GET | /api/schedules/upcoming | 다가오는 일정 캐시 조회 | ?size=10 | 200 + List<ScheduleSummary> | O |
| POST | /api/confirm-requests | 컴펌(결재) 요청 등록 | {scheduleId, approverId} | 201 Created {id, scheduleId, requesterId, approverId, status: 'PENDING', createdAt} | O(협업자) |
| PATCH | /api/confirm-requests/{id}/status | 컨펌 요청 승인/반려 처리  | {status, feedback?}※ status: APPROVED/REJECTED  | 200 OK {id, scheduleId, status, feedback, updatedAt} | O(담당자) |
| GET | /api/confirm-requests | 결재 보관함 (내가 보낸/받은 요청) | ?type=RECEIVE 또는 SEND&status=&size=&cursorId=  | 200 OK CursorResponse<ConfirmRequestResponse>  | O |

인증 방식 ADR 작성

# 001. 인증 방식으로  Redis Session을 선택한 이유

## Context
- 운영 관리형 서비스라 사용자 차단, 강제 로그아웃, 권한 변경 반영 정책이 중요하다.
- JWT도 고려했지만 access token은 만료 전까지 기본적으로 유효하다.

## Decision
- Redis Session 기반 인증을 사용한다.

## Consequences
- 사용자별 세션 조회·삭제 정책을 구현하면 서버가 로그인 상태를 통제할 수 있다.
- 세션 저장소인 Redis가 필요하다.
- 마이크로서비스나 외부 API 구조에서는 JWT가 더 적합할 수 있다.

> 현재 12주차 코드에는 사용자 차단·권한 변경 시 기존 세션을 즉시 찾아 삭제하는 기능까지는 구현하지 않았습니다. Redis Session 선택만으로 자동 해결되지 않으며, 이후 사용자별 세션 인덱스와 무효화 정책을 추가해야 합니다.


# 성능/테스트/운영 검증 계획 작성

성능
- EXPLAIN으로 일정 목록 쿼리 인덱스 확인
- N+1 발생 여부 확인
- 캐시 적용 전/후 응답 시간 비교
- DB 스키마 변경은 Flyway migration으로 관리 예정
  테스트
- 상태 전이 성공/실패 테스트
- 권한 없는 수정 실패 테스트
- Repository 쿼리 테스트
  운영

- ErrorResponse에 traceId 포함
- Actuator health check
- GitHub Actions로 테스트 자동화 예정

# 패키지 구조 초안

auth/  
역할 : 회원가입, 로그인, 로그아웃 및 Session 검증을 담당합니다.

user/  
역할 : 사용자 정보 조회 및 상태 관리를 담당합니다.

schedule/  
역할 : 스케줄 및 협업의 도메인입니다.

common/  
역할 : 공통 유틸 및 예외 처리합니다.

config/  
역할 : 애플리케이션 설정입니다.

# 회원가입 시 이메일 중복 검증을 DB 유니크 제약 + Service exists 체크 둘 다 걸어서 이중으로 방어하는 구조로 설계했습니다.
1. 1단계 방어에서는 서비스 레이어가 검증을 합니다.  
   DB에 불필요한 INSERT 요청을 보내기전에 빠르게 조회 쿼리만으로 중복을 반별하여 서버 자원을 아낄 수 있습니다.  
   하지만 미세한 시간 차로 동시에 들어오는 요청의 경우, 두 요청 모두 아직 DB에 저장되기 전이므로 exists 검증을 동시에 통과하는 허점이 있습니다.
2. 2단계 방어에서는 Database 레이어 검증을 합니다.    
   DB 엔진 수준에서 동일한 이메일 저장을 물리적으로 차단하기 때문에, 동시 요청이 들어오더라도 데이터가 중복 저장 되는 것을 막아줍니다.  

## 트러블슈팅: 일정 목록의 안정적인 Cursor 페이징
### 문제
scheduledAt만 Cursor로 사용하면 같은 시각의 일정이 여러 건일 때
다음 페이지에서 누락되거나 중복될 수 있었다.
### 원인
scheduledAt 하나만으로는 전체 정렬 순서가 결정되지 않았다.
### 해결
scheduledAt DESC, id DESC로 정렬하고,
다음 페이지 조건도 scheduledAt + id 복합 조건으로 맞췄다.
size + 1건을 조회해 별도 COUNT 없이 hasNext를 판단했다.
### 검증
같은 scheduledAt의 일정과 작성·참여 일정을 섞은 통합 테스트에서
페이지 사이의 누락과 중복이 없음을 확인했다.


## 트러블슈팅: 참여자 목록 N+1
### 문제
참여자 3명의 nickname을 응답에 넣을 때 SQL이 4번 실행됐다.
### 원인
ScheduleParticipant.user가 LAZY이고 DTO 변환 중 User에 접근했다.
### 해결
해당 조회에 @EntityGraph(attributePaths = "user")를 적용했다.
### 결과
Hibernate Statistics 기준 4 queries에서 1 query로 줄었다.
### 선택 이유와 제한

현재 조회는 to-one User를 함께 가져오고 페이징하지 않아
EntityGraph가 간결했다. 컬렉션 Fetch Join과 페이징은 무조건 결합하지 않는다.
  

# 프로젝트 소개 
일정을 생생하고 여러 참여자를 초대하여 승인과 상태 변경을 관리하는 협업 일정 관리 앱입니다.
Spring Boot 와 JPA를 기반으로 인증,권한관리,참여자 승인, 일정 이력 관리 기능을 구현했으며 REST API와 테스트 코드를 통해 안정성을 검증했습니다.
### 기술 선택 이유
Spring Boot + JPA
반복적인 CRUD보다 비즈니스 로직 구현에 집중하기 위해 선택
객체 중심으로 도메인을 설계하고 연관관계를 자연스럽게 표현 가능
### 트러블 슈팅
트러블슈팅(N+1)
## 트러블슈팅: 참여자 목록 N+1
### 문제
참여자 3명의 nickname을 응답에 넣을 때 SQL이 4번 실행됐다.
### 원인
ScheduleParticipant.user가 LAZY이고 DTO 변환 중 User에 접근했다.
### 해결
해당 조회에 @EntityGraph(attributePaths = "user")를 적용했다.
### 결과
Hibernate Statistics 기준 4 queries에서 1 query로 줄었다.
### 선택 이유와 제한

현재 조회는 to-one User를 함께 가져오고 페이징하지 않아
EntityGraph가 간결했다. 컬렉션 Fetch Join과 페이징은 무조건 결합하지 않는다.

# 이력서 한 줄 소개
JWT 기반 인증과 참여자 승인, 일정 이력 관리 기능을 구현한 협업 일정 관리 REST API 프로젝트


