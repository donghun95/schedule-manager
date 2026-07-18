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

API 명세서

| 메서드 | URL | 설명 | 요청 | 응답 | 인증 | 
| :--- | :--- |:--- |:--- |:--- |:--- |
| POST | /api/auth/signup | 회원가입 | {email,password,nickname} | 201 + UserResponse | X |
| POST | /api/auth/login | 로그인 | {email,password} | 200 + UserResponse | X |
| POST | /api/auth/logout | 로그아웃 | - | 204 | O |
| GET | /api/users/me | 내 정보 조회 | - | 200 + UserResponse | O | 
| POST | /api/schedules | 스케줄 등록 | {title,description,scheduledAt} | 201 + Location | O | 
| GET | /api/schedules | 내 스케줄 목록 | ?status=&fromDate=&toDate=&cursorId=&size= | 200 + CursorResponse<ScheduleResponse> | O | 
| GET | /api/schedules/{id} | 스케줄 단건 조회 | - | 200 + ScheduleDetailResponse | O |
| PATCH | /api/schedules/{id} | 스케줄 부분 수정 | {title?,description?,scheduledAt?,version} | 200 + ScheduleResponse | O(작성자) | 
| PATCH | /api/schedules/{id}/status | 상태 변경 | {toStatus,version} | 200 + ScheduleResponse | O(작성자/참여자) | 
| DELETE | /api/schedules/{id} | 스케줄 삭제 | - | 204 | O(작성자) | 
| POST | /api/schedules/{id}/participants | 참여자 추가 | {userId} | 201 | O(작성자) | 
| DELETE | /api/schedules/{id}/participants/{userId} | 참여자 제거 | - | 204 | O(작성자) | 
| GET | /api/schedules/upcoming | 다가오는 일정 캐시 조회 | ?size=10 | 200 + List<ScheduleSummary> | O |
| POST | /api/confirm-requests | 컴펌(결재) 요청 등록 | {scheduleId, approverId} | 201 Created {id, scheduleId, requesterId, approverId, status: 'PENDING', createdAt} | O(협업자) |
| PATCH | /api/confirm-requests/{id}/status | 컨펌 요청 승인/반려 처리  | {status, feedback?}※ status: APPROVED/REJECTED  | 200 OK {id, scheduleId, status, feedback, updatedAt} | O(담당자) |
| GET | /api/confirm-requests | 결재 보관함 (내가 보낸/받은 요청) | ?type=RECEIVE 또는 SEND&status=&size=&cursorId=  | 200 OK CursorResponse<ConfirmRequestResponse>  | O |

 인증 방식 ADR 작성

# 001. 인증 방식으로  Redis Session을 선택한 이유

## Context
- 운영 관리형 서비스라 사용자 차단, 강제 로그아웃, 권한 변경 즉시 반영이 중요하다.
- JWT도 고려했지만 access token은 만료 전까지 기본적으로 유효하다.

## Decision
- Redis Session 기반 인증을 사용한다.
  
## Consequences
- 서버가 로그인 상태를 통제할 수 있다.
- 세션 저장소인 Redis가 필요하다.
- 마이크로서비스나 외부 API 구조에서는 JWT가 더 적합할 수 있다.


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

- ErrorResponse에 traceId 포함 예정
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

