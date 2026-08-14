package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleStatusHistoryRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleParticipantStatusApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ScheduleStatusHistoryRepository historyRepository;

    @Autowired
    ScheduleParticipantRepository participantRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

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
    void 참여자_상태전이_이력_HTTP_계약을_검증한다() throws Exception {
        long ownerId = signup("owner13@example.com", "owner13");
        long participantId = signup("participant13@example.com", "participant13");
        long outsiderId = signup("outsider13@example.com", "outsider13");
        HttpSession ownerSession = login("owner13@example.com");
        HttpSession participantSession = login("participant13@example.com");
        HttpSession outsiderSession = login("outsider13@example.com");

        MvcResult created = mockMvc.perform(post("/api/schedules")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "13주차 일정",
                                  "description": "참여자와 상태 이력 검증",
                                  "scheduledAt": "2099-08-13T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long scheduleId = number(created, "$.id");

        mockMvc.perform(post("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(ownerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_002"));

        mockMvc.perform(post("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(participantId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        matchesPattern("/api/schedules/\\d+/participants/\\d+")))
                .andExpect(jsonPath("$.userId").value(participantId));

        mockMvc.perform(patch(
                        "/api/schedules/{scheduleId}/participants/me/accept",
                        scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                outsiderSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(outsiderId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E_403_001"));

        mockMvc.perform(post("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":999999}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E_404_003"));

        mockMvc.perform(post("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(participantId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E_409_006"));

        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                outsiderSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/schedules/{id}/status", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"IN_PROGRESS","version":0}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/schedules/{id}/status", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"DONE","version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E_409_004"));

        mockMvc.perform(patch("/api/schedules/{id}/status", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"IN_PROGRESS","version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch("/api/schedules/{id}/status", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"DONE","version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E_409_002"));

        mockMvc.perform(patch("/api/schedules/{id}/status", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"IN_PROGRESS","version":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E_409_005"));

        mockMvc.perform(delete("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E_409_007"));

        mockMvc.perform(patch("/api/schedules/{id}/status", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"DONE","version":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(get("/api/schedules/{id}/history", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fromStatus").value("PLANNED"))
                .andExpect(jsonPath("$[0].toStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].changedBy").value(ownerId))
                .andExpect(jsonPath("$[0].changedAt").isNotEmpty())
                .andExpect(jsonPath("$[1].fromStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].toStatus").value("DONE"));

        mockMvc.perform(delete(
                        "/api/schedules/{scheduleId}/participants/{userId}",
                        scheduleId,
                        outsiderId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E_404_002"));

        mockMvc.perform(delete(
                        "/api/schedules/{scheduleId}/participants/{userId}",
                        scheduleId,
                        participantId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/schedules/{id}/history", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 예정_일정은_참여자_연결을_정리한_뒤_삭제한다() throws Exception {
        signup("delete-owner@example.com", "delete-owner");
        long participantId = signup("delete-participant@example.com", "delete-participant");
        HttpSession ownerSession = login("delete-owner@example.com");

        MvcResult created = mockMvc.perform(post("/api/schedules")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "삭제 가능한 예정 일정",
                                  "scheduledAt": "2099-08-14T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long scheduleId = number(created, "$.id");

        mockMvc.perform(post("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(participantId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/schedules/{id}", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(
                participantRepository.existsByScheduleIdAndUserId(scheduleId, participantId))
                .isFalse();
        org.assertj.core.api.Assertions.assertThat(scheduleRepository.findById(scheduleId))
                .isEmpty();
    }

    @Test
    void 참여자_목록은_작성자와_참여자에게만_사용자_정보를_반환한다() throws Exception {
        signup("list-owner@example.com", "list-owner");
        long firstParticipantId = signup("list-first@example.com", "list-first");
        long secondParticipantId = signup("list-second@example.com", "list-second");
        long thirdParticipantId = signup("list-third@example.com", "list-third");
        signup("list-outsider@example.com", "list-outsider");

        HttpSession ownerSession = login("list-owner@example.com");
        HttpSession firstParticipantSession = login("list-first@example.com");
        HttpSession thirdParticipantSession = login("list-third@example.com");
        HttpSession outsiderSession = login("list-outsider@example.com");

        MvcResult created = mockMvc.perform(post("/api/schedules")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "참여자 목록 일정",
                                  "scheduledAt": "2099-08-15T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long scheduleId = number(created, "$.id");

        addParticipant(ownerSession, scheduleId, firstParticipantId);
        addParticipant(ownerSession, scheduleId, secondParticipantId);
        addParticipant(ownerSession, scheduleId, thirdParticipantId);

        // firstParticipant만 초대 수락
        mockMvc.perform(patch(
                        "/api/schedules/{scheduleId}/participants/me/accept",
                        scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                firstParticipantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());

        // thirdParticipant는 초대 거절
        mockMvc.perform(patch(
                        "/api/schedules/{scheduleId}/participants/me/reject",
                        scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                thirdParticipantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))

                .andExpect(jsonPath("$[0].userId").value(firstParticipantId))
                .andExpect(jsonPath("$[0].nickname").value("list-first"))
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$[0].joinedAt").isNotEmpty())

                .andExpect(jsonPath("$[1].userId").value(secondParticipantId))
                .andExpect(jsonPath("$[1].nickname").value("list-second"))
                .andExpect(jsonPath("$[1].status").value("PENDING"))

                .andExpect(jsonPath("$[2].userId").value(thirdParticipantId))
                .andExpect(jsonPath("$[2].nickname").value("list-third"))
                .andExpect(jsonPath("$[2].status").value("REJECTED"));

        mockMvc.perform(get("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                firstParticipantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(firstParticipantId))
                .andExpect(jsonPath("$[0].nickname").value("list-first"))
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));

        mockMvc.perform(get("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                outsiderSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E_403_001"));

        mockMvc.perform(get("/api/schedules/999999/participants")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E_404_001"));
    }

    @Test
    void 신규_API의_미인증과_입력값_오류_계약을_검증한다() throws Exception {
        mockMvc.perform(post("/api/schedules/1/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E_401_001"));

        mockMvc.perform(delete("/api/schedules/1/participants/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E_401_001"));

        mockMvc.perform(patch("/api/schedules/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"IN_PROGRESS","version":0}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E_401_001"));

        mockMvc.perform(get("/api/schedules/1/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E_401_001"));

        signup("validation-owner@example.com", "validation-owner");
        HttpSession ownerSession = login("validation-owner@example.com");

        mockMvc.perform(post("/api/schedules/1/participants")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_001"));

        mockMvc.perform(patch("/api/schedules/1/status")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_001"));

        mockMvc.perform(patch("/api/schedules/1/status")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus":"UNKNOWN","version":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_001"));
    }

    @Test
    void PENDING_참여자는_일정_이력_참여자목록을_조회할_수_없다() throws Exception {

        long ownerId = signup("owner13@example.com", "owner13");
        long participantId = signup("participant13@example.com", "participant13");

        HttpSession ownerSession = login("owner13@example.com");
        HttpSession participantSession = login("participant13@example.com");


        MvcResult created = mockMvc.perform(post("/api/schedules")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "PENDING 권한 테스트",
                                  "scheduledAt": "2099-08-16T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long scheduleId = number(created, "$.id");
        addParticipant(ownerSession, scheduleId, participantId);

        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E_403_001"));

        mockMvc.perform(get("/api/schedules/{id}/history", scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E_403_001"));

        mockMvc.perform(get("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E_403_001"));

    }

    @Test
    void REJECTED_참여자는_일정_이력_참여자목록을_조회할_수_없다() throws Exception {

        signup("owner13@example.com", "owner13");
        long participantId = signup("participant13@example.com", "participant13");

        HttpSession ownerSession = login("owner13@example.com");
        HttpSession participantSession = login("participant13@example.com");


        MvcResult created = mockMvc.perform(post("/api/schedules")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "REJECTED 권한 테스트",
                                  "scheduledAt": "2099-08-16T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long scheduleId = number(created, "$.id");
        addParticipant(ownerSession, scheduleId, participantId);

        mockMvc.perform(patch(
                        "/api/schedules/{scheduleId}/participants/me/reject",
                        scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E_403_001"));

        mockMvc.perform(get("/api/schedules/{id}/history", scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E_403_001"));

        mockMvc.perform(get("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E_403_001"));

    }


    @Test
    void ACCEPTED_참여자는_일정_이력_참여자목록을_조회할_수_있다() throws Exception {

        signup("owner13@example.com", "owner13");
        long participantId = signup("participant13@example.com", "participant13");

        HttpSession ownerSession = login("owner13@example.com");
        HttpSession participantSession = login("participant13@example.com");


        MvcResult created = mockMvc.perform(post("/api/schedules")
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "ACCEPTED 권한 테스트",
                                  "scheduledAt": "2099-08-16T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long scheduleId = number(created, "$.id");
        addParticipant(ownerSession, scheduleId, participantId);

        mockMvc.perform(patch(
                        "/api/schedules/{scheduleId}/participants/me/accept",
                        scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/schedules/{id}", scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/schedules/{id}/history", scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                participantSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(participantId));

    }

    @Test
    void PENDING_초대만_내_초대_목록에_조회된다() throws Exception {

        // 1. 사용자 생성
        signup("invite-owner@example.com", "invite-owner");

        long pendingUserId =
                signup("invite-pending@example.com", "invite-pending");

        long acceptedUserId =
                signup("invite-accepted@example.com", "invite-accepted");

        long rejectedUserId =
                signup("invite-rejected@example.com", "invite-rejected");


        // 2. 로그인
        HttpSession ownerSession =
                login("invite-owner@example.com");

        HttpSession pendingSession =
                login("invite-pending@example.com");

        HttpSession acceptedSession =
                login("invite-accepted@example.com");

        HttpSession rejectedSession =
                login("invite-rejected@example.com");


        // 3. Owner가 일정 생성
        MvcResult created = mockMvc.perform(post("/api/schedules")
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "title": "초대 목록 테스트",
                              "scheduledAt": "2099-08-20T10:00:00"
                            }
                            """))
                .andExpect(status().isCreated())
                .andReturn();

        long scheduleId = number(created, "$.id");


        // 4. 세 사용자를 일정에 초대
        // 이 시점에서는 모두 PENDING
        addParticipant(ownerSession, scheduleId, pendingUserId);
        addParticipant(ownerSession, scheduleId, acceptedUserId);
        addParticipant(ownerSession, scheduleId, rejectedUserId);


        // 5. acceptedUser는 초대 수락
        mockMvc.perform(patch(
                        "/api/schedules/{scheduleId}/participants/me/accept",
                        scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                acceptedSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());


        // 6. rejectedUser는 초대 거절
        mockMvc.perform(patch(
                        "/api/schedules/{scheduleId}/participants/me/reject",
                        scheduleId)
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                rejectedSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isNoContent());


        // 7. PENDING 사용자는 자신의 초대를 조회할 수 있다.
        mockMvc.perform(get("/api/schedules/invitations")
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                pendingSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].scheduleId").value(scheduleId))
                .andExpect(jsonPath("$[0].title").value("초대 목록 테스트"))
                .andExpect(jsonPath("$[0].scheduledAt")
                        .value("2099-08-20T10:00:00"))
                .andExpect(jsonPath("$[0].ownerNickname")
                        .value("invite-owner"))
                .andExpect(jsonPath("$[0].status")
                        .value("PENDING"));


        // 8. 이미 수락한 사용자는 PENDING 초대가 없다.
        mockMvc.perform(get("/api/schedules/invitations")
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                acceptedSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));


        // 9. 이미 거절한 사용자도 PENDING 초대가 없다.
        mockMvc.perform(get("/api/schedules/invitations")
                        .sessionAttr(
                                "SPRING_SECURITY_CONTEXT",
                                rejectedSession.getAttribute("SPRING_SECURITY_CONTEXT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }


    private long signup(String email, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123",
                                  "nickname": "%s"
                                }
                                """.formatted(email, nickname)))
                .andExpect(status().isCreated())
                .andReturn();
        return number(result, "$.id");
    }

    private void addParticipant(
            HttpSession ownerSession,
            long scheduleId,
            long participantId) throws Exception {
        mockMvc.perform(post("/api/schedules/{id}/participants", scheduleId)
                        .sessionAttr("SPRING_SECURITY_CONTEXT",
                                ownerSession.getAttribute("SPRING_SECURITY_CONTEXT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(participantId)))
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

    private long number(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(
                result.getResponse().getContentAsString(),
                path)).longValue();
    }
}
