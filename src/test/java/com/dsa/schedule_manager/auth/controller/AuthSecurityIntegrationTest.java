package com.dsa.schedule_manager.auth.controller;

import com.dsa.schedule_manager.auth.service.UserPrincipal;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void unauthenticatedJsonRequestReturns401WithoutCreatingSession() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("SCHEDULEID"))
                .andExpect(jsonPath("$.code").value("E_401_001"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void malformedSignupJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_001"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void invalidSignupReturnsFieldErrorsWithoutRejectedPassword() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hoon@example.com",
                                  "password": "short",
                                  "nickname": "동훈"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_001"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").doesNotExist());
    }

    @Test
    void loginChangesExistingSessionIdAndErasesCredentials() throws Exception {
        signup();
        MockHttpSession session = new MockHttpSession();
        String previousId = session.getId();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hoon@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(session.getId()).isNotEqualTo(previousId);
        SecurityContext context = (SecurityContext) session.getAttribute(
                "SPRING_SECURITY_CONTEXT"
        );
        UserPrincipal principal = (UserPrincipal) context.getAuthentication().getPrincipal();
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        signup();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hoon@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E_401_001"));
    }

    @Test
    void blockedUserLoginReturns401() throws Exception {
        signup();
        User user = userRepository.findByEmail("hoon@example.com").orElseThrow();
        user.block();
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hoon@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E_401_001"));
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, org.hamcrest.Matchers.containsString("POST")))
                .andExpect(jsonPath("$.code").value("E_405_001"));
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("E_415_001"));
    }

    private void signup() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hoon@example.com",
                                  "password": "password123",
                                  "nickname": "동훈"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
