package com.dsa.schedule_manager.schedule.controller;

import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
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
class ScheduleApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
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
                .andExpect(jsonPath("$", hasSize(1)));

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
