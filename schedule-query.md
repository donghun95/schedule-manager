<img width="1506" height="420" alt="Screenshot 2026-08-07 at 1 15 59 AM" src="https://github.com/user-attachments/assets/931a2464-3a3f-4e8a-b131-43f01f93ef33" />



1. 전체 테스트 결과

<img width="540" height="420" alt="Screenshot 2026-08-07 at 1 18 49 AM" src="https://github.com/user-attachments/assets/70ce35ac-7c2a-4c05-9372-9b9239d8964d" />

2. Cursor 첫 페이지와 다음 페이지 응답
- n+1 before
<img width="540" height="420" alt="Screenshot 2026-08-07 at 1 43 41 AM" src="https://github.com/user-attachments/assets/c272abcd-b237-42e3-be85-368dd6390a93" />

- n+1 after
  
2026-08-07 01:33:45 [main] [] DEBUG org.hibernate.SQL - 
    select
        sp1_0.id,
        sp1_0.created_at,
        sp1_0.schedule_id,
        sp1_0.updated_at,
        sp1_0.user_id,
        u1_0.id,
        u1_0.created_at,
        u1_0.email,
        u1_0.nickname,
        u1_0.password,
        u1_0.role,
        u1_0.status,
        u1_0.updated_at 
    from
        schedule_participants sp1_0 
    join
        users u1_0 
            on u1_0.id=sp1_0.user_id 
    where
        sp1_0.schedule_id=? 
    order by
        sp1_0.id
Hibernate: 
    select
        sp1_0.id,
        sp1_0.created_at,
        sp1_0.schedule_id,
        sp1_0.updated_at,
        sp1_0.user_id,
        u1_0.id,
        u1_0.created_at,
        u1_0.email,
        u1_0.nickname,
        u1_0.password,
        u1_0.role,
        u1_0.status,
        u1_0.updated_at 
    from
        schedule_participants sp1_0 
    join
        users u1_0 
            on u1_0.id=sp1_0.user_id 
    where
        sp1_0.schedule_id=? 
    order by
        sp1_0.id  
[N+1 After] prepared statements = 1  

3. N+1 Before·After 테스트

<img width="1525" height="359" alt="Screenshot 2026-08-07 at 1 56 08 AM" src="https://github.com/user-attachments/assets/05f0e890-b462-454f-9a9b-aad06e5c2962" />

4. EXPLAIN 

