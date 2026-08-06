package com.dsa.schedule_manager.auth.service;

import com.dsa.schedule_manager.auth.dto.LoginRequest;
import com.dsa.schedule_manager.auth.dto.SignupRequest;
import com.dsa.schedule_manager.auth.dto.UserResponse;
import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.domain.UserRole;
import com.dsa.schedule_manager.user.domain.UserStatus;
import com.dsa.schedule_manager.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

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
    @Mock
    SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @InjectMocks AuthService sut;

    @Test
    void signup() {
        //given
        given(userRepository.existsByEmail("hoon@example.com")).willReturn(true);
        SignupRequest req = new SignupRequest("hoon@example.com", "password123", "동훈");

        // when & then
        assertThatThrownBy(() -> sut.signup(req))
                .isInstanceOf(BusinessException.class);
        then(userRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void signupMapsConcurrentUniqueConstraintViolationToEmailAlreadyUsed() {
        SignupRequest request =
                new SignupRequest("hoon@example.com", "password123", "동훈");
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encoded-password");
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("duplicate email"));

        assertThatThrownBy(() -> sut.signup(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.EMAIL_ALREADY_USED));
    }

    @Test
    void loginAppliesSessionFixationProtectionBeforeSavingContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = mock(Authentication.class);
        UserPrincipal principal = new UserPrincipal(
                1L,
                "hoon@example.com",
                null,
                "동훈",
                UserRole.USER,
                UserStatus.ACTIVE
        );
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(principal);

        UserResponse result = sut.login(
                new LoginRequest("hoon@example.com", "password123"),
                request,
                response
        );

        assertThat(result.id()).isEqualTo(1L);
        InOrder order = inOrder(sessionAuthenticationStrategy, securityContextRepository);
        order.verify(sessionAuthenticationStrategy)
                .onAuthentication(authentication, request, response);
        order.verify(securityContextRepository)
                .saveContext(any(), org.mockito.ArgumentMatchers.eq(request),
                        org.mockito.ArgumentMatchers.eq(response));
    }
}
