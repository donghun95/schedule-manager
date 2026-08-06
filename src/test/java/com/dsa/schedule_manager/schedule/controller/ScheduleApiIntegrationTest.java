package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleStatusHistoryRepository;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ScheduleParticipantRepository participantRepository;

    @Autowired
    ScheduleStatusHistoryRepository historyRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        participantRepository.deleteAll();
        scheduleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void scheduleCrudAuthorizationAndVersionContract() throws Exception {
        signup("owner@example.com", "owner");
        HttpSession ownerSession = login("owner@example.com");

        MvcResult created = mockMvc.perform(post("/api/schedules")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "12주차 일정",
                                  "description": "권한과 낙관적 락 검증",
                                  "scheduledAt": "2099-07-30T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/schedules/\\d+")))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();

        String createdBody = created.getResponse().getContentAsString();
        long scheduleId = ((Number) JsonPath.read(createdBody, "$.id")).longValue();
        long staleVersion = ((Number) JsonPath.read(createdBody, "$.version")).longValue();

        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/schedules")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.hasNext").value(false));

        mockMvc.perform(patch("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"수정된 일정","version":%d}
                                """.formatted(staleVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(staleVersion + 1));

        mockMvc.perform(patch("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"오래된 수정","version":%d}
                                """.formatted(staleVersion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E_409_002"));

        signup("other@example.com", "other");
        HttpSession otherSession = login("other@example.com");

        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                otherSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                otherSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"권한 없는 수정","version":%d}
                                """.formatted(staleVersion + 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                otherSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/schedules/not-a-number")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_001"));
        mockMvc.perform(get("/api/schedules/999999")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void scheduleListRejectsInvalidPagingAndDateParameters() throws Exception {
        signup("query@example.com", "query-user");
        HttpSession session = login("query@example.com");

        mockMvc.perform(get("/api/schedules")
                        .param("size", "0")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                session.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_001"));

        mockMvc.perform(get("/api/schedules")
                        .param("cursorId", "10")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                session.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_003"));

        mockMvc.perform(get("/api/schedules")
                        .param("fromAt", "2099-08-02T10:00:00")
                        .param("toAt", "2099-08-01T10:00:00")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                session.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_004"));
    }

    @Test
    void scheduleListBindsCompositeCursorAndSerializesTwoPages() throws Exception {
        signup("cursor-owner@example.com", "cursor-owner");
        signup("cursor-participant@example.com", "cursor-participant");
        HttpSession ownerSession = login("cursor-owner@example.com");
        HttpSession participantSession = login("cursor-participant@example.com");
        User owner = userRepository.findByEmail("cursor-owner@example.com").orElseThrow();
        User participant = userRepository.findByEmail("cursor-participant@example.com").orElseThrow();

        Schedule scheduleD = scheduleRepository.save(Schedule.create(
                owner.getId(), "일정 D", null, LocalDateTime.of(2099, 9, 1, 10, 0)));
        Schedule scheduleB = scheduleRepository.save(Schedule.create(
                owner.getId(), "일정 B", null, LocalDateTime.of(2099, 9, 2, 10, 0)));
        Schedule scheduleC = scheduleRepository.save(Schedule.create(
                owner.getId(), "일정 C", null, LocalDateTime.of(2099, 9, 2, 10, 0)));
        Schedule scheduleA = scheduleRepository.save(Schedule.create(
                owner.getId(), "일정 A", null, LocalDateTime.of(2099, 9, 3, 10, 0)));
        participantRepository.save(ScheduleParticipant.of(scheduleC, participant));

        MvcResult firstPage = mockMvc.perform(get("/api/schedules")
                        .param("size", "2")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].title").value(contains("일정 A", "일정 C")))
                .andExpect(jsonPath("$.items[*].accessType").value(contains("OWNER", "OWNER")))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andReturn();
        String firstBody = firstPage.getResponse().getContentAsString();
        String cursorScheduledAt = JsonPath.read(firstBody, "$.nextCursorScheduledAt");
        Number cursorId = JsonPath.read(firstBody, "$.nextCursorId");

        mockMvc.perform(get("/api/schedules")
                        .param("cursorScheduledAt", cursorScheduledAt)
                        .param("cursorId", cursorId.toString())
                        .param("size", "2")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].title").value(contains("일정 B", "일정 D")))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursorScheduledAt").value(nullValue()))
                .andExpect(jsonPath("$.nextCursorId").value(nullValue()));

        Schedule outsideRange = scheduleRepository.save(Schedule.create(
                owner.getId(), "기간 밖 참여 일정", null, LocalDateTime.of(2099, 8, 31, 10, 0)));
        Schedule afterRange = scheduleRepository.save(Schedule.create(
                owner.getId(), "종료 범위 밖 참여 일정", null, LocalDateTime.of(2099, 9, 4, 10, 0)));
        Schedule doneInRange = scheduleRepository.save(Schedule.create(
                owner.getId(), "완료된 참여 일정", null, LocalDateTime.of(2099, 9, 2, 9, 0)));
        doneInRange.changeStatus(ScheduleStatus.IN_PROGRESS);
        doneInRange.changeStatus(ScheduleStatus.DONE);
        scheduleRepository.save(doneInRange);
        participantRepository.save(ScheduleParticipant.of(outsideRange, participant));
        participantRepository.save(ScheduleParticipant.of(afterRange, participant));
        participantRepository.save(ScheduleParticipant.of(doneInRange, participant));

        mockMvc.perform(get("/api/schedules")
                        .param("status", "PLANNED")
                        .param("fromAt", "2099-09-01T10:00:00")
                        .param("toAt", "2099-09-03T10:00:00")
                        .param("size", "10")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(scheduleC.getId()))
                .andExpect(jsonPath("$.items[0].accessType").value("PARTICIPANT"));

        // 테스트 데이터 순서를 명시해 같은 시각의 보조 정렬 조건도 고정합니다.
        org.assertj.core.api.Assertions.assertThat(scheduleC.getId()).isGreaterThan(scheduleB.getId());
        org.assertj.core.api.Assertions.assertThat(scheduleA.getId()).isGreaterThan(scheduleC.getId());
        org.assertj.core.api.Assertions.assertThat(scheduleD.getId()).isLessThan(scheduleB.getId());
    }

    private void signup(String email, String nickname) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123",
                                  "nickname": "%s"
                                }
                                """.formatted(email, nickname)))
                .andExpect(status().isCreated());
    }

    private HttpSession login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getRequest().getSession(false);
    }
}
