package com.dsa.schedule_manager.auth.service;

import com.dsa.schedule_manager.auth.dto.SignupRequest;
import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    SecurityContextRepository securityContextRepository;

    @InjectMocks AuthService sut;

    @Test
    void signup() {
        //given
        given(userRepository.existsByEmail("hoon@example.com")).willReturn(true);
        SignupRequest req = new SignupRequest("hoon@example.com", "password123", "동훈");

        // when & then
        assertThatThrownBy(() -> sut.signup(req))
                .isInstanceOf(BusinessException.class);
        then(userRepository).should(never()).save(any());
    }
}
