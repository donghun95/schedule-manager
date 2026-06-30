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
| CustomAppbar | 직접 만들어 본 커스텀 앱바입니다. |
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




