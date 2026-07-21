package com.dsa.schedule_manager.auth;

import com.dsa.schedule_manager.auth.dto.LoginRequest;
import com.dsa.schedule_manager.auth.dto.SignupRequest;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    @DisplayName("로그인 성공 시 세션 ID가 새로 발급되어 변경된다")
    void sessionFixationProtectionTest() throws Exception {

        SignupRequest signupRequest = new SignupRequest("hoon@example.com", "password123", "동훈");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // 1. 로그인 전 세션 생성 (미인증 임시 세션)
        MockHttpSession oldSession = (MockHttpSession) mockMvc.perform(get("/api/auth/me"))
                .andReturn().getRequest().getSession(true);

        assertThat(oldSession).isNotNull();
        String oldSessionId = oldSession.getId();

        // 2. 로그인 수행 (이전 세션을 들고 로그인)
        LoginRequest loginRequest = new LoginRequest("hoon@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .session(oldSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 3. 로그인 후 재발급된 새로운 세션 ID 비교 검증
        MockHttpSession newSession = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(newSession).isNotNull();
        assertThat(newSession.getId()).isNotEqualTo(oldSessionId);
    }
}