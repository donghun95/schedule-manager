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
 - Redis 7.4 또는 사용 가능한 Redis 호환 서버   
 
 환경 변수
 - DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD
 - REDIS_HOST / REDIS_PORT
